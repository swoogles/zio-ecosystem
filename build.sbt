ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %%% "zio" % "2.0.0-RC1",
    "com.softwaremill.sttp.client3" %%% "core" % "3.3.18",
    //      "org.scala-lang.modules" %%% "scala-xml" % "2.0.1",
    // https://mvnrepository.com/artifact/com.flowtick/xmls
    ("com.flowtick" %%% "xmls" % "0.1.11").cross(CrossVersion.for3Use2_13),
    "com.lihaoyi" %%% "pprint" % "0.7.0",
    "io.circe" %%% "circe-core" % "0.15.0-M1",
    "io.circe" %%% "circe-generic" % "0.15.0-M1",
    "com.lihaoyi" %%% "upickle" % "1.4.3",
    ("org.scala-graph" %%% "graph-core" % "1.13.3").cross(CrossVersion.for3Use2_13),
  )
)


lazy val root = (project in file("."))
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(),
//    mainClass in Compile:= Some("org.ziverge.DependencyExplorer"),
//    (mainClass in FastOptStage) := Some("org.ziverge.DependencyExplorerX")
    
  ).settings(sharedSettings).dependsOn(shared)

lazy val scalaJsExplorer = (project in file("scalaJsExplorer"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      //      "com.raquo" %%% "laminar" % "0.14.2",
      "com.raquo" %%% "laminar" % "0.14.2",
      "com.raquo" %%% "waypoint" % "0.5.0",
    ),
    scalaJSUseMainModuleInitializer := true,
    //    mainClass in Compile:= Some("org.ziverge.DependencyExplorer"),
    //    (mainClass in FastOptStage) := Some("org.ziverge.DependencyExplorerX")

  ).settings(sharedSettings).dependsOn(shared)

lazy val shared = (project in file("shared"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    sharedSettings
  )
lazy val cbBuild = taskKey[Unit]("Execute the shell script")

cbBuild := {
  (scalaJsExplorer/Compile/fastOptJS).value
//  (Compile/scalafmt).value
  import scala.sys.process._
  //  "ls ./target/scala-2.13" !
  (
//    Process("mkdir ./src/main/resources/compiledJavascript") #||
    Process("cp /home/bfrasure/Repositories/ziverge-hack-day-dec-2021/scalaJsExplorer/target/scala-3.1.0/zioecosystemtracker-fastopt.js ./src/main/resources/compiledJavascript/zioecosystemtracker-fastopt.js") #||
    Process("echo \"there\"")
    )!
}