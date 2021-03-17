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
import common.*
import proof.Proof

import scala.concurrent.duration.*
import scala.language.postfixOps

class BlockchainSuite extends munit.FunSuite:
  val config = PersistenceTestKitSnapshotPlugin.config.withFallback(EventSourcedBehaviorTestKit.config)
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "blockchain-test", config)
  
  val testKit = ActorTestKit(system)

  override def afterAll(): Unit = testKit.shutdownTestKit()
  
  val testNode = NodeId(Address("test-node"))
  val txnDate: Timestamp = 1615870157483L // 16/03/2021 - 15:49
  
  test("Correctly initiate an empty Chain") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, testNode))
    val probe = testKit.createTestProbe[Chain|BlockIndex|Hash]()

    blockchain ! Blockchain.GetChain(probe.ref)
    probe.expectMessage(1000 millis, EmptyChain)

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, BlockIndex(0))

    blockchain ! Blockchain.GetLastHash(probe.ref)
    probe.expectMessage(1000 millis, Hash("1"))
  }
  
  test("correctly add a new block") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, testNode))
    val probe = testKit.createTestProbe[Chain|BlockIndex|Hash]()

    // a sends 1L unit to b
    val transactions = List(Transaction(Address("a"), Address("b"), 1L))
    val proof = Proof(1L)
    blockchain ! Blockchain.AddBlockCommand(transactions, proof, txnDate, probe.ref)
    probe.expectMessage(1000 millis, BlockIndex(1))

    blockchain ! Blockchain.GetChain(probe.ref)
    probe.expectMessageType[ChainLink](1000 millis)

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, BlockIndex(1))

    blockchain ! Blockchain.GetLastHash(probe.ref)
    probe.expectMessage(1000 millis, Hash("9cd156b1af8e8ff6848dd2187a940a079d85da62edc301e0c93bd0582a1fecf3"))
  }
  
  test("correctly recover from a snapshot") {
    val blockchain = testKit.spawn(Blockchain(EmptyChain, testNode))
    val probe = testKit.createTestProbe[Chain|BlockIndex|Hash]()

    blockchain ! Blockchain.GetLastIndex(probe.ref)
    probe.expectMessage(1000 millis, BlockIndex(1))

    blockchain ! Blockchain.GetLastHash(probe.ref)
    probe.expectMessage(1000 millis, Hash("9cd156b1af8e8ff6848dd2187a940a079d85da62edc301e0c93bd0582a1fecf3"))

    blockchain ! Blockchain.GetChain(probe.ref)
    val chainlink = probe.expectMessageType[ChainLink](1000 millis)
    assertEquals(chainlink.values.head.sender, Address("a"))
  }
