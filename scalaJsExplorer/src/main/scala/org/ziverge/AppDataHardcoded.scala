package org.ziverge

import zio.{Console, ZIO}
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

object AppDataHardcoded extends ZioEcosystem:
  val getJsData =
    val connectedX = JsDataConnected.connected
    val allX       = JsData.allProjectData
    for
      graph: Graph[Project, DiEdge] <- ZIO.succeed(ScalaGraph(allX))
    yield FullAppData(connectedX, allX, graph)

  val snapshot: ZIO[Any, Nothing, FullAppData] = getJsData
