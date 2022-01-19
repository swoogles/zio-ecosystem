package org.ziverge

import upickle.default.{read, write}
import upickle.default.{macroRW, ReadWriter as RW, *}

case class PullRequest(number: Int, title: String, html_url: String)
object PullRequest {

  implicit val rw: RW[PullRequest]            = macroRW
}

object Github {
  
  import sttp.model.Uri
  import zio.ZIO

  def pullRequests(inputUrlIgnored: String): ZIO[Any, Throwable, String] = {
    import sttp.client3.*
    val backend = HttpURLConnectionBackend()
    for
      _ <- ZIO.debug("Going to construct URL")
      url <-
        ZIO
          .fromEither(
            Uri.safeApply(
              scheme = "https",
              host = "api.github.com": String,
              path = "repos/zio/zio-nio/pulls".split("\\/").toSeq
            )
          )
          .mapError(new Exception(_))
      _ <- ZIO.debug("constructed URL: " + url)
      r    <- ZIO(basicRequest.get(url).send(backend))
      body <- ZIO.fromEither(r.body).mapError(new Exception(_))
      _ <- ZIO.debug("Pull request body: " + read[Seq[PullRequest]](body))
    yield body
  }
}
