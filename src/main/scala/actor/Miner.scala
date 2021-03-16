package zicoin
package actor

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.pattern.StatusReply
import akka.{NotUsed, Done}
import blockchain.*
import common.Hash
import exception.{InvalidProofException, MinerBusyException}
import proof.{ProofOfWork, Proof}
import scala.concurrent.{Future, ExecutionContext}

object Miner: 
  enum Command:
    case Validate(hash: Hash, proof: Proof, replyTo: ActorRef[StatusReply[Done]]) 
    case Mine(hash: Hash, replyTo: ActorRef[StatusReply[Proof]]) 
  export Command.*

  enum PrivateCommand: 
    case Ready
  export PrivateCommand.*
  
  type MinerMessage = Command | PrivateCommand

  def apply(): Behavior[Command] = 
    Behaviors.setup[MinerMessage] { ctx => 
      new Miner(ctx).ready()
    }.narrow

class Miner private (context: ActorContext[Miner.MinerMessage]):
  import Miner.*
  private val log = context.log
  implicit val ec: ExecutionContext = context.executionContext

  // maybe replace this with context function
  private def validate(): PartialFunction[MinerMessage, Behavior[MinerMessage]] = 
    case Validate(hash, proof, replyTo) => 
      log.info(s"Validating proof $proof")
      if ProofOfWork.validProof(hash, proof) then
        log.info(s"proof is valid!")
        replyTo ! StatusReply.ack()
      else 
        log.info(s"proof is not valid!")
        replyTo ! StatusReply.error(new InvalidProofException(hash, proof))
      Behaviors.same

  def busy(): Behavior[MinerMessage] = Behaviors.receiveMessagePartial { 
    validate().orElse{
      case Mine(hash, replyTo) => 
        log.info(s"I'm already mining")
        replyTo ! StatusReply.error(new MinerBusyException("Miner is busy"))
        Behaviors.same
      case Ready => 
        log.info(s"I'm ready to mine")
        ready()
    }
  }

  def ready(): Behavior[MinerMessage] = Behaviors.receiveMessagePartial {
    validate().orElse{
      case Mine(hash, replyTo) => 
        log.info(s"Mining hash $hash...")
        Future { 
          ProofOfWork.proofOfWork(hash) 
        } foreach { proof => 
          /* TODO :
            Figure out if instead of directly sending to other actor 
            should we first send to ourself as described in 
            https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html

            I suppose this shouldn't really be a problem if only sending messages
          */
          replyTo ! StatusReply.success(proof) 
          context.self ! Ready
        }
        busy()
    }
  }