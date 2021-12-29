package org.ziverge

import zio.ZIO
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

object AppDataHardcoded:
  val getJsData =
    val connectedX = JsDataConnected.connected
    val allX       = JsData.allProjectData
    for
      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allX))
    yield FullAppData(connectedX, allX, graph)
