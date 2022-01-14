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
  def changeTarget(newTarget: String) = copy(targetProject = Some(newTarget))

private case object LoginPageOriginal extends Page

object DependencyViewerLaminar:
  import com.raquo.laminar.api.L._

  private val router = DependencyExplorerRouting.router

  def ExpandableProjectCard(
      project: ConnectedProjectData,
      busPageInfo: DependencyExplorerPage,
      fullAppDataLive: FullAppData
  ) =

    val toggleContentVisibility =
      Observer[org.scalajs.dom.html.Element](onNext =
        anchor =>
          println("Click click boom.")
          anchor
            .parentElement
            .querySelector(".card-content")
            .classList
            .toggle("is-hidden")
      // router.pushState(page.copy(filterUpToDateProjects = checkboxState))
      )

    val copySbtDependencyToClipboard =
      Observer[String](onNext =
        sbtText =>
          println("Click click boom.")
          dom.window.navigator.clipboard.writeText(sbtText)
      )



    def upToDateCheckbox(page: DependencyExplorerPage) =
      Observer[String](onNext =
        checkboxState =>

          println("Clicked on artifactId : " + checkboxState)
          val element = dom.document.getElementById(checkboxState)
          if (dom.document.getElementById(checkboxState) != null)
            element.scrollIntoView(top = true)
      )
    project match
      case ConnectedProjectData(
            project,
            version,
            dependencies,
            blockers,
            dependants,
            zioDep,
            latestZio // TODO Use
          ) => {

                      val onLatestZioDep: Option[ZioDep] => Boolean =
                        zioDep =>
                          zioDep.fold(true)(zDep =>
                            // TODO
                            zDep.zioDep.typedVersion.compareTo(fullAppDataLive.currentZioVersion) ==
                              0
                          )

                      val onLatestZio: ProjectMetaData => Boolean =
                        p =>
                          p.zioDep
                            .fold(true)(zDep =>
                              zDep.typedVersion.compareTo(fullAppDataLive.currentZioVersion) == 0
                            )
                      val projectIsUpToDate =
                        dependencies.forall(dep => onLatestZio(dep)) && onLatestZioDep(zioDep)
        div(
            div(
              cls := "container",
              idAttr := project.artifactIdQualifiedWhenNecessary,
              div(
                cls := "card is-fullwidth",
                header(
                  cls := "card-header",
                  inContext { thisNode => // TODO Move to header
                    val blah: org.scalajs.dom.html.Element = thisNode.ref
                    onClick.mapTo(thisNode.ref) --> toggleContentVisibility
                  },
                  p(cls := "card-header-title", 
                            UpToDateIcon(projectIsUpToDate),
                              project.githubUrl match
                                case Some(githubUrl) =>
                                  a(href := githubUrl, project.artifactIdQualifiedWhenNecessary)
                                case None =>
                                  project.artifactIdQualifiedWhenNecessary
                  ),
                  a(
                    cls := "card-header-icon card-toggle",
                    i(cls := "fa fa-angle-down") // TODO Fix icons
                  )
                ),
                div(
                  cls := "card-content is-hidden",
                  div(
                    cls := "content", {
                      

                      val dependencyDivs: Seq[Div] =
                            dependencies.map(dep =>
                              div(
                                cls := s"box ${colorUpToDate(onLatestZio(dep))}",
                                onClick.mapTo(dep.project.artifactIdQualifiedWhenNecessary) -->
                                  upToDateCheckbox(busPageInfo),
                                dep.project.artifactIdQualifiedWhenNecessary
                              )
                            )

                      println("Number of dependants: " + dependants.length)
                      val usedBy: Seq[Div] = // TODO This doesn't seem to be working :(
                            dependants.map(dep =>
                              div(
                                cls := s"box ${colorUpToDate(onLatestZio(dep))}",
                                dep.project.artifactIdQualifiedWhenNecessary
                              )
                            )

                      div(
                        cls    := "columns",
                        div(
                          cls :="column", span("Current Version: "), code(
                            inContext { thisNode => // TODO Move to header
                              onClick.mapTo(Render.sbtStyle(project,version)) --> copySbtDependencyToClipboard
                            },
                            Render.sbtStyle(project, version)
                          )
                        ),
                        div(
                          cls := "column",
                          span(
                            cls := s"box ${colorUpToDate(onLatestZioDep(zioDep))}",
                            "ZIO Version: " + zioDep.map(_.zioDep.version).getOrElse("N/A")
                          )
                        ),
                        div(cls := "column", div(span("Dependencies"), dependencyDivs.toSeq)),
                        div(cls := "column", div(span(s"Used By (${usedBy.length})"),usedBy.toSeq)),

                      )
                    }
                  )
                )
              )
            )
        )
          }
    end match
  end ExpandableProjectCard

  def constructPage(
      busPageInfo: DependencyExplorerPage,
      viewUpdate: Observer[String],
      fullAppData: AppDataAndEffects
  ) =
    def upToDateCheckbox(page: DependencyExplorerPage) =
      Observer[String](onNext =
        checkboxState =>
          println("Clicked on artifactId : " + checkboxState)
          val element = dom.document.getElementById(checkboxState)
          if (dom.document.getElementById(checkboxState) != null)
            element.scrollIntoView(top = true)
      )

    div(
      div(
        child <--
          fullAppData
            .dataSignal
            .map { fullAppDataLive =>
              fullAppDataLive match
                case None =>
                  div("No info to display!")
                case Some(fullAppDataLive) =>
                  val manipulatedData: Seq[ConnectedProjectData] =
                    FullAppData.filterData(
                      fullAppDataLive,
                      busPageInfo.dataView,
                      busPageInfo.filterUpToDateProjects,
                      busPageInfo.targetProject
                    )

                  div(
                    div(
                      // tr(
                      //   th("Artifact"),
                      //   th("Depends on ZIO Version"),
                      //   th(busPageInfo.dataView.toString)
                      // ),
                      manipulatedData.map { connectedProject =>
                        ExpandableProjectCard(connectedProject, busPageInfo, fullAppDataLive)

                      }
                    )
                  )

            }
      )
    )
  end constructPage

  def UpToDateIcon(upToDate: Boolean) =
    span(
      cls := "icon",
      img(
        src :=
          (if (upToDate)
             "/images/glyphicons-basic-739-check.svg"
           else
             "/images/glyphicons-basic-847-square-alert.svg")
      )
    )

  def colorUpToDate(upToDate: Boolean) =
    if (upToDate)
      "has-background-primary"
    else
      "has-background-warning"

  def labelledInput(labelContent: String, inputElement: ReactiveHtmlElement[dom.html.Element]) =
    // Param Type: DomHtmlElement
    div(
      cls := "field is-horizontal",
      div(cls := "field-label is-normal", label(cls := "label", labelContent)),
      div(cls := "field-body", div(cls := "field", p(cls := "control", inputElement)))
    )

  def renderMyPage($loginPage: Signal[DependencyExplorerPage], fullAppData: AppDataAndEffects) =

    val clickObserver = Observer[dom.MouseEvent](onNext = ev => dom.console.log(ev.screenX))
    def viewUpdate(page: DependencyExplorerPage) =
      Observer[String](onNext =
        dataView =>
          router.pushState(
            page.copy(dataView = DataView.fromString(dataView).getOrElse(DataView.Blockers))
          )
      )

    def refreshObserver(page: DependencyExplorerPage) =
      Observer[Int](onNext = dataView => println("should refresh here"))

    def printTextInput(page: DependencyExplorerPage) =
      Observer[String](onNext = text => router.pushState(page.changeTarget(text)))

    def upToDateCheckbox(page: DependencyExplorerPage) =
      Observer[Boolean](onNext =
        checkboxState =>
          println("Checkbox state: " + checkboxState)
          router.pushState(page.copy(filterUpToDateProjects = checkboxState))
      )

    val refresh = EventStream.periodic(5000)

//    val clickBus = new EventBus[]
    div(
      section(
        cls := "hero is-primary",
        div(
          cls := "hero-body",
          p(cls := "title", "ZIO Ecosystem"),
          p(
            cls := "subtitle",
            "Check which projects are on the latest and greatest version of ZIO!"
          )
        )
      ),
      child <--
        $loginPage.map((busPageInfo: DependencyExplorerPage) =>
          val observer = refreshObserver(busPageInfo)
          div(
            // refresh --> refreshObserver(busPageInfo),
            // refresh --> observer,
            labelledInput(
              "Hide up-to-date projects",
              input(
                typ := "checkbox",
                onClick.mapToChecked --> upToDateCheckbox(busPageInfo),
                defaultChecked := busPageInfo.filterUpToDateProjects
              )
            ),
            // TextInput().amend(onInput --> printTextInput),
            // Param Type: DomHtmlElement
            labelledInput(
              "Filter results by",
              input(
                typ         := "text",
                cls         := "input",
                placeholder := busPageInfo.targetProject.getOrElse(""),
                size        := 25,
                value       := busPageInfo.targetProject.getOrElse(""),
                placeholder := "Search for...",
                onMountFocus,
                inContext { thisNode =>
                  onInput.mapTo(thisNode.ref.value) --> printTextInput(busPageInfo)
                }
              )
            ),
            labelledInput(
              "Project introspection",
              select(
                cls := "select",
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
              )
            ),
            constructPage(busPageInfo, viewUpdate(busPageInfo), fullAppData)
          )
        )
    )
  end renderMyPage

  def app(fullAppData: AppDataAndEffects): Div =
    div(child <-- DependencyExplorerRouting.splitter(fullAppData).$view)
end DependencyViewerLaminar

import com.raquo.laminar.api.L.Signal
case class AppDataAndEffects(dataSignal: Signal[Option[FullAppData]])

object DependencyExplorer extends ZIOAppDefault:

  def logic: ZIO[Console, Throwable, Unit] =
    for
      console <- ZIO.environmentWith[Console](x => x.get)

      // This shows that currently, we're only getting this information once upon loading.
      // We can envision some small changes that let us
      _ <-
        ZIO {
          val appHolder = dom.document.getElementById("landing-message")
          import com.raquo.laminar.api.L.{*, given}
          val dataSignal
              : Signal[Option[FullAppData]] = // TODO Maybe make this Signal[Option[FullAppData]] ?
            AjaxEventStream
              .get("/projectData") // EventStream[dom.XMLHttpRequest]
              .map(req => Some(read[FullAppData](req.responseText))) // EventStream[String]
              .toSignal(None)                                        // TODO Maybe make this
          appHolder.innerHTML = ""
          com
            .raquo
            .laminar
            .api
            .L
            .render(appHolder, DependencyViewerLaminar.app(AppDataAndEffects(dataSignal)))
        }
    yield ()

  def run = logic.provide(ZLayer.succeed(DevConsole.word))

end DependencyExplorer
