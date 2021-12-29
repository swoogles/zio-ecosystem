package org.ziverge

import zio.Console

import upickle.default.{read, write}

object PConsole:
  def zprint[T: upickle.default.ReadWriter](x: T) =
    val pickled   = write(x)
    val depickled = read[T](pickled)
    Console.printLine(pprint(x, height = Int.MaxValue))
