package org.ziverge

import upickle.default.{read, write}
import zio.{Chunk, Console, Task, ZIO, ZIOAppDefault, durationInt, ZLayer}
import upickle.default.{macroRW, ReadWriter as RW, *}
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.ziverge.DataView.*
import com.raquo.airstream.web.AjaxEventStream

/* Potential issue with ZIO-2.0.0-RC1 + SBT 1.6.1 */
sealed private trait Page

case class DependencyExplorerPage(
    targetProject: Option[String],
    dataView: DataView,
    filterUpToDateProjects: Boolean
) extends Page:
  def changeTarget(newTarget: String) =
    // dataView match {
    // case Dependencies =>
    // println("DependencyExplorerPage.changeTarget: " + newTarget)
    // Option.when(newTarget.nonEmpty)(copy(dataView = Dependencies(Some(newTarget))))
    // .getOrElse(copy(targetProject = Some(newTarget)))
    // case _ =>
    // }
    copy(targetProject = Some(newTarget))

private case object LoginPageOriginal extends Page

object DependencyViewerLaminar:
  import com.raquo.laminar.api.L._

  private val router = DependencyExplorerRouting.router

  def constructPage(
      busPageInfo: DependencyExplorerPage,
      pageUpdateObserver: Observer[DependencyExplorerPage],
      selectZioObserver: Observer[DependencyExplorerPage],
      viewUpdate: Observer[String],
      fullAppData: AppDataAndEffects
  ) =
    val filterUpToDateProjects = false // TODO Add to DependencyExplorerPage
    val upToDate: ConnectedProjectData => Boolean =
      p =>
        if (busPageInfo.filterUpToDateProjects)
          println("Only getting out-of-date projects")
          p.blockers.nonEmpty ||
          p.zioDep.fold(true)(zDep => zDep.zioDep.typedVersion.compareTo(fullAppData.fullAppData.currentZioVersion) < 0) &&
          !Data.coreProjects.contains(p.project)
        else
          println("Accepting all projects")
          true

    val dynamicHeader =
      busPageInfo.dataView match {
        case Dependencies => "Dependencies"
        case Dependents => "Dependendents"
        case Json => "N/A"
        case Blockers => "Blockers"
        case DotGraph => "N/A"
      }

    div(
      div(
        child <--
          fullAppData
            .dataSignal
            .map { fullAppDataLive =>

              val userFilter: ConnectedProjectData => Boolean =
                busPageInfo.targetProject match
                  case Some(filter) =>
                    project =>


                      val artifactMatches = project.project.artifactId.contains(filter)
                    // TODO Make this a function in a better spot
                    // project.dependants.exists(_.project.artifactId.contains(filter)) ||
                      busPageInfo.dataView match {
                        case Dependencies => artifactMatches || project.dependencies.exists(_.project.artifactId.contains(filter))
                        case Dependents => artifactMatches || project.dependants.exists(_.project.artifactId.contains(filter))
                        case Blockers => artifactMatches || project.blockers.exists(_.project.artifactId.contains(filter))
                        case Json => artifactMatches 
                        case DotGraph => artifactMatches 
                      }
                  case None =>
                    project => true


              val manipulatedData =
                fullAppDataLive
                  .connected
                  .filter(p=> upToDate(p) && userFilter(p))

              val dynamicHeader =
                busPageInfo.dataView match {
                  case Dependencies => "Dependencies"
                  case Dependents => "Dependendents"
                  case Json => "N/A"
                  case Blockers => "Blockers"
                  case DotGraph => "N/A"
                }
              div(
                table(
                  tr(
                    th("Artifact"),
                    th("Group"),
                    th("Version"),
                    th("ZIO Dep"),
                    th(dynamicHeader),
                  ),
                  manipulatedData.map{case ConnectedProjectData(

                    project,
                    version,
                    dependencies,
                    blockers,
                    dependants,
                    zioDep
                  ) =>
                    val dataColumn: Set[String] =
                      busPageInfo.dataView match {
                        case Dependencies =>dependencies.map(_.project.artifactId)
                        case Dependents =>dependants.map(_.project.artifactId)
                        case Json => Set()
                        case Blockers => blockers.map(_.project.artifactId)
                        case DotGraph => Set()
                      }
                    tr(
                      td(project.artifactId),
                      td(project.group),
                      td(version.value.replace("Version(", "").replace(")","")), // TODO Why does Version show up after the live data load?
                      td(zioDep.map(_.zioDep.version).getOrElse("N/A")),
                      td(dataColumn.mkString(",")),
                    )
                    },
                )
              )
            }
      ),
      // TODO Better result type so we can properly render different schemas
      // button("Select fake proejct", onClick.mapTo(busPageInfo) --> pageUpdateObserver),
      // button("Select ZIO", onClick.mapTo(busPageInfo) --> selectZioObserver)
    )
  end constructPage

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
          router.pushState(
            page.copy(dataView = DataView.fromString(dataView).getOrElse(DataView.Blockers))
          )
      )

    def refreshObserver(page: DependencyExplorerPage) =
      Observer[Int](onNext =
        dataView => println("should refresh here")
      // DataView.fromString(dataView).foreach(x => router.pushState(page.copy(dataView = x)))
      )

    def printTextInput(page: DependencyExplorerPage) =
      Observer[String](onNext =
        text =>
          router.pushState(page.changeTarget(text))
          println("Text: " + text)
      // DataView.fromString(dataView).foreach(x => router.pushState(page.copy(dataView = x)))
      )

    def upToDateCheckbox(page: DependencyExplorerPage) =
      Observer[Boolean](onNext =
        checkboxState =>
          println("Checkbox state: " + checkboxState)
          router.pushState(page.copy(filterUpToDateProjects = checkboxState))
      )

    val refresh = EventStream.periodic(5000)

//    val clickBus = new EventBus[]
    div(
      child <--
        $loginPage.map((busPageInfo: DependencyExplorerPage) =>
          val observer = refreshObserver(busPageInfo)
          div(
            // refresh --> refreshObserver(busPageInfo),
            refresh --> observer,
            div(
              span("Filter up-to-date projects"),
              input(
                typ := "checkbox",
                onClick.mapToChecked --> upToDateCheckbox(busPageInfo),
                defaultChecked := busPageInfo.filterUpToDateProjects
              )
            ),
            // TextInput().amend(onInput --> printTextInput),
            input(
              typ         := "text",
              placeholder := busPageInfo.targetProject.getOrElse(""),
              size        := 25,
              value       := busPageInfo.targetProject.getOrElse(""),
              onMountFocus,
              inContext { thisNode =>
                onInput.mapTo(thisNode.ref.value) --> printTextInput(busPageInfo)
              }
            ),
            select(
              inContext { thisNode =>
                onChange.mapTo(thisNode.ref.value.toString) --> viewUpdate(busPageInfo)
              },
              DataView
                .values
                .map(dataView =>
                  option(
                    value    := dataView.toString,
                    selected := (dataView == busPageInfo.dataView),
                    dataView.toString
                  )
                )
                .toSeq
            ),
            constructPage(
              busPageInfo,
              pageUpdateObserver,
              selectZioObserver,
              viewUpdate(busPageInfo),
              fullAppData
            )
          )
        )
    )
  end renderMyPage

  def app(fullAppData: AppDataAndEffects): Div =
    div(child <-- DependencyExplorerRouting.splitter(fullAppData).$view)
end DependencyViewerLaminar

import com.raquo.laminar.api.L.Signal
case class AppDataAndEffects(
    // TODO Get rid of redundant first field
    fullAppData: FullAppData,
    refreshAppData: () => FullAppData,
    dataSignal: Signal[FullAppData]
)

object DependencyExplorer extends ZIOAppDefault:

  // import com.raquo.laminar.api.L.{*, given}

  val refreshProjectData =
    () =>
      zio
        .Runtime
        .default
        .unsafeRun(
          ZioEcosystem
            .snapshot
            .provide(
              ZLayer.succeed[ZioEcosystem](AppDataHardcoded),
              ZLayer.succeed(DevConsole.word)
            )
        )
  val serverResponse =
    AjaxEventStream
      .get(
        url = "/api/kittens",
        responseType = "application/json"
      )                             // EventStream[dom.XMLHttpRequest]
      .map(req => req.responseText) // EventStream[String]

  def logic: ZIO[ZioEcosystem & Console, Throwable, Unit] =
    for
      appData <- ZioEcosystem.snapshot
      bag     <- ZIO.environmentWith[ZioEcosystem](x => x.get)
      console <- ZIO.environmentWith[Console](x => x.get)

      refresh =
        () =>
          zio
            .Runtime
            .default
            .unsafeRun(ZioEcosystem.snapshot.provide(ZLayer.succeed(bag), ZLayer.succeed(console)))
      // This shows that currently, we're only getting this information once upon loading.
      // We can envision some small changes that let us
      _ <-
        ZIO {
          val appHolder = dom.document.getElementById("landing-message")
          import com.raquo.laminar.api.L.{*, given}
          val dataSignal: Signal[FullAppData] =
            AjaxEventStream
              .get("/projectData")                             // EventStream[dom.XMLHttpRequest]
              .map(req => read[FullAppData](req.responseText)) // EventStream[String]
              .toSignal(appData)
          appHolder.innerHTML = ""
          com
            .raquo
            .laminar
            .api
            .L
            .render(
              appHolder,
              DependencyViewerLaminar
                .app(AppDataAndEffects(appData, refreshProjectData, dataSignal))
            )
        }
    yield ()

  def run =
    logic.provide(ZLayer.succeed[ZioEcosystem](AppDataHardcoded), ZLayer.succeed(DevConsole.word))

end DependencyExplorer
