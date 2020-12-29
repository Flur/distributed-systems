package master

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.{JsonParser, enrichAny}

import scala.concurrent.Future
import scala.io.StdIn

case class ReplicatedMessage(id: Int, data: String)
case class InputMessage(text: String)
case class AddMessageEvent(message: ReplicatedMessage, eventType: String = "add-message")
case class GetMessagesEvent(eventType: String = "get-messages")
case class EventOutputWithMessages(eventType: String, data: List[ReplicatedMessage])

object Main {
  // needed to run the route
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher


  // formats for unmarshalling and marshalling
  implicit val inputMessageFormat = jsonFormat1(InputMessage)
  implicit val replicationMessageFormat = jsonFormat2(ReplicatedMessage)
  implicit val addMessageEventFormat = jsonFormat2(AddMessageEvent)
  implicit val getMessagesEventFormat = jsonFormat1(GetMessagesEvent)
  implicit val listMessagesEventFormat = jsonFormat2(EventOutputWithMessages)

  // todo
  var secondariesSocketsPorts: List[Int] = List()
  var lastMessageId = 0

  var messages: List[ReplicatedMessage] = List()

  def main(args: Array[String]) {
    val httpPort = args(0).toInt

    secondariesSocketsPorts = args(1).toInt :: args(2).toInt :: secondariesSocketsPorts

    val route: Route =
      get {
        pathSingleSlash {
          complete(messages)
          }
      } ~
        // redundant api, could be deleted
      get {
        path("/from-secondaries") {
          onSuccess(getMessagesFromSecondaries()) {
            (responses: List[List[ReplicatedMessage]]) => {
              var messagesMap: Map[Int, String] = Map()

              // remove duplicates with map by id
              responses.foreach((messages) =>
                messages.foreach((m) => {
                  messagesMap = messagesMap + (m.id -> m.data)
              }))

              complete(messagesMap.values.toList)
            }}
        }
      } ~
      post {
        pathSingleSlash {
          entity(as[InputMessage]) { message =>
            onSuccess(addMessage(message)) { (responses) => {
              complete("Success")
            }
            }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", httpPort)
    println(s"Server online at http://localhost:${httpPort}/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }

  def addMessage(message: InputMessage): Future[List[String]] = {
    lastMessageId += 1

    val newMessage = ReplicatedMessage(lastMessageId, message.text)

    messages = newMessage :: messages

    val futures: List[Future[String]] = secondariesSocketsPorts
      .map((port) => SocketClient.run(port, AddMessageEvent(newMessage).toJson.toString()))

    Future.sequence(futures)
  }

  def getMessagesFromSecondaries(): Future[List[List[ReplicatedMessage]]] = {
    val futures = secondariesSocketsPorts.map((port) => {
      val event = GetMessagesEvent().toJson.toString()

      SocketClient.run(port, event).transform(
        (event) => JsonParser(event).convertTo[EventOutputWithMessages].data,
        error => error
        )
    })

    Future.sequence(futures)
  }
}