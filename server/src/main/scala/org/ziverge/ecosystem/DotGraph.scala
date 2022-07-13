package org.ziverge.ecosystem

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

object DotGraph:

  def render(graph: Graph[Project, DiEdge]): String =
    /* There should be a cleaner way to do this, but I can't figure out what in the flying heck a
     * "dotRoot" and an "edgeTransfomer" are. Not worth my time or brain cells to figure it out.
     * http://www.scala-graph.org/guides/dot.html */
    "digraph {\n" +
      graph
        .edges
        .map { x =>
          val lib       = x.head
          val dependent = x.tail.head
          s""" "${lib.artifactIdQualifiedWhenNecessary}"->"${dependent
            .artifactIdQualifiedWhenNecessary}" """
        }
        .mkString("\n") + "\n}"
  end render
