package org.ziverge.ecosystem

import com.raquo.airstream.web.AjaxEventStream
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.ziverge.ecosystem.{Bulma, Glyphicons, Routing}
import org.ziverge.*
import upickle.default.{macroRW, read, write, ReadWriter as RW, *}
import urldsl.errors.DummyError
import urldsl.language.QueryParameters

sealed private trait Page

case class DependencyExplorerPage(targetProject: Option[String], filterUpToDateProjects: Boolean) extends Page:
  def changeTarget(newTarget: String) = copy(targetProject = Some(newTarget))

private case object LoginPageOriginal extends Page

object DependencyViewerLaminar:
  import com.raquo.laminar.api.L.*
  import Bulma._

  private val router = Routing.router

  def ConnectedProjectsContainer(
                                  title: String,
                                  connectedProjects: Seq[ProjectMetaDataSmall],
                                  currentZioVersion: Version
                                ) =

    val scrollToProject =
      Observer[String](onNext =
        checkboxState =>
          Option(
            dom.document.getElementById(checkboxState)
          ).foreach{element =>
            element.querySelector(".card-content").classList.remove("is-hidden")
            element.scrollIntoView(top = true)
          }
      )

    div(
      Bulma.boxed.p3,
      span(
        Bulma.size5,
        s"$title ",
        small(Bulma.hasTextGrey, s"(${connectedProjects.length})")
      ),
      connectedProjects.map(dep =>
        a(
          Bulma.boxed.p3,
          colorUpToDate(dep.onLatestZio(currentZioVersion)),
          onClick.mapTo(dep.project.artifactIdQualifiedWhenNecessary) -->
            scrollToProject,
          dep.project.artifactIdQualifiedWhenNecessary
        )
      )
    )

  def Column(content: ReactiveHtmlElement[org.scalajs.dom.HTMLElement]*) =
    div(Bulma.column, content.toSeq)

  def Columns(content: ReactiveHtmlElement[org.scalajs.dom.HTMLElement]*) =
    div(Bulma.columns,
      content.toSeq.map(Column(_))
    )


  def GitStuff(project: Project,
               relevantPr: Option[PullRequest]
              ) =
    project
      .githubUrl
      .map(githubUrl =>
          div(
            relevantPr
              .map(pr =>
                Seq(
                  div(
                    a(
                      Bulma.size5.button.isInfo.m3,
                      href := pr.html_url,
                      "ZIO Upgrade PR*"
                    )
                  ),
                  div(small("* Best Effort. Not guaranteed to be relevant."))
                )
              )
          )
      ).getOrElse(div())

  def ExpandableProjectCard(project: ConnectedProjectData, currentZioVersion: Version) =
    project match
      case connectedProject @ ConnectedProjectData(
      project,
      version,
      dependencies,
      dependants,
      zioDep,
      latestZio, // TODO Use
      relevantPr
      ) =>
          div(
            Bulma.container,
            idAttr := project.artifactIdQualifiedWhenNecessary,
            div(
              Bulma.card.isFullWidth,
              header(
                Bulma.cardHeader,
                inContext { thisNode =>
                  val blah: org.scalajs.dom.html.Element = thisNode.ref
                  onClick.mapTo(thisNode.ref) -->
                    (anchor => anchor.parentElement.querySelector(".card-content").classList.toggle("is-hidden"))
                },
                p(
                  Bulma.cardHeaderTitle,
                  UpToDateIcon(connectedProject.onLatestZioDep),
                  project.artifactIdQualifiedWhenNecessary
                ),
                a(
                  Bulma.cardHeaderIcon.cardToggle,
                  i(Bulma.fa.faAngleDown) // TODO Fix icons
                )
              ),
              div(
                Bulma.cardContent.isHidden,
                div(
                  Bulma.content,
                    Columns(
                      // TODO Turn these raw divs into component defs
                      div(
                          a(
                            Bulma.size5,
                            href := project.githubUrl.getOrElse(""),
                            img(
                              src := "images/GitHub-Mark-64px.png",
                              styleAttr := "width: 1.0em; height: 1.0em;"
                            )
                          ),
                          Bulma.size5,
                        span(
                          Bulma.p3,
                          span("Latest: "),
                          span(
                            code(project.sbtDependency(version)),
                            ClipboardIcon(project.sbtDependency(version))
                          )
                        )
                        ),
                      ),
                          GitStuff(
                            project,
                              relevantPr
                          ),
                        div(
                          Bulma.p3,
                          span(
                            "ZIO Version: ",
                          ),
                          span(
                            Bulma.hasTextWeightBold,
                            zioDep.map(dep => dep.zioDep.typedVersion.value).getOrElse("N/A")
                          )
                        )
                      ),
                  Columns(
                    ConnectedProjectsContainer("Depends on", dependencies, currentZioVersion),
                    ConnectedProjectsContainer("Used By ", dependants, currentZioVersion)
                  )
                )
              )
          )
    end match
  end ExpandableProjectCard

  def ProjectListings(busPageInfo: DependencyExplorerPage, fullAppData: AppDataAndEffects) =
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
                  filterData(fullAppDataLive, busPageInfo.filterUpToDateProjects, busPageInfo.targetProject)

                div(
                  EcosystemSummary(
                    // TODO Probably want to move this bit of logic into FullAppData
                    numberOfTrackedProjects = fullAppDataLive.connected.length,
                    numberOfCurrentProjects = fullAppDataLive.connected.count(_.onLatestZioDep)
                  ),
                  div(
                    manipulatedData.map { connectedProject =>
                      ExpandableProjectCard(connectedProject, fullAppDataLive.currentZioVersion)

                    }
                  )
                )

          }
    )
  end ProjectListings

  def UpToDateIcon(upToDate: Boolean) =
    span(
      Bulma.icon,
      img(
        src :=
          (if (upToDate)
            Glyphicons.check
           else
            Glyphicons.squareAlert )
      )
    )

  def ClipboardIcon(content: String) =
    a(
      Bulma.icon,
      onClick.mapTo(content) --> (sbtText => dom.window.navigator.clipboard.writeText(sbtText)),
      img(src := Glyphicons.clipboard)
    )

  def colorUpToDate(upToDate: Boolean): Mod[ReactiveHtmlElement[_]] =
    if (upToDate)
      Bulma.hasBackgroundSuccess
    else
      Bulma.hasBackgroundWarning

  def labelledInput(labelContent: String, inputElement: ReactiveHtmlElement[dom.html.Element]) =
    div(
      Bulma.field.isHorizontal,
      div(Bulma.fieldLabel.isNormal, label(Bulma.labelB, labelContent)),
      div(Bulma.fieldBody, div(Bulma.field, p(Bulma.control, inputElement)))
    )

  def EcosystemSummary(numberOfTrackedProjects: Int, numberOfCurrentProjects: Int) =
    div(
      Bulma.boxed.p3,
      div("Total Tracked Projects: " + numberOfTrackedProjects),
      div("Up-to-date Projects: " + numberOfCurrentProjects)
    )

  def renderMyPage($loginPage: Signal[DependencyExplorerPage], fullAppData: AppDataAndEffects) =

    def updateFilterInUrl(page: DependencyExplorerPage) =
      Observer[String](onNext =
        dataView =>
          router.pushState(
            page.copy()
          )
      )

    def updateSearchParameterInUrl(page: DependencyExplorerPage) =
      Observer[String](onNext = text => router.pushState(page.changeTarget(text)))

    def upToDateCheckbox(page: DependencyExplorerPage) =
      Observer[Boolean](onNext =
        checkboxState => router.pushState(page.copy(filterUpToDateProjects = checkboxState))
      )

    div(
      child <--
        $loginPage.map((busPageInfo: DependencyExplorerPage) =>
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
              "Show projects that involve:",
              input(
                typ         := "text",
                Bulma.inputB,
                placeholder := busPageInfo.targetProject.getOrElse(""),
                size        := 25,
                value       := busPageInfo.targetProject.getOrElse(""),
                placeholder := "Search for...",
                onMountFocus,
                inContext { thisNode =>
                  onInput.mapTo(thisNode.ref.value) --> updateSearchParameterInUrl(busPageInfo)
                }
              )
            ),
            ProjectListings(busPageInfo, fullAppData)
          )
        )
    )
  end renderMyPage

  def app(fullAppData: AppDataAndEffects): Div =
    div(child <-- Routing.splitter(fullAppData).$view)

end DependencyViewerLaminar

import com.raquo.laminar.api.L.Signal
case class AppDataAndEffects(dataSignal: Signal[Option[FullAppData]])

object DependencyExplorer extends App:

  // This shows that currently, we're only getting this information once upon loading.
  val appHolder = dom.document.getElementById("landing-message")
  val dataSignal
      : Signal[Option[FullAppData]] =
    AjaxEventStream
      .get("/projectData")
      .map(req => Some(read[FullAppData](req.responseText)))
      .toSignal(None)
  appHolder.innerHTML = ""
  com
    .raquo
    .laminar
    .api
    .L
    .render(appHolder, DependencyViewerLaminar.app(AppDataAndEffects(dataSignal)))

end DependencyExplorer
