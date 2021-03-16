package zicoin
package actor

import akka.actor.typed.ActorRef
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.*
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.util.Timeout
import exception.*
import blockchain.*
import proof.Proof
import blockchain.EmptyChain
import scala.concurrent.duration.*
import scala.language.postfixOps

class BlockchainSuite extends munit.FunSuite:
  val testKit = ActorTestKit(EventSourcedBehaviorTestKit.config)

  test("Correctly add a new block") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, NodeId("test")))
    val probe = testKit.createTestProbe[Chain|Int|Hash]()
    
    blockchain ! Blockchain.GetChain(probe.ref)
    probe.expectMessage(1000 millis, EmptyChain)

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, 0)
    
    blockchain ! Blockchain.GetLastHash(probe.ref)
  }

  test("Should be busy while mining a new block") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, NodeId("test")))
    val probe1 = testKit.createTestProbe[StatusReply[Proof]]()
    val probe2 = testKit.createTestProbe[StatusReply[Proof]]()
  }