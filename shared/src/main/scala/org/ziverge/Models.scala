package org.ziverge

import sttp.model.Uri
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt}
import zio.Console.printLine
import scalax.collection.Graph
import scalax.collection.GraphPredef.*
import scalax.collection.GraphEdge.*
import scalax.collection.edge.LDiEdge
import scalax.collection.edge.Implicits.*
import upickle.default.{macroRW, ReadWriter as RW, *}

import java.time.{OffsetDateTime, ZoneId}

class Models {}
case class Project(group: String, artifactId: String):
  val groupUrl = group.replaceAll("\\.", "/")
  def versionedArtifactId(scalaVersion: ScalaVersion) =
    artifactId + "_" + scalaVersion.mvnFriendlyVersion

object Project:
  implicit val rw: RW[Project] = macroRW

case class VersionedProject(project: Project, version: String):
  val typedVersion = Version(version)

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
  val typedVersion = Version(version)

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

case class ConnectedProjectData(
    project: Project,
    version: Version,
    dependencies: Set[ProjectMetaData],
    blockers: Set[ProjectMetaData],
    dependants: Set[ProjectMetaData],
    zioDep: Option[ZioDep]
)
object ConnectedProjectData:
  implicit val versionRw: RW[Version] = readwriter[String].bimap[Version](_.toString, Version(_))
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
              Version.compareVersions(value, currentZioVersion) < 0
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

case class FullAppData(
    connected: Seq[ConnectedProjectData],
    all: Seq[ProjectMetaData],
    graph: Graph[Project, DiEdge]
)
