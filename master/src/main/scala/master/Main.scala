package master

import java.util.concurrent.CountDownLatch
import java.io.IOException

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.{enrichAny}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.io.StdIn

case class ReplicatedMessage(id: Int, data: String)
case class InputMessage(text: String)
case class InputMessageWithWriteConcern(text: String, writeConcern: Int)
case class AddMessageEvent(message: ReplicatedMessage, eventType: String = "add-message")
case class GetMessagesEvent(eventType: String = "get-messages")
case class EventOutputWithMessages(eventType: String, data: List[ReplicatedMessage])
case class HealthStatus(Secondary0: String, Secondary2: String)

object Main {
  // needed to run the route
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  // formats for unmarshalling and marshalling
  implicit val inputMessageFormat = jsonFormat1(InputMessage)
  implicit val inputMessageWithWriteConcernFormat = jsonFormat2(InputMessageWithWriteConcern)
  implicit val replicationMessageFormat = jsonFormat2(ReplicatedMessage)
  implicit val addMessageEventFormat = jsonFormat2(AddMessageEvent)
  implicit val getMessagesEventFormat = jsonFormat1(GetMessagesEvent)
  implicit val listMessagesEventFormat = jsonFormat2(EventOutputWithMessages)
  implicit val healthStatusFormat = jsonFormat2(HealthStatus)

  var secondariesSocketsPorts: List[Int] = List()
  var lastMessageId = 0

  var messages: List[ReplicatedMessage] = List()
  var DEFAULT_WRITE_CONCERN = 1;
  var messagesToWrite: Map[Int, List[String]] = Map()
  var healthStatus = mutable.Map(("Secondary" -> "Healthy"), ("Secondary2" -> "Healthy"))

  def main(args: Array[String]) {
    val httpPort = args(0).toInt

    val secondaryPort = args(1).toInt
    val secondaryPort2 = args(2).toInt
    val inDocker = args.lift(3)

    if (inDocker.isDefined && inDocker.get == "-docker") {
      SocketClient.init(true)
    }

    secondariesSocketsPorts = secondaryPort :: secondaryPort2 :: secondariesSocketsPorts

    DEFAULT_WRITE_CONCERN = secondariesSocketsPorts.length + 1

    HealthCheck.init(secondaryPort, "Secondary", healthStatus)
    HealthCheck.init(secondaryPort2, "Secondary2", healthStatus)

    initServer(httpPort)
  }

  def initServer(httpPort: Int): Unit = {
    val route: Route =
      get {
        pathSingleSlash {
          complete(messages)
        }
      } ~
      get {
        path("health") {
          complete(HealthStatus(healthStatus.get("Secondary").get, healthStatus.get("Secondary2").get))
        }
      } ~
      post {
        pathSingleSlash {
          entity(as[InputMessage]) { message =>
            onSuccess(addMessage(message.text, DEFAULT_WRITE_CONCERN)) { (responses) => {
              complete("Success")
            }
            }
          }
        }
      }  ~ post {
        path("writeConcern") {
          entity(as[InputMessageWithWriteConcern]) { message =>
            onSuccess(addMessage(message.text, message.writeConcern)) { (responses) => {
              complete("Success")
            }
            }
          }
        }
      } ~ post {
        path("writeConcernQuorum") {
          entity(as[InputMessageWithWriteConcern]) { message => {
            if (haveQuorum(message.writeConcern - 1)) {
              onSuccess(addMessage(message.text, message.writeConcern)) { (responses) => {
                complete("Success")
              }
              }
            } else {
              complete("Can't add message, no quorum")
            }
          }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", httpPort)
    println(s"Server online at http://localhost:${httpPort}/\nPress RETURN to stop...")
    StdIn.readLine()

    // this is not working in docker, docker run only with -i and without -d
    //    bindingFuture
    //      .flatMap(_.unbind()) // trigger unbinding from the port
    //      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }

  def haveQuorum(quorum: Int): Boolean = {
    val healthyNodes: mutable.Map[String, String] = healthStatus.clone.filter((t) => t._2 == "Healthy")

    println(healthyNodes.size)

    healthyNodes.size >= quorum
  }

  def addMessage(message: String, writeConcern: Int): Future[String] = {
    lastMessageId += 1

    val newMessage = ReplicatedMessage(lastMessageId, message)

    messages = newMessage :: messages

    val countDownLatch = new CountDownLatch(writeConcern - 1)

    val replicatedMessage = AddMessageEvent(newMessage).toJson.toString()

    secondariesSocketsPorts
      .map((port) => SocketClient.sendMessage(port, replicatedMessage, countDownLatch)
        .recoverWith({
          case e: IOException => retryMessage(port, replicatedMessage, countDownLatch)
        }))

    countDownLatch.await()

    Future.successful("Success")
  }

  def retryMessage(port: Int, message: String, writeConcernCountDownLatch: CountDownLatch): Future[String] = {
      var res = ""
      val localCountDownLatch = new CountDownLatch(1)

      val cancellable = system.scheduler.scheduleWithFixedDelay(0.seconds, 10.seconds) {
        case class MyRunnable() extends Runnable {
          def run(): Unit = {
            res = try {
              SocketClient.sendMessageNoFuture(port, message, writeConcernCountDownLatch)
            } catch {
              case e: IOException =>
                println(s"no connection for ${port} will retry in 10 seconds")
                res
            }

            // if we have response
            if (res != "") {
              localCountDownLatch.countDown()
            }
          }
        }

        MyRunnable()
      }

    localCountDownLatch.await()

    cancellable.cancel()

    Future(res)
  }

}