package org.ziverge.ecosystem

import sttp.model.Uri
import upickle.default.{macroRW, ReadWriter as RW, *}

import java.time.{OffsetDateTime, ZoneId}

case class GithubRepo(org: String, name: String)

case class DependencyProjectUI(group: String, artifactId: String):

  lazy val artifactIdQualifiedWhenNecessary: String =
    if (!TrackedProjects.coreProjects.exists(coreProject => coreProject.group == group && coreProject.artifactId == artifactId) && artifactId == "zio")
      s"${group}.${artifactId}"
    else
      artifactId
      
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

  def stripScalaVersionFromArtifact(): Project =
    ScalaVersion
      .values
      .find(scalaVersion => artifactId.endsWith("_" + scalaVersion.mvnFriendlyVersion))
      .map(scalaVersion =>
        copy(artifactId = artifactId.replace("_" + scalaVersion.mvnFriendlyVersion, ""))
      )
      .getOrElse(this)

case class VersionedProjectUI(project: DependencyProjectUI, typedVersion: Version)

case class ProjectMetaDataSmall(project: DependencyProjectUI, typedVersion: Version, zioDep: Option[Version]):
  def onLatestZio(currentZioVersion: Version): Boolean =
    zioDep.fold(true)(zioVersion => zioVersion.compareTo(currentZioVersion) == 0)

enum DependencyType:
  case Direct, Transitive
  
case class ZioDep(zioDep: VersionedProjectUI, dependencyType: DependencyType)

case class ConnectedProjectData(
                                 project: Project,
                                 version: Version,
                                 dependencies: Seq[ProjectMetaDataSmall],
                                 dependants: Seq[ProjectMetaDataSmall],
                                 zioDep: Option[ZioDep],
                                 latestZio: Version,
                                 relevantPr: Option[PullRequest] = None
                               ):
  // TODO Turn into an apply value
  lazy val onLatestZioDep: Boolean =
    zioDep.fold(
      dependencies.forall(dep => dep.onLatestZio(latestZio))
    )(zDep =>
      zDep.zioDep.typedVersion.compareTo(latestZio) == 0
    )
    
case class FullAppData(connected: Seq[ConnectedProjectData], currentZioVersion: Version, scalaVersion: ScalaVersion)
case class PullRequest(number: Int, title: String, html_url: String)

case class ProjectGroup(group: String, projects: Seq[Project])
object ZioDep:
  implicit val rw: RW[ZioDep] = macroRW

object DependencyProjectUI:

  implicit val rw: RW[DependencyProjectUI] = macroRW

object Project:
  implicit val rw: RW[Project] = macroRW
  def fromMaven(groupId: String, artifactId: String): Project =
    val strippedProject =
      Project(groupId, artifactId).stripScalaVersionFromArtifact()
    TrackedProjects
      .projects
      .find(knownProject =>
        knownProject.group == strippedProject.group &&
          knownProject.artifactId == strippedProject.artifactId
      )
      .getOrElse(Project(groupId, artifactId))

    
object VersionedProjectUI:
  implicit val rw: RW[VersionedProjectUI] = macroRW


object ProjectMetaDataSmall:
  implicit val rw: RW[ProjectMetaDataSmall] = macroRW
  def apply(project: Project, version: Version, dependencies: Seq[VersionedProjectUI]): ProjectMetaDataSmall =
    val zioDep: Option[VersionedProjectUI] =
      dependencies
        .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")

    val projectUi = DependencyProjectUI(project.group, project.artifactId)
    ProjectMetaDataSmall(projectUi, version, zioDep.map(_.typedVersion))


object DependencyType:
  implicit val rw: RW[DependencyType] = macroRW

object ConnectedProjectData:
  implicit val versionRw: RW[Version]       = readwriter[String].bimap[Version](_.value, Version(_))
  implicit val rw: RW[ConnectedProjectData] = macroRW

object FullAppData:

  implicit val rw: RW[FullAppData]            = macroRW

end FullAppData

import upickle.default.{macroRW, ReadWriter as RW, *}

object PullRequest:

  implicit val rw: RW[PullRequest] = macroRW