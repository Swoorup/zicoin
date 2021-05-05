package zicoin
package actor

import akka.actor.typed.pubsub.Topic
import akka.actor.typed.pubsub.Topic.Publish
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.persistence.testkit.scaladsl.{EventSourcedBehaviorTestKit, PersistenceTestKit, SnapshotTestKit}
import akka.pattern.StatusReply
import akka.util.Timeout
import scala.concurrent.duration.*
import common.*
import blockchain.*

class NodeSuite extends munit.FunSuite:
  val config = PersistenceTestKitSnapshotPlugin.config.withFallback(EventSourcedBehaviorTestKit.config)
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "blockchain-test", config)
  implicit lazy val timeout: Timeout = Timeout(5.seconds)
  
  val testKit = ActorTestKit(system)

  override def afterAll(): Unit = testKit.shutdownTestKit()

  val testNode = NodeId(Address("test-node"))
  val transaction = Transaction(Address("a"), Address("b"), 1L)

  test("it works") {

    val testProbe = testKit.createTestProbe[BlockIndex|StatusReply[BlockIndex]]()
    val mediator = testKit.createTestProbe[Topic.Command[Node.PeerCommand]]()
    val node = testKit.spawn(Node(testNode, mediator.ref))

    node ! Node.GetLastBlockIndex(testProbe.ref)
    testProbe.expectMessage(BlockIndex(0))

    node ! Node.AddTransaction(transaction, testProbe.ref)
    testProbe.expectMessage(StatusReply.Success(BlockIndex(1)))
    node ! Node.AddTransaction(transaction, testProbe.ref)
    testProbe.expectMessage(StatusReply.Success(BlockIndex(1)))
    node ! Node.AddTransaction(transaction, testProbe.ref)
    testProbe.expectMessage(StatusReply.Success(BlockIndex(1)))
    node ! Node.AddTransaction(transaction, testProbe.ref)
    testProbe.expectMessage(StatusReply.Success(BlockIndex(1)))
  }