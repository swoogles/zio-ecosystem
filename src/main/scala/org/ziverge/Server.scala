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


object DependencyServer extends ZIOAppDefault:

  import upickle.default.{read, write}
  val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> otherPathsIncludingRoot =>
            Response(Status.Ok, Headers.empty,
              HttpData.fromStream {
              ZStream.fromFile(Paths.get("src/main/resources/index.html").toFile).refineOrDie(_ => ???)
            }
          )
    case Method.GET -> !! / "json" => Response.json("""{"greetings": "Hello World!"}""")
  }

  val app2: HttpApp[Any, Throwable] =
    Http.collect[Request] {
      case Method.GET -> zhttp.http.!! / "compiledJavascript" / "zioecosystemtracker-fastopt.js" =>
        Response(Status.Ok, Headers.empty,
          HttpData.fromStream {
            ZStream
              .fromFile(
                Paths
                  .get("src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")
                  .toFile
              )
              .refineOrDie(_ => ???)
          }
        )
      case Method.GET -> zhttp.http.!! / "images" / path =>
        Response(
          status = Status.Ok,
          headers =
            Headers.contentType("image/svg+xml"),
          HttpData
          .fromFile(new File(s"src/main/resources/images/$path"))
        )

      case Method.GET -> zhttp.http.!! / "projectData" =>
        val appData      = CrappySideEffectingCache.fullAppData.get
        val responseText = Chunk.fromArray(write(appData).getBytes)
          Response(
            status = Status.Ok,
            headers =
              Headers
                .contentLength(responseText.length.toLong)
                .combine(Headers.contentType("application/json")),
            data = HttpData.fromStream(ZStream.fromChunk(responseText))
          )
      case Method.GET -> otherPathsIncludingRoot =>
        Response(Status.Ok, Headers.empty,
          HttpData.fromStream {
            ZStream.fromFile(Paths.get("src/main/resources/index.html").toFile).refineOrDie(_ => ???)
          }
        )
    }

  override def run =
    (
      for
        port <- ZIO.succeed(sys.env.get("PORT")).debug("Port")
        _ <-
          SharedLogic
            .fetchAppDataAndRefreshCache(ScalaVersion.V2_13)
            .tapError(error => ZIO.debug("Error during data fetch: " + error))
            .orDie
            .repeat(Schedule.spaced(30.minutes))
            .fork
        _ <- Server.start(port.map(_.toInt).getOrElse(8090), app2)
      yield ()).exitCode
end DependencyServer
