package org.ziverge

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
      currentZioVersion: org.ziverge.Version <-
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
              ZIO.fromEither(ConnectedProjectData(x, allProjectsMetaData, graph, currentZioVersion))
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