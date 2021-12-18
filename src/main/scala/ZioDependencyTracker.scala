package org.ziverge

import sttp.model.Uri
import zio.ZIOAppDefault
import zio.ZIO
import zio.durationInt
import zio.Console.printLine
import scalax.collection.Graph // or scalax.collection.mutable.Graph
import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
import scalax.collection.edge.LDiEdge
import scalax.collection.edge.Implicits._ // shortcuts

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
      
case class ProjectMetaData private(project: Project, version: String, dependencies: Set[VersionedProject]):
  val zioDep: Option[VersionedProject] = dependencies.find(project => project.project.artifactId == "zio") // TODO Fix

object ProjectMetaData {
  def withZioDependenciesOnly(project: VersionedProject, dependencies: Set[VersionedProject]): ProjectMetaData =
    ProjectMetaData(project.project, project.version, dependencies.filter(isAZioLibrary))
    
  def renderRow(project: ProjectMetaData): String =
    val renderedZioDependency = project.zioDep.fold("transitive")(_.version)
    val renderedDependencies = project.dependencies.map(dep => dep.project.artifactId).mkString(",")
    f"${project.project.artifactId}%-30s ${"ZIO " + renderedZioDependency}%-20s ${renderedDependencies}"
}

case class ConnectedProjectData private(project: Project, version: String, dependencies: Set[VersionedProject], dependendants: Set[String], zioDep: Option[VersionedProject])
object ConnectedProjectData :
  def apply(projectMetaData: ProjectMetaData, dependendencyGraph: Graph[String, DiEdge]): ConnectedProjectData =
    val node = dependendencyGraph.nodes.find(_.value == projectMetaData.project.artifactId).getOrElse(throw new RuntimeException("No node for: "+ projectMetaData.project.artifactId))
    val dependents: Set[String] =
      node.diSuccessors.map(_.value.toString)
      
    ConnectedProjectData (projectMetaData.project, projectMetaData.version, projectMetaData.dependencies, dependents, projectMetaData.zioDep )


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
  yield XML.loadString(body)
  
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
    latestVersion <- latestVersionOfArtifact(project, scalaVersion).tapError( error => printLine("Failed to get latest version of: " + project.artifactId))
    version = (latestVersion \ "versioning" \ "latest").text
    latestProject = VersionedProject(project, version)
  yield latestProject


def dependenciesFor(pom: Elem, scalaVersion: ScalaVersion): Set[VersionedProject] =
  val dependencies = pom \ "dependencies" \ "dependency"
  dependencies.map( node => VersionedProject.stripped(Project((node \ "groupId").text, (node \ "artifactId").text), (node \ "version").text)).toSet
  
def isAZioLibrary(project: VersionedProject) =
  project.project.artifactId.contains("zio") || project.project.group.contains("zio")
  
def renderInSbtStyle(project: Project, version: String) =
  project.group + "::" + project.artifactId + ":" + version

def renderDependencies(projectMetaData: ProjectMetaData) =
  val targetProject = projectMetaData.project
  val dependencies = projectMetaData.dependencies
  "Target Project: " + renderInSbtStyle(targetProject, projectMetaData.version) + "\n" +
  "Dependencies:\n" +
    dependencies
      .map(project => "  " + renderInSbtStyle(project.project, projectMetaData.version))
      .mkString("\n")
  
def serializeDotGraph(project: ProjectMetaData) =
  project.dependencies.map(dependency => s"""   "${dependency.project.artifactId}" -> "${project.project.artifactId}"  ;""").mkString("\n")
  
def serializeDotGraphs(projects: Set[ProjectMetaData]) =
  projects.map(serializeDotGraph).mkString("\n")

def projectMetaDataFor( project: Project, scalaVersion: ScalaVersion ) =
  for
    versionedProject <- latestProjectOnMaven(project, ScalaVersion.V2_13)
    pomFile <- pomFor(versionedProject, ScalaVersion.V2_13).tapError( error => printLine("Failed to get POM for: " + project.artifactId))
  yield ProjectMetaData.withZioDependenciesOnly(versionedProject, dependenciesFor(pomFile, ScalaVersion.V2_13))
  
def renderGraph(graph: Graph[String, DiEdge]): String =
  graph.edges.map(_.toString.replace("~","-")).mkString("\n")

object ZioDependencyTracker extends ZIOAppDefault:
  val projects = List(
    Project("dev.zio", "zio"),
    Project("dev.zio", "zio-cache"),
    Project("dev.zio", "zio-test-sbt"),
    Project("com.github.ghostdogpr", "caliban"),
    Project("dev.zio", "zio-optics"),
    Project("dev.zio", "zio-streams"),
    Project("dev.zio", "zio-json"),
    Project("dev.zio", "zio-query"),
    Project("dev.zio", "zio-schema"),
    Project("dev.zio", "zio-config"),
    Project("dev.zio", "zio-kafka"),
    Project("dev.zio", "zio-ftp"),
    Project("io.github.vigoo", "zio-aws-dynamodb"),
    Project("io.github.vigoo", "zio-aws-core"),
    Project("io.github.vigoo", "zio-aws-core"),
    Project("dev.zio", "zio-prelude"),
    Project("dev.zio", "zio-prelude-macros"),
    Project("dev.zio", "zio-interop-reactivestreams"),
    Project("dev.zio", "zio-interop-scalaz7x"),
    Project("dev.zio", "zio-interop-twitter"),
    Project("nl.vroste", "zio-amqp"),
    Project("dev.zio", "zio-interop-guava"),
    Project("io.7mind.izumi", "distage-core"),
    Project("io.7mind.izumi", "logstage-core"),
    Project("com.github.poslegm", "munit-zio"),
    Project("com.coralogix", "zio-k8s-client"),
    Project("com.softwaremill.sttp.client3", "async-http-client-backend-zio"), // todo wrong zio version 3.3.18
    Project("io.d11", "zhttp"),
    Project("dev.zio", "zio-interop-cats"),
    Project("dev.zio", "zio-nio"),
    Project("dev.zio", "zio-opentracing"),
    Project("dev.zio", "zio-zmx"),
    Project("dev.zio", "zio-actors"),
    Project("dev.zio", "zio-logging"),
    Project("dev.zio", "zio-metrics"),
    Project("dev.zio", "zio-process"),
    Project("dev.zio", "zio-akka-cluster"),
    
    Project("dev.zio", "zio-rocksdb"),
    Project("dev.zio", "zio-s3"),
    Project("dev.zio", "zio-opencensus"),
    Project("dev.zio", "zio-opentelemetry"),
    Project("dev.zio", "zio-opentracing"),
    Project("io.github.ollls", "zio-tls-http"),
    Project("com.vladkopanev", "zio-saga-core"),
    Project("io.scalac", "zio-slick-interop"),
    Project("dev.zio", "zio-sqs"),
    Project("dev.zio", "zio-webhooks"),
    
//    Project("com.github.jczuchnowski", "zio-pulsar"), // Scala 3 Only
    Project("nl/vroste", "rezilience"),
    Project("nl/vroste", "zio-kinesis"),
    Project("io.getquill", "quill-jdbc-zio"),
    Project("io.github.gaelrenoux", "tranzactio"),
    Project("info.senia", "zio-test-akka-http"),
    Project("io.github.neurodyne", "zio-arrow"),
    Project("io.github.neurodyne", "zio-aws-s3"),
  ).sortBy(_.artifactId)
  
  def run =
    for
      allProjectsMetaData <- ZIO.foreachPar(projects)( project => projectMetaDataFor(project, ScalaVersion.V2_13))
      graph: Graph[String, DiEdge] <- ZIO {
        Graph(
        allProjectsMetaData
          .flatMap { project =>
            project.dependencies.map {
              dependency => dependency.project.artifactId ~> project.project.artifactId
            }

          }*
        )
      }
      _ <- printLine(renderGraph(graph))
      node <- ZIO(graph.nodes.map(node => node.diPredecessors).mkString(","))
      connectedProjects = allProjectsMetaData.map(ConnectedProjectData.apply(_, graph)).sortBy(_.dependendants.size).reverse
      _ <- printLine(connectedProjects
        .map{project=>
          val renderedZioDependency = 
            if (List("zio", "zio-streams", "zio-test", "zio-test-sbt").contains(project.project.artifactId))
              "is a core project"
            else
              project.zioDep
                .fold("has a transitive ZIO dependency")(dep => "is on ZIO " + dep.version)
          if (project.dependendants.nonEmpty)
            f"${project.project.artifactId}%-30s ${renderedZioDependency} and required by ${project.dependendants.size} projects: " + project.dependendants.mkString(",")
          else
            f"${project.project.artifactId}%-30s ${renderedZioDependency} and has no dependants."
          }.mkString("\n")
      )
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