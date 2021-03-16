package zicoin
package actor

import akka.actor.typed.ActorRef
import akka.actor.testkit.typed.scaladsl.{ActorTestKit}
import akka.actor.typed.scaladsl.AskPattern._
import akka.pattern.StatusReply
import akka.util.Timeout
import exception.*
import proof.Proof
import common.*

class MinerSuite extends munit.FunSuite:
  val testKit = ActorTestKit()
  
  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("Should be ready when requested") {
    val miner = testKit.spawn(Miner())
    val probe = testKit.createTestProbe[StatusReply[Proof]]()

    /// minor should be ready
    miner ! Miner.Mine(Hash("1"), probe.ref)
    probe.expectMessage(StatusReply.success(Proof(7178)))
  }

  test("Should be busy while mining a new block") {
    val miner = testKit.spawn(Miner())
    val probe1 = testKit.createTestProbe[StatusReply[Proof]]()
    val probe2 = testKit.createTestProbe[StatusReply[Proof]]()

    /// minor should be ready
    miner ! Miner.Mine(Hash("1"), probe1.ref)

    /// minor should now be busy
    miner ! Miner.Mine(Hash("1"), probe2.ref)
    probe2.expectMessage(StatusReply.error(MinerBusyException("Miner is busy")))
  }