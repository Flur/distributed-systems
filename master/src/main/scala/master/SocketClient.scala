package master

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.{Socket, UnknownHostException}
import akka.actor.ActorSystem
import scala.concurrent.Future

object SocketClient {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  def run(port: Int, eventMessage: String): Future[String] = {
    Future {
      val echoSocket = new Socket("localhost", port)
      val out = new PrintWriter(echoSocket.getOutputStream, true)
      val in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream))
      var result = ""

      try {
        out.println(eventMessage)

        println(s"Send ${eventMessage} to socket on port ${port}")

        result = in.readLine()
      } catch {
        case e: UnknownHostException =>
          System.err.println("Don't know about host " + port)
          System.exit(1)
        case e: IOException =>
          System.err.println("Couldn't get I/O for the connection to " + port)
          System.exit(1)
      } finally {
        if (echoSocket != null) echoSocket.close()
        if (out != null) out.close()
        if (in != null) in.close()
      }

      result
    }
  }
}