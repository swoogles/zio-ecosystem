Talk @ 9:55 AM
# Problem

ZIO 2.0 approaches!
Build libraries on RCs, please!

---
# How It Started

- "We want to find out where everyone is at"

---
### How It Started

<img class="full-slide-image" width="850" src="images/spreadsheet_alpha.png" alt="ZIO World Presentation" />

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
![ecosystem.svg](ecosystem.svg)
---

```
zio-interop-reactivestreams    is on ZIO 2.0.0-RC2 and required by 3 projects: zio-aws-core,async-http-client-backend-zio,zio-amqp
zio-interop-cats               is on ZIO 2.0.0-RC2 and required by 2 projects: distage-core,tranzactio
zio-query                      is on ZIO 2.0.0-RC2 and required by 1 projects: caliban
zio-prelude                    is on ZIO 2.0.0-RC2 and required by 1 projects: zio-schema
zio-nio                        is on ZIO 2.0.0-RC2 and required by 1 projects: zio-actors
zio-json                       is on ZIO 2.0.0-RC2 and required by 1 projects: caliban
zio-zmx                        is on ZIO 2.0.0-RC2 and has no dependants.

```

---
# Current Web Phase

- ZHTTP
- *TODO DEMO* "advanced" open-PR query code
- 
---

pullRequests  
&nbsp; &nbsp;  .find(pr =>  
&nbsp; &nbsp; &nbsp; &nbsp;     pr.title.contains("zio") && pr.title.contains("2"))  

---
# Limitations
- *Only* checks projects against the latest version of ZIO
- List of tracked projects is hard-coded in src code

---
# Potential Future Features
TODO Turn these into Github issues before talk
- Show all transitive dependencies/dependents
- Checkboxes to construct multi-library SBT snippet
- More information on each project card
- Incorporate drawn graphs
