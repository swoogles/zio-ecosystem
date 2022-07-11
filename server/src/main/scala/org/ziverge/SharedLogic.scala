package org.ziverge

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import zio.ZIO

import java.time.Instant

object CrappySideEffectingCache:
  var fullAppData: Option[FullAppData] = None
  var timestamp: Instant               = Instant.parse("2018-11-30T18:35:24.00Z")

object SharedLogic:
  def fetchAppDataAndRefreshCache(scalaVersion: ScalaVersion) =
    for
      _   <- zio.Console.printLine("Getting fresh data")
      res <- fetchAppData(scalaVersion)
      _ <-
        ZIO.attempt {
          CrappySideEffectingCache.fullAppData = Some(res)
        }
    yield res

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
                        println("Github Error: " + githubError)
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