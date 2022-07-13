package org.ziverge.ecosystem

import zio.test.assertTrue

object VersionSpec extends zio.test.ZIOSpecDefault:
  val spec =
    suite("ZIO 2.x SmartAssertions")(
      test("compareTo")(
        assertTrue(Version.compareVersions(Version("2.0.0-RC2"), Version("2.0.0-RC1")) == 1)
      ),
      test("compareTo equal")(
        assertTrue(Version.compareVersions(Version("2.0.0-RC2"), Version("2.0.0-RC2")) == 0)
      ),
      test("compareTo major")(
        assertTrue(Version.compareVersions(Version("2.0.0-RC2"), Version("1.0.13")) == 1)
      ),
      test("compareTo minor")(
        assertTrue(Version.compareVersions(Version("1.0.13"), Version("1.1.03")) == -1)
      )
    )
