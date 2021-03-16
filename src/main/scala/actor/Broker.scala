package zicoin
package actor

import akka.NotUsed
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, Behavior }
import blockchain.*

object Broker:
  enum Command:
    case AddTransaction(txn: Transaction)
    case GetTransaction(replyTo: ActorRef[List[Transaction]])
    case Clear()
  export Command.*

  def apply(): Behavior[Command] = Behaviors.setup[Command]{ context =>
    val log = context.log
    log.info(s"Creation of broker")
    var pending: List[Transaction] = List.empty

    Behaviors.receiveMessage {
      case AddTransaction(txn) =>
        pending = txn :: pending 
        log.info(s"Added $txn to pending transactions")
        Behaviors.same
      case GetTransaction(replyTo) =>
        log.info(s"Getting pending transactions")
        replyTo ! pending
        Behaviors.same
      case Clear() =>
        pending = List()
        log.info("Clear pending transaction List")
        Behaviors.same
    }
  }
  .narrow
