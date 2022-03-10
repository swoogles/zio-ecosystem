Talk @ 9:55 AM
# Problem

ZIO is approaching the 2.0 release.

It is still being developed, but is reasonably stable.

We want to encourage people to try out the RCs and release their libraries on top of it.

---
# How It Started

- Join Ziverge during the development of ZIO 2
- "Create a spreadsheet to track where everyone is at"

---
# Front-end agnostic logic
- Find the latest release of project on Maven
- Examine POM file to determine: 
  - Version of ZIO it depends on
  - ZIO project dependencies
  - ZIO projects that are dependent on it
  - Use ScalaGraph to achieve that
    - Show rendered graph example?

---
# CLI Phase

- Used ZIO-cli
  - *TODO DEMO* 1 cool feature/API decision
- Output just goes to terminal
  - Useful for me, but not for thee
- Constructed 3 different views of the data before I got annoyed

---
# Current Web Phase

- ZHTTP
- *TODO DEMO* "advanced" open-PR query code

---
# Limitations
- *Only* checks projects against the latest version of ZIO
- List of tracked projects is hard-coded in src code

---
# Potential Future Features
- Show all transitive dependencies/dependents
- Checkboxes to construct multi-library SBT snippet