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
      currentZioVersion: Version
  ) =

    val toggleContentVisibility =
      Observer[org.scalajs.dom.html.Element](onNext =
        anchor =>
          anchor.parentElement.querySelector(".card-content").classList.toggle("is-hidden")
      )

    val scrollToProject =
      Observer[String](onNext =
        checkboxState =>

          val element = dom.document.getElementById(checkboxState)
          if (dom.document.getElementById(checkboxState) != null)
            element.scrollIntoView(top = true)
      )
    project match
      case ConnectedProjectData(
            project,
            version,
            dependencies,
            dependants,
            zioDep,
            latestZio // TODO Use
          ) => {

        val onLatestZioDep: Option[ZioDep] => Boolean =
          zioDep =>
            zioDep.fold(true)(zDep =>
              // TODO
              zDep.zioDep.typedVersion.compareTo(currentZioVersion) == 0
            )

        val onLatestZio: ProjectMetaData => Boolean =
          p =>
            p.zioDep
              .fold(true)(zDep =>
                zDep.typedVersion.compareTo(currentZioVersion) == 0
              )
        val projectIsUpToDate =
          dependencies.forall(dep => onLatestZio(dep)) && onLatestZioDep(zioDep)
        div(
          div(
            cls    := "container",
            idAttr := project.artifactIdQualifiedWhenNecessary,
            div(
              cls := "card is-fullwidth",
              header(
                cls := "card-header",
                inContext { thisNode =>
                  val blah: org.scalajs.dom.html.Element = thisNode.ref
                  onClick.mapTo(thisNode.ref) --> toggleContentVisibility
                },
                p(
                  cls := "card-header-title",
                  UpToDateIcon(projectIsUpToDate),
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


                    def ConnectedProjectsContainer(title: String, connectedProjects: Seq[ProjectMetaData]) =
                          div(
                            cls := "box p-3",
                            span(
                              cls := "subtitle is-3",
                              s"$title ",
                              small(cls:="has-text-grey-dark", s"(${connectedProjects.length})")
                            ),
                            connectedProjects.map(dep =>
                              a(
                                cls := s"box p-3 ${colorUpToDate(onLatestZio(dep))}",
                                onClick.mapTo(dep.project.artifactIdQualifiedWhenNecessary) -->
                                  scrollToProject,
                                dep.project.artifactIdQualifiedWhenNecessary
                              )
                            ).toSeq
                          )

                    val usedBy: Seq[Div] =
                      dependants.map(dep =>
                        div(
                          cls := s"box p-3 ${colorUpToDate(onLatestZio(dep))}",
                          dep.project.artifactIdQualifiedWhenNecessary
                        )
                      )

                    div(
                      div(
                        cls := "columns",
                        div(
                          cls := "column",
                          div(cls := "subtitle is-3", "Current Version: "),
                          div(
                            code(Render.sbtStyle(project, version)),
                            ClipboardIcon(Render.sbtStyle(project, version))
                          )
                        ),
                        (
                          project.githubUrl match
                            case Some(githubUrl) =>
                              div(
                                cls := "column",
                                a(cls := "button is-size-4 is-info", href := githubUrl, "Github")
                              )
                            case None =>
                              div()
                        ),
                        div(
                          cls := "column",
                          div(cls:="subtitle is-3", "ZIO Version: "), div(
                            cls := s"box p-3 ${colorUpToDate(onLatestZioDep(zioDep))}",
zioDep.map(_.zioDep.version).getOrElse("N/A")
                          )
                        )
                      ),
                      div(
                        cls := "columns",
                        div(
                          cls := "column",
                          ConnectedProjectsContainer("Depends on", dependencies)
                        ),
                        div(
                          cls := "column",
                          div(
                            cls := "box p-3",
                            ConnectedProjectsContainer("Used By ", dependants)
                          )
                        )
                      )
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

  def ProjectListings(
      busPageInfo: DependencyExplorerPage,
      viewUpdate: Observer[String],
      fullAppData: AppDataAndEffects
  ) =
    def upToDateCheckbox(page: DependencyExplorerPage) =
      Observer[String](onNext =
        checkboxState =>
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
                      manipulatedData.map { connectedProject =>
                        ExpandableProjectCard(connectedProject, fullAppDataLive.currentZioVersion)

                      }
                    )
                  )

            }
      )
    )
  end ProjectListings

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

  def ClipboardIcon(sbtLink: String) =

    val copySbtDependencyToClipboard =
      Observer[String](onNext =
        sbtText =>
          dom.window.navigator.clipboard.writeText(sbtText)
      )

    a(
      cls := "icon",
      onClick.mapTo(sbtLink) --> copySbtDependencyToClipboard,
      img(src := "/images/glyphicons-basic-30-clipboard.svg")
    )

  def colorUpToDate(upToDate: Boolean) =
    if (upToDate)
      "has-background-success"
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
          router.pushState(page.copy(filterUpToDateProjects = checkboxState))
      )

    val refresh = EventStream.periodic(5000)

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
            labelledInput(
              "Hide up-to-date projects",
              input(
                typ := "checkbox",
                onClick.mapToChecked --> upToDateCheckbox(busPageInfo),
                defaultChecked := busPageInfo.filterUpToDateProjects
              )
            ),
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
            ProjectListings(busPageInfo, viewUpdate(busPageInfo), fullAppData)
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
