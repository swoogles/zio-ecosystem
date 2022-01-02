package org.ziverge

import ujson.Js
import zio.Console.printLine
import zio.{Chunk, ZIO}
import upickle.default.{macroRW, ReadWriter as RW, *}

enum DataView(val name: String):
  case Dependencies(filter: Option[String]) extends DataView("Dependencies")
  case Dependents extends DataView("Dependents")
  case Json extends DataView("Json")
  case Blockers extends DataView("Blockers")
  case DotGraph extends DataView("DotGraph")

object DataView:
  val valueStrings =
    List("Dependencies" , "Dependents" , "Json" , "Blockers" , "DotGraph")

  def fromStrings(
      args: Chunk[String]
  ): Option[DataView] = // TODO Decide whether to do with multiple args
    args.flatMap(fromString).headOption

  def fromString(args: String): Option[DataView] =  args match {
    // TODO Decide whether to do with multiple args
    // TODO YIKES. All these crazy matching issues :/
    case s"Dependencies(Some($filter))" => Some(Dependencies(filter=None))
    case s"Dependencies(None)" => Some(Dependencies(filter=None))
    case s"Dependencies" => Some(Dependencies(filter=None))
    case "Dependents" => Some(Dependents)
    case "Json" => Some(Json)
    case "Blockers" => Some(Blockers)
    case "DotGraph" => Some(DotGraph)
    case failedValue => throw new Exception("Unrecognized value: " + failedValue)
  }
    // values.find(_.toString == args)

  import upickle.default.ReadWriter.join

  implicit val dependenciesRW: RW[Dependencies] = macroRW
  implicit val explorerRW: RW[DataView] = macroRW

//  val dtWriter: Writer[DataView] ={
//    case t => Js.Str(format(t))
//  }
//  val dtReader = Reader[DataView]{
//    case Js.Str(time) =>
//      try {
//        Data.fromString(time).get
//      }
//      catch {
//        case _: Exception =>
//          DataView.Blockers
//      }
//  }

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
          p.zioDep.fold(true)(zDep => zDep.zioDep.typedVersion.compareTo(currentZioVersion) < 0) &&
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
    println("DataView in view logic: " + dataView)
    dataView match
      case DataView.Dependencies(filterOpt) =>
        SummaryLogic
          .manipulateAndRender(
            fullAppData.connected.filter(project=>
              filterOpt match {
                case Some(filter) => 
                  // TODO Make this a function in a better spot
                  // project.dependants.exists(_.project.artifactId.contains(filter)) ||
                    project.dependencies.exists(_.project.artifactId.contains(filter)) || 
                    project.project.artifactId.contains(filter)
                case None => true
              }
              
              ),
            _.dependencies.size,
            p =>
              if (p.dependencies.nonEmpty)
                s"Depends on ${p.dependencies.size} projects: " +
                  p.dependencies.map(_.project.artifactId).mkString(",")
              else
                "Does not depend on any known ecosystem library."
          )
          .mkString("\n")
      case DataView.Dependents =>
        SummaryLogic
          .manipulateAndRender(
            fullAppData.connected,
            _.dependants.size,
            p =>
              if (p.dependants.nonEmpty)
                f"Required by ${p.dependants.size} projects: " +
                  p.dependants.map(_.project.artifactId).mkString(",")
              else
                "Has no dependents"
          )
          .mkString("\n")
      case DataView.Json =>
        Json.render(fullAppData.connected)
      case DataView.Blockers =>
        SummaryLogic
          .manipulateAndRender(
            fullAppData.connected,
            _.blockers.size,
            p =>
              if (p.blockers.nonEmpty)
                s"is blocked by ${p.blockers.size} projects: " +
                  p.blockers.map(blocker => Render.sbtStyle(blocker.project)).mkString(",")
              else
                "Is not blocked by any known ecosystem library."
          )
          .mkString("\n")
      case DataView.DotGraph =>
        DotGraph.render(fullAppData.graph)
    end match
  end viewLogic
end SummaryLogic
