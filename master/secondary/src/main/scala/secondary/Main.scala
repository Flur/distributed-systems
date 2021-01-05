package secondary

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, JsonParser, enrichAny}

case class ReplicatedMessage(id: Int, data: String)
case class MasterEvent(eventType: String, message: Option[JsValue] = None)
case class EventOutput(eventType: String)

object Main {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val myMessageFormat = jsonFormat2(ReplicatedMessage)
  implicit val wsEventFormat = jsonFormat2(MasterEvent)
  implicit val wsEventOutputFormat = jsonFormat1(EventOutput)

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
          complete(getMessages)
        }
      }

    // could be run in Future?
    val th = new Thread(() => {
      // this is blocking server
      SocketServer.run(socketPort, receiveStringFromMaster)
    })

    th.start()

    // this is not working in docker, docker run only with -i and without -d
    val bindingFuture = Http().newServerAt("0.0.0.0", httpPort).bind(route)
    println(s"Server online at http://localhost:${httpPort}/\nPress RETURN to stop...")
//    StdIn.readLine() // let it run until user presses return
//    bindingFuture
//      .flatMap(_.unbind()) // trigger unbinding from the port
//      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def receiveStringFromMaster(data: String): String = {
    val event = JsonParser(data).convertTo[MasterEvent]

    if (event.eventType == "add-message") {
      val message = event.message.get.convertTo[ReplicatedMessage]

      if (shouldSleep) {
        // todo for first iteration
        Thread.sleep(20000)
      }

      deduplication(message)

      return EventOutput("ok").toJson.toString()
    }

    if (event.eventType == "health-check") {
      return EventOutput("ok").toJson.toString()
    }

    EventOutput("fail").toJson.toString()
  }

  def deduplication(message: ReplicatedMessage):Unit = {
    val m = messages.find((m) => m.id == message.id)

    if (m.isEmpty) {
      messages = message :: messages
    }

    println(message)
    println(m)
    println(messages)
  }

  def getMessages(): List[ReplicatedMessage] = {
    var filteredMessages: List[ReplicatedMessage] = List()
    var isAll = false

    for ((m, i) <- messages.zipWithIndex) {
      val nextMessage = messages.lift(i + 1)

      if (!isAll) {
        filteredMessages = m :: filteredMessages
      }

      if (nextMessage.isEmpty || nextMessage.get.id != m.id + 1) {
        isAll = true
      }
    }

    println(messages)
    println(filteredMessages)

    filteredMessages
  }
}
