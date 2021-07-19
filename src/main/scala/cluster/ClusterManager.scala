package zicoin.cluster

import akka.actor.typed.*
import akka.actor.typed.scaladsl.*
import zicoin.actor.NodeId
import akka.cluster.typed.Cluster
import akka.cluster.MemberStatus

object ClusterManager:
  enum ClusterMessage:
    case GetMembers(replyTo: ActorRef[List[String]])
  export ClusterMessage.*

  def apply(nodeId: NodeId): Behavior[ClusterMessage] = Behaviors.setup { ctx => 
    val cluster: Cluster = Cluster(ctx.system)
    val listener: ActorRef[ClusterListener.Event] = ctx.spawn(ClusterListener(nodeId, cluster), "clusterlistener")

    Behaviors.receiveMessage {
      case GetMembers(replyTo) => 
        replyTo ! cluster.state.members.filter(_.status == MemberStatus.up)
          .map(_.address.toString)
          .toList

        Behaviors.same
    }
  }