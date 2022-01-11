enablePlugins(JavaAppPackaging)
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %%% "zio" % "2.0.0-RC1",
    "com.lihaoyi" %%% "pprint" % "0.7.0",
    "com.lihaoyi" %%% "upickle" % "1.4.3",
    ("org.scala-graph" %%% "graph-core" % "1.13.3").cross(CrossVersion.for3Use2_13),
  )
)

mainClass in Compile := Some("org.ziverge.DependencyServer")

name := "ZioEcosystemTracker"
idePackagePrefix := Some("org.ziverge")
libraryDependencies ++= Seq(
  "io.d11" %% "zhttp"      % "1.0.0.0-RC21",
  "com.lihaoyi" %%% "pprint" % "0.7.0",
  "com.lihaoyi" %%% "upickle" % "1.4.3",
  ("com.flowtick" %%% "xmls" % "0.1.11").cross(CrossVersion.for3Use2_13),
  ("com.softwaremill.sttp.client3" %%% "core" % "3.3.18"),
  "dev.zio" %%% "zio" % "1.0.13", // Upgrade once zhttp is migrated to ZIO 2
)
    

dependsOn(shared)

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
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %%% "core" % "3.3.18",
    ),
    sharedSettings
  )

import scala.sys.process._

lazy val fastOptJSCopyToServer = taskKey[Unit]("Build JS application and then copy to Server static resources directory")
fastOptJSCopyToServer := {
  (scalaJsExplorer/Compile/fastOptJS).value
  Process("cp ./scalaJsExplorer/target/scala-3.1.0/zioecosystemtracker-fastopt.js ./src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")!
}

lazy val fullOptJSCopyToServer = taskKey[Unit]("Compile and copy JS app")
fullOptJSCopyToServer := {
  (scalaJsExplorer/Compile/fullOptJS).value
  Process("cp ./scalaJsExplorer/target/scala-3.1.0/zioecosystemtracker-opt/main.js ./src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js")!
}