package org.ziverge

import com.*
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

object Routing:

  import com.raquo.waypoint.{param, Route, Router, root, SplitRender, endOfSegments}
  import upickle.default.{macroRW, ReadWriter as RW, read, write}

  //  implicit private val AppModeRW: ReadWriter[AppMode] = macroRW
  implicit private val explorerRW: RW[DependencyExplorerPage] = macroRW
  implicit private val rw: RW[Page] = macroRW

  private val encodePage
  : DependencyExplorerPage => (Option[String], Option[String], Option[Boolean]) =
    page => (page.targetProject, Some(page.dataView.toString), Some(page.filterUpToDateProjects))

  val x = 3
  private val decodePage
  : ((Option[String], Option[String], Option[Boolean])) => DependencyExplorerPage = {
    case (targetProject, dataView, filterUpToDateProjects) =>
      DependencyExplorerPage(
        targetProject = targetProject,
        dataView = dataView.flatMap(DataView.fromString).getOrElse(DataView.Dependencies),
        filterUpToDateProjects = filterUpToDateProjects.getOrElse(false)
      )
  }

  val params: QueryParameters[(Option[String], Option[String], Option[Boolean]), DummyError] =
    param[String]("targetProject").? & param[String]("dataView").? &
      param[Boolean]("filterUpToDateProjects").?

  private val prodRoute =
    Route.onlyQuery[DependencyExplorerPage, (Option[String], Option[String], Option[Boolean])](
      encode = encodePage,
      decode = decodePage,
      pattern = (root / endOfSegments) ? params
    )

  import com.raquo.laminar.api.L
  val router =
    new Router[Page](
      routes =
        List(
          prodRoute
        ),
      getPageTitle = _.toString, // mock page title (displayed in the browser tab next to favicon)
      serializePage = page => write(page)(rw), // serialize page data for storage in History API log
      deserializePage = pageStr => read(pageStr)(rw), // deserialize the above
      routeFallback =
        _ =>
          DependencyExplorerPage(
            targetProject = None,
            dataView = DataView.Dependencies,
            filterUpToDateProjects = false
          ),
    )(
      $popStateEvent =
        L.windowEvents.onPopState, // this is how Waypoint avoids an explicit dependency on Laminar
      owner = L.unsafeWindowOwner // this router will live as long as the window
    )

  import com.raquo.laminar.api.L.{div, HtmlElement}
  import com.raquo.laminar.api.L.textToNode

  def splitter(fullAppData: AppDataAndEffects) =
    SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[DependencyExplorerPage](DependencyViewerLaminar.renderMyPage(_, fullAppData))
      .collectStatic(LoginPageOriginal) {
        div("Login page")
      }
end Routing
