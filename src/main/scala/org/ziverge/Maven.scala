package org.ziverge

import sttp.model.Uri
import zio.ZIO

import scala.xml.{Elem, XML}

object Maven:
  private def mavenHttpCall(url: String): ZIO[Any, Throwable, Elem] =
    import sttp.client3.*
    val backend = HttpURLConnectionBackend()
    for
      url <-
        ZIO
          .fromEither(
            Uri.safeApply(
              scheme = "https",
              host = "repo1.maven.org": String,
              path = url.split("\\/").toSeq
            )
          )
          .mapError(new Exception(_))
      r    <- ZIO.attempt(basicRequest.get(url).send(backend))
      body <- ZIO.fromEither(r.body).mapError(new Exception(_))
    yield XML.loadString(body)

  private def latestVersionOfArtifact(
      project: Project,
      scalaVersion: ScalaVersion
  ): ZIO[Any, Throwable, Elem] =
    val urlString =
      s"maven2/${project.groupUrl}/${project.versionedArtifactId(scalaVersion)}/maven-metadata.xml"
    mavenHttpCall(urlString)

  def latestProjectOnMaven(
      project: Project,
      scalaVersion: ScalaVersion
  ): ZIO[Any, Throwable, VersionedProject] =
    for
      latestVersion <-
        latestVersionOfArtifact(project, scalaVersion).mapError(error =>
          new Exception("Failed to get latest version of: " + project.artifactId)
        )
      version       = (latestVersion \ "versioning" \ "latest").text
      latestProject = VersionedProject(project, version)
    yield latestProject

  def projectMetaDataFor(
      project: Project,
      scalaVersion: ScalaVersion
  ): ZIO[Any, Throwable, ProjectMetaData] =
    for
      versionedProject <- latestProjectOnMaven(project, ScalaVersion.V2_13)
      pomFile <-
        pomFor(versionedProject, ScalaVersion.V2_13)
          // TODO better error type
          .mapError(error => new Exception("Failed to get POM for: " + project.artifactId))
    yield ProjectMetaData.withZioDependenciesOnly(versionedProject, dependenciesFor(pomFile))

  def pomFor(project: VersionedProject, scalaVersion: ScalaVersion): ZIO[Any, Throwable, Elem] =
    def pomFile(project: VersionedProject) =
      s"${project.project.versionedArtifactId(scalaVersion)}-${project.version}.pom"

    val fileName = pomFile(project)
    val urlString =
      s"maven2/${project.project.groupUrl}/${project.project.versionedArtifactId(scalaVersion)}/${project
        .version}/${fileName}"
    mavenHttpCall(urlString)

  private def dependenciesFor(pom: Elem) =
    val dependencies = pom \ "dependencies" \ "dependency"
    dependencies
      .map { node =>
        val version = (node \ "version")
          .text
          .replaceAll("\\[", "")
          .replaceAll("\\]", "")
          .replaceAll("\\(", "")
          .replaceAll("\\)", "")
          .split(",")
          .last
        VersionedProject
          .stripped(Project.fromMaven((node \ "groupId").text, (node \ "artifactId").text), version)
      }
      .toSeq
end Maven
