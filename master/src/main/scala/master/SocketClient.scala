package master

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.{Socket, UnknownHostException}
import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem

import scala.concurrent.Future

object SocketClient {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  def sendMessage(port: Int, eventMessage: String, countDownLatch: CountDownLatch): Future[String] = {
    Future {
      sendMessageNoFuture(port, eventMessage, countDownLatch)
    }
  }

  def sendMessageNoFuture(port: Int, eventMessage: String, countDownLatch: CountDownLatch): String = {
    val result = sendSimpleMessage(port, eventMessage)
    countDownLatch.countDown()

    println(s"Send ${eventMessage} to socket on port ${port}")
    println(s"Socket close with ack ${result}")

    result
  }

  def sendSimpleMessage(port: Int, eventMessage: String): String = {
    //  host.docker.internal
    val socket = new Socket("0.0.0.0", port)

    val out = new PrintWriter(socket.getOutputStream, true)
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    var result = ""

    out.println(eventMessage)

    result = in.readLine()

    if (socket != null) socket.close()
    if (out != null) out.close()
    if (in != null) in.close()

    result
  }
}