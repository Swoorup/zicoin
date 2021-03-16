package zicoin
package actor

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.*
import akka.pattern.StatusReply
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.persistence.testkit.scaladsl.{EventSourcedBehaviorTestKit, PersistenceTestKit, SnapshotTestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import exception.*
import blockchain.*
import proof.Proof

import scala.concurrent.duration.*
import scala.language.postfixOps

class BlockchainSuite extends munit.FunSuite:
  val config = PersistenceTestKitSnapshotPlugin.config.withFallback(EventSourcedBehaviorTestKit.config)
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "blockchain-test", config)
  
  val testKit = ActorTestKit(system)

  override def afterAll(): Unit = testKit.shutdownTestKit()
  
  val txnDate: Long = 1615870157483 // 16/03/2021 - 15:49
  
  test("Correctly initiate an empty Chain") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, NodeId("test")))
    val probe = testKit.createTestProbe[Chain|Int|Hash]()

    blockchain ! Blockchain.GetChain(probe.ref)
    probe.expectMessage(1000 millis, EmptyChain)

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, 0)

    blockchain ! Blockchain.GetLastHash(probe.ref)
    probe.expectMessage(1000 millis, "1")
  }
  
  test("correctly add a new block") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, NodeId("test")))
    val probe = testKit.createTestProbe[Chain|Int|Hash]()

    // a sends 1L unit to b
    val transactions = List(Transaction("a", "b", 2L))
    val proof = Proof(1L)
    blockchain ! Blockchain.AddBlockCommand(transactions, proof, txnDate, probe.ref)
    probe.expectMessage(1000 millis, 1)

    blockchain ! Blockchain.GetChain(probe.ref)
    probe.expectMessageType[ChainLink](1000 millis)

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, 1)

    blockchain ! Blockchain.GetLastHash(probe.ref)
    probe.expectMessage(1000 millis, "939baab997cfed9830d9dec5ca27a73461e73831dca7770e2d9c68eaced25f70")
  }
  
  test("correctly recover from a snapshot") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, NodeId("test")))
    val probe = testKit.createTestProbe[Chain|Int|Hash]()

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, 1)

    blockchain ! Blockchain.GetLastHash(probe.ref)
    probe.expectMessage(1000 millis, "939baab997cfed9830d9dec5ca27a73461e73831dca7770e2d9c68eaced25f70")

    blockchain ! Blockchain.GetChain(probe.ref)
    val chainlink = probe.expectMessageType[ChainLink](1000 millis)
    assertEquals(chainlink.values.head.sender, "a")
  }
