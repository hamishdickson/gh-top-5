package topFive

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.argonaut._
import _root_.argonaut._, Argonaut._

import scala.util.Properties

import scalaz._

case class GhTopFive(host: String, port: Int) {
  val service = HttpService {
    case GET -> Root / "top-5-repos" / userName => {
      GhClient.getTop5Repos(userName) match {
        case \/-(v) => Ok(v.asJson)
        case -\/(e) => NotFound(e.asJson)
      }
    }
  }

  val builder = BlazeBuilder
    .bindHttp(port, host)
    .mountService(service)
}

object GhTopFive {
  val ip =   "0.0.0.0"
  val port = Properties.envOrElse("HTTP_PORT", "5000").toInt

  def main(args: Array[String]): Unit = {
    println(s"Starting REST server on port: $port and ip: $ip")

    GhTopFive(ip, port)
      .builder
      .run
      .awaitShutdown()
  }
}
