package org.ziverge

//import org.ziverge.SharedLogic;

import zio.*
import zio.test.*

object SharedLogicSpec extends ZIOSpecDefault:
  def spec = suite("SharedLogic")(
    test("fullAppData")(
      for
        _ <- SharedLogic.fetchAppData(ScalaVersion.V2_13).debug
      yield assertCompletes
    )
  )

//object SharedLogicTest
