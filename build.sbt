ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "ZioEcosystemTracker",
    idePackagePrefix := Some("org.ziverge"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.0-RC1",
      "com.softwaremill.sttp.client3" %% "core" % "3.3.18",
      "org.scala-lang.modules" %% "scala-xml" % "2.0.1",
      ("org.scala-graph" %% "graph-core" % "1.13.3").cross(CrossVersion.for3Use2_13)
    )
    
  )
