package org.ziverge

import zio.ZIO
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

trait ZioEcosystem:
  def snapshot: ZIO[Any, Nothing, FullAppData]

object ZioEcosystem:
  def snapshot: ZIO[ZioEcosystem, Nothing, FullAppData] =
    ZIO.serviceWithZIO(_.snapshot)

object AppDataHardcoded extends ZioEcosystem:
  val getJsData =
    val connectedX = JsDataConnected.connected
    val allX       = JsData.allProjectData
    for
      graph: Graph[Project, DiEdge] <- ZIO.succeed(ScalaGraph(allX))
    yield FullAppData(connectedX, allX, graph)

  val snapshot: ZIO[Any, Nothing, FullAppData] =
    getJsData