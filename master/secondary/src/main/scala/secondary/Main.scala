package secondary

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, JsonParser, enrichAny}

import scala.io.StdIn

case class ReplicatedMessage(id: Int, data: String)
case class MasterEvent(eventType: String, message: Option[JsValue] = None)
case class EventOutputWithMessages(eventType: String, data: List[ReplicatedMessage])
case class EventOutput(eventType: String)

object Main {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val myMessageFormat = jsonFormat2(ReplicatedMessage)
  implicit val wsEventFormat = jsonFormat2(MasterEvent)
  implicit val wsEventOutputFormat = jsonFormat1(EventOutput)
  implicit val wsEventOutputWithMessagesFormat = jsonFormat2(EventOutputWithMessages)

  implicit var messages: List[ReplicatedMessage] = List()
  var shouldSleep = false

  def main(args: Array[String]) {
    val httpPort = args(0).toInt
    val socketPort = args(1).toInt

    val sleep = args.lift(2)

    if (sleep.isDefined && sleep.get == "-sleep") {
      shouldSleep = true
    }



    val route: Route =
      get {
        pathSingleSlash {
          complete(messages)
        }
      }

    // could be run in Future?
    val th = new Thread(() => {
      // this is blocking server
      SocketServer.run(socketPort, receiveStringFromMaster)
    })

    th.start()

    val bindingFuture = Http().newServerAt("localhost", httpPort).bind(route)
    println(s"Server online at http://localhost:${httpPort}/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def receiveStringFromMaster(data: String): String = {
    val event = JsonParser(data).convertTo[MasterEvent]

    if (event.eventType == "add-message") {
      val message = event.message.get.convertTo[ReplicatedMessage]

      messages = message :: messages

      if (shouldSleep) {
        // todo for first iteration
        Thread.sleep(5000)
      }


      return EventOutput("ok").toJson.toString()
    }

    println("d", event)

    if (event.eventType == "get-messages") {
      return EventOutputWithMessages("ok", messages).toJson.toString()
    }

    EventOutput("fail").toJson.toString()
  }
}
