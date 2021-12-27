package org.ziverge

import scala.collection.immutable.HashSet

object Data:
  val zioCore = Project("dev.zio", "zio")
  val coreProjects =
    List(
      zioCore,
      Project("dev.zio", "zio-test"),
      Project("dev.zio", "zio-test-sbt"),
      Project("dev.zio", "zio-test-magnolia"),
      Project("dev.zio", "zio-internal-macros"),
      Project("dev.zio", "zio-stacktracer"),
      Project("dev.zio", "izumi-reflect"),
      Project("dev.zio", "zio-streams"),
      Project("dev.zio", "izumi-reflect-thirdparty-boopickle-shaded")
    )

  val projects =
    coreProjects ++
      List(
        Project("dev.zio", "zio-cache"),
        Project("com.github.ghostdogpr", "caliban"),
        Project("dev.zio", "zio-optics"),
        Project("dev.zio", "zio-json"),
        Project("dev.zio", "zio-query"),
        Project("dev.zio", "zio-schema"),
        Project("dev.zio", "zio-config"),
        Project("dev.zio", "zio-config-typesafe"),
        Project("dev.zio", "zio-kafka"),
//        Project("dev.zio", "zio-ftp"),
        Project("io.github.vigoo", "zio-aws-core"),
        Project("dev.zio", "zio-prelude"),
        Project("dev.zio", "zio-prelude-macros"),
//        Project("dev.zio", "zio-interop-reactivestreams"),
//        Project("dev.zio", "zio-interop-scalaz7x"),
//        Project("dev.zio", "zio-interop-twitter"),
//        Project("nl.vroste", "zio-amqp"),
//        Project("dev.zio", "zio-interop-guava"),
//        Project("io.7mind.izumi", "distage-core"),
//        Project("io.7mind.izumi", "logstage-core"),
//        Project("com.github.poslegm", "munit-zio"),
//        Project("com.coralogix", "zio-k8s-client"),
//        Project("com.softwaremill.sttp.client3", "zio"),
//        Project("com.softwaremill.sttp.client3", "httpclient-backend-zio"),
//        Project("com.softwaremill.sttp.client3", "async-http-client-backend-zio"),
//        Project("com.softwaremill.sttp.shared", "zio"),
        Project("io.d11", "zhttp"),
        Project("dev.zio", "zio-interop-cats"),
        Project("dev.zio", "zio-nio"),
        Project("dev.zio", "zio-zmx"),
        Project("dev.zio", "zio-actors"),
        Project("dev.zio", "zio-logging"),
        Project("dev.zio", "zio-metrics"),
        Project("dev.zio", "zio-process"),
        Project("dev.zio", "zio-akka-cluster"),
//        Project("dev.zio", "zio-rocksdb"),
//        Project("dev.zio", "zio-s3"),
//        Project("dev.zio", "zio-opencensus"),
//        Project("dev.zio", "zio-opentelemetry"),
//        Project("dev.zio", "zio-opentracing"),
//        Project("io.github.ollls", "zio-tls-http"),
//        Project("com.vladkopanev", "zio-saga-core"),
//        Project("io.scalac", "zio-slick-interop"),
//        Project("dev.zio", "zio-sqs"),
//        Project("dev.zio", "zio-webhooks"),
        // Project("com.github.jczuchnowski",
        // "zio-pulsar"), // Scala 3 Only
        Project("nl.vroste", "rezilience"),
        Project("nl.vroste", "zio-kinesis"),
        Project("io.getquill", "quill-zio"),
        Project("io.getquill", "quill-jdbc-zio"),
//        Project("io.github.gaelrenoux", "tranzactio"),
//        Project("info.senia", "zio-test-akka-http"),
        Project("io.github.neurodyne", "zio-arrow")
//        Project("io.github.neurodyne", "zio-aws-s3")
      ).sortBy(_.artifactId)

  val awsSubprojects =
    List(
      Project("io.github.vigoo", "zio-aws-dynamodb"),
      Project("io.github.vigoo", "zio-aws-netty"),
      Project("io.github.vigoo", "zio-aws-sqs"),
      Project("io.github.vigoo", "zio-aws-kinesis"),
      Project("io.github.vigoo", "zio-aws-cloudwatch")
    )

  val sampleProjectsMetaData =
    List(
      ProjectMetaData(
        project =
          Project(
            group = "com.softwaremill.sttp.client3",
            artifactId = "async-http-client-backend-zio"
          ),
        version = "3.3.18",
        dependencies =
          Set(
            VersionedProject(
              project = Project(group = "com.softwaremill.sttp.client3", artifactId = "zio"),
              version = "3.3.18"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
              version = "1.3.8"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "com.github.ghostdogpr", artifactId = "caliban"),
        version = "1.3.1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-query"),
              version = "0.2.10"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.13"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.13"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.13"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.13"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-json"),
              version = "0.1.5"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.7mind.izumi", artifactId = "distage-core"),
        version = "1.0.8",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-cats"),
              version = "2.5.1.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "izumi-reflect"),
              version = "1.1.3-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.7mind.izumi", artifactId = "logstage-core"),
        version = "1.0.8",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "izumi-reflect"),
              version = "1.1.3-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "com.github.poslegm", artifactId = "munit-zio"),
        version = "0.0.3",
        dependencies =
          Set(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.11"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.getquill", artifactId = "quill-jdbc-zio"),
        version = "3.12.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "io.getquill", artifactId = "quill-zio"),
              version = "3.12.0"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.getquill", artifactId = "quill-zio"),
        version = "3.12.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "nl/vroste", artifactId = "rezilience"),
        version = "0.7.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.github.gaelrenoux", artifactId = "tranzactio"),
        version = "3.0.0-M1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-magnolia"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-cats"),
              version = "3.1.1.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.11"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.d11", artifactId = "zhttp"),
        version = "1.0.0.0-RC18",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio"),
        version = "2.0.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-stacktracer"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "izumi-reflect"),
              version = "2.0.8"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-internal-macros"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-actors"),
        version = "0.0.9",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-nio"),
              version = "1.0.0-RC9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.3"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.3"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.3"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-config-typesafe"),
              version = "1.0.0-RC30-1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-akka-cluster"),
        version = "0.2.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.0"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "nl.vroste", artifactId = "zio-amqp"),
        version = "0.2.2",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
              version = "1.3.5"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.11"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.github.neurodyne", artifactId = "zio-arrow"),
        version = "0.2.1",
        dependencies =
          Set(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.0-RC20"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.0-RC20"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.0-RC20"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.github.vigoo", artifactId = "zio-aws-core"),
        version = "3.17.100.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-config"),
              version = "1.0.10"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
              version = "1.3.8"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-config-typesafe"),
              version = "1.0.10"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.github.vigoo", artifactId = "zio-aws-dynamodb"),
        version = "3.17.100.3",
        dependencies =
          Set(
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-core"),
              version = "3.17.100.3"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.github.neurodyne", artifactId = "zio-aws-s3"),
        version = "0.4.13",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.0-RC20"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.0-RC20"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.0-RC20"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-cache"),
        version = "0.2.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-config"),
        version = "1.0.10",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.9"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-ftp"),
        version = "0.3.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-nio"),
              version = "1.0.0-RC9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-interop-cats"),
        version = "3.3.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-stacktracer"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-interop-guava"),
        version = "31.0.0.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
        version = "2.0.0-M2",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-M4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-M4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "2.0.0-M4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-M4"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-interop-scalaz7x"),
        version = "7.3.3.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.8"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.8"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.8"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-interop-twitter"),
        version = "20.10.0.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.3"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.3"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.3"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-json"),
        version = "0.2.0-M3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "com.coralogix", artifactId = "zio-k8s-client"),
        version = "1.4.2",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-nio"),
              version = "1.0.0-RC11"
            ),
            VersionedProject(
              project =
                Project(
                  group = "com.softwaremill.sttp.client3",
                  artifactId = "async-http-client-backend-zio"
                ),
              version = "3.3.17"
            ),
            VersionedProject(
              project =
                Project(
                  group = "com.softwaremill.sttp.client3",
                  artifactId = "httpclient-backend-zio"
                ),
              version = "3.3.17"
            ),
            VersionedProject(
              project = Project(group = "com.softwaremill.sttp.client3", artifactId = "zio"),
              version = "3.3.17"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-logging"),
              version = "0.5.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-config"),
              version = "1.0.6"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-config-typesafe"),
              version = "1.0.6"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-process"),
              version = "0.5.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-kafka"),
        version = "0.17.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "nl/vroste", artifactId = "zio-kinesis"),
        version = "5dd51766a6de8d3233c1846ad75deb185e05b8a0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.4-2"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-logging"),
              version = "0.5.6"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.4-2"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-netty"),
              version = "3.15.82.2"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-dynamodb"),
              version = "3.15.82.2"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-kinesis"),
              version = "3.15.82.2"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.4-2"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
              version = "1.3.0.7-2"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-cloudwatch"),
              version = "3.15.82.2"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.4-2"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-core"),
              version = "3.15.82.2"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-logging"),
        version = "0.5.14",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-metrics"),
        version = "1.0.13",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-nio"),
        version = "1.0.0-RC12",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-opencensus"),
        version = "0.8.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-opentelemetry"),
        version = "0.8.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-opentracing"),
        version = "0.8.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-opentracing"),
        version = "0.8.3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-optics"),
        version = "0.2.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-prelude"),
        version = "1.0.0-RC9",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-prelude-macros"),
              version = "1.0.0-RC9"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-prelude-macros"),
        version = "1.0.0-RC9",
        dependencies = HashSet()
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-process"),
        version = "0.7.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-query"),
        version = "0.3.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-rocksdb"),
        version = "0.3.0",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.0-RC18"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.0-RC18"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.0-RC18"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-s3"),
        version = "0.3.7",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-nio"),
              version = "1.0.0-RC11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
              version = "1.3.5"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.11"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.11"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "com.vladkopanev", artifactId = "zio-saga-core"),
        version = "0.4.0",
        dependencies =
          Set(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.0"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.0"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-schema"),
        version = "0.1.5",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-prelude"),
              version = "1.0.0-RC7"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.scalac", artifactId = "zio-slick-interop"),
        version = "0.4",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.10"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-interop-reactivestreams"),
              version = "1.3.5"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.10"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-sqs"),
        version = "0.4.2",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.4"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-netty"),
              version = "3.15.79.1"
            ),
            VersionedProject(
              project = Project(group = "io.github.vigoo", artifactId = "zio-aws-sqs"),
              version = "3.15.79.1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-streams"),
        version = "2.0.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "info.senia", artifactId = "zio-test-akka-http"),
        version = "2.0.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
        version = "2.0.0-RC1",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "2.0.0-RC1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "2.0.0-RC1"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "io.github.ollls", artifactId = "zio-tls-http"),
        version = "1.2-m3",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.6"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-json"),
              version = "0.1.4"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.6"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.6"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-webhooks"),
        version = "0.1.3",
        dependencies =
          HashSet(
            VersionedProject(
              project =
                Project(
                  group = "com.softwaremill.sttp.client3",
                  artifactId = "async-http-client-backend-zio"
                ),
              version = "3.3.17"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-streams"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-prelude"),
              version = "1.0.0-RC8"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-json"),
              version = "0.1.5"
            )
          )
      ),
      ProjectMetaData(
        project = Project(group = "dev.zio", artifactId = "zio-zmx"),
        version = "0.0.11",
        dependencies =
          HashSet(
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-json"),
              version = "0.2.0-M1"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio-test-sbt"),
              version = "1.0.12"
            ),
            VersionedProject(
              project = Project(group = "dev.zio", artifactId = "zio"),
              version = "1.0.12"
            )
          )
      )
    )
end Data
