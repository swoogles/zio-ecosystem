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
  lazy val artifactIdQualifiedWhenNecessary =
    if (!Data.coreProjects.contains(this) && artifactId == "zio")
      s"${group}.${artifactId}"
    else
      artifactId

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
  ): Either[Throwable, Option[ZioDep]] =
    projectMetaData.zioDep match
      case Some(value) =>
        Right(Some(ZioDep(zioDep = value, dependencyType = DependencyType.Direct)))
      case None =>
        val zioDeps: Set[VersionedProject] =
          projectMetaData
            .dependencies
            .flatMap(dependency =>
              allProjectsMetaData.find(_.project == dependency.project).flatMap(_.zioDep)
            )
        val res: Option[VersionedProject] =
          zioDeps
            // .flatten
            .minByOption(_.typedVersion)
        Right(res.map(project => Some(ZioDep(project, DependencyType.Transitive))).getOrElse(None))
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
  ): Either[Throwable, ConnectedProjectData] = // TODO More specific error type
    for
      node <-
        dependendencyGraph
          .nodes
          .find(_.value == projectMetaData.project)
          .toRight(new Exception("Missing value in dependency graph"))
      dependents = node.diSuccessors.map(_.value)
      typedDependants <-
        Right(
          dependents.flatMap(dependent =>
            allProjectsMetaData
              .find(_.project == dependent)
              .toRight(new Exception("Missing projects metadata entry"))
              .toSeq
          )
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
      typedDependencies.toSet,
      blockers.toSet,
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
    graph: String,
    currentZioVersion: Version,
    scalaVersion: ScalaVersion
)

object FullAppData:

  // implicit val graphRw: RW[Graph[org.ziverge.Project, scalax.collection.GraphEdge.DiEdge]] =
  // macroRW
  // scalax.collection.Graph[org.ziverge.Project, scalax.collection.GraphEdge.DiEdge]
  implicit val scalaVersion: RW[ScalaVersion] = macroRW
  implicit val rw: RW[FullAppData]            = macroRW


  def filterData(fullAppData: FullAppData, dataView: DataView, filterUpToDateProjects: Boolean, userFilterFromPage: Option[String]) =
    import org.ziverge.DataView.*
    val onLatestZioConnected: ConnectedProjectData => Boolean =
      p => p.zioDep
            .fold(true)(zDep =>
              zDep.zioDep.typedVersion.compareTo(fullAppData.currentZioVersion) < 0 // TODO Fix comparison?
            )

    val filterCoreProjects: ConnectedProjectData => Boolean =
      p => !Data.coreProjects.contains(p.project)

    val userFilter: ConnectedProjectData => Boolean =
      userFilterFromPage match
        case Some(filter) =>
          project =>

            val normalizedFilter = filter.toLowerCase

            val artifactMatches =
              project.project.artifactId.toLowerCase.contains(normalizedFilter)
            // TODO Make this a function in a better spot
            // project.dependants.exists(_.project.artifactId.contains(filter)) ||
            val introspectedDataMatches = 
              dataView match
                case Dependencies =>
                    project
                      .dependencies
                      .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter))
                case Dependents =>
                    project
                      .dependants
                      .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter))
                case Blockers =>
                    project
                      .blockers
                      .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter))
            artifactMatches && introspectedDataMatches
        case None =>
          project => true

    val upToDate: ConnectedProjectData => Boolean =
      p =>
        if (filterUpToDateProjects)
          p.blockers.nonEmpty ||
          onLatestZioConnected(p) && !Data.coreProjects.contains(p.project)
        else
          true && !Data.coreProjects.contains(p.project)


    fullAppData
      .connected
      .filter(p => upToDate(p) && userFilter(p) && filterCoreProjects(p))