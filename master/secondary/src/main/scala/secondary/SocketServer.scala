package secondary

import java.io._
import java.net.ServerSocket
import akka.actor.ActorSystem
import scala.concurrent.{Future}
import scala.util.{Try}


object SocketServer {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  def run(port: Int, callback: String => String): Unit = {
    def serve(stream: InputStream, streamOut: OutputStream): Unit = {
      val out = new PrintWriter(streamOut, true);
      val in = new BufferedReader(new InputStreamReader(stream));

      val inputData = in.readLine()

      println(s"Socket server received ${inputData}")

      val outData = callback(inputData)

      println(s"Socket server send ${outData}")

      out.println(outData)
    }

    val serverSocket = new ServerSocket(port)

    println(s"Started Socket Server on port ${port}")

    while(true) {
      val socket = serverSocket.accept

      Future { serve(socket.getInputStream, socket.getOutputStream) } onComplete {(v) => Try(socket.close())}
    }
  }
}
