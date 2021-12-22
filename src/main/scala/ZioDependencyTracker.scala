package org.ziverge

import sttp.model.Uri
import zio.{Chunk, Console, ZIO, ZIOAppDefault, durationInt}
import zio.Console.printLine
import scalax.collection.Graph
import scalax.collection.GraphPredef.*
import scalax.collection.GraphEdge.*
import scalax.collection.edge.LDiEdge
import scalax.collection.edge.Implicits.*

import java.lang.module.ModuleDescriptor.Version
import scala.xml.{Elem, XML}

case class Project(group: String, artifactId: String):
  val groupUrl = group.replaceAll("\\.", "/")
  def versionedArtifactId(scalaVersion: ScalaVersion) =
    artifactId + "_" + scalaVersion.mvnFriendlyVersion

case class VersionedProject(project: Project, version: String):
  val typedVersion = Version.parse(version)

object VersionedProject:
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

case class ZioDep(zioDep: VersionedProject, dependencyType: DependencyType)
object ZioDep:

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
    val currentZioVersion = Version.parse("2.0.0-RC1")
    for
      args <- this.getArgs
      allProjectsMetaData: Seq[ProjectMetaData] <-
        ZIO.foreachPar(Data.projects) { project =>
          Maven.projectMetaDataFor(project, ScalaVersion.V2_13)
        }
//      allProjectsMetaData: Seq[ProjectMetaData]<- ZIO(Data.sampleProjectsMetaData)
      // I used this to get a persistent version
      // that could be tested against without
      // continously hitting Maven
//      _ <- ZIO(pprint.pprintln(allProjectsMetaData, height = Int.MaxValue))
      filteredProjects = allProjectsMetaData
//          .filter(p => p.project.artifactId != "zio" || Data.coreProjects.contains(p.project))

      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allProjectsMetaData))
      connectedProjects: Seq[ConnectedProjectData] <-
        ZIO.foreach(filteredProjects)(
          ConnectedProjectData(_, allProjectsMetaData, graph, currentZioVersion)
        )
      _ <-
        args match
          case Chunk("json") =>
            printLine(Json.render(connectedProjects))
          case Chunk("dot") =>
            printLine(DotGraph.render(graph))
          case Chunk("dependents") =>
            manipulateAndRender(
              connectedProjects,
              _.dependants.size,
              p =>
                if (p.dependants.nonEmpty)
                  f"Required by ${p.dependants.size} projects: " +
                    p.dependants.map(_.project.artifactId).mkString(",")
                else
                  "Has no dependents"
            )
          case Chunk("dependencies") =>
            manipulateAndRender(
              connectedProjects,
              _.dependencies.size,
              p =>
                if (p.dependencies.nonEmpty)
                  s"Depends on ${p.dependencies.size} projects: " +
                    p.dependencies.map(_.project.artifactId).mkString(",")
                else
                  "Does not depend on any known ecosystem library."
            )
          case Chunk("blockers") =>
            manipulateAndRender(
              connectedProjects,
              _.blockers.size,
              p =>
                if (p.blockers.nonEmpty)
                  s"is blocked by ${p.blockers.size} projects: " +
                    p.blockers.map(blocker => Render.sbtStyle(blocker.project)).mkString(",")
                else
                  "Is not blocked by any known ecosystem library."
            )
          case _ =>
            ZIO.fail("Unrecognized CLI arguments")
    yield ()
    end for
  end run

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
              f"${Render.sbtStyle(project.project, project.version)}%-50s ${renderedZioDependency} and " +
                connectionMessage(project)
            }.mkString("\n")
        )
    yield ()
    end for
  end manipulateAndRender
end ZioDependencyTracker
/* TODO Cli Options
 * --include-core-deps
 * --dotfile
 * --include-version-deps
 * --targetProject */

/* TODO Questions
 *
 * Connected Component Datastructure JGraphT.org Calculate longest paths between nodes Topological
 * sort Visit each node Only keep edges that reduce the number of connected components in the graph
 *
 * TODO Use color to indicate if a version of this project has been published that uses ZIO2 */
