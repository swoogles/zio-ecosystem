package org.ziverge

import zio.ZIO
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge

object ZioEcosystem:
  def snapshot: ZIO[ZioEcosystem, Nothing, FullAppData] = ZIO.serviceWithZIO(_.snapshot)

trait ZioEcosystem:
  def snapshot: ZIO[Any, Nothing, FullAppData]
