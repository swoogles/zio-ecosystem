package org.ziverge

import upickle.default.{read, write}
import com.raquo.waypoint.{param, root}
import pprint.PPrinter
//import localized.{Localized}
import sttp.model.Uri
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt}
import zio.Console.printLine
import scalax.collection.Graph
import scalax.collection.GraphPredef.*
import scalax.collection.GraphEdge.*
import scalax.collection.edge.LDiEdge
import scalax.collection.edge.Implicits.*
import upickle.default.{macroRW, ReadWriter as RW, *}
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

import java.time.{OffsetDateTime, ZoneId}
import org.scalajs.dom

object LaminarApp:
  import com.raquo.laminar.api.L._

  import com.raquo.laminar.api.L
  import com.raquo.waypoint._
  import upickle.default._

  sealed private trait Page

  private case class DependencyExplorerPage(
      time: Option[String], // TODO Make this a WallTime instead
      targetProject: Option[String],
      dataView: DataView
  ) extends Page {}

  private case object LoginPageOriginal extends Page

  //  implicit private val AppModeRW: ReadWriter[AppMode] = macroRW
  implicit private val explorerRW: ReadWriter[DependencyExplorerPage] = macroRW
  implicit private val rw: ReadWriter[Page]                           = macroRW

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


  private val router =
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

  private def renderMyPage($loginPage: Signal[DependencyExplorerPage], fullAppData: FullAppData) =

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

  private def splitter(fullAppData: FullAppData) =
    SplitRender[Page, HtmlElement](router.$currentPage)
      .collectSignal[DependencyExplorerPage](renderMyPage(_, fullAppData))
      .collectStatic(LoginPageOriginal) {
        div("Login page")
      }

  def app(fullAppData: FullAppData): Div = div(child <-- splitter(fullAppData).$view)
end LaminarApp

import zio.{Console, ZIO, ZIOAppDefault}

object DependencyExplorer extends ZIOAppDefault:

  import com.raquo.laminar.api.L.{*, given}
  import org.scalajs.dom
//  import org.scalajs.dom.experimental.serviceworkers.*

  // TODO: Rope zio-cli into this thing to make
  // the command line interface The
  // Right Way (TM). (The following may be the
  // sloppiest CLI args handling that
  // I have ever written.)
  def zprint[T: upickle.default.ReadWriter](x: T) =
    val pickled   = write(x)
    val depickled = read[T](pickled)
    Console.printLine(pprint(x, height = Int.MaxValue))

  val getJsData =
    val connectedX = JsDataConnected.connected
    val allX       = JsData.allProjectData
    for
      graph: Graph[Project, DiEdge] <- ZIO(ScalaGraph(allX))
    yield FullAppData(connectedX, allX, graph)

  def run =
    for
      //      _ <- ZIO.debug(Localized.)
      //      _ <- ZIO.debug(read[Seq[ProjectMetaData]](Localized.content))
      //      _ <- ZIO.debug(read[Seq[ConnectedProjectData]](Localized.content2))

//      appData <- SharedLogic.fetchAppData // TODO Local version of this?
      appData <- getJsData
      _       <- zprint(appData.all)
      _ <-
        ZIO {
          val appHolder = dom.document.getElementById("landing-message")
          appHolder.innerHTML = ""
          com.raquo.laminar.api.L.render(appHolder, LaminarApp.app(appData))
        }
      _ <- ZIO.debug("Laminar stuff goes here ZZZ")
    yield ()
end DependencyExplorer
