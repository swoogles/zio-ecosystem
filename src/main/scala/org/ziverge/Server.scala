package org.ziverge

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

import zhttp.http._
import zhttp.http.Header
import zhttp.service.Server
import zio._
import zio.stream.ZStream
import java.nio.file.Paths

object DependencyServer extends App:

  import upickle.default.{read, write}
  val app: Http[Any, Nothing, Request, Response[Any, Nothing]] =
    Http.collectM[Request] {
      case Method.GET -> Root / "text" =>
        ZIO.succeed(Response.text("Hello World!"))
      case Method.GET -> Root / "files" / fileName =>
        ZIO.succeed {
          println("retrieving file: " + fileName)
          val content =
            HttpData.fromStream {
              ZStream.fromFile(Paths.get("src/main/resources/index.html")).refineOrDie(_ => ???).provideLayer(zio.blocking.Blocking.live)
            }
          Response.http(data = content)
        }
      case Method.GET -> Root / "files" / "compiledJavascript" / "zioecosystemtracker-fastopt.js" =>
        ZIO.succeed {
          val content =
            HttpData.fromStream {
              ZStream.fromFile(Paths.get("src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")).refineOrDie(_ => ???).provideLayer(zio.blocking.Blocking.live)
            }
          Response.http(data = content)
        }
      case Method.GET -> !! / "projectData" =>
        for
          appData <- SharedLogic.fetchAppData(ScalaVersion.V2_13).orDie
          _       <- ZIO.debug("Ready to return all this sweet data: " + appData)
          responseText = Chunk.fromArray(write(appData).getBytes)
        yield Response.http(
          status = Status.OK,
          headers = Headers.contentLength(responseText.length.toLong),
          data = HttpData.fromStream(ZStream.fromChunk(responseText))
          // Encoding content using a ZStream
        )
    }

  // val empty: ZTraceElement =
  // Tracer.instance.empty
  // implicit val trace: ZTraceElement = zio.ZTraceElement.empty
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (
      for
        // port <- zio.System.env("PORT")(ZTraceElement.empty)
        port <- ZIO(sys.env.get("PORT"))
        // port <- ZIO(Some("8090"))
        _ <- ZIO.debug("PORT result: " + port)
        _ <- Server.start(port.map(_.toInt).getOrElse(8090), app)
      yield ()
    ).exitCode
end DependencyServer

object SharedLogic:
  def fetchAppData(scalaVersion: ScalaVersion): ZIO[Any, Throwable, FullAppData] =
    for
      currentZioVersion <- Maven.projectMetaDataFor(Data.zioCore, scalaVersion).map(_.typedVersion)
      allProjectsMetaData: Seq[ProjectMetaData] <-
        ZIO.foreach(Data.projects) { project =>
          Maven.projectMetaDataFor(project, scalaVersion)
        }
      _                             <- ZIO.debug("got first project!")
      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allProjectsMetaData))
      connectedProjects: Seq[ConnectedProjectData] <-
        ZIO.foreach(allProjectsMetaData)(x =>
          for
            _ <- ZIO.debug("Getting Data for " + x.project.artifactId)
            res <-
              ZIO.fromEither(ConnectedProjectData(x, allProjectsMetaData, graph, currentZioVersion))
            _ <- ZIO.debug("Res: " + res)
          yield res
        )
    yield FullAppData(
      connectedProjects,
      allProjectsMetaData,
      DotGraph.render(graph),
      currentZioVersion,
      scalaVersion
    )
end SharedLogic

// object DumbMain {
//   final def main(args: Array[String]): Unit =
//     DependencyServer.main(args)
// }
