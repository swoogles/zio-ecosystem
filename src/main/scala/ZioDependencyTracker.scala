package org.ziverge

import pprint.PPrinter
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import upickle.default.{read, write}
import zio.Console.printLine
import zio.{Console, Task, ZIO, ZIOAppDefault}

object ZioDependencyTracker extends ZIOAppDefault:
  def run =
    for
      _ <- ZIO.debug("Running")
      args <- this.getArgs
      FullAppData(connected, all, graph) <-
        if (args.contains("--cached-jvm"))
          for
            connectedX <- FileIO.readResource[Seq[ConnectedProjectData]]("connectedProjectData.txt")
            allX <- FileIO.readResource[Seq[ProjectMetaData]]("allProjectsMetaData.txt")
            graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allX))
          yield FullAppData(connectedX, allX, graph)
        else
          for
            FullAppData(connected, all, graph) <- SharedLogic.fetchAppData
            currentZioVersion <- Maven.projectMetaDataFor(Data.zioCore, ScalaVersion.V2_13).map(_.typedVersion)
            filteredProjects = all // .filter(p => p.project.artifactId != "zio" || Data.coreProjects.contains(p.project))

            // TODO Clear out files before writing new versions
//            _ <- ZIO.foreach(connected) {connectedProject => FileIO.saveAsResource(connectedProject, s"${Render.sbtStyle(connectedProject.project)}.txt")}
            _ <- FileIO.saveAsResource(connected, "connectedProjectData.txt")
            _ <- FileIO.saveAsResource(all, "allProjectsMetaData.txt")
          yield FullAppData(connected, all, graph)
      _ <-
        if (args.contains("json") )
          printLine(Json.render(connected))
        else if (args.contains("dot") )
          printLine(DotGraph.render(graph))
        else if (args.contains("dependents") )
          manipulateAndRender(
            connected,
            _.dependants.size,
            p =>
              if (p.dependants.nonEmpty)
                f"Required by ${p.dependants.size} projects: " +
                  p.dependants.map(_.project.artifactId).mkString(",")
              else
                "Has no dependents"
          )
        else if (args.contains("dependencies") )
          manipulateAndRender(
            connected,
            _.dependencies.size,
            p =>
              if (p.dependencies.nonEmpty)
                s"Depends on ${p.dependencies.size} projects: " +
                  p.dependencies.map(_.project.artifactId).mkString(",")
              else
                "Does not depend on any known ecosystem library."
          )
        else if (args.contains("blockers") )
          manipulateAndRender(
            connected,
            _.blockers.size,
            p =>
              if (p.blockers.nonEmpty)
                s"is blocked by ${p.blockers.size} projects: " +
                  p.blockers.map(blocker => Render.sbtStyle(blocker.project)).mkString(",")
              else
                "Is not blocked by any known ecosystem library."
          )
        else
          ZIO.fail("Unrecognized CLI arguments")
    yield ()
    end for
  end run

  object FileIO:

    import java.io.{File, FileWriter}
    def saveAsResource[T : upickle.default.ReadWriter](
                                                        connectedProjects: T,
                                                        fileName: String
                                                      )   =
      ZIO.debug("fileName: " + fileName) *>
        ZIO {
          val file = new File(s"src/main/resources/$fileName")
          if (!file.exists()) file.createNewFile()
          val fileWriter = new FileWriter(file)
          fileWriter.write(PPrinter.BlackWhite.apply(connectedProjects, height = Int.MaxValue).toString)
          fileWriter.close()
        }

    def readResource[T : upickle.default.ReadWriter](
                                                      fileName: String
                                                    ): Task[T] =
      ZIO {
        val src = scala.io.Source.fromFile(s"src/main/resources/$fileName")
        val res = read[T](src.mkString)
        src.close()
        res
      }

  //      val file = root/"tmp"/"test.txt"
  //      file.overwrite("hello")
  //      file.appendLine().append("world")
  //      assert(file.contentAsString == "hello\nworld")


  def zprint[T : upickle.default.ReadWriter](x: T) =
    val pickled = write(x)
    val depickled = read[T](pickled)
    Console.printLine(pprint(x, height = Int.MaxValue))

  enum DataView:
    case Dependencies, Dependents, Json, Blockers

  def manipulateAndRender(
                           connectedProjects: Seq[ConnectedProjectData],
                           sort: ConnectedProjectData => Integer,
                           connectionMessage: ConnectedProjectData => String
                         ): ZIO[Console, Any, Unit] =
    val currentZioVersion = Version("2.0.0-RC1")
    for
      _ <-
        printLine(
          connectedProjects
            .filter(p =>
              p.blockers.nonEmpty ||
                p.zioDep
                  .fold(true)(zDep => zDep.zioDep.typedVersion.compareTo(currentZioVersion) < 0) &&
                  !Data.coreProjects.contains(p.project)
            ) // TODO Where to best provide this?
            .sortBy(sort)
            .reverse
            .sortBy(p => Render.sbtStyle(p.project)) // TODO remove after demo run
            .map { project =>
              val renderedZioDependency =
                if (Data.coreProjects.contains(project.project))
                  "is a core project"
                else
                  ZioDep.render(project.zioDep)
              f"${Render.sbtStyle(project.project)}%-50s ${renderedZioDependency} and " +
                connectionMessage(project)
            }.mkString("\n")
        )
    yield ()
    end for
  end manipulateAndRender
end ZioDependencyTracker


/* TODO Cli Options
 * --include-core-deps
 * --include-version-deps
 * --targetProject */





object SharedLogic:
  val fetchAppData: ZIO[Any, Object, FullAppData] =
    for
      currentZioVersion <- Maven.projectMetaDataFor(Data.zioCore, ScalaVersion.V2_13).map(_.typedVersion)
      allProjectsMetaData: Seq[ProjectMetaData] <-
        ZIO.foreachPar(Data.projects) { project =>
          Maven.projectMetaDataFor(project, ScalaVersion.V2_13)
        }
      filteredProjects = allProjectsMetaData
      //          .filter(p => p.project.artifactId != "zio" || Data.coreProjects.contains(p.project))

      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allProjectsMetaData))
      connectedProjects: Seq[ConnectedProjectData] <-
        ZIO.foreach(filteredProjects)(
          ConnectedProjectData(_, allProjectsMetaData, graph, currentZioVersion)
        )
    yield FullAppData(connectedProjects, allProjectsMetaData, graph)

