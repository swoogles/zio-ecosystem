package org.ziverge

import sttp.model.Uri
import zio.ZIOAppDefault
import zio.ZIO
import zio.durationInt

import scala.xml.{Elem, XML}

case class Project(group: String, artifactId: String) {
  val groupUrl = group.replaceAll("\\.", "/")
  def versionedArtifactId(scalaVersion: ScalaVersion) =
    artifactId + "_" + scalaVersion.mvnFriendlyVersion
}
enum ScalaVersion(val mvnFriendlyVersion: String) {
  case V2_11 extends ScalaVersion("2.11")
  case V2_12 extends ScalaVersion("2.12")
  case V2_13 extends ScalaVersion("2.13")
  case V3 extends ScalaVersion("3")
}

case class VersionedProject(project: Project, version: String)
object VersionedProject:
  def stripped(project: Project, version: String): VersionedProject =
    VersionedProject(stripScalaVersionFromArtifact(project), version)
    
  private def stripScalaVersionFromArtifact(project: Project): Project =
    ScalaVersion.values.find( scalaVersion => project.artifactId.endsWith("_" + scalaVersion.mvnFriendlyVersion))
      .map( scalaVersion => project.copy(artifactId = project.artifactId.replace("_"+scalaVersion.mvnFriendlyVersion, "")))
      .getOrElse(project)
      
case class ProjectMetaData(project: VersionedProject, dependencies: Set[VersionedProject]) {
  dependencies.find(project => project.project.artifactId == "zio").foreach(x => println(s"${project.project.artifactId} Zio version : " + x.version))
}

def latestVersionOfArtifact(project: Project, scalaVersion: ScalaVersion) = {
  import sttp.client3._
  val backend = HttpURLConnectionBackend()
  val urlString = s"maven2/${project.groupUrl}/${project.versionedArtifactId(scalaVersion)}/maven-metadata.xml"

  for
    url <- ZIO.fromEither(Uri.safeApply(scheme = "https", host = "repo1.maven.org": String, path = urlString.split("\\/").toSeq))
    r <- ZIO {
      basicRequest
        .get(url).send(backend)
    }
    body <- ZIO.fromEither(r.body)
  yield XML.loadString(
    body
  )
  
}

def pomFor(project: VersionedProject, scalaVersion: ScalaVersion) = {
  import sttp.client3._
  val backend = HttpURLConnectionBackend()
  def pomFile(project: VersionedProject) =
    s"${project.project.versionedArtifactId(scalaVersion)}-${project.version}.pom"
  val fileName = pomFile(project)
  val urlString = s"maven2/${project.project.groupUrl}/${project.project.versionedArtifactId(scalaVersion)}/${project.version}/${fileName}"

  for
    url <- ZIO.fromEither(Uri.safeApply(scheme = "https", host = "repo1.maven.org": String, path = urlString.split("\\/").toSeq))
    r <- ZIO {
      basicRequest
        .get(url).send(backend)
    }
    body <- ZIO.fromEither(r.body)
  yield XML.loadString(
    body
  )

}

def latestProjectOnMaven(project: Project, scalaVersion: ScalaVersion) =
  for
    latestVersion <- latestVersionOfArtifact(project, scalaVersion)
    version = (latestVersion \ "versioning" \ "latest").text
    latestProject = VersionedProject(project, version)
  yield latestProject


def dependenciesFor(pom: Elem, scalaVersion: ScalaVersion): Seq[VersionedProject] =
  val dependencies = pom \ "dependencies" \ "dependency"
  dependencies.map( node => VersionedProject.stripped(Project((node \ "groupId").text, (node \ "artifactId").text), (node \ "version").text))
  
def isAZioLibrary(project: VersionedProject) =
  project.project.artifactId.contains("zio") || project.project.group.contains("zio")
  
def renderInSbtStyle(project: VersionedProject) =
  project.project.group + "::" + project.project.artifactId + ":" + project.version

def renderDependencies(projectMetaData: ProjectMetaData) =
  val targetProject = projectMetaData.project
  val dependencies = projectMetaData.dependencies
  "Target Project: " + renderInSbtStyle(targetProject) + "\n" +
  "Dependencies:\n" +
    dependencies
      .map(project => "  " + renderInSbtStyle(project))
      .mkString("\n")
  
def serializeDotGraph(project: ProjectMetaData) =
  project.dependencies.map(dependency => s"""   "${dependency.project.artifactId}" -> "${project.project.project.artifactId}"  ;""").mkString("\n")
  
def serializeDotGraphs(projects: Set[ProjectMetaData]) =
  projects.map(serializeDotGraph).mkString("\n")

val zioCore = Project("dev.zio", "zio_2.13")

def projectMetaDataFor( project: Project, scalaVersion: ScalaVersion ) =
  for
    versionedProject <- latestProjectOnMaven(project, ScalaVersion.V2_13)
    pomFile <- pomFor(versionedProject, ScalaVersion.V2_13)
    zioDeps = dependenciesFor(pomFile, ScalaVersion.V2_13).filter(isAZioLibrary)
  yield ProjectMetaData(versionedProject, zioDeps.toSet)

object ZioDependencyTracker extends ZIOAppDefault:
  val projects = Set(
    Project("dev.zio", "zio-streams"),
    Project("dev.zio", "zio-prelude"),
    Project("dev.zio", "zio-prelude-macros"),
    Project("nl.vroste", "zio-amqp"),
//    Project("dev.zio", "zio-cli"),
    Project("dev.zio", "zio"),
    Project("dev.zio", "zio-json"),
    Project("dev.zio", "zio-query"),
    Project("dev.zio", "zio-schema"),
    Project("dev.zio", "zio-kafka"), // TODO Why isn't this showing up?
    Project("dev.zio", "zio-optics"),
//    Project("dev.zio", "zio-flow"),
    Project("com.github.ghostdogpr", "caliban"),
    Project("io.github.vigoo", "zio-aws-dynamodb"),
    Project("io.github.vigoo", "zio-aws-core"),
    Project("io.d11", "zhttp"),
    Project("dev.zio", "zio-interop-cats"),
    Project("nl/vroste", "rezilience"),
//    Project("com.coralogix", "zio-k8s-client"),
  )
  
  def run =
    for
      allProjectsMetaData <- ZIO.foreach(projects)( project => ZIO.sleep(1.seconds) *> projectMetaDataFor(project, ScalaVersion.V2_13))
//      _ <- zio.Console.printLine(serializeDotGraphs(allProjectsMetaData))
    yield ()

/*
  TODO Questions
    - Where do we want to display versions
    - Which versions do we want to show?
      - Eg current dependency version VS what is available
    - Should we include snapshot releases?

  Connected Component Datastructure
    JGraphT.org
      Calculate longest paths between nodes
    Topological sort
      Visit each node
      Only keep edges that reduce the number of connected components in the graph

  TODO Use color to indicate if a version of this project has been published that uses ZIO2
*/