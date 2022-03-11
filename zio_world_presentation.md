Talk @ 9:55 AM
# ZIO Ecosystem 

<div class="grid-container">
  <div class="grid-item">
    <p>
    Bill Frasure
    </p>

    <p>
      <a href="https://github.com/swoogles">
        github.com/swoogles
      </a>
    </p>

    <p>
      <a href="mailto:bill@billdingsoftware.com">
        bill@billdingsoftware.com
      </a>
    </p>
  </div>
  <div class="grid-item">
    <img class="full-slide-image" width="350" src="images/BilldingLogo.png" alt="Billding" />
  </div>
</div>


---

# Situation

ZIO 2.0 approaches!

We need the community to build on it!

---

# How It Started

- "Who is publishing on the 2.x releases?"
- "Who is working towards a release?"

---
# How It Started

<img class="full-slide-image" width="850" src="images/spreadsheet_alpha.png" alt="ZIO World Presentation" />

---
# Downsides
- Inconsistent verbiage/focus between projects
- Tables only take you so far
- Manually-entered data is stale immediately

---

# Win - Got a colleague interested

---
# Interesting Graphs

<img class="full-slide-image" width="600" src="images/ecosystem_graph.png" alt="Graph" />
Credit - Will Harvey

---
# Interesting Graphs

<img class="full-slide-image" width="520" src="images/ZioMetroSubGraph.png" alt="Graph" />

Credit - Will Harvey

---

# Downside - Output requires a person

---

# Win - We focused on connections

---
# Core Logic
- Find the latest release of project on Maven
- Examine POM file to determine: 
    - Version of ZIO it depends on
    - ZIO project dependencies
    - ZIO projects that depend on it

---
###  CLI Phase

<img class="full-slide-image" width="850" src="images/terminal_output.png" alt="Terminal Output" />

Built with ZIO-cli

---
### CLI Phase Downsides

- Output just goes to terminal
- Built 3 views before I got annoyed
- Useful, but not _easy_


---
# To the Web!

- Built with ZHTTP

---
# To the Web!

*DEMO*

- ZIO-config
  -  A solid, middle-network library that is green on both sides
- ZIO-nio
    - If you are comfortable with ZIO-nio, you might be able to help dependent projects upgrade
- TrazactIO
  - For active PR


---

# Advanced Machine-learning Algorithm

---

<pre>
pullRequests  
  .find(pr =>  
    pr.title.contains("zio") && pr.title.contains("2"))  
</pre>

---
# Potential Future Features

https://github.com/swoogles/zio-ecosystem/issues

---
# Thanks

Will Harvey, for the inspiring graphs and spurring me to take this beyond a spreadsheet

Adam Fraser, for guidance and goal-setting

Ziverge, for attracting enthusiastic engineers and letting them build