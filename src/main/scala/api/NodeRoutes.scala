package com.elleflorio.scalachain.api

import zicoin.actor.Node
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.actor.typed.scaladsl.AskPattern.*
import akka.util.Timeout
import zicoin.blockchain.{Chain, Transaction, given}
import zicoin.cluster.ClusterManager
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*

import scala.concurrent.Future
import scala.concurrent.duration.*

trait NodeRoutes:

  implicit def system: ActorSystem[?]

  def node: ActorRef[Node.Command]
  def clusterManager: ActorRef[ClusterManager.ClusterMessage]

  given timeout: Timeout = Timeout(5.seconds)

  lazy val statusRoutes: Route = pathPrefix("status") {
    concat(
      pathEnd {
        concat(
          get {
            val statusFuture: Future[Chain] = node.ask{ Node.GetStatus(_) }
            onSuccess(statusFuture) { status =>
              complete(StatusCodes.OK, status)
            }
          }
        )
      },
      pathPrefix("members") {
        concat(
          pathEnd {
            concat(
              get {
                val membersFuture: Future[List[String]] = clusterManager.ask { ClusterManager.GetMembers(_) }
                onSuccess(membersFuture) { members =>
                  complete(StatusCodes.OK, members)
                }
              }
            )
          }
        )
      }
    )
  }

//   lazy val transactionRoutes: Route = pathPrefix("transactions") {
//     concat(
//       pathEnd {
//         concat(
//           get {
//             val transactionsRetrieved: Future[List[Transaction]] =
//               (node ? GetTransactions).mapTo[List[Transaction]]
//             onSuccess(transactionsRetrieved) { transactions =>
//               complete(transactions.toList)
//             }
//           },
//           post {
//             entity(as[Transaction]) { transaction =>
//               val transactionCreated: Future[Int] =
//                 (node ? AddTransaction(transaction)).mapTo[Int]
//               onSuccess(transactionCreated) { done =>
//                 complete((StatusCodes.Created, done.toString))
//               }
//             }
//           }
//         )
//       }
//     )
//   }

//   lazy val mineRoutes: Route = pathPrefix("mine") {
//     concat(
//       pathEnd {
//         concat(
//           get {
//             node ! Mine
//             complete(StatusCodes.OK)
//           }
//         )
//       }
//     )
//   }