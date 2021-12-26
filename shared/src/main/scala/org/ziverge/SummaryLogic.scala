package org.ziverge

import zio.Console.printLine
import zio.{Chunk, ZIO}

enum DataView:
  case Dependencies, Dependents, Json, Blockers, DotGraph
  
object DataView:
  def fromString(args: Chunk[String]) = // TODO Decide whether to do with multiple args
    if (args.contains("json") )
      ZIO(Json)
    else if (args.contains("dot") )
      ZIO(DotGraph)
    else if (args.contains("dependents") )
      ZIO(Dependents)
    else if (args.contains("dependencies") )
      ZIO(Dependencies)
    else if (args.contains("blockers") )
      ZIO(Blockers)
    else
      ZIO.fail("Unrecognized CLI arguments")
    
  
  

object SummaryLogic:

  def manipulateAndRender(
                           connectedProjects: Seq[ConnectedProjectData],
                           sort: ConnectedProjectData => Integer,
                           connectionMessage: ConnectedProjectData => String
                         ) =
    val currentZioVersion = Version("2.0.0-RC1")
    connectedProjects
      .filter(p =>
        p.blockers.nonEmpty ||
          p.zioDep
            .fold(true)(zDep => zDep.zioDep.typedVersion.compareTo(currentZioVersion) < 0) &&
            !Data.coreProjects.contains(p.project)
      ) // TODO Where to best provide this?
      .sortBy(sort)
      .reverse
      .sortBy(p => Render.sbtStyle(p.project)) // TODO remove after demo run
      .map { project =>
        val renderedZioDependency =
          if (Data.coreProjects.contains(project.project))
            "is a core project"
          else
            ZioDep.render(project.zioDep)
        f"${Render.sbtStyle(project.project)}%-50s ${renderedZioDependency} and " +
          connectionMessage(project)
      }
  end manipulateAndRender

  def viewLogic(dataView: DataView, fullAppData: FullAppData): Any =
    dataView match {
      case DataView.Dependencies =>
        SummaryLogic.manipulateAndRender(
          fullAppData.connected,
          _.dependencies.size,
          p =>
            if (p.dependencies.nonEmpty)
              s"Depends on ${p.dependencies.size} projects: " +
                p.dependencies.map(_.project.artifactId).mkString(",")
            else
              "Does not depend on any known ecosystem library."
        ).mkString("\n")
      case DataView.Dependents =>
        SummaryLogic.manipulateAndRender(
          fullAppData.connected,
          _.dependants.size,
          p =>
            if (p.dependants.nonEmpty)
              f"Required by ${p.dependants.size} projects: " +
                p.dependants.map(_.project.artifactId).mkString(",")
            else
              "Has no dependents"
        ).mkString("\n")
      case DataView.Json => Json.render(fullAppData.connected)
      case DataView.Blockers =>
        SummaryLogic.manipulateAndRender(
          fullAppData.connected,
          _.blockers.size,
          p =>
            if (p.blockers.nonEmpty)
              s"is blocked by ${p.blockers.size} projects: " +
                p.blockers.map(blocker => Render.sbtStyle(blocker.project)).mkString(",")
            else
              "Is not blocked by any known ecosystem library."
        ).mkString("\n")
      case DataView.DotGraph => DotGraph.render(fullAppData.graph)
    }

