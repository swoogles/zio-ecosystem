Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"

val zioVersion = "2.0.0-RC6"

lazy val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %%% "zio" % zioVersion,
    "dev.zio" %%% "zio-test" % zioVersion % "test",
    "dev.zio" %%% "zio-test-sbt" % zioVersion % "test",
    "com.lihaoyi" %%% "pprint" % "0.7.0",
    "com.lihaoyi" %%% "upickle" % "1.4.3",
    ("org.scala-graph" %%% "graph-core" % "1.13.3").cross(CrossVersion.for3Use2_13),
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
)

lazy val server = (project in file("server"))
  .settings(

    mainClass in Compile := Some("org.ziverge.DependencyServer"),
//    mainClass := Some("org.ziverge.DependencyServer"),

    name := "ZioEcosystemTracker",
//    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      "io.d11" %% "zhttp"      % "2.0.0-RC9",
      "com.lihaoyi" %%% "pprint" % "0.7.0",
      "com.lihaoyi" %%% "upickle" % "1.4.3",
      ("com.flowtick" %%% "xmls" % "0.1.11").cross(CrossVersion.for3Use2_13),
      ("com.softwaremill.sttp.client3" %%% "core" % "3.3.18"),
      "dev.zio" %%% "zio" % zioVersion, // Upgrade once zhttp is migrated to ZIO 2
      "dev.zio" %%% "zio-test" % zioVersion % "test",
      "dev.zio" %%% "zio-test-sbt" % zioVersion % "test",
    ),


  ).dependsOn(shared)
  .enablePlugins(JavaAppPackaging)

enablePlugins(JavaAppPackaging)

lazy val scalaJsExplorer = (project in file("scalaJsExplorer"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "0.14.2",
      "com.raquo" %%% "waypoint" % "0.5.0",
    ),
    scalaJSUseMainModuleInitializer := true,

  ).settings(sharedSettings).dependsOn(shared)

lazy val shared = (project in file("shared"))
//  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %%% "core" % "3.3.18",
    ),
    sharedSettings
  )

lazy val root = (project in file("."))
  .settings(
    mainClass in Compile := Some("org.ziverge.DependencyServer"),
//    mainClass := Some("org.ziverge.DependencyServer"),
    name := "ZioEcosystemTracker",
  )
  .aggregate(
    server,
    scalaJsExplorer,
    shared
  ).dependsOn(server)
  .enablePlugins(JavaAppPackaging)

import sbt.Keys.name

import scala.sys.process._

lazy val fastOptJSCopyToServer = taskKey[Unit]("Build JS application and then copy to Server static resources directory")
fastOptJSCopyToServer := {
  (scalaJsExplorer/Compile/fastOptJS).value
  println("doing stuff")
  Process("cp ./scalaJsExplorer/target/scala-3.1.0/zioecosystemtracker-fastopt.js ./src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")!
}

lazy val fastLinkJSCopyToServer = taskKey[Unit]("Build JS application and then copy to Server static resources directory")
fastLinkJSCopyToServer := {
  (scalaJsExplorer/Compile/fastLinkJS).value
  Process("cp ./scalaJsExplorer/target/scala-3.1.0/zioecosystemtracker-fastopt.js ./src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")!
}

lazy val fullOptJSCopyToServer = taskKey[Unit]("Compile and copy JS app")
fullOptJSCopyToServer := {
  (scalaJsExplorer/Compile/fullOptJS).value
  Process("cp ./scalaJsExplorer/target/scala-3.1.0/zioecosystemtracker-opt/main.js ./src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")!
}