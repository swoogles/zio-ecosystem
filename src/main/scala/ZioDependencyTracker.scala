package org.ziverge

import sttp.model.Uri
import zio.ZIOAppDefault
import zio.{Chunk, ZIO}
import zio.durationInt
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
    dependants: Set[ProjectMetaData],
    zioDep: Option[ZioDep]
)
object ConnectedProjectData:
  def apply(
      projectMetaData: ProjectMetaData,
      allProjectsMetaData: Seq[ProjectMetaData],
      dependendencyGraph: Graph[String, DiEdge]
  ): ConnectedProjectData =
    // TODO Convert runtime exceptions to values handled by ZIO
    val node =
      dependendencyGraph
        .nodes
        .find(_.value == projectMetaData.project.artifactId)
        .getOrElse(throw new RuntimeException("No node for: " + projectMetaData.project.artifactId))
    val dependents: Set[String] = node.diSuccessors.map(_.value.toString)

    val typedDependants =
      dependents.map(dependent =>
        allProjectsMetaData
          .find(_.project.artifactId == dependent)
          .getOrElse(throw new RuntimeException("Missing metadata for dependent: " + dependent))
      )

    val typedDependencies =
      projectMetaData
        .dependencies
        .map(dependent =>
          allProjectsMetaData
            .find(_.project == dependent.project)
            .getOrElse(throw new RuntimeException("Missing metadata for dependency: " + dependent))
        )

    val zioDep =
      projectMetaData.zioDep match
        case Some(value) =>
          Some(ZioDep(zioDep = value, dependencyType = DependencyType.Direct))
        case None =>
          projectMetaData
            .dependencies
            .flatMap { dependency =>
              val res: ProjectMetaData =
                allProjectsMetaData
                  .find(_.project == dependency.project)
                  .getOrElse(
                    throw new RuntimeException(
                      "Missing metadata entry for " + dependency.project + ". Available: " +
                        allProjectsMetaData.map(_.project).mkString(",")
                    )
                  )
              res.zioDep
            }
            .headOption
            .map(ZioDep(_, DependencyType.Transitive))

    ConnectedProjectData(
      projectMetaData.project,
      Version.parse(projectMetaData.version),
      typedDependencies,
      typedDependants,
      zioDep
    )
  end apply
end ConnectedProjectData

def isAZioLibrary(project: VersionedProject) =
  Data.projects.contains(project.project)

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



object ScalaGraph:
  def apply(allProjectsMetaData: Seq[ProjectMetaData]): Graph[String, DiEdge] =
    Graph(
      allProjectsMetaData.flatMap { project =>
        project
          .dependencies
          .map { dependency =>
            dependency.project.artifactId ~> project.project.artifactId
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
      allProjectsMetaData: Seq[ProjectMetaData] <-
        ZIO.foreachPar(Data.projects) { project =>
          Maven.projectMetaDataFor(project, ScalaVersion.V2_13)
        }
//      allProjectsMetaData: Seq[ProjectMetaData]<- ZIO(Data.sampleProjectsMetaData)
      // I used this to get a persistent version
      // that could be tested against without
      // continously hitting Maven
//      _ <- ZIO(pprint.pprintln(allProjectsMetaData, height = Int.MaxValue))

      graph: Graph[String, DiEdge] <- ZIO(ScalaGraph(allProjectsMetaData))
      _ <-
        args match
          case Chunk("dot") =>
            printLine(DotGraph.render(graph))
          case Chunk("dependents") =>
            connectAndRender(
              allProjectsMetaData,
              graph,
              _.dependants.size,
              p =>
                if (p.dependants.nonEmpty)
                  f"Required by ${p.dependants.size} projects: " +
                    p.dependants.map(_.project.artifactId).mkString(",")
                else
                  "Has no dependents"
            )
          case Chunk("dependencies") =>
            connectAndRender(
              allProjectsMetaData,
              graph,
              _.dependencies.size,
              p =>
                if (p.dependencies.nonEmpty)
                  s"Depends on ${p.dependencies.size} projects: " +
                    p.dependencies.map(_.project.artifactId).mkString(",")
                else
                  "Does not depend on any known ecosystem library."
            )
          case _ =>
            ZIO.fail("Unrecognized CLI arguments")
    yield ()

  def connectAndRender(
      allProjectsMetaData: Seq[ProjectMetaData],
      graph: Graph[String, DiEdge],
      sort: ConnectedProjectData => Integer,
      connectionMessage: ConnectedProjectData => String
  ) =
    for
      connectedProjects <-
        ZIO(
          allProjectsMetaData
            .map(ConnectedProjectData.apply(_, allProjectsMetaData, graph))
            .sortBy(sort)
            .reverse
        )
      _ <-
        printLine(
          connectedProjects
            .map { project =>
              val renderedZioDependency =
                if (
                  List("zio", "zio-streams", "zio-test", "zio-test-sbt")
                    .contains(project.project.artifactId)
                )
                  "is a core project"
                else
                  ZioDep.render(project.zioDep)
              f"${project.project.artifactId}%-30s ${renderedZioDependency} and " +
                connectionMessage(project)
            }
            .mkString("\n")
        )
    yield ()
end ZioDependencyTracker

/* TODO Questions
 * - Where do we want to display versions
 * - Which versions do we want to show?
 * - Eg current dependency version VS what is available
 * - Should we include snapshot releases?
 *
 * Connected Component Datastructure JGraphT.org Calculate longest paths between nodes Topological
 * sort Visit each node Only keep edges that reduce the number of connected components in the graph
 *
 * TODO Use color to indicate if a version of this project has been published that uses ZIO2 */
