package org.ziverge.ecosystem

import org.ziverge.*
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

object Routing:

  import com.raquo.waypoint.{Route, Router, SplitRender, endOfSegments, param, root}
  import upickle.default.{macroRW, read, write, ReadWriter as RW}

  //  implicit private val AppModeRW: ReadWriter[AppMode] = macroRW
  implicit private val explorerRW: RW[DependencyExplorerPage] = macroRW
  implicit private val rw: RW[Page]                           = macroRW

  private val encodePage: DependencyExplorerPage => (Option[String], Option[Boolean]) =
    page => (page.targetProject, Some(page.filterUpToDateProjects))

  private val decodePage: ((Option[String], Option[Boolean])) => DependencyExplorerPage = {
    case (targetProject, filterUpToDateProjects) =>
      DependencyExplorerPage(
        targetProject = targetProject,
        filterUpToDateProjects = filterUpToDateProjects.getOrElse(false)
      )
  }

  val params: QueryParameters[(Option[String], Option[Boolean]), DummyError] =
    param[String]("targetProject").? & param[Boolean]("filterUpToDateProjects").?

  private val prodRoute =
    Route.onlyQuery[DependencyExplorerPage, (Option[String], Option[Boolean])](
      encode = encodePage,
      decode = decodePage,
      pattern = (root / endOfSegments) ? params
    )

  import com.raquo.laminar.api.L
  val router =
    new Router[Page](
      routes = List(prodRoute),
      getPageTitle = _.toString, // mock page title (displayed in the browser tab next to favicon)
      serializePage = page => write(page)(rw), // serialize page data for storage in History API log
      deserializePage = pageStr => read(pageStr)(rw), // deserialize the above
      routeFallback =
        _ => DependencyExplorerPage(targetProject = None, filterUpToDateProjects = false),
    )(
      $popStateEvent =
        L.windowEvents.onPopState, // this is how Waypoint avoids an explicit dependency on Laminar
      owner = L.unsafeWindowOwner  // this router will live as long as the window
    )

  import com.raquo.laminar.api.L.{HtmlElement, div, textToNode}

  def splitter(fullAppData: AppDataAndEffects) =
    SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[DependencyExplorerPage](DependencyViewerLaminar.renderMyPage(_, fullAppData))
      .collectStatic(LoginPageOriginal) {
        div("Login page")
      }
end Routing
