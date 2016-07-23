package topFive

import org.scalatest.{FlatSpec, Matchers}
import scalaz._
import Scalaz._

/**
  * Note, these tests use the real gh api. Using scalamock would have also been possible here
  */
class GhClientIT extends FlatSpec with Matchers {

  "The gh client" should "return a 404 when an invalid user is passed" in {
    val r: String \/ List[Repo] = GhClient.getTop5Repos("ekmettslaisfna")

    r should be (-\/("404 Not Found"))
  }

  it should "treat silly characters as invalid users" in {
    val badNames = List("%20", "!") // `null`, `nil` etc are actually real users!

    val rs: List[String \/ List[Repo]] = badNames.map(GhClient.getTop5Repos(_))

    for {
      r <- rs
    } yield (r should be (-\/("404 Not Found")))
  }

  it should "return an empty list when there are no repos for a valid user" in {
    val r: String \/ List[Repo] = GhClient.getTop5Repos("Foodity-Neil")

    r should be (\/-(List()))

    r.isOrdered should be (true)
    r.is5OrLess should be (true)
  }

  it should "return a sorted list with less than 5 repos when there are less than less than 5 repos" in {
    val r: String \/ List[Repo] = GhClient.getTop5Repos("SheCanCodeCN")

    r should be (\/-(List(Repo("SheCanCodeCN/hub",5591), Repo("SheCanCodeCN/bootstrap-social",3199))))

    r.isOrdered should be (true)
    r.is5OrLess should be (true)
  }

  it should "return a sorted list with 5 repos when more than 5 repos are available" in {
    val r: String \/ List[Repo] = GhClient.getTop5Repos("DavidGregory084")

    r should be (\/-(List(Repo("DavidGregory084/scala",143208), Repo("DavidGregory084/cats",5854), Repo("DavidGregory084/shapeless",4527), Repo("DavidGregory084/il2ssd",1836), Repo("DavidGregory084/obviously-no-deficiencies",1660))))

    r.isOrdered should be (true)
    r.is5OrLess should be (true)
  }

  it should "return a sorted list with 5 repos when there are more than 100 repos available" in {
val r: String \/ List[Repo] = GhClient.getTop5Repos("ekmett")

    r should be (\/-(List(Repo("ekmett/ekmett.github.com",118292), Repo("ekmett/ghc",86268), Repo("ekmett/homebrew",48729), Repo("ekmett/lens",26971), Repo("ekmett/bytestring",12907))))

    r.isOrdered should be (true)
    r.is5OrLess should be (true)
  }

  // this will hit your rate limit, only run if you're not interested in using the api for the next hour...
  ignore should "return a 403 when the rate limit has been reached" in {
    val ls = Range(1, 61).map(_.toString)
    val rs = ls.map(GhClient.getTop5Repos(_))

    rs.contains(-\/("403 Forbidden")) should be (true)
  }

  implicit class TestUtils(rs: String \/ List[Repo]) {
    val isOrdered: Boolean = rs match {
      case -\/(_) => false
      case \/-(r) => r.foldRight((0, true)){ case (Repo(_, s), b) => (s, s >= b._1 && b._2) }._2
    }

    val is5OrLess: Boolean = rs match {
      case -\/(_) => true
      case \/-(r) => r.size <= 5
    }
  }
}
