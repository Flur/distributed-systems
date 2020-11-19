package secondary

import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

import scala.util.parsing.json.{JSON, JSONObject, Parser}
import scala.collection.mutable

object Main extends App {
  var messages: mutable.Map[String, String] = mutable.Map()

  val service = new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] =
      req.method match {
        case http.Method.Get => get(req)
        case http.Method.Post => post(req)
      }
    }

  val server = Http.serve(":8081", service)
  Await.ready(server)

  def get(req: http.Request): Future[http.Response] = {
    val res = http.Response(req.version, http.Status.Ok)
    val contentString = JSONObject(messages.toMap).toString()

    println(s"get map of  ${contentString}")

    res.contentString = contentString
    res.setContentTypeJson()

    Thread.sleep(5000)

    Future.value(
      res
    )
  }

  def post(req: http.Request): Future[http.Response] = {
    val res = http.Response(req.version, http.Status.Ok)
    val content = req.getContentString()
    val parsedContent = JSON.parseFull(content).orNull.asInstanceOf[Map[String, String]]


    messages = messages ++ parsedContent

    println(s"appended message ${parsedContent}")

    Future.value(
      res
    )
  }
}
