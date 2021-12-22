package org.ziverge

import java.lang.module.ModuleDescriptor.Version

object Json:
  import io.circe._, io.circe.generic.auto._, io.circe.syntax._

  implicit val encodeFoo: Encoder[Version] = new Encoder[Version] {
    final def apply(a: Version): Json = 
      io.circe.Json.fromString(a.toString)
  }
  
  def render(connectedProjects: Seq[ConnectedProjectData]) =
    connectedProjects.asJson
