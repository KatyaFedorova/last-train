# Research: Matrix Web Game on GitHub Pages

## Current state (verified 2026-09-25)

- `bb spec`: 86 examples, 0 failures.
- Acceptance: all **56 scenarios in 18 feature files pass**. That run used the Babashka `gherkin-parser` from `.swarmforge/tools/Acceptance-Pipeline-Specification`. `bb acceptance` itself fails only because it looks for a `gherkin-parser` binary on PATH, and SwarmForge's `swarm_tool.sh` isn't on PATH outside its panes.
- SwarmForge (coder and refactorer lanes, plus `handoffd`) is **still running** and committing to `main`. Between 69b2add and 3a011ec it added a **server-rendered** web page: `src/last_train/web.clj`, `adapter/last_train/web_main.clj` and `bin/last-train-web`. That page posts HTML forms to a Babashka HTTP server, with dropdowns and buttons, on `localhost:8080`.
- That page can't run on GitHub Pages, which only serves static files. It also conflicts with the spec: moves must be typed only (FR-004), and there must be no server (FR-013).

## R1 — How to run the existing Clojure game in a static page

- **Decision**: **Scittle**, the SCI Clojure interpreter for the browser (the same interpreter family as Babashka). It loads from jsDelivr, pinned to one exact version.
  - The four core namespaces become `.cljc`: `logic`, `english`, `game` and `puzzles`. Babashka runs them for the CLI and tests, and Scittle runs the same files in the browser.
- **Rationale**:
  - The same source files run in both places, so FR-001 (same behavior as the terminal) holds by construction.
  - There is no build toolchain. Publishing just copies files.
  - The constitution says "prefer Babashka", and Scittle is SCI, the interpreter Babashka runs on.
  - The code is small (~360 lines of core), so interpretation speed doesn't matter.
- **Portability fixes needed**:
  - `english.clj` calls `java.util.regex.Pattern/quote`. This becomes a `#?(:clj … :cljs …)` reader conditional, with a JS regex-escape on the cljs side.
  - `game.clj` uses the `(?i)` prefix. cljs `re-pattern` turns a leading `(?i)` into a JS flag, so no change is needed. A spec will cover it.
  - `parse-long` and `update-vals` exist in cljs 1.11+, which Scittle bundles. A browser smoke test will confirm this.
- **Alternatives rejected**:
  - *shadow-cljs / cljs compile*: this needs a Node + JVM build in CI and locally, which is heavier for no user benefit.
  - *Rewriting in JavaScript*: two copies of the rules would drift, which breaks FR-001.
  - *Keeping the server page*: it can't be hosted on Pages.

## R2 — Terminal UI

- **Decision**: a single static `index.html` + `terminal.css` + `terminal.cljs`.
  - The page shows a scrolling transcript `<pre>` and one `<input>` prompt line with `autocapitalize=off`, `autocorrect=off` and `spellcheck=false`.
  - A pure `last-train.terminal` `.cljc` namespace holds all session logic: new game, random puzzle choice, `new`/`help` commands and the syntax hint. The DOM glue in `web/terminal.cljs` only appends lines and reads input.
  - The falling-code background is one `<canvas>`, drawn only when `prefers-reduced-motion` is not set.
- **Rationale**: the constitution requires testable logic to be separate from environment-bound code. The DOM glue stays tiny, and everything else is spec'd in Babashka.
- **Colors**: text is `#33ff66` on `#000`, a contrast ratio of about 15:1, which is well above AA (FR-011). Dim text is `#1fae4a`, about 7:1.

## R3 — Web acceptance (FR-016) — REVISED 2026-09-25: no browser automation

- **Superseded decision (kept for history)**: etaoin, run from Babashka, drives headless Chrome through chromedriver.
- **Current decision (user)**: no browser tests. The web step handlers call `last-train.terminal/boot` and `submit` directly, the same functions `web/terminal.cljs` calls. They also check the built `build/site/index.html` for `#command`, `#transcript`, `noscript` and no role leaks. The live site gets an HTTP 200 check for `index.html` and every `.cljc` file. chromedriver is not needed. The `web_steps.clj` step handlers are rewritten to:
  - open the page from a local static server
  - type commands into the prompt
  - read the transcript text from the DOM.
- **Rationale**:
  - The existing APS runtime, generator and Speclj runner stay unchanged. Only the web step handlers change.
  - etaoin works under Babashka.
  - GitHub's `ubuntu-latest` runners ship Chrome and chromedriver.
  - Locally this needs `brew install chromedriver`; Chrome is already installed.
- **Alternatives rejected**:
  - *Playwright (Node)*: it would need a second test runtime and a bridge to the Clojure step handlers.
  - *Checking HTML strings only*: this breaks FR-016, and it can't see what Scittle renders.

## R4 — Running the acceptance suite without SwarmForge (FR-015)

- **Decision**: `bin/acceptance` resolves the parser in this order:
  1. `gherkin-parser` on PATH
  2. the Babashka APS tool at `$APS_HOME`, which defaults to `.swarmforge/tools/Acceptance-Pipeline-Specification`
  3. if neither exists, a clear error that names both options.
- CI clones `unclebob/Acceptance-Pipeline-Specification` at a pinned commit into `$APS_HOME`.
- **Rationale**:
  - The constitution prefers the Babashka APS tools.
  - A fresh clone has no `.swarmforge/` directory, because it's excluded in `.git/info/exclude`.
- `bb acceptance` also starts and stops the local static server around the web scenarios.

## R5 — Puzzles (FR-007…007c)

- **Decision**:
  - Keep `reference` and add 4 hand-authored puzzles, each named: `reference`, `commuters`, `night-shift`, `terminus` and `red-eye`.
  - Each has a distinct true world, so different seats are the Agent across puzzles.
  - A search script (`bb puzzle-search`, a dev-only tool) proposes candidate openings, which are checked against the §2.5 rules using `logic/consistent` and `logic/solvable?`.
  - The chosen puzzles are written into `puzzles.cljc` by hand, with names and bios.
- **Validity check**: `puzzles/valid?` is spec'd for each puzzle, and a feature file (`puzzle-catalog.feature`) runs it for every name. It checks:
  - `true-world ∈ consistent(opening)`
  - `2 ≤ |consistent| ≤ 4`
  - at least 2 distinct Agent seats remain
  - `¬solvable?(1)` and `solvable?(2)`.
- **Selection**:
  - `puzzles/pick` takes a random source and the previous name, and never returns the previous name.
  - The web page reads `?puzzle=<name>`. An unknown name shows a message and falls back to a random puzzle.
  - The CLI gains `--seed <name>` for every name, plus `--seed random`.
  - The CLI default stays `reference`, so the existing CLI features don't change.
- **Alternative rejected**: a runtime random generator (spec Q2 option C), which is out of scope.

## R6 — Publishing (FR-013, FR-014, SC-006)

- **Decision**: a GitHub Actions workflow, `.github/workflows/pages.yml`, triggered on push to `main`. It has two jobs:
  1. `verify`: install Babashka, clone APS, run `bb spec` and `bb acceptance`.
  2. `deploy`: needs `verify`. It runs `bb site` to copy static files into `build/site/`, then `actions/upload-pages-artifact` and `actions/deploy-pages`.
- Pages source is "GitHub Actions". Enable it once with `gh api -X POST repos/KatyaFedorova/last-train/pages -f build_type=workflow`.
- **URL**: `https://katyafedorova.github.io/last-train/`. Every asset path is relative, so the `/last-train/` sub-path works.
- **Alternative rejected**: a `gh-pages` branch pushed from a local script. It's manual, which breaks SC-006, and nothing checks it.

## R7 — What happens to the SwarmForge server page

- **Decision**: replace it.
  - Delete: `src/last_train/web.clj`, `adapter/last_train/web_main.clj`, `spec/last_train/web_spec.clj`, `property/last_train/web_property_spec.clj`, `hardening/last_train/web_hardening_spec.clj` and `.metrics/mutate/last_train/web.edn`.
  - `bin/last-train-web` becomes "serve `build/site/` locally", using `babashka.http-server`.
- **Rationale**: it's the wrong architecture for static hosting, and its dropdown and button controls contradict FR-004.
- **Precondition**: **stop SwarmForge before implementing**. Otherwise both will commit to `main` and conflict. Its coder lane is actively extending this server page.
