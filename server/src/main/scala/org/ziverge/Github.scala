package org.ziverge

import upickle.default.{read, write}

object Github:
  import sttp.model.Uri
  import zio.ZIO

  def pullRequests(project: GithubRepo): ZIO[Any, Throwable, Option[PullRequest]] =
    import sttp.client3.*
    val backend = HttpURLConnectionBackend()
    for
      url <-
        ZIO
          .fromEither(
            Uri
              .safeApply(
                scheme = "https",
                host = "api.github.com": String,
                path = s"repos/${project.org}/${project.name}/pulls".split("\\/").toSeq
              )
              .map(_.withParam("state", "open"))
          )
          .mapError(new Exception(_))

      // TODO Do this once on application startup, rather than attempting every time.
      accessToken <-
        ZIO
          .fromOption(sys.env.get("GITHUB_ACCESS_TOKEN"))
          .orElseFail(new Exception("Missing GITHUB_ACCESS_TOKEN"))
      r <- ZIO.attempt(basicRequest.get(url).auth.bearer(accessToken).send(backend))
      pullRequests <-
        ZIO.fromEither(r.body).mapError(new Exception(_)).map(read[Seq[PullRequest]](_))
      relevantPr =
        pullRequests
          .find(pr => pr.title.toLowerCase.contains("zio") && pr.title.toLowerCase.contains("2"))
    yield relevantPr
    end for
  end pullRequests
end Github
