package com.evdokimovn

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.routing.{Broadcast, FromConfig}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.evdokimovn.actors.MeasurementsWriter.{Click, Impression}
import com.evdokimovn.actors.{MeasurementsWriter, StatisticsReader, Supervisor}
import com.paulgoldbaum.influxdbclient.InfluxDB
import sun.misc.{Signal, SignalHandler}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object WebServer {

  def main(args: Array[String]) {

    implicit val system = ActorSystem("backend-actors")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    val influxdb = InfluxDB.connect("localhost", 8086).selectDatabase("analytics")

    val writer = system.actorOf(MeasurementsWriter.props(influxdb).withRouter(FromConfig()), name = "writerActor")
    val reader = system.actorOf(StatisticsReader.props(influxdb).withRouter(FromConfig()), name = "readerActor")
    val supervisor = system.actorOf(Props(classOf[Supervisor], writer, system))

    val route =
      path("analytics") {
        get {
          parameters('timestamp.as[Long]) { timestamp =>
            onComplete((reader ask StatisticsReader.AggregateStatistics(timestamp)).mapTo[Future[StatisticsReader.Statistics]].flatten) {
              case Success(value) => complete {
                s"""unique_users,${value.users}
clicks,${value.clicks}
impressions,${value.impressions}"""
              }
              case Failure(ex) => complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
            }
          }
        } ~
          post {
            parameters('timestamp.as[Long], 'user.as[String], 'click ?) { (t, user, click) =>
              complete {
                val message = click match {
                  case None => MeasurementsWriter.Metric(timestamp = t, user = user, action = Impression)
                  case Some(_) => MeasurementsWriter.Metric(timestamp = t, user = user, action = Click)
                }
                writer ! MeasurementsWriter.Persist(message)
                StatusCodes.Created
              }
            }
          }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/.")

    Signal.handle(new Signal("INT"), new SignalHandler() {
      def handle(sig: Signal) {
        bindingFuture
          .flatMap(_.unbind())
          .flatMap(_ => Future.successful(writer ! Broadcast(PoisonPill)))
          // System will be gracefully shutdown by supervisor
          // actor when all writer actors will finish processing its messages
          .flatMap(_ => system.whenTerminated)
          .onComplete(_ => System.exit(0))

      }
    })
  }
}