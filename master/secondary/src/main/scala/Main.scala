package docs.http.scaladsl

import akka.actor.ActorSystem
import akka.Done
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import io.socket.client.{IO, Socket}

import scala.concurrent.Future

object Main {
  val messages = List()

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    import system.dispatcher

    // Future[Done] is the materialized value of Sink.foreach,
    // emitted when the stream completes
    val incoming: Sink[Message, Future[Done]] =
    Sink.foreach[Message] {
      case message: TextMessage.Strict =>
        println(message.text)
      case _ =>
      // ignore other message types
    }

    // send this as a message over the WebSocket
    val outgoing = Source.single(TextMessage("hello world!"))

    // flow to use (note: not re-usable!)
    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest("ws://localhost:8080/ws"))

    // the materialized value is a tuple with
    // upgradeResponse is a Future[WebSocketUpgradeResponse] that
    // completes or fails when the connection succeeds or fails
    // and closed is a Future[Done] with the stream completion from the incoming sink
    val (upgradeResponse, closed) =
    outgoing
      .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
      .toMat(incoming)(Keep.both) // also keep the Future[Done]
      .run()

    // just like a regular http request we can access response status which is available via upgrade.response.status
    // status code 101 (Switching Protocols) indicates that server support WebSockets
    val connected = upgradeResponse.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Future.successful(Done)
      } else {
        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }

    // in a real application you would not side effect here
//    connected.onComplete(println)
//    closed.foreach(_ => println("closed"))
  }
}


package master

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn
import scala.concurrent.Future

object Main {
  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher

  var orders: List[Item] = Nil
  var messages: List[MyMessage] = Nil

  // domain model
  final case class Item(name: String, id: Long)
  final case class Order(items: List[Item])

  final case class MyMessage(id: Int, data: String)

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)

  // formats for unmarshalling and marshalling
  implicit val messageFormat = jsonFormat2(MyMessage)

  val secondaries = List("8081", "8082")

  // (fake) async database query api
  def fetchMessages(): Future[List[MyMessage]] = {
    Future.successful(messages)
  }

  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) => items ::: orders
      case _            => orders
    }
    Future { Done }
  }

  def addMessage(message: MyMessage): Future[Done] = {
    messages = message :: messages

    Future { Done }
  }

  def saveMessage(): Unit = {
    println("save message")
  }

  def getMessages(): Unit = {
    println("get message")
  }

  def main(args: Array[String]) {


    // The Greeter WebSocket Service expects a "name" per message and
    // returns a greeting message for that name
    val greeterWebSocketService =
    Flow[Message]
      .mapConcat { (i) => {
        // we match but don't actually consume the text message here,
        // rather we simply stream it back as the tail of the response
        // this means we might start sending the response even before the
        // end of the incoming message has been received
        //        case tm: TextMessage => TextMessage.apply(Source.single("Hello ")) :: Nil
        val message = i match {
          case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
          case bm: BinaryMessage =>
            // ignore binary messages but drain content to avoid the stream being clogged
            bm.dataStream.runWith(Sink.ignore)
            Nil
        }

        println(message)

        message
      }
      }

    // The Greeter WebSocket Service expects a "name" per message and
    // returns a greeting message for that name
    //    def greeter: Flow[Message, Message, Any] =
    //      Flow[Message].mapConcat {
    //        case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
    //        case bm: BinaryMessage =>
    //          // ignore binary messages but drain content to avoid the stream being clogged
    //          bm.dataStream.runWith(Sink.ignore)
    //          Nil
    //      }

    val route: Route =
      get {
        pathSingleSlash {
          val maybeItem: Future[List[MyMessage]] = fetchMessages()

          onSuccess(maybeItem) { m =>
            complete(m)
          }
        }
      } ~
        post {
          pathSingleSlash {
            entity(as[MyMessage]) { message =>
              val saved: Future[Done] = addMessage(message)
              onComplete(saved) { done =>
                complete("order created")
              }
            }
          }
        } ~ path("ws") {
        handleWebSocketMessages(greeterWebSocketService)
      }

    //    val route = complete("yeah")

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

  }
}