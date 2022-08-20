package org.ziverge.ecosystem

import upickle.default.{macroRW, ReadWriter as RW, *}

case class Version(value: String) extends Comparable[Version]:
  def compareTo(other: Version) = Version.compareVersions(this, other)
  val renderForWeb = // TODO Why does Version show up after the live data load?
    if (!value.contains("."))
      "Hash Snapshot"
    else
      value.replace("Version(", "").replace(")", "")

object Version:
  implicit val rw: RW[Version] = macroRW
  def compareVersions(version1: Version, version2: Version): Int =
    var comparisonResult = 0
    // TODO Actually handle Milestones and RCs
    val version1Splits =
      version1
        .value
        .split("\\.")
    val version2Splits =
      version2
        .value
        .split("\\.")

    val major1 = version1Splits(0).toInt
    val minor1 = version1Splits(1).toInt
//    val patch1 = version1Splits(2).toInt

    val major2 = version2Splits(0).toInt
    val minor2 = version2Splits(1).toInt
//    val patch2 = version2Splits(2).toInt

    val maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length)

    if (major1.compareTo(major2) != 0)
      major1.compareTo(major2)
    else
      if (minor1.compareTo(minor2) != 0)
        minor1.compareTo(minor2)
      else
        0
//    var i                        = 0
//    while (i < maxLengthOfVersionSplits && comparisonResult == 0) {
//      val v1 =
//          version1Splits(i)
//      val v2 =
//          version2Splits(i)
//      val compare = v1.compareTo(v2)
//      if (compare != 0) {
//        comparisonResult = compare
//      }
//      i = i + 1
//    }
//    comparisonResult
  end compareVersions
end Version
