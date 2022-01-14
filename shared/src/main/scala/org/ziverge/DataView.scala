package org.ziverge

import ujson.Js
import zio.Console.printLine
import zio.{Chunk, ZIO}
import upickle.default.{macroRW, ReadWriter as RW, *}
import java.io.ObjectInputStream
import java.io.ByteArrayInputStream

enum DataView:
  case Dependencies, Dependents, Blockers

object DataView:

  def fromStrings(
      args: Chunk[String]
  ): Option[DataView] = // TODO Decide whether to do with multiple args
    args.flatMap(fromString).headOption

  def fromString(args: String): Option[DataView] = values.find(_.toString == args)

  import upickle.default.ReadWriter.join

  implicit val explorerRW: RW[DataView] = macroRW
