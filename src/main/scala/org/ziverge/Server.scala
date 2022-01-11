package org.ziverge

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import zhttp.http._
import zhttp.http.Header
import zhttp.service.Server
import zio._
import zio.duration.durationInt
import zio.stream.ZStream
import java.nio.file.Paths
import java.time.Instant

object CrappySideEffectingCache:
  var fullAppData: Option[FullAppData] = None
  var timestamp: Instant               = Instant.parse("2018-11-30T18:35:24.00Z")

object DependencyServer extends App:

  import upickle.default.{read, write}
  val app: Http[Any, Nothing, Request, Response[Any, Nothing]] =
    Http.collectM[Request] {
      case Method.GET -> Root / "text" =>
        ZIO.succeed(Response.text("Hello World!"))
      case Method.GET -> Root =>
        ZIO.succeed {
          val content =
            HttpData.fromStream {
              ZStream
                .fromFile(Paths.get("src/main/resources/index.html"))
                .refineOrDie(_ => ???)
                .provideLayer(zio.blocking.Blocking.live)
            }
          Response.http(data = content)
        }
      case Method.GET -> Root / "compiledJavascript" / "zioecosystemtracker-fastopt.js" =>
        ZIO.succeed {
          val content =
            HttpData.fromStream {
              ZStream
                .fromFile(
                  Paths.get("src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")
                )
                .refineOrDie(_ => ???)
                .provideLayer(zio.blocking.Blocking.live)
            }
          Response.http(data = content)
        }
      case Method.GET -> Root / "images" / path =>
        ZIO.succeed {
          val content =
            HttpData.fromStream {
              ZStream
                .fromFile(Paths.get(s"src/main/resources/images/$path"))
                .refineOrDie(_ => ???)
                .provideLayer(zio.blocking.Blocking.live)
            }
          Response.http(data = content)
        }
      case Method.GET -> !! / "projectData" =>
        val appData      = CrappySideEffectingCache.fullAppData.get
        val responseText = Chunk.fromArray(write(appData).getBytes)
        ZIO.succeed(
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
    (
      for
        port <- ZIO(sys.env.get("PORT"))
        _    <- ZIO.debug("PORT result: " + port)
        _ <-
          SharedLogic
            .fetchAppDataAndRefreshCache(ScalaVersion.V2_13)
            .orDie
            .repeat(Schedule.spaced(30.minutes))
            .fork
        _ <- Server.start(port.map(_.toInt).getOrElse(8090), app)
      yield ()
    ).exitCode
end DependencyServer

object SharedLogic:
  def fetchAppDataAndRefreshCache(scalaVersion: ScalaVersion) =
    for
      now <- ZIO(Instant.now())
      ageOfCache = java.time.Duration.between(now, CrappySideEffectingCache.timestamp)
      res <-
        if (
          ageOfCache.compareTo(java.time.Duration.ofHours(1)) > 0 ||
          CrappySideEffectingCache.fullAppData.isEmpty
        )
          println("Getting fresh data")
          fetchAppData(scalaVersion)
        else
          println("Using cached data")
          ZIO(CrappySideEffectingCache.fullAppData.get)
      _ <-
        ZIO {
          CrappySideEffectingCache.fullAppData = Some(res)
          CrappySideEffectingCache.timestamp = now
        }
    yield res

  def fetchAppData(scalaVersion: ScalaVersion): ZIO[Any, Throwable, FullAppData] =
    for
      currentZioVersion <- Maven.projectMetaDataFor(Data.zioCore, scalaVersion).map(_.typedVersion)
      allProjectsMetaData: Seq[ProjectMetaData] <-
        ZIO.foreachPar(Data.projects) { project =>
          Maven.projectMetaDataFor(project, scalaVersion)
        }
      _                             <- ZIO.debug("got first project!")
      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allProjectsMetaData))
      connectedProjects: Seq[ConnectedProjectData] <-
        ZIO.foreach(allProjectsMetaData)(x =>
          for
            res <-
              ZIO.fromEither(ConnectedProjectData(x, allProjectsMetaData, graph, currentZioVersion))
          yield res
        )
      res =
        FullAppData(
          connectedProjects,
          allProjectsMetaData,
          DotGraph.render(graph),
          currentZioVersion,
          scalaVersion
        )
    yield res
  end fetchAppData
end SharedLogic
