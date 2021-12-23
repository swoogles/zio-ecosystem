package org.ziverge

enum DataView:
  case Dependencies, Dependents, Json, Blockers

object SummaryLogic:

  def manipulateAndRender(
                           connectedProjects: Seq[ConnectedProjectData],
                           sort: ConnectedProjectData => Integer,
                           connectionMessage: ConnectedProjectData => String
                         ) =
    val currentZioVersion = Version("2.0.0-RC1")
    connectedProjects
      .filter(p =>
        p.blockers.nonEmpty ||
          p.zioDep
            .fold(true)(zDep => zDep.zioDep.typedVersion.compareTo(currentZioVersion) < 0) &&
            !Data.coreProjects.contains(p.project)
      ) // TODO Where to best provide this?
      .sortBy(sort)
      .reverse
      .sortBy(p => Render.sbtStyle(p.project)) // TODO remove after demo run
      .map { project =>
        val renderedZioDependency =
          if (Data.coreProjects.contains(project.project))
            "is a core project"
          else
            ZioDep.render(project.zioDep)
        f"${Render.sbtStyle(project.project)}%-50s ${renderedZioDependency} and " +
          connectionMessage(project)
      }
  end manipulateAndRender

