package org.ziverge.ecosystem

import org.ziverge.*
import sttp.model.Uri
import upickle.default.{macroRW, ReadWriter as RW, *}

import java.time.{OffsetDateTime, ZoneId}

case class ProjectMetaData(project: Project, typedVersion: Version, dependencies: Seq[VersionedProject]):
  val zioDep: Option[VersionedProject] =
    dependencies
      .find(project => project.project.artifactId == "zio" && project.project.group == "dev.zio")


object ProjectMetaData:
  private def isAZioLibrary(project: VersionedProject) = TrackedProjects.projects.contains(project.project)
  def withZioDependenciesOnly(
                               project: VersionedProject,
                               dependencies: Seq[VersionedProject]
                             ): ProjectMetaData =
    ProjectMetaData(project.project, project.typedVersion, dependencies.filter(isAZioLibrary))

  def apply(data: ProjectMetaData): ProjectMetaDataSmall =
    ProjectMetaDataSmall(
      DependencyProjectUI(data.project.group, data.project.artifactId), data.typedVersion, data.zioDep.map(_.typedVersion)
    )

  def getUnderlyingZioDep(
                           projectMetaData: ProjectMetaData,
                           allProjectsMetaData: Seq[ProjectMetaData],
                           currentZioVersion: Version
                         ): Either[Throwable, Option[ZioDep]] = {
    projectMetaData.zioDep match
      case Some(value) =>
        Right(Some(ZioDep(zioDep = VersionedProjectUI(
          DependencyProjectUI(value.project.group, value.project.artifactId),
          value.typedVersion

        ), dependencyType = DependencyType.Direct)))
      case None =>
        if (TrackedProjects.coreProjects.contains(projectMetaData.project))
          Right(
            Some(
              ZioDep(
                zioDep = VersionedProjectUI(DependencyProjectUI(TrackedProjects.zioCore.group, TrackedProjects.zioCore.artifactId), currentZioVersion),
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
                  dep.typedVersion
                )
                , DependencyType.Transitive))
            )
          )
  }
end ProjectMetaData

case class VersionedProject(project: Project, version: String):
  val typedVersion = Version(version) // TODO Convert to apply method, so that we are never holding the raw String version in this class

object VersionedProject:
  def stripped(project: Project, version: String): VersionedProject =
    ecosystem.VersionedProject(stripScalaVersionFromArtifact(project), version)

  def stripScalaVersionFromArtifact(project: Project): Project =
    ScalaVersion
      .values
      .find(scalaVersion => project.artifactId.endsWith("_" + scalaVersion.mvnFriendlyVersion))
      .map(scalaVersion =>
        project
          .copy(artifactId = project.artifactId.replace("_" + scalaVersion.mvnFriendlyVersion, ""))
      )
      .getOrElse(project)
