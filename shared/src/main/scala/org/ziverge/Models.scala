package org.ziverge

import sttp.model.Uri
import upickle.default.{macroRW, ReadWriter as RW, *}

import java.time.{OffsetDateTime, ZoneId}

case class GithubRepo(org: String, name: String)

case class DependencyProjectUI(group: String, artifactId: String):

  lazy val artifactIdQualifiedWhenNecessary =
    if (!TrackedProjects.coreProjects.contains(this) && artifactId == "zio")
      s"${group}.${artifactId}"
    else
      artifactId

object DependencyProjectUI:

  implicit val rw: RW[DependencyProjectUI] = macroRW

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
  implicit val rw: RW[Project] = macroRW
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

case class VersionedProject(project: Project, version: String):
  val typedVersion = Version(version)

object VersionedProject:
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
    
case class VersionedProjectUI(project: DependencyProjectUI, version: String):
  val typedVersion = Version(version)

object VersionedProjectUI:
  implicit val rw: RW[VersionedProjectUI] = macroRW

case class ProjectMetaDataSmall(project: DependencyProjectUI, typedVersion: Version, zioDep: Option[Version]): // TODO Change zioDep type?
  def onLatestZio(currentZioVersion: Version): Boolean =
    zioDep.fold(true)(zioVersion => zioVersion.compareTo(currentZioVersion) == 0)

object ProjectMetaDataSmall:
  implicit val rw: RW[ProjectMetaDataSmall] = macroRW
  def apply(project: Project, version: String, dependencies: Seq[VersionedProjectUI]): ProjectMetaDataSmall =
    val zioDep: Option[VersionedProjectUI] =
      dependencies
        .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")

    val typedVersion = Version(version)
    val projectUi = DependencyProjectUI(project.group, project.artifactId)
    ProjectMetaDataSmall(projectUi, typedVersion, zioDep.map(_.typedVersion))

  def apply(data: ProjectMetaData): ProjectMetaDataSmall =
    ProjectMetaDataSmall(
      DependencyProjectUI(data.project.group, data.project.artifactId), data.typedVersion, data.zioDep.map(_.typedVersion)
    )

case class ProjectMetaData(project: Project, version: String, dependencies: Seq[VersionedProject]):
  val zioDep: Option[VersionedProject] =
    dependencies
      .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")
  val typedVersion = Version(version)

object ProjectMetaData:
  private def isAZioLibrary(project: VersionedProject) = TrackedProjects.projects.contains(project.project)
  def withZioDependenciesOnly(
      project: VersionedProject,
      dependencies: Seq[VersionedProject]
  ): ProjectMetaData =
    ProjectMetaData(project.project, project.version, dependencies.filter(isAZioLibrary))

  def getUnderlyingZioDep(
      projectMetaData: ProjectMetaData,
      allProjectsMetaData: Seq[ProjectMetaData],
      currentZioVersion: Version
  ): Either[Throwable, Option[ZioDep]] = {
    if (projectMetaData.project.artifactId.contains("parser"))
      println("zio-parser time!")

    projectMetaData.zioDep match
      case Some(value) =>
        Right(Some(ZioDep(zioDep = VersionedProjectUI(
          DependencyProjectUI(value.project.group, value.project.artifactId),
          value.version
          
        ), dependencyType = DependencyType.Direct)))
      case None =>
        if (TrackedProjects.coreProjects.contains(projectMetaData.project))
          Right(
            Some(
              ZioDep(
                zioDep = VersionedProjectUI(DependencyProjectUI(TrackedProjects.zioCore.group, TrackedProjects.zioCore.artifactId), currentZioVersion.value),
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
              Some(ZioDep(
                VersionedProjectUI(
                  DependencyProjectUI(dep.project.group, dep.project.artifactId),
                  dep.version
                )
                , DependencyType.Transitive))
            )
          )
  }
end ProjectMetaData

enum DependencyType:
  case Direct, Transitive

object DependencyType:
  implicit val rw: RW[DependencyType] = macroRW

case class ZioDep(zioDep: VersionedProjectUI, dependencyType: DependencyType)
object ZioDep:
  implicit val rw: RW[ZioDep] = macroRW

case class ConnectedProjectData(
    project: Project,
    version: Version,
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

case class FullAppData(connected: Seq[ConnectedProjectData], currentZioVersion: Version, scalaVersion: ScalaVersion)

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