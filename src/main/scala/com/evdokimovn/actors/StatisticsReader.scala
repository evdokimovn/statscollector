package com.evdokimovn.actors

import java.text.SimpleDateFormat
import java.util.TimeZone

import akka.actor.{Actor, Props}
import com.paulgoldbaum.influxdbclient.Database

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StatisticsReader(database: Database) extends Actor {

  import StatisticsReader._

  private val timestampFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  timestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))

  override def receive: Receive = {
    case AggregateStatistics(timestamp) => {
      val timestampToHour = timestampFormatter.format(timestamp - (timestamp % 3600000))
      val sumQuery =
        s"""select sum(click) as clicks, sum(impression) as impressions from analytics where time >= '${timestampToHour}' and time < '${timestampToHour}' + 1h"""

      val sumRequestFuture = database.query(sumQuery)
      val usersRequestFuture = database.query("select count(distinct(\"user\")) as \"users\" from analytics")


     val stats: Future[Statistics] = for {actionsStats <- sumRequestFuture; usersCount <- usersRequestFuture} yield {
        actionsStats.series match {
          case (h :: _) => {
            val records = h.records(0)
            val users = usersCount.series.head.records(0)("users")
            Statistics(clicks = records("clicks").asInstanceOf[BigDecimal].toLong,
              impressions = records("impressions").asInstanceOf[BigDecimal].toLong, users = users.asInstanceOf[BigDecimal].toLong)
          }
          case _ => Statistics()
        }
      }

      sender() ! stats
    }
    case _ => ()
  }
}


object StatisticsReader {

  def props(connection: Database): Props =
    Props(classOf[StatisticsReader], connection)

  sealed trait ReaderMessage

  final case class AggregateStatistics(timestamp: Long) extends ReaderMessage

  final case class Statistics(clicks: Long = 0, impressions: Long = 0, users: Long = 0)

}