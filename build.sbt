ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    "dev.zio" %%% "zio" % "2.0.0-RC1",
    //      "org.scala-lang.modules" %%% "scala-xml" % "2.0.1",
    // https://mvnrepository.com/artifact/com.flowtick/xmls
    "com.lihaoyi" %%% "pprint" % "0.7.0",
    "com.lihaoyi" %%% "upickle" % "1.4.3",
    ("org.scala-graph" %%% "graph-core" % "1.13.3").cross(CrossVersion.for3Use2_13),
  )
)


lazy val root = (project in file("."))
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      ("com.softwaremill.sttp.client3" %%% "core" % "3.3.18"),
      ("com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.3.18"),
    ),
//    mainClass in Compile:= Some("org.ziverge.DependencyExplorer"),
//    (mainClass in FastOptStage) := Some("org.ziverge.DependencyExplorerX")
    
  ).settings(sharedSettings).dependsOn(shared)

lazy val server = (project in file("server"))
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      "io.d11" %% "zhttp"      % "1.0.0.0-RC21",
      "com.lihaoyi" %%% "pprint" % "0.7.0",
      "com.lihaoyi" %%% "upickle" % "1.4.3",
      ("com.flowtick" %%% "xmls" % "0.1.11").cross(CrossVersion.for3Use2_13),
      ("org.scala-graph" %%% "graph-core" % "1.13.3").cross(CrossVersion.for3Use2_13),
      ("com.softwaremill.sttp.client3" %%% "core" % "3.3.18"),
      // ("com.softwaremill.sttp.client3" %%% "httpclient-backend-zio" % "3.3.18"),

      "dev.zio" %%% "zio" % "1.0.13",
      // ("com.softwaremill.sttp.client3" %%% "core" % "3.3.18"),
      // ("com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.3.18"),
    ),
//    mainClass in Compile:= Some("org.ziverge.DependencyExplorer"),
//    (mainClass in FastOptStage) := Some("org.ziverge.DependencyExplorerX")
    
  ) .dependsOn(shared)

lazy val scalaJsExplorer = (project in file("scalaJsExplorer"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      //      "com.raquo" %%% "laminar" % "0.14.2",
      ("com.flowtick" %%% "xmls" % "0.1.11").cross(CrossVersion.for3Use2_13),
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

    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %%% "core" % "3.3.18",
    ),
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