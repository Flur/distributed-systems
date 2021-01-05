package master

import java.io.IOException

import akka.actor.ActorSystem
import spray.json.DefaultJsonProtocol.jsonFormat1
import spray.json.enrichAny
import spray.json.DefaultJsonProtocol._

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class HealthCheckMessage(eventType: String = "health-check")

object HealthCheck {
  implicit val system = ActorSystem()

  implicit val inputMessageFormat = jsonFormat1(HealthCheckMessage)
  val healthCheckMessage = HealthCheckMessage().toJson.toString()

  def init(port: Int, secondaryKey: String, healthStatusMap: mutable.Map[String, String])(implicit executor: ExecutionContext): Unit = {

    val cancellable = system.scheduler.scheduleWithFixedDelay(0.seconds, 10.seconds) {
      case class MyHealthCheck() extends Runnable {
        def run(): Unit = {
          try {
            master.SocketClient.sendSimpleMessage(port, healthCheckMessage)

            healthStatusMap += (secondaryKey -> "Healthy")
            println(s"${secondaryKey} status is Healthy")
          } catch {
            case e: IOException => {
              val status = healthStatusMap.get(secondaryKey)
              if (status.get == "Suspected") {
                healthStatusMap += (secondaryKey -> "Unhealthy")
              } else if (status.get == "Healthy") {
                // todo add two times Suspected
                healthStatusMap += (secondaryKey -> "Suspected")
              }
              println(s"${secondaryKey} status is ${status.get}")
            }
          }
        }
      }

      MyHealthCheck()
    }
  }
}