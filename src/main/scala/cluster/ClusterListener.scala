package zicoin.cluster

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.*
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe
import zicoin.actor.NodeId

object ClusterListener {

  // internal adapted cluster events only
  enum Event:
    case ReachabilityChange(reachabilityEvent: ReachabilityEvent) 
    case MemberChange(event: MemberEvent) 

  def apply(nodeId: NodeId, cluster: Cluster): Behavior[Event] = Behaviors.setup { ctx =>
    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(Event.MemberChange.apply)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent])

    val reachabilityAdapter = ctx.messageAdapter(Event.ReachabilityChange.apply)
    cluster.subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])

    Behaviors.receiveMessage { message =>
      message match {
        case Event.ReachabilityChange(reachabilityEvent) =>
          reachabilityEvent match {
            case UnreachableMember(member) =>
              ctx.log.info(s"Node ${nodeId} - Member detected as unreachable: ${member}")
            case ReachableMember(member) =>
              ctx.log.info(s"Node ${nodeId} - Member back to reachable: ${member}")
          }

        case Event.MemberChange(changeEvent) =>
          changeEvent match {
            case MemberUp(member) =>
              ctx.log.info(s"Node ${nodeId} - Member is Up: ${member.address}")
            case MemberRemoved(member, previousStatus) =>
              ctx.log.info(s"Node ${nodeId} - Member is Removed: ${member.address} after ${previousStatus}")
            case _: MemberEvent => // ignore
          }
      }
      Behaviors.same
    }
  }
}