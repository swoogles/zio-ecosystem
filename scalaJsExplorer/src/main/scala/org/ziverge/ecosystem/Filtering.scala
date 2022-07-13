package org.ziverge

import org.ziverge.ecosystem.{ConnectedProjectData, FullAppData, TrackedProjects}


def filterData(fullAppData: FullAppData, filterUpToDateProjects: Boolean, userFilterFromPage: Option[String]) =

  val userFilter: ConnectedProjectData => Boolean =
    userFilterFromPage match
      case Some(filter) =>
        println("Front-end userFilter: " + filter)
        project =>
          val normalizedFilter = filter.toLowerCase

          val artifactMatches = project.project.artifactId.toLowerCase.contains(normalizedFilter)
          // TODO Make this a function in a better spot
          val introspectedDataMatches =
            project
              .dependencies
              .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter)) ||
              project
                .dependants
                .exists(_.project.artifactId.toLowerCase.contains(normalizedFilter))
          artifactMatches || introspectedDataMatches
      case None =>
        println("Skipping non-existent Front-end user filter")
        project => true

  val upToDate: ConnectedProjectData => Boolean =
    p =>
      if (filterUpToDateProjects)
        !p.onLatestZioDep && !TrackedProjects.coreProjects.contains(p.project)
      else
        true

  fullAppData.connected.filter(p => upToDate(p) && userFilter(p))
end filterData
