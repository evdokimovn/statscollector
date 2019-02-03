package com.evdokimovn.actors

import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, Props}
import com.paulgoldbaum.influxdbclient.Parameter.Precision
import com.paulgoldbaum.influxdbclient.{Database, Point}

class MeasurementsWriter(connection: Database) extends Actor {

  import MeasurementsWriter._

  override def receive: Receive = {
    case Persist(metric) => {
      val point = Point("analytics", metric.timestamp).addField("user", metric.user)
      val future = connection.write(
        precision = Precision.MILLISECONDS,
        point = metric.action match {
          case Click => point.addField("click", 1).addField("impression", 0)
          case Impression => point.addField("impression", 1).addField("click", 0)
        }
      )
      future pipeTo(self)
    }
    case _: Boolean => () //send result of writing to database
    case _ => ()
  }
}


object MeasurementsWriter {

  def props(connection: Database): Props =
    Props(classOf[MeasurementsWriter], connection)

  sealed trait WriterMessage

  final case class Metric(timestamp: Long, user: String, action: Action)

  sealed trait Action

  final case object Impression extends Action

  final case object Click extends Action

  final case class Persist(data: Metric) extends WriterMessage

}