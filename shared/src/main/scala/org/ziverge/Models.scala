package org.ziverge

import sttp.model.Uri
import upickle.default.{macroRW, ReadWriter as RW, *}

import java.time.{OffsetDateTime, ZoneId}

case class GithubRepo(org: String, name: String)

class Models {}
case class Project(group: String, artifactId: String, githubUrl: Option[String] = None):
  val groupUrl = group.replaceAll("\\.", "/")
  def versionedArtifactId(scalaVersion: ScalaVersion) =
    artifactId + "_" + scalaVersion.mvnFriendlyVersion
  lazy val artifactIdQualifiedWhenNecessary =
    if (!TrackedProjects.coreProjects.contains(this) && artifactId == "zio")
      s"${group}.${artifactId}"
    else
      artifactId
  val githubOrgAndRepo: Option[GithubRepo] =
    githubUrl.map { url =>
      val pieces = url.stripPrefix("https://github.com/").split("/")
      GithubRepo(pieces(0), pieces(1))
    }

  def sbtDependency(version: Version) =
    s""" "${group}" %% "${artifactId}" % "${version.renderForWeb}" """

object Project:
  def fromMaven(groupId: String, artifactId: String): Project =
    val strippedProject =
      VersionedProject.stripScalaVersionFromArtifact(Project(groupId, artifactId))
    TrackedProjects
      .projects
      .find(knownProject =>
        knownProject.group == strippedProject.group &&
          knownProject.artifactId == strippedProject.artifactId
      )
      .getOrElse(Project(groupId, artifactId))
  implicit val rw: RW[Project] = macroRW

case class VersionedProject(project: Project, version: String):
  val typedVersion = Version(version)

object VersionedProject:
  implicit val rw: RW[VersionedProject] = macroRW
  def stripped(project: Project, version: String): VersionedProject =
    VersionedProject(stripScalaVersionFromArtifact(project), version)

  def stripScalaVersionFromArtifact(project: Project): Project =
    ScalaVersion
      .values
      .find(scalaVersion => project.artifactId.endsWith("_" + scalaVersion.mvnFriendlyVersion))
      .map(scalaVersion =>
        project
          .copy(artifactId = project.artifactId.replace("_" + scalaVersion.mvnFriendlyVersion, ""))
      )
      .getOrElse(project)

case class ProjectMetaDataSmall(project: Project, typedVersion: Version, zioDep: Option[VersionedProject]):
  def onLatestZio(currentZioVersion: Version): Boolean =
    zioDep.fold(true)(zDep => zDep.typedVersion.compareTo(currentZioVersion) == 0)

object ProjectMetaDataSmall:
  implicit val rw: RW[ProjectMetaDataSmall] = macroRW
  def apply(project: Project, version: String, dependencies: Seq[VersionedProject]): ProjectMetaDataSmall =
    val zioDep: Option[VersionedProject] =
      dependencies
        .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")

    val typedVersion = Version(version)
    ProjectMetaDataSmall(project, typedVersion, zioDep)

  def apply(data: ProjectMetaData): ProjectMetaDataSmall =
    ProjectMetaDataSmall(
      data.project, data.typedVersion, data.zioDep
    )

case class ProjectMetaData(project: Project, version: String, dependencies: Seq[VersionedProject]):
  val zioDep: Option[VersionedProject] =
    dependencies
      .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")
  val typedVersion = Version(version)

  def onLatestZio(currentZioVersion: Version): Boolean =
    zioDep.fold(true)(zDep => zDep.typedVersion.compareTo(currentZioVersion) == 0)

object ProjectMetaData:
  implicit val rw: RW[ProjectMetaData] = macroRW
  def withZioDependenciesOnly(
      project: VersionedProject,
      dependencies: Seq[VersionedProject]
  ): ProjectMetaData =
    ProjectMetaData(project.project, project.version.toString, dependencies.filter(isAZioLibrary))

  def getUnderlyingZioDep(
      projectMetaData: ProjectMetaData,
      allProjectsMetaData: Seq[ProjectMetaData],
      currentZioVersion: Version
  ): Either[Throwable, Option[ZioDep]] = {
    if (projectMetaData.project.artifactId.contains("parser"))
      println("zio-parser time!")

    projectMetaData.zioDep match
      case Some(value) =>
        Right(Some(ZioDep(zioDep = value, dependencyType = DependencyType.Direct)))
      case None =>
        if (TrackedProjects.coreProjects.contains(projectMetaData.project))
          Right(
            Some(
              ZioDep(
                zioDep = VersionedProject(TrackedProjects.zioCore, currentZioVersion.value),
                dependencyType = DependencyType.Direct
              )
            )
          )
        else
          Right(
            projectMetaData
              // TODO Remove this usage of dependencies and access a new field that provides the same info
              .dependencies
              .find( dep =>
                TrackedProjects.coreProjects.contains(dep.project)
              ).flatMap( dep =>
              Some(ZioDep(dep, DependencyType.Transitive))
            )
          )
  }
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
    // These 2 fields keep track of a whole extra layer of dependencies that we *do not need*
    // TODO Change to smaller type
    dependencies: Seq[ProjectMetaDataSmall],
    dependants: Seq[ProjectMetaDataSmall],
    zioDep: Option[ZioDep],
    latestZio: Version,
    relevantPr: Option[PullRequest] = None
):
  lazy val onLatestZioDep: Boolean =
    zioDep.fold(
      dependencies.forall(dep => dep.onLatestZio(latestZio))
    )(zDep =>
      zDep.zioDep.typedVersion.compareTo(latestZio) == 0
    )

object ConnectedProjectData:
  implicit val versionRw: RW[Version]       = readwriter[String].bimap[Version](_.value, Version(_))
  implicit val rw: RW[ConnectedProjectData] = macroRW

end ConnectedProjectData

def isAZioLibrary(project: VersionedProject) = TrackedProjects.projects.contains(project.project)

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
    s""" "${project.group}" %% "${project.artifactId}" % "${version.renderForWeb}" """

  def sbtStyle(project: Project) = project.group + "::" + project.artifactId


case class FullAppData(connected: Seq[ConnectedProjectData], currentZioVersion: Version, scalaVersion: ScalaVersion)
case class FullAppDataLegacy(connected: Seq[ConnectedProjectData], graph: String, currentZioVersion: Version, scalaVersion: ScalaVersion)

object FullAppData:

  implicit val scalaVersion: RW[ScalaVersion] = macroRW
  implicit val rw: RW[FullAppData]            = macroRW

  def filterData(
      fullAppData: FullAppData,
      dataView: DataView,
      filterUpToDateProjects: Boolean,
      userFilterFromPage: Option[String]
  ) =
    import org.ziverge.DataView.*
    val onLatestZioConnected: ConnectedProjectData => Boolean =
      p =>
        p.zioDep
          .fold(true) { zDep =>
            println("Comparing")
            val res =
              zDep.zioDep.typedVersion.compareTo(fullAppData.currentZioVersion) <
                0 // TODO Fix comparison?
            println("Compared")
            res
          }

    val userFilter: ConnectedProjectData => Boolean =
      userFilterFromPage match
        case Some(filter) =>
          project =>

            val normalizedFilter = filter.toLowerCase

            val artifactMatches = project.project.artifactId.toLowerCase.contains(normalizedFilter)
            // TODO Make this a function in a better spot
            // project.dependants.exists(_.project.artifactId.contains(filter)) ||
            val introspectedDataMatches =
              project
                .dependencies
                .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter)) ||
                project
                  .dependants
                  .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter))
            artifactMatches || introspectedDataMatches
        case None =>
          project => true

    val upToDate: ConnectedProjectData => Boolean =
      p =>
        if (filterUpToDateProjects)
          !p.onLatestZioDep && !TrackedProjects.coreProjects.contains(p.project)
        else
          true

    fullAppData.connected.filter(p => upToDate(p) && userFilter(p))
  end filterData
end FullAppData

import upickle.default.{macroRW, ReadWriter as RW, *}

case class PullRequest(number: Int, title: String, html_url: String)
object PullRequest:

  implicit val rw: RW[PullRequest] = macroRW

case class ProjectGroup(group: String, projects: Seq[Project])