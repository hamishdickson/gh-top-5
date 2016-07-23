package topFive

import org.http4s.client.blaze._
import org.http4s.client.UnexpectedStatus

import _root_.argonaut._, Argonaut._

import scalaz._
import scalaz.concurrent.Task

import org.log4s.getLogger

object GhClient {
  private val logger = getLogger
  val client = PooledHttp1Client()

  /**
    * Github would normally limit this request to 20 repos, if you want a different
    * page size, you need to specify that in the request.
    *
    * Here we set the page size is set to the number of repos the user has. This is for 2 reasons, firstly
    * the gh api has rate limiting in place (60 per hour for unauthenticated requests). So it's better
    * to know for sure you only need to make 2 requests per user (one for user details, another for
    * their repo information) than potentually have to make many many more than that.
    * 
    * Secondly, this makes the code much simplier when it comes to collecting data and sorting it
    *
    * Other approaches:
    * While this approach is OK for small amounts of data (this is still fast for the most prolific
    * user I know on gh, Ed Kmett @ekmett, who has 211 repos), it wouldn't be approprite for other services.
    * In that case, you might use FreeApplicative to fetch mutiple pages of data as an async service. This
    * should be very fast, but this would hit any rate limits very quickly
    */
  def getTop5Repos(un: String): String \/ List[Repo] = {
    logger.info(s"get top 5 repos for user $un")

    val res: String \/ List[Repo] = for {
      ud <- userData(un)
      rs <- allRepos(ud)
    } yield rs

    sortAndTake(res)
  }

  def allRepos(user: User): String \/ List[Repo] = {
    val ghUserDataTask: Task[String] =
      client.expect[String](s"https://api.github.com/users/${user.name}/repos?per_page=${user.numberOfRepos}")
    val ghUserData: Throwable \/ String = ghUserDataTask.attemptRun

    for {
      maybeUser <- valueOrStatusMessage(ghUserData)
      repos <- Parse.decodeEither[List[Repo]](maybeUser)
    } yield repos
  }

  /*
   * Find how many repos a specific user has
   */
  def userData(u: String): String \/ User = {
    val ghResponseTask: Task[String] = client.expect[String](s"https://api.github.com/users/$u")
    val ghResponse: Throwable \/ String = ghResponseTask.attemptRun

    for {
      x <- valueOrStatusMessage(ghResponse)
      y <- Parse.decodeEither[User](x)
    } yield y
  }

  /**
    * Sort and return the top 5 repos if right, if left then return the error string unchanged
    *
    * As there are going to be fewer than a couple of hundred records here, sortWith should be
    * performant
    */
  def sortAndTake(x: String \/ List[Repo]): String \/ List[Repo] = x.map(_.sortWith(_.size > _.size).take(5))

  def valueOrStatusMessage[A](tOrA: Throwable \/ A): String \/ A = tOrA match {
    case \/-(v) => \/-(v)
    case -\/(e) => e match {
      case UnexpectedStatus(us) => {
        logger.warn(s"unexpected status $us")
        -\/(us.toString)
      }
      case _ => {
        logger.error(s"unknown error $e")
        -\/(e.toString)
      }
    }
  }

  implicit def reposDecodedJson: DecodeJson[Repo] = DecodeJson( raw => for {
    name <- raw.get[String]("full_name")
    size <- raw.get[Int]("size")
  } yield Repo(name, size))

  implicit def userDecodedJson: DecodeJson[User] = DecodeJson( raw => for {
    name <- raw.get[String]("login")
    numRepos <- raw.get[Int]("public_repos")
  } yield User(name, numRepos))
}

final case class User(name: String, numberOfRepos: Int)
final case class Repo(name: String, size: Int)

object Repo {
  implicit def RepoCodecJson: CodecJson[Repo] =
    casecodec2(Repo.apply, Repo.unapply)("name", "size")
}
