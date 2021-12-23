package org.ziverge

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

object DotGraph:

  def render(graph: Graph[Project, DiEdge]): String =
    /* There should be an easier way to do this, but I can't figure out what in the flying heck a
     * "dotRoot" and an "edgeTransfomer" are. Not worth my time or brain cells to figure it out.
     * http://www.scala-graph.org/guides/dot.html */
    val Edge = "^(.+)~>(.+)$".r
    s"""|digraph {
        |${
      graph
        .edges
        .map {
          _.toString match
            case Edge("zio", "zio") | Edge("zio-streams", "zio") =>
              /* TODO: These two edges introduce cycles into the dependency graph.
             * This could be due to a bug of some sort in the dependency graph construction. For
             * now, ignore these edges in the dot output. */
              ""
            case Edge(src, tgt) =>
              s"""  "$src"->"$tgt";"""
        }
        .mkString("\n")
    }
        |}""".stripMargin
  end render
end DotGraph
