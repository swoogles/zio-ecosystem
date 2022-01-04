package org.ziverge

import upickle.default.{read, write}
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt, ZLayer}
import upickle.default.{macroRW, ReadWriter as RW, *}
import urldsl.errors.DummyError
import urldsl.language.QueryParameters
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement

object DependencyExplorerRouting:
  import upickle.default.{macroRW, ReadWriter as RW, *}
  import com.raquo.waypoint._
  import com.raquo.laminar.api.L

  //  implicit private val AppModeRW: ReadWriter[AppMode] = macroRW
  implicit private val explorerRW: RW[DependencyExplorerPage] = macroRW
  implicit private val rw: RW[Page]                           = macroRW

  private val encodePage
      : DependencyExplorerPage => (Option[String], Option[String], Option[Boolean]) =
    page => (page.targetProject, Some(page.dataView.toString), Some(page.filterUpToDateProjects))

  private val decodePage
      : ((Option[String], Option[String], Option[Boolean])) => DependencyExplorerPage = {
    case (targetProject, dataView, filterUpToDateProjects) =>
      DependencyExplorerPage(
        targetProject = targetProject,
        dataView = dataView.flatMap(DataView.fromString).getOrElse(DataView.Blockers),
        filterUpToDateProjects = filterUpToDateProjects.getOrElse(false)
      )
  }

  // implicit val dataViewPrinter: urldsl.vocabulary.Printer[org.ziverge.DataView] = new
  // urldsl.vocabulary.Printer[org.ziverge.DataView] {
  //   def print(t: DataView) =
  //     write(t)
  // }

  // implicit val dataViewReader: urldsl.vocabulary.FromString[org.ziverge.DataView,
  // urldsl.errors.DummyError] = new urldsl.vocabulary.FromString[org.ziverge.DataView,
  // urldsl.errors.DummyError] {
  //   def fromString(str: String): Either[DummyError, DataView] =

  //     try {
  //       Right(read[DataView](str))
  //     } catch {
  //       case failure =>
  //         println("Could not parse value: " + failure)
  //         Left(DummyError.dummyError)
  //   }

  // }

  // val params: QueryParameters[(Option[String], Option[String], Option[String], Option[Boolean]),
  // DummyError] =
  val params: QueryParameters[(Option[String], Option[String], Option[Boolean]), DummyError] =
    param[String]("targetProject").? & param[String]("dataView").? &
      param[Boolean]("filterUpToDateProjects").?

  private val devRoute =
    Route.onlyQuery[DependencyExplorerPage, (Option[String], Option[String], Option[Boolean])](
      encode = encodePage,
      decode = decodePage,
      pattern = (root / "index_dev.html" / endOfSegments) ? params
    )

  private val prodRoute =
    Route.onlyQuery[DependencyExplorerPage, (Option[String], Option[String], Option[Boolean])](
      encode = encodePage,
      decode = decodePage,
      pattern = (root / endOfSegments) ? params
    )

  val router =
    new Router[Page](
      routes =
        List(
          prodRoute
          //      devRoute,
        ),
      getPageTitle = _.toString, // mock page title (displayed in the browser tab next to favicon)
      serializePage = page => write(page)(rw), // serialize page data for storage in History API log
      deserializePage = pageStr => read(pageStr)(rw), // deserialize the above
      routeFallback =
        _ =>
          DependencyExplorerPage(
            targetProject = None,
            dataView = DataView.Blockers,
            filterUpToDateProjects = false
          ),
    )(
      $popStateEvent =
        L.windowEvents.onPopState, // this is how Waypoint avoids an explicit dependency on Laminar
      owner = L.unsafeWindowOwner  // this router will live as long as the window
    )

  import com.raquo.laminar.api.L._
  // import com.raquo.laminar.api.L.{div, HtmlElement}

  def splitter(fullAppData: AppDataAndEffects) =
    SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[DependencyExplorerPage](DependencyViewerLaminar.renderMyPage(_, fullAppData))
      .collectStatic(LoginPageOriginal) {
        div("Login page")
      }
end DependencyExplorerRouting
