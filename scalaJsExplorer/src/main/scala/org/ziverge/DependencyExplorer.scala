package org.ziverge

import upickle.default.{read, write}
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt, ZLayer}
import upickle.default.{macroRW, ReadWriter as RW, *}
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement

sealed private trait Page

case class DependencyExplorerPage(
    time: Option[String], // TODO Make this a WallTime instead
    targetProject: Option[String],
    dataView: DataView
) extends Page:
  def changeTarget(newTarget: String) = copy(targetProject = Some(newTarget))

private case object LoginPageOriginal extends Page

object DependencyViewerLaminar:
  import com.raquo.laminar.api.L._

  private val router = DependencyExplorerRouting.router

  def constructPage(
      busPageInfo: DependencyExplorerPage,
      pageUpdateObserver: Observer[DependencyExplorerPage],
      selectZioObserver: Observer[DependencyExplorerPage],
      viewUpdate: Observer[String],
      fullAppData: FullAppData
  ) =
    div(
      div("time query param value: " + busPageInfo.time),
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
        onClick.mapTo(busPageInfo) --> pageUpdateObserver
      ),
      button("Select ZIO", onClick.mapTo(busPageInfo) --> selectZioObserver)
    )

  def renderMyPage($loginPage: Signal[DependencyExplorerPage], fullAppData: AppDataAndEffects) =

    val clickObserver = Observer[dom.MouseEvent](onNext = ev => dom.console.log(ev.screenX))
    val pageUpdateObserver =
      Observer[DependencyExplorerPage](onNext =
        page => router.pushState(page.changeTarget("fake.click.project"))
      )
    val selectZioObserver =
      Observer[DependencyExplorerPage](onNext =
        page => router.pushState(page.changeTarget("dev.zio.zio"))
      )
    def viewUpdate(page: DependencyExplorerPage) =
      Observer[String](onNext =
        dataView =>
          DataView.fromString(dataView).foreach(x => router.pushState(page.copy(dataView = x)))
      )

    def refreshObserver(page: DependencyExplorerPage) =
      Observer[Int](onNext =
        dataView =>
          println("should refresh here")
          // DataView.fromString(dataView).foreach(x => router.pushState(page.copy(dataView = x)))
      )

    val refresh = EventStream.periodic(5000)

//    val clickBus = new EventBus[]
    div(
      child <--
        $loginPage.map((busPageInfo: DependencyExplorerPage) => {
            val observer = refreshObserver(busPageInfo) 
            div(
              // refresh --> refreshObserver(busPageInfo),
              refresh --> observer,
              select(
                inContext { thisNode =>
                  onChange.mapTo(thisNode.ref.value.toString) --> viewUpdate(busPageInfo)
                },
                DataView
                  .values
                  .map(dataView => option(value := dataView.toString, dataView.toString))
                  .toSeq
              ),
              constructPage(
                busPageInfo,
                pageUpdateObserver,
                selectZioObserver,
                viewUpdate(busPageInfo),
                fullAppData.fullAppData
              )
            )
              }
        )
    )
  end renderMyPage

  def app(fullAppData: AppDataAndEffects): Div =
    div(child <-- DependencyExplorerRouting.splitter(fullAppData).$view)
end DependencyViewerLaminar

case class AppDataAndEffects(
  fullAppData: FullAppData, refreshAppData: () => FullAppData
)

object DependencyExplorer extends ZIOAppDefault:

  // import com.raquo.laminar.api.L.{*, given}

  val refreshProjectData =
    () => 
      zio.Runtime.default.unsafeRun(ZioEcosystem.snapshot
        .provide(ZLayer.succeed[ZioEcosystem](AppDataHardcoded), ZLayer.succeed(DevConsole.word))
      )

  def logic: ZIO[ZioEcosystem & Console, Throwable, Unit] =
    for
      appData <- ZioEcosystem.snapshot
      bag <- ZIO.environmentWith[ZioEcosystem]( x => x.get)
      console <- ZIO.environmentWith[Console]( x => x.get)

      refresh =

        () => 
        zio.Runtime.default.unsafeRun(ZioEcosystem.snapshot
          .provide(ZLayer.succeed(bag), ZLayer.succeed(console))
        )
      // This shows that currently, we're only getting this information once upon loading.
      // We can envision some small changes that let us 
      _ <-
        ZIO {
          val appHolder = dom.document.getElementById("landing-message")
          appHolder.innerHTML = ""
          com.raquo.laminar.api.L.render(appHolder, DependencyViewerLaminar.app(AppDataAndEffects(appData, refreshProjectData)))
        }
    yield ()

  def run =
    logic.provide(ZLayer.succeed[ZioEcosystem](AppDataHardcoded), ZLayer.succeed(DevConsole.word))

end DependencyExplorer
