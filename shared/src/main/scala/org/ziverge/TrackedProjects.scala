package org.ziverge

import scala.collection.immutable.HashSet

object TrackedProjects:
  val zioCore = Project("dev.zio", "zio", Some("https://github.com/zio/zio"))
  val coreProjects =
    List(
      zioCore,
      Project("dev.zio", "zio-test", Some("https://github.com/zio/zio")),
      Project("dev.zio", "zio-test-sbt", Some("https://github.com/zio/zio")),
      Project("dev.zio", "zio-test-magnolia", Some("https://github.com/zio/zio")),
      Project("dev.zio", "zio-internal-macros", Some("https://github.com/zio/zio")),
      Project("dev.zio", "zio-stacktracer", Some("https://github.com/zio/zio")),
      Project("dev.zio", "zio-streams", Some("https://github.com/zio/zio")),
      Project("dev.zio", "izumi-reflect", Some("https://github.com/zio/izumi-reflect")),
      Project(
        "dev.zio",
        "izumi-reflect-thirdparty-boopickle-shaded",
        Some("https://github.com/zio/izumi-reflect")
      )
    )

  val projects =
    (coreProjects ++
      List(
        Project("dev.zio", "zio-cache", Some("https://github.com/zio/zio-cache")),
        Project("dev.zio", "caliban-deriving", Some("https://github.com/zio/caliban-deriving")),
        Project("com.github.ghostdogpr", "caliban", Some("https://github.com/ghostdogpr/caliban")),
        Project("dev.zio", "zio-optics", Some("https://github.com/zio/zio-optics")),
        Project("dev.zio", "zio-json", Some("https://github.com/zio/zio-json")),
        Project("dev.zio", "zio-query", Some("https://github.com/zio/zio-query")),
        Project("dev.zio", "zio-schema", Some("https://github.com/zio/zio-schema")),
        Project("dev.zio", "zio-cli", Some("https://github.com/zio/zio-cli")),
        Project("dev.zio", "zio-config", Some("https://github.com/zio/zio-config")),
        Project("dev.zio", "zio-config-typesafe", Some("https://github.com/zio/zio-config")),
        Project("dev.zio", "zio-kafka", Some("https://github.com/zio/zio-kafka")),
        Project("dev.zio", "zio-ftp", Some("https://github.com/zio/zio-ftp")),
        // Project("dev.zio", "zio-mock", Some("https://github.com/zio/zio-mock")), // TODO Enable
        // when published
        Project("dev.zio", "zio-aws-core", Some("https://github.com/zio/zio-aws")),
        Project("dev.zio", "zio-prelude", Some("https://github.com/zio/zio-prelude")),
        Project("dev.zio", "zio-prelude-macros", Some("https://github.com/zio/zio-prelude")),
        Project(
          "dev.zio",
          "zio-interop-reactivestreams",
          Some("https://github.com/zio/interop-reactive-streams")
        ),
        Project("dev.zio", "zio-interop-scalaz7x", Some("https://github.com/zio/interop-scalaz")),
        Project("dev.zio", "zio-interop-twitter", Some("https://github.com/zio/interop-twitter")),
        Project("nl.vroste", "zio-amqp", Some("https://github.com/svroonland/zio-amqp")),
        Project("dev.zio", "zio-interop-guava", Some("https://github.com/zio/interop-guava")),
        Project("io.7mind.izumi", "distage-core", Some("https://github.com/7mind/izumi")),
        Project("io.7mind.izumi", "logstage-core", Some("https://github.com/7mind/izumi")),
        Project("com.github.poslegm", "munit-zio", Some("https://github.com/poslegm/munit-zio")),
        Project("com.coralogix", "zio-k8s-client", Some("https://github.com/coralogix/zio-k8s")),
        Project(
          "com.softwaremill.sttp.client3",
          "zio",
          Some("https://github.com/softwaremill/sttp")
        ),
        Project(
          "com.softwaremill.sttp.tapir",
          "tapir-json-zio",
          Some("https://github.com/softwaremill/tapir")
        ),
        Project(
          "com.softwaremill.sttp.client3",
          "httpclient-backend-zio",
          Some("https://github.com/softwaremill/sttp")
        ),
        Project(
          "com.softwaremill.sttp.client3",
          "async-http-client-backend-zio",
          Some("https://github.com/softwaremill/sttp")
        ),
        Project(
          "com.softwaremill.sttp.shared",
          "zio",
          Some("https://github.com/softwaremill/sttp")
        ),
        Project("io.d11", "zhttp", Some("https://github.com/dream11/zio-http")),
        Project("dev.zio", "zio-interop-cats", Some("https://github.com/zio/interop-cats")),
        Project("dev.zio", "zio-nio", Some("https://github.com/zio/zio-nio")),
        Project("dev.zio", "zio-zmx", Some("https://github.com/zio/zio-zmx")),
        Project("dev.zio", "zio-parser", Some("https://github.com/zio/zio-parser")),
        Project("dev.zio", "zio-actors", Some("https://github.com/zio/zio-actors")),
        Project("dev.zio", "zio-logging", Some("https://github.com/zio/zio-logging")),
        Project("dev.zio", "zio-metrics", Some("https://github.com/zio/zio-metrics")),
        Project("dev.zio", "zio-process", Some("https://github.com/zio/zio-process")),
        Project("dev.zio", "zio-akka-cluster", Some("https://github.com/zio/zio-akka-cluster")),
        Project("dev.zio", "zio-rocksdb", Some("https://github.com/zio/zio-rocksdb")),
        Project("dev.zio", "zio-s3", Some("https://github.com/zio/zio-s3")),
        Project("dev.zio", "zio-opencensus", Some("https://github.com/zio/zio-telemetry")),
        Project("dev.zio", "zio-opentelemetry", Some("https://github.com/zio/zio-telemetry")),
        Project("dev.zio", "zio-opentracing", Some("https://github.com/zio/zio-telemetry")),
        Project("io.github.ollls", "zio-tls-http", Some("https://github.com/ollls/zio-tls-http")),
        Project(
          "com.vladkopanev",
          "zio-saga-core",
          Some("https://github.com/VladKopanev/zio-saga")
        ),
        Project(
          "io.scalac",
          "zio-slick-interop",
          Some("https://github.com/ScalaConsultants/zio-slick-interop")
        ),
        Project("dev.zio", "zio-sqs", Some("https://github.com/zio/zio-sqs")),
        Project("dev.zio", "zio-webhooks", Some("https://github.com/zio/zio-webhooks")),
        // Project("com.github.jczuchnowski", "zio-pulsar"), // Scala 3 Only
        Project("nl.vroste", "rezilience", Some("https://github.com/svroonland/rezilience")),
        Project("nl.vroste", "zio-kinesis", Some("https://github.com/svroonland/zio-kinesis")),
        Project("io.getquill", "quill-zio", Some("https://github.com/zio/zio-quill")),
        Project("io.getquill", "quill-jdbc-zio", Some("https://github.com/zio/zio-quill")),
        Project(
          "io.github.gaelrenoux",
          "tranzactio",
          Some("https://github.com/gaelrenoux/tranzactio")
        ),
        Project(
          "info.senia",
          "zio-test-akka-http",
          Some("https://github.com/senia-psm/zio-test-akka-http")
        ),
        Project("io.github.neurodyne", "zio-arrow", Some("https://github.com/zio-mesh/zio-arrow")),
        Project("dev.akif", "e-zio", Some("https://github.com/makiftutuncu/e")),
        Project("dev.zio", "zio-redis", Some("https://github.com/zio/zio-redis")),
        Project("com.thesamet.scalapb.zio-grpc", "zio-grpc-core", Some("https://github.com/scalapb/zio-grpc")),
        Project("com.thesamet.scalapb.zio-grpc", "zio-grpc-codegen", Some("https://github.com/scalapb/zio-grpc")),
        // TODO Figure out how to get weaver-zio to show the ZIO dependency for weaver-zio-core
//        Project("com.disneystreaming", "weaver-zio", Some("https://github.com/disneystreaming/weaver-test")),
//        Project("com.disneystreaming", "weaver-zio-core", Some("https://github.com/disneystreaming/weaver-test")),
        Project("com.softwaremill.sttp.tapir", "tapir-zio", Some("https://github.com/softwaremill/tapir")),

      ) ++ desertProjects.projects ++ clippProjects.projects ++ proxProjects.projects ).sortBy(_.artifactId)

  lazy val desertProjects = ProjectGroup(
    "Desert Projects",
    List(
//      Project("io.github.vigoo", "desert-core", Some("https://github.com/vigoo/desert")),
      Project("io.github.vigoo", "desert-zio", Some("https://github.com/vigoo/desert")),
//      Project("io.github.vigoo", "desert-cats", Some("https://github.com/vigoo/desert")),
//      Project("io.github.vigoo", "desert-cats-effect", Some("https://github.com/vigoo/desert")),
//      Project("io.github.vigoo", "desert-akka", Some("https://github.com/vigoo/desert")),
    )
  )

  lazy val clippProjects = ProjectGroup(
    "Clipp Projects",
    List(
//      Project("io.github.vigoo", "clipp", Some("https://github.com/vigoo/clipp")),
//      Project("io.github.vigoo", "clipp-core", Some("https://github.com/vigoo/clipp")),
//      Project("io.github.vigoo", "clipp-cats", Some("https://github.com/vigoo/clipp")),
//      Project("io.github.vigoo", "clipp-cats-effect", Some("https://github.com/vigoo/clipp")),
      Project("io.github.vigoo", "clipp-zio-2", Some("https://github.com/vigoo/clipp")),
    )
  )

  lazy val proxProjects = ProjectGroup(
    "Prox Projects",
    List(
      Project("io.github.vigoo", "prox-zstream-2", Some("https://github.com/vigoo/prox")),
    )
  )

//  val awsSubprojects =
//    List(
//      Project("dev.zio", "zio-aws-dynamodb"),
//      Project("dev.zio", "zio-aws-netty"),
//      Project("dev.zio", "zio-aws-sqs"),
//      Project("dev.zio", "zio-aws-kinesis"),
//      Project("dev.zio", "zio-aws-cloudwatch"),
//      Project("io.github.neurodyne", "zio-aws-s3")
//    )

end TrackedProjects
