package org.ziverge

import upickle.default.{macroRW, ReadWriter as RW, *}

case class Version(value: String) extends Comparable[Version]:
  def compareTo(other: Version) = Version.compareVersions(this, other)
  val renderForWeb =
    if (!value.contains("."))
      "Hash Snapshot"
      
    else
      value.replace("Version(", "").replace(")","")

object Version:
  implicit val rw: RW[Version] = macroRW
  def compareVersions(version1: Version, version2: Version): Int =
    var comparisonResult = 0
    // TODO Actually handle Milestones and RCs
    val version1Splits =
      version1
        .value
        .split("\\.")
        .filter(segment => !segment.contains("RC") && !segment.contains("M"))
    val version2Splits =
      version2
        .value
        .split("\\.")
        .filter(segment => !segment.contains("RC") && !segment.contains("M"))
    val maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length)
    var i                        = 0
    while (i < maxLengthOfVersionSplits && comparisonResult == 0) {
      val v1 =
        if (i < version1Splits.length)
          version1Splits(i).toInt
        else
          0
      val v2 =
        if (i < version2Splits.length)
          version2Splits(i).toInt
        else
          0
      val compare = v1.compareTo(v2)
      if (compare != 0) {
        comparisonResult = compare
      }
      i = i + 1
    }
    comparisonResult
  end compareVersions
end Version
