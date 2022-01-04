package org.ziverge

import zhttp.http._
import zhttp.service.Server
import zio._
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

object DependencyServer extends App {

  import upickle.default.{read, write}
  val app: Http[Any, Nothing, Request, Response[Any, Nothing]] = Http.collectM[Request] {
    case Method.GET -> Root / "text" => ZIO.succeed(Response.text("Hello World!"))
    case Method.GET -> !! / "projectData" => 
        for 
            appData <- SharedLogic.fetchAppData(ScalaVersion.V2_13).orDie
        yield Response.json(write(appData))
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app).exitCode
}


object SharedLogic:
  def fetchAppData(scalaVersion: ScalaVersion): ZIO[Any, Throwable, FullAppData] =
    for
      currentZioVersion <-
        Maven.projectMetaDataFor(Data.zioCore, scalaVersion).map(_.typedVersion)
      allProjectsMetaData: Seq[ProjectMetaData] <-
        ZIO.foreach(Data.projects) { project =>
          Maven.projectMetaDataFor(project, scalaVersion)
        }
      _ <- ZIO.debug("got first project")
      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allProjectsMetaData))
      connectedProjects: Seq[ConnectedProjectData] <-
        ZIO.foreach(allProjectsMetaData)( x => 
          ZIO.debug("Getting data for " + x.project.artifactId) *> ZIO.fromEither(ConnectedProjectData(x, allProjectsMetaData, graph, currentZioVersion))
        )
    yield FullAppData(connectedProjects, allProjectsMetaData, DotGraph.render(graph), currentZioVersion, scalaVersion)
end SharedLogic

// object DumbMain {
//   final def main(args: Array[String]): Unit =
//     DependencyServer.main(args)
// }