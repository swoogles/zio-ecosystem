package org.ziverge

//import org.ziverge.ecosystem.SharedLogic;

import org.ziverge.ecosystem.{ScalaVersion, SharedLogic, Version}
import zio.*
import zio.test.*

object SharedLogicSpec extends ZIOSpecDefault:
  def spec = suite("SharedLogic")(
    suite("all projects")(
      test("fullAppData")(
        for
          data <- SharedLogic.fetchAppData(ScalaVersion.V2_13)
        yield assertCompletes
      )
    ),
    suite("individual projects")(
      test("zio-cron")(
        for
//          _ <- SharedLogic.fetchAppDataAndRefreshCache(ScalaVersion.V2_13)
          res <- SharedLogic.projectData("io.github.jkobejs", "zio-cron", Version("1.0.0"), ScalaVersion.V2_13)
        yield assertCompletes
      )
    )
  )

//object SharedLogicTest
