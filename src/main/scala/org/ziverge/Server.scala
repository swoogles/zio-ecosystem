package org.ziverge

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import zhttp.http.*
import zhttp.http.Header
import zhttp.service.Server
import zio.*
import zio.durationInt
import zio.stream.ZStream

import java.nio.file.Paths
import java.io.File
import java.time.Instant

object DependencyServer extends App:

  import upickle.default.{read, write}
  val app =
    Http.collectHttp[Request] {
      case Method.GET -> Root =>
        Http.fromStream {
          ZStream.fromFile(Paths.get("src/main/resources/index.html").toFile).refineOrDie(_ => ???)
        }
      case Method.GET -> Root / "compiledJavascript" / "zioecosystemtracker-fastopt.js" =>
        Http.fromStream {
          ZStream
            .fromFile(
              Paths
                .get("src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")
                .toFile
            )
            .refineOrDie(_ => ???)
        }
      case Method.GET -> Root / "images" / path =>
        Http
          .fromFile(new File(s"src/main/resources/images/$path"))
          .setHeaders(Headers.contentType("image/svg+xml"))

      case Method.GET -> !! / "projectData" =>
        val appData      = CrappySideEffectingCache.fullAppData.get
        val responseText = Chunk.fromArray(write(appData).getBytes)
        Http.response(
          Response.http(
            status = Status.OK,
            headers =
              Headers
                .contentLength(responseText.length.toLong)
                .combine(Headers.contentType("application/json")),
            data = HttpData.fromStream(ZStream.fromChunk(responseText))
          )
        )
    }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for
        port <- ZIO(sys.env.get("PORT"))
        _ <-
          SharedLogic
            .fetchAppDataAndRefreshCache(ScalaVersion.V2_13)
            .tapError(error => ZIO.debug("Error during data fetch: " + error))
            .orDie
            .repeat(Schedule.spaced(30.minutes))
            .fork
        _ <- Server.start(port.map(_.toInt).getOrElse(8090), app)
      yield ()).exitCode
end DependencyServer


