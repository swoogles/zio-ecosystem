package org.ziverge

//import org.ziverge.SharedLogic;

import zio.*
import zio.test.*

object SharedLogicSpec extends ZIOSpecDefault:
  def spec = suite("SharedLogic")(
    suite("all projects")(
      test("fullAppData")(
        for
          _ <- SharedLogic.fetchAppData(ScalaVersion.V2_13)
        yield assertCompletes
      )
    ),
    suite("individual projects")(
      test("zio-cron")(
        for
//          _ <- SharedLogic.fetchAppDataAndRefreshCache(ScalaVersion.V2_13)
          res <- SharedLogic.statusOf("io.github.jkobejs", "zio-cron", Version("1.0.0"), ScalaVersion.V2_13).debug
        yield assertCompletes
      )
    )
  )

//object SharedLogicTest
