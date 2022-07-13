package org.ziverge.ecosystem

import org.ziverge.*
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import zio.ZIO

import java.time.Instant

object SharedLogic:
  private object CrappySideEffectingCache:
    var fullAppData: Option[FullAppData] = None
    var timestamp: Instant               = Instant.parse("2018-11-30T18:35:24.00Z")

  def fetchAppDataAndRefreshCache(scalaVersion: ScalaVersion) =
    for
      _   <- zio.Console.printLine("Getting fresh data")
      res <- fetchAppData(scalaVersion)
      _ <-
        ZIO.attempt {
          CrappySideEffectingCache.fullAppData = Some(res)
        }
    yield res

  def projectData(group: String, artifactId: String, currentZioVersion: Version, scalaVersion: ScalaVersion): ZIO[Any, Serializable, ConnectedProjectData] =
    for
      _ <- ZIO.when(CrappySideEffectingCache.fullAppData.isEmpty)(SharedLogic.fetchAppDataAndRefreshCache(ScalaVersion.V2_13))
      res <- ZIO.fromOption (
        CrappySideEffectingCache.fullAppData.get.connected.find(p => p.project.group == group && p.project.artifactId == artifactId)
      )
//      _ <- ZIO.attempt(
//        pprint.pprintln(res)
//      )
    yield res

  def fetchAppDataWhenAppropriate(scalaVersion: ScalaVersion): ZIO[Any, Throwable, FullAppData] =
    for
      _ <- ZIO.when(CrappySideEffectingCache.fullAppData.isEmpty)(SharedLogic.fetchAppDataAndRefreshCache(scalaVersion))
    yield CrappySideEffectingCache.fullAppData.get

  def fetchAppData(scalaVersion: ScalaVersion): ZIO[Any, Throwable, FullAppData] =
    for
      currentZioVersion: Version <-
        Maven.projectMetaDataFor(TrackedProjects.zioCore, scalaVersion).map(_.typedVersion)
      allProjectsMetaData <-
        ZIO.collectAllSuccessesPar(TrackedProjects.projects.map{ project =>
          Maven.projectMetaDataFor(project, scalaVersion)
        }
        )
      // TODO Do Pull Request query here
      graph: Graph[Project, DiEdge] <- ZIO.attempt(ScalaGraph(allProjectsMetaData))
      connectedProjects: Seq[ConnectedProjectData] <-
        ZIO.collectAllSuccessesPar(allProjectsMetaData.map(x =>
          for
            res <-
              ZIO.fromEither(ConnectedProjectServer(x, allProjectsMetaData, graph, currentZioVersion))
                .tapError(error => ZIO.debug("Error constructing ConnectedProjectData: " + error))
            finalProject <-
              if (res.onLatestZioDep)
                ZIO.succeed(res)
              else
                res
                  .project
                  .githubOrgAndRepo
                  .map(project =>
                    Github
                      .pullRequests(project)
                      .map { prOpt =>
                        res.copy(relevantPr = prOpt)
                      }
                      .catchAll { case githubError =>
//                        println("Github Error: " + githubError) // TODO Delete this once token lookup is done properly?
                        ZIO.succeed(res)
                      }
                  )
                  .getOrElse(ZIO.succeed(res))
          // ZIO.debug(res.project.artifactId + " upToDate: " +  res.projectIsUpToDate)
          // TODO Look for PRs here
          yield finalProject
        )
        )
      res =
        FullAppData(connectedProjects, currentZioVersion, scalaVersion)
    yield res //.copy(connected = res.connected.take(5))
  end fetchAppData
end SharedLogic

// TODO Move around
import scalax.collection.Graph
import scalax.collection.GraphEdge.*
import scalax.collection.GraphPredef.*
import scalax.collection.edge.Implicits.*
import scalax.collection.edge.LDiEdge
object ConnectedProjectServer:

  def apply(
             projectMetaData: ProjectMetaData,
             allProjectsMetaData: Seq[ProjectMetaData],
             dependendencyGraph: Graph[Project, DiEdge],
             currentZioVersion: Version
           ): Either[Throwable, ConnectedProjectData] = // TODO More specific error type
    for
      node <-
        dependendencyGraph
          .nodes
          .find { node =>
            // TODO Do we need this?
            val nodeProject: Project = node.value.asInstanceOf[Project]
            nodeProject.artifactId == projectMetaData.project.artifactId &&
              nodeProject.group == projectMetaData.project.group
          }
          .toRight {
            new Exception(
              s"Missing value in dependency graph for ${projectMetaData.project}"
            )
          }
      dependents = node.diSuccessors.map(_.value)
      typedDependants <-
        Right(
          dependents
            .flatMap(dependent =>
              allProjectsMetaData
                .find(_.project == dependent)
                .toRight(new Exception("Missing projects metadata entry"))
                .toSeq
            )
            .toSeq
        )
      typedDependencies <-
        Right(
          projectMetaData
            .dependencies
            .flatMap(dependency =>
              allProjectsMetaData
                .find(_.project == dependency.project)
                .toRight(new Exception("Missing dependency entry for: " + dependency.project))
                .toSeq
            )
        )
      zioDep <-
        ProjectMetaData.getUnderlyingZioDep(projectMetaData, allProjectsMetaData, currentZioVersion)
      // Instead of yielding here, assign value, check if it's on the latest ZIO, and then query for
      // open PRs if not
      connectedProject =
        ConnectedProjectData(
          projectMetaData.project,
          projectMetaData.typedVersion,
          typedDependencies.map(ProjectMetaData.apply),
          typedDependants.map(ProjectMetaData.apply),
          zioDep,
          currentZioVersion
        )
    yield connectedProject
  end apply
  
object ScalaGraph:
  def apply(allProjectsMetaData: Seq[ProjectMetaData]): Graph[Project, DiEdge] =
    Graph(
      allProjectsMetaData.flatMap { project =>
        project
          .dependencies
          .map { dependency =>
            dependency.project ~> project.project
          }
      }*
    )
