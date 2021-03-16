package zicoin
package actor

import common.{Timestamp, Hash}
import blockchain.*
import proof.*
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

opaque type NodeId = String
object NodeId:
  def apply(a: String): NodeId = a
  extension (a: NodeId) def persistenceId: String = s"chainer-$a"

object Blockchain:
  case class State(chain: Chain)

  enum BlockchainEvent: 
    case BlockAddedEvent(transactions: List[Transaction], proof: Proof, timestamp: Timestamp)

  enum BlockchainCommand:
    case AddBlockCommand(transactions: List[Transaction], proof: Proof, timestamp: Timestamp, replyTo: ActorRef[BlockIndex]) 
    case GetChain(replyTo: ActorRef[Chain])
    case GetLastHash(replyTo: ActorRef[Hash])
    case GetLastIndex(replyTo: ActorRef[BlockIndex])
  
  export BlockchainCommand.*, BlockchainEvent.*

  def apply(chain: Chain, nodeId: NodeId): Behavior[BlockchainCommand] =
    Behaviors.setup{ ctx => 
      new Blockchain(chain, nodeId, ctx).run()
    }

class Blockchain private (chain: Chain, nodeId: NodeId, actorContext: ActorContext[Blockchain.BlockchainCommand]): 
  import Blockchain.*
  val log = actorContext.log

  def commandHandler(state: State, command: BlockchainCommand): Effect[BlockchainEvent, State] = 
    command match 
      case AddBlockCommand(transactions, proof, timestamp, replyTo) => 
        Effect.persist(BlockAddedEvent(transactions, proof, timestamp)).thenRun{ s =>
          log.info(s"Added block ${s.chain.index} containing ${transactions.size} transactions")
          replyTo ! s.chain.index
        }
      case GetChain(replyTo)  => 
        replyTo ! state.chain
        Effect.none
      case GetLastHash(replyTo) => 
        replyTo ! state.chain.hash
        Effect.none
      case GetLastIndex(replyTo) =>
        replyTo ! state.chain.index
        Effect.none
  
  def eventHandler(state: State, event: BlockchainEvent): State = 
    event match
      case BlockAddedEvent(transactions, proof, timestamp) =>
        State(ChainLink(state.chain.index.increment(), proof, transactions, timestamp = timestamp) :: state.chain)
  
  def run(): Behavior[BlockchainCommand] =
    EventSourcedBehavior[BlockchainCommand, BlockchainEvent, State](
      persistenceId = PersistenceId.ofUniqueId(nodeId.persistenceId),
      emptyState = State(chain),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).snapshotWhen{
      case (state, BlockAddedEvent(_,_,_), sequenceNumber) => true
    }

