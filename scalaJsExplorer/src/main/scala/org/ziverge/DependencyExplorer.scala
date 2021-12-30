package org.ziverge

import upickle.default.{read, write}
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt, ZLayer}
import upickle.default.{macroRW, ReadWriter as RW, *}
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

import org.scalajs.dom

sealed private trait Page

case class DependencyExplorerPage(
    time: Option[String], // TODO Make this a WallTime instead
    targetProject: Option[String],
    dataView: DataView
) extends Page {}

private case object LoginPageOriginal extends Page


object DependencyExplorerRouting:
  import upickle.default.{macroRW, ReadWriter as RW, *}
  import com.raquo.waypoint._
  import com.raquo.laminar.api.L

  //  implicit private val AppModeRW: ReadWriter[AppMode] = macroRW
  implicit private val explorerRW: RW[DependencyExplorerPage] = macroRW
  implicit private val rw: RW[Page]                           = macroRW

  private val encodePage
      : DependencyExplorerPage => (Option[String], Option[String], Option[String]) =
    page => (page.time, page.targetProject, Some(page.dataView.toString))

  private val decodePage
      : ((Option[String], Option[String], Option[String])) => DependencyExplorerPage = {
    case (time, targetProject, dataView) =>
      DependencyExplorerPage(
        time = time,
        targetProject = targetProject,
        dataView = dataView.flatMap(DataView.fromString).getOrElse(DataView.Blockers)
      )
  }

  val params: QueryParameters[(Option[String], Option[String], Option[String]), DummyError] =
    param[String]("time").? & param[String]("targetProject").? & param[String]("dataView").?

  private val devRoute =
    Route.onlyQuery[DependencyExplorerPage, (Option[String], Option[String], Option[String])](
      encode = encodePage,
      decode = decodePage,
      pattern = (root / "index_dev.html" / endOfSegments) ? params
    )

  private val prodRoute =
    Route.onlyQuery[DependencyExplorerPage, (Option[String], Option[String], Option[String])](
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
            time = None, // TODO Make this a WallTime instead
            targetProject = None,
            dataView = DataView.Blockers
          ),
    )(
      $popStateEvent =
        L.windowEvents.onPopState, // this is how Waypoint avoids an explicit dependency on Laminar
      owner = L.unsafeWindowOwner  // this router will live as long as the window
    )

  import com.raquo.laminar.api.L._
  // import com.raquo.laminar.api.L.{div, HtmlElement}
  
  def splitter(fullAppData: FullAppData) =
    SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[DependencyExplorerPage](LaminarApp.renderMyPage(_, fullAppData))
      .collectStatic(LoginPageOriginal) {
        div("Login page")
      }

object LaminarApp:
  import com.raquo.laminar.api.L._

  private val router = DependencyExplorerRouting.router


  def renderMyPage($loginPage: Signal[DependencyExplorerPage], fullAppData: FullAppData) =

    val clickObserver = Observer[dom.MouseEvent](onNext = ev => dom.console.log(ev.screenX))
    val pageUpdateObserver =
      Observer[DependencyExplorerPage](onNext =
        page => router.pushState(page.copy(targetProject = Some("fake.click.project")))
      )
    val selectZioObserver =
      Observer[DependencyExplorerPage](onNext =
        page => router.pushState(page.copy(targetProject = Some("dev.zio.zio")))
      )
    def viewUpdate(page: DependencyExplorerPage) =
      Observer[String](onNext =
        dataView =>
          DataView.fromString(dataView).foreach(x => router.pushState(page.copy(dataView = x)))
      )

//    val clickBus = new EventBus[]
    div(
      child <--
        $loginPage.map((busPageInfo: DependencyExplorerPage) =>
          div(
            div("time query param value: " + busPageInfo.time),
            select(
              inContext { thisNode =>
                onChange.mapTo(thisNode.ref.value.toString) --> viewUpdate(busPageInfo)
              },
              DataView
                .values
                .map(dataView => option(value := dataView.toString, dataView.toString))
                .toSeq
            ),
            div(
              // TODO Better result type so we can properly render different schemas
              SummaryLogic.viewLogic(busPageInfo.dataView, fullAppData) match
                case content: String =>
                  content.split("\n").map(p(_)).toSeq
                case other =>
                  other.toString
            ),
            button(
              "Select fake proejct",
              onClick.map(_ => busPageInfo) --> pageUpdateObserver

              //          clickObserver
            ),
            button("Select ZIO", onClick.map(_ => busPageInfo) --> selectZioObserver)
          )
        )
    )
  end renderMyPage

  def app(fullAppData: FullAppData): Div = div(child <-- DependencyExplorerRouting.splitter(fullAppData).$view)
end LaminarApp

object DependencyExplorer extends ZIOAppDefault:

  // import com.raquo.laminar.api.L.{*, given}

  def logic: ZIO[ZioEcosystem & Console, Throwable, Unit] =
    for
//      appData <- SharedLogic.fetchAppData // TODO Local version of this?
      appData <- ZioEcosystem.snapshot // TODO Call abstract method that delegates
      // _ <- PConsole.zprint(appData.all)
      _ <- Console.printLine("Some plain ole' string")
      _ <- Console.printLine(appData.all.head)
      _ <-
        ZIO {
          val appHolder = dom.document.getElementById("landing-message")
          appHolder.innerHTML = ""
          com.raquo.laminar.api.L.render(appHolder, LaminarApp.app(appData))
        }
    yield ()

  def run = logic.provide(ZLayer.succeed[ZioEcosystem](AppDataHardcoded), ZLayer.succeed(DevConsole.word))

end DependencyExplorer
