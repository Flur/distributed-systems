//import com.twitter.finagle.{Http, Service, http}
//import com.twitter.util.{Await, Future}
//
//import scala.collection.mutable
//import scala.util.parsing.json.{JSON, JSONObject}
//
//object Main22 extends App {
//  var messages: mutable.Map[String, String] = mutable.Map()
//
//  val service = new Service[http.Request, http.Response] {
//    def apply(req: http.Request): Future[http.Response] =
//      req.method match {
//        case http.Method.Get => get(req)
//        case http.Method.Post => post(req)
//      }
//  }
//
//  val server = Http.serve(":8081", service)
//  Await.ready(server)
//
//  def get(req: http.Request): Future[http.Response] = {
//    val res = http.Response(req.version, http.Status.Ok)
//    val contentString = JSONObject(messages.toMap).toString()
//
//    println(s"get map of  ${contentString}")
//
//    res.contentString = contentString
//    res.setContentTypeJson()
//
//    Future.value(
//      res
//    )
//  }
//
//  // todo to sockets
//  def post(req: http.Request): Future[http.Response] = {
//    val res = http.Response(req.version, http.Status.Ok)
//    val content = req.getContentString()
//    val parsedContent = JSON.parseFull(content).orNull.asInstanceOf[Map[String, String]]
//
//    Thread.sleep(5000)
//
//    messages = messages ++ parsedContent
//
//    println(s"appended message ${parsedContent}")
//
//    Future.value(
//      res
//    )
//  }
//}