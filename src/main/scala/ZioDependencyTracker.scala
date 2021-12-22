package org.ziverge

import sttp.model.Uri
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt}
import zio.Console.printLine
import scalax.collection.Graph
import scalax.collection.GraphPredef.*
import scalax.collection.GraphEdge.*
import scalax.collection.edge.LDiEdge
import scalax.collection.edge.Implicits.*

import java.lang.module.ModuleDescriptor.Version
import scala.xml.{Elem, XML}
import upickle.default.{macroRW, ReadWriter as RW}
import upickle.default.*

case class Project(group: String, artifactId: String):
  val groupUrl = group.replaceAll("\\.", "/")
  def versionedArtifactId(scalaVersion: ScalaVersion) =
    artifactId + "_" + scalaVersion.mvnFriendlyVersion
    
object Project:
  implicit val rw: RW[Project] = macroRW

case class VersionedProject(project: Project, version: String):
  val typedVersion = Version.parse(version)

object VersionedProject:
  implicit val rw: RW[VersionedProject] = macroRW
  def stripped(project: Project, version: String): VersionedProject =
    VersionedProject(stripScalaVersionFromArtifact(project), version)

  private def stripScalaVersionFromArtifact(project: Project): Project =
    ScalaVersion
      .values
      .find(scalaVersion => project.artifactId.endsWith("_" + scalaVersion.mvnFriendlyVersion))
      .map(scalaVersion =>
        project
          .copy(artifactId = project.artifactId.replace("_" + scalaVersion.mvnFriendlyVersion, ""))
      )
      .getOrElse(project)

case class ProjectMetaData(project: Project, version: String, dependencies: Set[VersionedProject]):
  val zioDep: Option[VersionedProject] =
    dependencies
      .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")
  val typedVersion = Version.parse(version)

object ProjectMetaData:
  implicit val rw: RW[ProjectMetaData] = macroRW
  def withZioDependenciesOnly(
      project: VersionedProject,
      dependencies: Set[VersionedProject]
  ): ProjectMetaData =
    ProjectMetaData(project.project, project.version.toString, dependencies.filter(isAZioLibrary))

  def getUnderlyingZioDep(
      projectMetaData: ProjectMetaData,
      allProjectsMetaData: Seq[ProjectMetaData]
  ): ZIO[Any, Throwable, Option[ZioDep]] =
    projectMetaData.zioDep match
      case Some(value) =>
        ZIO.succeed(Some(ZioDep(zioDep = value, dependencyType = DependencyType.Direct)))
      case None =>
        for
          rez <-
            ZIO.foreach(projectMetaData.dependencies)(dependency =>
              ZIO(allProjectsMetaData.find(_.project == dependency.project).flatMap(_.zioDep))
            )
        yield rez
          .flatten
          .minByOption(_.typedVersion)
          .map(project => ZioDep(project, DependencyType.Transitive))
end ProjectMetaData

enum DependencyType:
  case Direct, Transitive

object DependencyType:
  implicit val rw: RW[DependencyType] = macroRW

case class ZioDep(zioDep: VersionedProject, dependencyType: DependencyType)
object ZioDep:
  implicit val rw: RW[ZioDep] = macroRW

  def render(zioDep: Option[ZioDep]): String =
    zioDep.fold("Does not depend on ZIO") { dep =>
      dep.dependencyType match
        case DependencyType.Direct =>
          " directly depends on ZIO " + dep.zioDep.version
        case DependencyType.Transitive =>
          " Transitively depends on ZIO " + dep.zioDep.version
    }

case class ConnectedProjectData private (
    project: Project,
    version: Version,
    dependencies: Set[ProjectMetaData],
    blockers: Set[ProjectMetaData],
    dependants: Set[ProjectMetaData],
    zioDep: Option[ZioDep]
)
object ConnectedProjectData:
  implicit val versionRw: RW[Version] = readwriter[String].bimap[Version](_.toString, Version.parse(_))
  implicit val rw: RW[ConnectedProjectData] = macroRW
  
  def apply(
      projectMetaData: ProjectMetaData,
      allProjectsMetaData: Seq[ProjectMetaData],
      dependendencyGraph: Graph[Project, DiEdge],
      currentZioVersion: Version
  ): ZIO[Any, Object, ConnectedProjectData] = // TODO More specific error type
    for
      node <- ZIO.fromOption(dependendencyGraph.nodes.find(_.value == projectMetaData.project))
      dependents = node.diSuccessors.map(_.value)
      typedDependants: Set[ProjectMetaData] <-
        ZIO.foreach(dependents)(dependent =>
          ZIO.fromOption(allProjectsMetaData.find(_.project == dependent))
        )
      typedDependencies <-
        ZIO.foreach(projectMetaData.dependencies)(dependency =>
          ZIO
            .fromOption(allProjectsMetaData.find(_.project == dependency.project))
            .mapError(_ => "Missing dependency entry for: " + dependency.project)
        )
      zioDep <- ProjectMetaData.getUnderlyingZioDep(projectMetaData, allProjectsMetaData)
      blockers =
        typedDependencies.filter(p =>
          p.zioDep.map(_.typedVersion) match
            case Some(value) =>
              value.compareTo(currentZioVersion) < 0
            case None =>
              false
        )
    yield ConnectedProjectData(
      projectMetaData.project,
      projectMetaData.typedVersion,
      typedDependencies,
      blockers,
      typedDependants,
      zioDep
    )
  end apply
end ConnectedProjectData

def isAZioLibrary(project: VersionedProject) = Data.projects.contains(project.project)

object Render:
  def dependencies(projectMetaData: ProjectMetaData) =
    val targetProject = projectMetaData.project
    val dependencies  = projectMetaData.dependencies
    "Target Project: " + sbtStyle(targetProject, projectMetaData.typedVersion) + "\n" +
      "Dependencies:\n" +
      dependencies
        .map(project => "  " + sbtStyle(project.project, projectMetaData.typedVersion))
        .mkString("\n")

  def sbtStyle(project: Project, version: Version) =
    project.group + "::" + project.artifactId + ":" + version

  def sbtStyle(project: Project) = project.group + "::" + project.artifactId

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

object ZioDependencyTracker extends ZIOAppDefault:

  // TODO: Rope zio-cli into this thing to make
  // the command line interface The
  // Right Way (TM). (The following may be the
  // sloppiest CLI args handling that
  // I have ever written.)
  def run =
    for
      args <- this.getArgs
      (connected, all, graph) <- 
        if (args.contains("--cached"))
          for
            connectedX <- FileIO.readResource[Seq[ConnectedProjectData]]("connectedProjectData.txt")
            allX <- FileIO.readResource[Seq[ProjectMetaData]]("allProjectsMetaData.txt")
            graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allX))
          yield (connectedX, allX, graph)
        else
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
            _ <- FileIO.saveAsResource(connectedProjects, "connectedProjectData.txt")
            _ <- FileIO.saveAsResource(allProjectsMetaData, "allProjectsMetaData.txt")
          yield (connectedProjects, allProjectsMetaData, graph)
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
      ZIO {
        val file = new File(s"src/main/resources/$fileName")
        if (!file.exists()) file.createNewFile()
        val fileWriter = new FileWriter(file)
        fileWriter.write(write(connectedProjects))
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
//    import upickle.default.ReadWriter.join
    val pickled = write(x)
    val depickled = read[T](pickled)
//    Console.printLine(write(x))
    Console.printLine(pprint(x, height = Int.MaxValue))
    
  enum DataView:
    case Dependencies, Dependents, Json, Blockers

  def manipulateAndRender(
      connectedProjects: Seq[ConnectedProjectData],
      sort: ConnectedProjectData => Integer,
      connectionMessage: ConnectedProjectData => String
  ): ZIO[Console, Any, Unit] =
    val currentZioVersion = Version.parse("2.0.0-RC1")
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


object DependencyExplorer extends ZIOAppDefault:

  import com.raquo.laminar.api.L.{*, given}

  // TODO: Rope zio-cli into this thing to make
  // the command line interface The
  // Right Way (TM). (The following may be the
  // sloppiest CLI args handling that
  // I have ever written.)
  def run =
    ZIO.debug("Laminar stuff goes here ZZZ!!")
