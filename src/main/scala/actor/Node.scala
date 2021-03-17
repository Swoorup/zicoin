package zicoin
package actor

import blockchain.*
import proof.*
import common.{Timestamp, Hash}
import akka.{Done}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.Publish
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import akka.actor.typed.scaladsl.AskPattern.*
import akka.pattern.StatusReply
import akka.util.Timeout
import cats.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration.*

object Node:
  enum PeerCommand:
    case TransactionMessage(transaction: Transaction, nodeId: NodeId)
    case AddBlockMessage(proof: Proof, transactions: List[Transaction], timestamp: Timestamp, replyTo: ActorRef[StatusReply[Done]])
  export PeerCommand.*
  
  enum SelfCommand: 
    case CheckPowSolution(solution: Proof, replyTo: ActorRef[StatusReply[Done]])
  export SelfCommand.* 
  
  enum Command: 
    /// add transaction and get the block index which will ingest this transaction
    case AddTransaction(transaction: Transaction, replyTo: ActorRef[StatusReply[BlockIndex]])
    case Mine(replyTo: ActorRef[StatusReply[Done]])
    case GetTransactions(replyTo: ActorRef[List[Transaction]])
    case GetStatus(replyTo: ActorRef[Chain])
    case GetLastBlockIndex(replyTo: ActorRef[BlockIndex])
    case GetLastBlockHash(replyTo: ActorRef[Hash])
  export Command.*
  
  type NodeMessage = PeerCommand | SelfCommand | Command

  def apply(nodeId: NodeId, mediator: ActorRef[Topic.Command[PeerCommand]]): Behavior[Command] = 
    Behaviors.setup[NodeMessage]{ ctx => 
      new Node(nodeId, mediator, ctx).ready()
    }.narrow

  def createCoinbaseTransaction(nodeId: NodeId) = Transaction(Address.CoinBase, nodeId.address, 100)

class Node private (nodeId: NodeId, 
                    mediator: ActorRef[Topic.Command[Node.PeerCommand]],
                    context: ActorContext[Node.NodeMessage]): 
  import Node.*

  implicit lazy val timeout: Timeout = Timeout(5.seconds)
  implicit val system: ActorSystem[_] = context.system
  val ignoreRef = system.ignoreRef
  val log = context.log
  
  val broker = context.spawn(Broker(), "broker")
  val miner = context.spawn(Miner(), "miner")
  val blockchain = context.spawn(Blockchain(EmptyChain, nodeId), "blockchain")
  
  def ready(): Behavior[NodeMessage] = Behaviors.receiveMessage{

    case PeerCommand.TransactionMessage(transaction, messageNodeId) => 
      log.info(s"Received transaction message from $messageNodeId")
      if (messageNodeId != nodeId) broker ! Broker.AddTransaction(transaction)
      Behaviors.same

    case PeerCommand.AddBlockMessage(proof, transactions, ts, replyTo) => 
      context.self.askWithStatus[Done]{ CheckPowSolution(proof, _) } onComplete {
        case Failure(e) => replyTo ! StatusReply.error(e)
        case Success(_) =>
          broker ! Broker.DiffTransaction(transactions)
          blockchain ! Blockchain.AddBlockCommand(transactions, proof, ts, ignoreRef)
          replyTo ! StatusReply.ack()
      }
      Behaviors.same
        
    case Command.AddTransaction(transaction, replyTo) => 
      blockchain.ask[BlockIndex]{Blockchain.GetLastIndex(_)} onComplete {
        case Success(blockIndex) => 
          broker ! Broker.AddTransaction(transaction)
          mediator ! Publish(TransactionMessage(transaction, nodeId))
          replyTo ! StatusReply.success(blockIndex.increment())
        case Failure(e) => replyTo ! StatusReply.error(e)
      }
      Behaviors.same

    case Command.Mine(replyTo) =>
      val result: Future[Unit] = 
        for 
          lastHash <- blockchain.ask[Hash]{ Blockchain.GetLastHash(_) } 
          proof <- miner.askWithStatus[Proof]{ Miner.Mine(lastHash, _) }
          ts <- Monad[Future].pure(System.currentTimeMillis())
          currentTransactions <- 
            broker ! Broker.AddTransaction(createCoinbaseTransaction(nodeId))
            broker.ask[List[Transaction]]{ Broker.GetTransactions(_) }
        yield 
          mediator ! Publish(AddBlockMessage(proof, currentTransactions, ts, ignoreRef))

      result onComplete { 
        case Success(_) => replyTo ! StatusReply.ack()
        case Failure(err) => 
          log.error(s"Error mining solution: ${err.getMessage}")
          replyTo ! StatusReply.error(err)
      }
      
      Behaviors.same

    case SelfCommand.CheckPowSolution(solution, replyTo) => 
      blockchain.ask[Hash]{ Blockchain.GetLastHash(_) } onComplete {
        case Success(hash) => miner ! Miner.Validate(hash, solution, replyTo)
        case Failure(err) => replyTo ! StatusReply.error(err)
      }
      Behaviors.same
      
    case Command.GetTransactions(replyTo: ActorRef[List[Transaction]]) => 
      broker ! Broker.GetTransactions(replyTo)
      Behaviors.same

    case Command.GetStatus(replyTo) => 
      blockchain ! Blockchain.GetChain(replyTo)
      Behaviors.same
      
    case Command.GetLastBlockIndex(replyTo) => 
      blockchain ! Blockchain.GetLastIndex(replyTo)
      Behaviors.same
      
    case Command.GetLastBlockHash(replyTo) => 
      blockchain ! Blockchain.GetLastHash(replyTo)
      Behaviors.same
  }