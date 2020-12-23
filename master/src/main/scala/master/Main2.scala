//package master
//
//import com.twitter.finagle.{Http, Service, http}
//import com.twitter.util.{Await, Future}
//
//import scala.collection.mutable.ListBuffer
//import scala.util.parsing.json.{JSON, JSONObject, JSONArray}
//
//
//object Main2 extends App {
//  type SecondaryService = Service[http.Request, http.Response]
//
//  // add from env vars
//  var messagesCount = 0;
//  val secondariesPorts = List("8081", "8082" /*, "8083"*/)
//  val secondariesServices: List[SecondaryService] =
//    secondariesPorts.map((port: String) => Http.newService(s"host.docker.internal:${port}"))
//
//  val masterService = new Service[http.Request, http.Response] {
//    def apply(req: http.Request): Future[http.Response] = req.method match {
//      case http.Method.Get => get(req)
//      case http.Method.Post => post(req)
//    }
//
//  }
//
//  val server = Http.serve(":8080", masterService)
//  Await.ready(server)
//
//  def get(req: http.Request): Future[http.Response] = {
//    val requestsToSecondariesList = requestsToSecondaries(http.Method.Get)
//
//    Future.collect[http.Response](requestsToSecondariesList).flatMap(
//      (responses: Seq[http.Response]) => {
//        var messages: Map[String, String] = Map()
//        val res = http.Response(req.version, http.Status.Ok)
//
//
//        for (response <- responses) {
//          val content = response.getContentString()
//
//          val parsedContent = JSON.parseFull(content).orNull.asInstanceOf[Map[String, String]]
//
//          messages = messages ++ parsedContent
//        }
//
//        res.contentString = JSONArray(List.from(messages.values)).toString()
//        res.setContentTypeJson()
//
//        Future.value(res)
//      }
//    )
//  }
//
//  def post(req: http.Request): Future[http.Response] = {
//    val res = http.Response(req.version, http.Status.Ok)
//    val content = req.getContentString()
//    val parsedContent = JSON.parseFull(content).orNull.asInstanceOf[Map[String, String]]
//
//    messagesCount += 1
//    val message = Map(messagesCount.toString -> parsedContent("message"))
//    val requestsToSecondariesList = requestsToSecondaries(http.Method.Post, JSONObject(message).toString())
//
//    Future.collect[http.Response](requestsToSecondariesList).flatMap(
//      (responses: Seq[http.Response]) => {
//        for (response <- responses) {
//          // todo logging
//          println(s"response from secondaries ${response.getContentString()}")
//        }
//
//        Future.value(res)
//      }
//    )
//  }
//
//  def requestsToSecondaries(method: http.Method, contentString: String = ""): ListBuffer[Future[http.Response]] = {
//    var requestsToSecondaries = new ListBuffer[Future[http.Response]]()
//
//    for (secondaryService <- secondariesServices) {
//      val req = http.Request(method, "/")
//
//      if (contentString.nonEmpty) {
//        req.setContentTypeJson()
//        req.setContentString(contentString)
//      }
//
//      requestsToSecondaries += secondaryService(req)
//    }
//
//    requestsToSecondaries
//  }
//}
//
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import akka.http.scaladsl.server.Directives._
//import akka.stream.ActorMaterializer
//import scala.io.StdIn
//
//object WebServer {
//  def main(args: Array[String]) {
//
//    implicit val system = ActorSystem("my-system")
//    implicit val materializer = ActorMaterializer()
//    // needed for the future flatMap/onComplete in the end
//    implicit val executionContext = system.dispatcher
//
//    val route =
//      path("hello") {
//        get {
//          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
//        }
//      }
//
//    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
//
//    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
//    StdIn.readLine() // let it run until user presses return
//    bindingFuture
//      .flatMap(_.unbind()) // trigger unbinding from the port
//      .onComplete(_ => system.terminate()) // and shutdown when done
//  }
//}
