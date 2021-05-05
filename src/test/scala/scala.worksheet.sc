import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.*
import akka.pattern.StatusReply
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.persistence.testkit.{PersistenceTestKitPlugin, PersistenceTestKitSnapshotPlugin}
import akka.persistence.testkit.scaladsl.{EventSourcedBehaviorTestKit, PersistenceTestKit, SnapshotTestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import zicoin.blockchain.*
import zicoin.common.*
import zicoin.proof.Proof

import scala.concurrent.duration.*
import scala.language.postfixOps

val config = PersistenceTestKitSnapshotPlugin.config.withFallback(EventSourcedBehaviorTestKit.config)
implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "blockchain-test", config)

val testKit = ActorTestKit(system)