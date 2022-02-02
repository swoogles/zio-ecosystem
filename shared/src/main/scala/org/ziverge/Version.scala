package org.ziverge

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
    val version1Splits           = version1.value.split("\\.")
    val version2Splits           = version2.value.split("\\.")
    val maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length)
    var i                        = 0
    while (i < maxLengthOfVersionSplits && comparisonResult == 0) {
      val v1      = version1Splits(i)
      val v2      = version2Splits(i)
      val compare = v1.compareTo(v2)
      if (compare != 0) {
        comparisonResult = compare
      }
      i = i + 1
    }
//    println(s"Version1: $version1 Version2: $version2 comparison result: " + comparisonResult)
    comparisonResult
end Version
