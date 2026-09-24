---
description: "Task list for Matrix Web Game on GitHub Pages"
---

# Tasks: Matrix Web Game on GitHub Pages

**Input**: Design documents from `specs/001-matrix-web-game/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (player-commands.md, web-page.md, cli.md), quickstart.md

**Tests**: REQUIRED. The spec asks for them in FR-015, FR-016 and FR-007a, and the project constitution requires Speclj specs and APS acceptance. Write each spec or feature before its implementation, and see it fail first.

**Organization**: tasks are grouped by user story (US1 = play in browser, US2 = GitHub Pages, US3 = verifiable suite + docs).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- All paths are relative to the repository root `/Users/katya/last-train/last-train`

## Ground rules for every task

- Run `bb spec` after every source change. Run `bb acceptance` at each checkpoint. Run them one after the other, never at the same time.
- Specs use **Speclj** (`speclj.core`), not `clojure.test`.
- Do not hand-edit `.metrics/mutate/**` or the `# mutation-stamp` / `acceptance-mutation-manifest` headers in `features/*.feature`.
- Commit after each task or logical group, ending the message with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

## Phase 1: Setup

**Purpose**: make the repo safe to work in and the tools runnable.

- [X] T001 Confirm SwarmForge is stopped. `ps aux | grep -E "SwarmForge (Coder|Refactorer)|handoffd.bb" | grep -v grep` must print nothing. If it prints anything, STOP and ask the user to stop SwarmForge. Then `git pull --ff-only` on `main`.
- [X] T002 Make `bin/acceptance` resolve the parser in this order (research R4): (1) `gherkin-parser` on PATH; (2) `bb --config "$APS_HOME/bb.edn" gherkin-parser`, where `APS_HOME` defaults to `.swarmforge/tools/Acceptance-Pipeline-Specification`; (3) otherwise exit 1 with a message naming both options and the clone URL `https://github.com/unclebob/Acceptance-Pipeline-Specification`. Write the IR to `build/acceptance/ir/` as it does now.
- [X] T003 Run `bb acceptance`. Expect **56 examples, 0 failures**, the baseline recorded in research.md. Save the tail of the output in the commit message for T002.
- [X] T004 [P] (Done differently: Babashka's built-in `org.httpkit.server` is used, so no new dependency.) Add `org.babashka/http-server` (latest) to `:deps` in `bb.edn`. Run `bb -e "(require 'babashka.http-server)"` to confirm it loads. Do NOT add etaoin; there are no browser tests.
- [X] T005 [P] Create the `web/` directory (static page sources) with a `.gitkeep`. Confirm that `build/` is still listed in `.gitignore`.

---

## Phase 2: Foundational (blocking all stories)

**Purpose**: make the game core portable to the browser without changing behavior (research R1).

- [X] T006 `git mv` these files to `.cljc`, keeping the namespace names: `src/last_train/logic.clj` → `logic.cljc`, `english.clj` → `english.cljc`, `game.clj` → `game.cljc`, `puzzles.clj` → `puzzles.cljc`. Leave `cli.clj` as it is. Run `bb spec` and `bb acceptance`; both stay green.
- [X] T007 In `src/last_train/english.cljc`, replace `java.util.regex.Pattern/quote` inside `alternation` with a reader conditional: `#?(:clj (java.util.regex.Pattern/quote w) :cljs (clojure.string/replace w #"[.*+?^${}()|\[\]\\]" "\\$&"))`. Add a Speclj example in `spec/last_train/english_spec.clj` asserting that a persona name containing a dot (`"Mr. Grey"`) still resolves through `resolve-seat` and `seat-pattern`.
- [X] T008 Grep `src/last_train/*.cljc` for other JVM-only forms (`java.`, `Long/`, `Integer/`, `format`, `.getBytes`, `Math/`) and wrap each one in `#?(:clj … :cljs …)`. If none are found, record that in the commit message.
- [X] T009 Check the ClojureScript side without a browser. Use **nbb** (SCI on Node, the same interpreter as Scittle, with the same `:cljs` reader branch): add `bin/cljs-smoke`, which runs `npx --yes nbb@latest -cp src -e "(require '[last-train.game :as g] '[last-train.puzzles :as p]) (run! println (:output (g/start p/reference)))"`. Its output must equal the first 12 lines of `bin/last-train --seed reference </dev/null`. Also run `(g/handle state "ASK B Is D an Agent?")` to check that the `(?i)` regex works. If `parse-long`, `update-vals` or `(?i)` fail, fix them with reader conditionals in the `.cljc` files. Pin the Scittle version (latest on npm) in `research.md` R1. Add `bin/cljs-smoke` to `bb acceptance`'s preflight when `npx` is available, and print a skip notice otherwise.

**Checkpoint**: `bb spec` and `bb acceptance` are green, and the core runs unchanged in the browser.

---

## Phase 3: User Story 1 — Play the full game in a Matrix-style browser terminal (P1) 🎯 MVP

**Goal**: a static page (no server logic) with a black background and green terminal where the player types `ask`/`accuse`/`new`/`help`. It behaves exactly like the CLI, and a random puzzle is picked from a catalog of 5.

**Independent Test**: run `bin/last-train-web` and open `http://localhost:8080/?puzzle=reference`. Typing `ask B Is D an Agent?` then `accuse A ally D` shows `PERFECT RUN`, `Score: 200` and `GAME OVER`, the same as `printf 'ask B Is D an Agent?\naccuse A ally D\n' | bin/last-train --seed reference`. Then `new` starts a different puzzle.

### 3a — Unrecognised command shows the syntax (shared core change)

- [ ] T010 [US1] In `spec/last_train/game_spec.clj`, add these Speclj examples:
  - `(game/handle state "hello")` returns the state unchanged, with output `["Operator: \"Signal's noisy. Rephrase.\"" "Ask: ask <passenger> <question>   Accuse: accuse <passenger> [ally <passenger>]"]`.
  - `(game/handle state "ask B what is love?")` still returns `["Operator: \"Signal's noisy. Rephrase.\"" "Questions left: 3"]` (unchanged).
- [ ] T011 [US1] Implement it in `src/last_train/game.cljc`: in `handle`, the final fallback branch (input that matches neither `accuse` nor `ask`) returns the noisy line plus the syntax line. Pull the syntax string out into one private def and reuse it in `round-banner`. `bb spec` and `bb acceptance` stay green, including `features/cli-invalid-question.feature`.

### 3b — Pure terminal session (`last-train.terminal`)

- [ ] T012 [P] [US1] Write `spec/last_train/terminal_spec.clj` (Speclj) for the contract in `data-model.md` § Terminal session and `contracts/player-commands.md` § Web-only commands. Pass a deterministic `rand-int` stub. Cover:
  - `boot` with `:puzzle-param "reference"`: `:lines` begin with `LAST TRAIN` and the four reference opening lines, and include `Questions left: 3`.
  - `boot` with `"nope"`: the first line is `Unknown puzzle "nope". Boarding a random train.` with kind `:system`, then a game starts.
  - `boot` with `nil`: a random puzzle is used.
  - `submit "ask B Is D an Agent?"`: adds a `:player` line `> ask B Is D an Agent?`, then `Tomasz (B): "No. Mr. Grey is not an Agent."` with kind `:passenger`.
  - `submit "accuse A ally D"`: the outcome lines have kind `:outcome`, followed by `Type "new" to play again.`
  - `submit "new"`, `"restart"` and `"PLAY AGAIN"`: each starts a new game whose puzzle `:name` ≠ the previous one, both mid-game and after game over.
  - `submit "help"`: adds the syntax line, and the game state is unchanged.
  - Line kinds: lines starting `Operator:` are `:operator`; `<Name> (<Seat>): "…"` lines are `:passenger`; lines containing `WIN`, `LOSE`, `PERFECT RUN`, `Score:` or `GAME OVER` are `:outcome`; everything else is `:system`.
  - No line's text contains the words `true-world`, `:agent`, `:awake` or `:sleeper` before game over (FR-003).
- [ ] T013 [US1] Implement `src/last_train/terminal.cljc` (namespace `last-train.terminal`) with `boot`, `submit` and `prompt-enabled?` as in `data-model.md`. It must call `game/start`, `game/handle`, `puzzles/by-name` and `puzzles/pick`, and must not re-implement any game rule (constitution: IO-near modules don't reimplement domain questions). Until T023 lands, `pick` may only return `reference`. Keep the `new`-yields-a-different-puzzle examples of T012 marked `(pending)` until T025.

### 3c — Static page (DOM glue, untestable boundary kept thin)

- [ ] T014 [P] [US1] Create `web/index.html` following `contracts/web-page.md`:
  - `<meta name="viewport" content="width=device-width, initial-scale=1, interactive-widget=resizes-content">` and `<title>LAST TRAIN</title>`.
  - `<link rel="stylesheet" href="terminal.css">`.
  - `<canvas id="rain" aria-hidden="true">`.
  - `<div id="transcript" role="log" aria-live="polite">`.
  - A `<form id="prompt">` with a `<span>&gt;</span>` and `<input id="command" type="text" autocapitalize="off" autocorrect="off" spellcheck="false" autocomplete="off" enterkeyhint="send" aria-label="Command">`.
  - `<noscript>LAST TRAIN needs JavaScript enabled.</noscript>`.
  - Scittle `<script src>` pinned to the version from T009.
  - `<script type="application/x-scittle" src=…>` tags in this order: `last_train/logic.cljc`, `last_train/english.cljc`, `last_train/game.cljc`, `last_train/puzzles.cljc`, `last_train/terminal.cljc`, `terminal.cljs`.
  - All paths relative, with no leading `/`.
- [ ] T015 [P] [US1] Create `web/terminal.css`:
  - `:root{--bg:#000;--fg:#33ff66;--dim:#1fae4a}`. Body background `var(--bg)`, colour `var(--fg)`, with a monospace stack `"IBM Plex Mono", ui-monospace, Menlo, Consolas, monospace`.
  - Layout: `height:100dvh` grid with the transcript (`overflow-y:auto; white-space:pre-wrap; overflow-wrap:anywhere`) above a prompt row that stays in view.
  - `max-width:60rem`, centred, `padding:16px`.
  - Line classes `.line.operator`, `.line.passenger`, `.line.player` (dim), `.line.outcome` (bold, text-shadow glow) and `.line.system`.
  - The input has a transparent background, no border, the same colour, and `caret-color` the same green. A blinking block cursor comes from the `::after` of the prompt row (`@keyframes blink`).
  - The `#rain` canvas is fixed behind everything at `opacity:.18`.
  - `@media (prefers-reduced-motion: reduce){#rain{display:none} *{animation:none!important}}`.
  - Check that there is no horizontal scroll at 360px.
- [ ] T016 [US1] Create `web/terminal.cljs`, the DOM glue only (no game rules):
  - On load, read `?puzzle=` from `js/location.search` and call `terminal/boot` with `{:puzzle-param … :rand-int rand-int}`.
  - Render every `:lines` entry as a `div.line.<kind>`, with its text set through `textContent` and never `innerHTML`, then scroll the transcript to the bottom.
  - On form submit, prevent the default, send the trimmed value to `terminal/submit`, append only the new lines, clear the input and focus it again.
  - Set `document.body.dataset.ready = "true"` after the first render.
  - Start the falling-glyph `#rain` animation only when `(.-matches (js/matchMedia "(prefers-reduced-motion: reduce)"))` is false. Use half-width katakana plus digits, draw with `requestAnimationFrame` at about 20 fps, and resize on `resize`.

### 3d — Local site build and serve (replaces the server page)

- [ ] T017 [US1] Create `adapter/last_train/site.clj` (namespace `last-train.site`):
  - `build!` cleans `build/site/`, copies `web/*` there, and copies `src/last_train/{logic,english,game,puzzles,terminal}.cljc` to `build/site/last_train/`.
  - `serve!` takes a port, calls `build!`, then `babashka.http-server/exec` on `build/site`.
  - `-main` parses `[--port N]` (default 8080, same validation messages as the old `web/parse-args`: `Unknown option: x`, `Missing value for --port`, `Bad port: x`) and exits with code 2 on error.
  - Add `bb.edn` tasks `site` (runs `build!`) and `serve` (runs `-main`).
- [ ] T018 [US1] Rewrite `bin/last-train-web` to run `exec bb -m last-train.site "$@"` (or the `serve` task). Delete the server-rendered page and everything tied to it:
  - `git rm src/last_train/web.clj adapter/last_train/web_main.clj spec/last_train/web_spec.clj property/last_train/web_property_spec.clj hardening/last_train/web_hardening_spec.clj .metrics/mutate/last_train/web.edn`
  - Remove any `bb.edn` task that referenced them.
  - Grep for leftover `last-train.web` requires and fix them.

### 3e — Browser acceptance steps (FR-016)

- [ ] T019 [US1] In `acceptance/src/last_train/acceptance/web_steps.clj`, add a `page-html` helper that calls `last-train.site/build!` once per run and slurps `build/site/index.html`. No browser, no server, no chromedriver.
- [ ] T020 [US1] Rewrite `acceptance/src/last_train/acceptance/web_steps.clj` to drive `last-train.terminal` directly: `terminal/boot` and `terminal/submit`, the same functions `web/terminal.cljs` calls. Keep the same public fns used by `steps.clj`: `open-game`, `lines`, `new-lines`, `accuse`, `check-over`, `live-worlds`, `handlers`.
  - `open-game` → `(terminal/boot {:puzzle-param "reference" :rand-int (constantly 0)})`, stored under `:web`.
  - Ask/accuse submit `ask <seat> <question>` / `accuse <seat>[ ally <seat>]`.
  - "I enter X as a question for passenger B" submits `ask B X`.
  - `lines` returns the `:text` of every session line that isn't a `:player` line.
  - The prompt/accuse capability steps check that `page-html` contains `id="command"` and that the transcript has the syntax line containing `ask <passenger> <question>` and `accuse <passenger> [ally <passenger>]`.
  - "I do not see the passengers' true roles" checks that no transcript line and no part of `page-html` contains `:agent`, `:awake`, `:sleeper` or `true-world`.
  - `live-worlds` reads `[:web :game :live-worlds]`.
  - Remove the HTML form parsing and the `java.net.URLEncoder` import.
- [ ] T021 [US1] Update the wording of `features/web-game-start.feature` only where a step implies dropdowns, so it describes typed commands, e.g. `And I can type a question for a chosen passenger at the prompt`. Keep scenario names and headers. Update the matching regexes in `web_steps.clj`. Run `bb acceptance`: all 5 `web-game-*` features pass, and every other feature stays green.

### 3f — Puzzle catalog (FR-007…007c)

- [ ] T022 [P] [US1] Write `spec/last_train/puzzles_spec.clj` (Speclj):
  - `catalog` has at least 5 puzzles with unique `:name`s, and includes `"reference"`.
  - `(by-name "reference")` returns the existing reference puzzle, with the same `:true-world`, `:personas` and `:opening` as before.
  - For each puzzle, `(valid? p)` is true.
  - `valid?` is false for a puzzle whose true world is inconsistent with its opening, for one with only 1 Agent seat remaining, for one solvable in 1 question, and for one that needs 3 questions.
  - Persona names are distinct within each puzzle and none is `a`–`d`, `i`, `me` or `you`, compared case-insensitively.
  - The true worlds differ across the catalog, and at least 3 different Agent seats appear.
  - `(pick rand-int "reference")` never returns `"reference"` (check with 50 stubbed draws), and `(pick rand-int nil)` can return any puzzle.
- [ ] T023 [US1] In `src/last_train/puzzles.cljc`:
  - Add `:name "reference"` to `reference`.
  - Add `valid?`, implementing exactly: the true world is in `W = (logic/consistent opening)`, `2 ≤ |W| ≤ 4`, `(count (logic/agent-seats W)) ≥ 2`, `(not (logic/solvable? W 1))` and `(logic/solvable? W 2)`.
  - Add `catalog`, `by-name` and `pick`.
- [ ] T024 [US1] Create `adapter/last_train/puzzle_search.clj` (dev-only; add a `bb.edn` task `puzzle-search`).
  - For a given true world, enumerate candidate opening sets. Each set has one statement per seat, drawn from `[:is s r]`, `[:not [:is s r]]`, `[:same x y]`, `[:not [:same x y]]` and `[:count-eq :agent 0]`, filtered to statements the speaker can say (`logic/can-say?`).
  - Print the first 10 sets that pass `puzzles/valid?`, using the English from `english/render-statement`.
  - Run it for 4 true worlds with the Agent in different seats from the reference puzzle's (A), e.g. Agent B/Awake C, Agent C/Awake A, Agent D/Awake B, Agent B/Awake D.
- [ ] T025 [US1] Add 4 puzzles to `catalog` in `src/last_train/puzzles.cljc`, named `commuters`, `night-shift`, `terminus` and `red-eye`, each built from T024 output.
  - Give each 4 new persona names and one-line bios in the Matrix late-train tone. Persona names must be unique within each puzzle.
  - T022 must pass.
  - Then un-pend the `new`-gives-a-different-puzzle examples in `spec/last_train/terminal_spec.clj`.
- [ ] T026 [US1] Write `features/puzzle-catalog.feature` (a new APS feature file with no mutation header; the tools add it):
  - A Scenario Outline "Puzzle catalog 01 - every named puzzle follows the puzzle rules", with Examples of the 5 names, and steps `Given the puzzle named "<name>"`, `Then the puzzle follows the puzzle rules`.
  - A Scenario "Puzzle catalog 02 - a new game never repeats the puzzle just played": `Given I just played the puzzle named "reference"`, `When a new game picks a puzzle`, `Then it is not "reference"`.
  - Add the step handlers to `acceptance/src/last_train/acceptance/engine_steps.clj`.
- [ ] T027 [US1] In `src/last_train/cli.clj`:
  - `seeds` becomes every name in `puzzles/catalog`, plus `"random"`, which uses `puzzles/pick` with `rand-int`. The default stays `"reference"`.
  - The error message becomes `Unknown seed: <x>. Supported: reference, commuters, night-shift, terminus, red-eye, random`.
  - Update `spec/last_train/cli_spec.clj`. Per `contracts/cli.md`, a bad seed exits with code 2 through `adapter/last_train/main.clj`, which is unchanged.
- [ ] T028 [US1] Write `features/web-game-terminal.feature`:
  - "Web game terminal 01 - an unrecognised command shows the command syntax": open the reference game, type `hello`, see `Signal's noisy. Rephrase.`, see `accuse <passenger> [ally <passenger>]`, 3 questions remain.
  - "Web game terminal 02 - new starts a different puzzle": open the reference game, accuse D, type `new`, the opening statements are not the reference ones, and 3 questions remain.
  - "Web game terminal 03 - an unknown puzzle link boards a random train": open the page with puzzle `nope`, see `Unknown puzzle "nope". Boarding a random train.`, and 3 questions remain.
  - Add the step handlers in `web_steps.clj`. Run `bb acceptance` and confirm all features pass.

**Checkpoint (MVP)**: `bb spec` and `bb acceptance` are fully green. `bin/last-train-web` plays in the browser as described under Independent Test. Commit and push.

---

## Phase 4: User Story 2 — Play from a public GitHub Pages link (P2)

**Goal**: every push to `main` is verified and published to `https://katyafedorova.github.io/last-train/`.

**Independent Test**: after a push, `gh run watch` shows `verify` then `deploy` green within 10 minutes. The URL loads in a private window on desktop and on a phone, and a full game can be completed.

- [ ] T029 [US2] Create `.github/workflows/pages.yml`:
  - Trigger: `on: push: branches: [main]` and `workflow_dispatch`.
  - `permissions: contents: read, pages: write, id-token: write`; `concurrency: group: pages, cancel-in-progress: false`.
  - Job `verify` on `ubuntu-latest`:
    - `actions/checkout@v4`.
    - Install Babashka with `turtlequeue/setup-babashka@v1.7.0` (or the current maintained action), pinned to the same bb version as local `bb --version`.
    - Run `git clone --depth 1 https://github.com/unclebob/Acceptance-Pipeline-Specification "$RUNNER_TEMP/aps"`, with `APS_HOME` set to that path.
    - Run `bb spec`, then `bb acceptance`.
  - Job `deploy` (`needs: verify`, `environment: github-pages`): `bb site`, then `actions/configure-pages@v5`, `actions/upload-pages-artifact@v3` with `path: build/site`, and `actions/deploy-pages@v4`.
- [ ] T030 [US2] Enable Pages with the Actions source: `gh api -X POST repos/KatyaFedorova/last-train/pages -f build_type=workflow`. If it already exists, use `gh api -X PUT … -f build_type=workflow`. Then run `gh api repos/KatyaFedorova/last-train/pages -q .html_url` and confirm it prints `https://katyafedorova.github.io/last-train/`.
- [ ] T031 [US2] Push to `main` and run `gh run watch --exit-status`. If `verify` fails in CI but passes locally, fix the environment difference in the workflow, not the tests.
- [ ] T032 [US2] Check the live site over HTTP, with no browser: `curl -fsS` must return 200 for `https://katyafedorova.github.io/last-train/`, `terminal.css`, `terminal.cljs` and each `last_train/*.cljc`. The HTML must contain `id="command"`, and the Scittle script URL must return 200. Record the results in the commit message.

**Checkpoint**: the public URL plays the game, and pushes redeploy automatically.

---

## Phase 5: User Story 3 — Prove it works; docs (P3)

**Goal**: one documented command verifies everything on a clean checkout, and the README is accurate.

**Independent Test**: `git clone https://github.com/KatyaFedorova/last-train /tmp/lt && cd /tmp/lt && APS_HOME=<aps clone> bb spec && bb acceptance` is green, and following the README works.

- [ ] T033 [US3] Add a hardening check in `acceptance/spec/last_train/bin_acceptance_spec.clj` (Speclj, `acceptance-spec` task). Run `bin/acceptance` with `PATH` stripped of `gherkin-parser` and `APS_HOME` pointing to a nonexistent directory. Assert it exits 1 and that stderr names both `gherkin-parser` and `APS_HOME`.
- [ ] T034 [US3] Rewrite `README.md`:
  - Replace the "Play in a browser" section with: the public URL; how to play locally (`bin/last-train-web` → `http://localhost:8080/`, and `?puzzle=<name>`); the commands `ask`, `accuse`, `new` and `help`.
  - Terminal: the `--seed` names and `random`.
  - Development: prerequisites (bb, APS clone and `APS_HOME`), then `bb spec`, `bb acceptance`, `bb property`, `bb puzzle-search`.
  - Deployment: this is automatic on push to `main`, through `.github/workflows/pages.yml`.
  - Remove any mention of the server-rendered page.
- [ ] T035 [US3] Do a clean-checkout verification in the scratch directory: clone the repo, set `APS_HOME`, run `bb spec && bb acceptance`, and paste the final counts into the PR or commit message.

---

## Phase 6: Polish & Cross-Cutting

- [ ] T036 [P] Add `property/last_train/terminal_property_spec.clj` (Speclj + test.check, kept under `bb property` only). It replaces the deleted web property spec. For random command sequences over the reference puzzle, the web session's non-`:player` lines equal the CLI transcript from `game/start`/`game/handle` for the same inputs (FR-001, SC-002). Before game over, no line reveals a role.
- [ ] T037 [P] Accessibility pass on `web/terminal.css`/`web/index.html`:
  - Verify contrast: `#33ff66` on `#000` and `#1fae4a` on `#000` must both be at least 4.5:1, and should be about 15:1 and 7:1.
  - Check that `#command` has a visible `:focus-visible` style, and that the page is fully usable with the keyboard alone.
- [ ] T038 Run the constitution tools one at a time on the changed source (`src/last_train/*.cljc`, `cli.clj`): `crap4clj` with cloverage, `dry4clj`, and differential `clj-mutate` (`--max-workers 4`, no `--mutate-all`). If the SwarmForge wrappers are broken (see memory), use direct `bb` invocations. Fix any CRAP or DRY findings in `terminal.cljc` and `puzzles.cljc`.
- [ ] T039 Walk through `specs/001-matrix-web-game/quickstart.md` steps 1–5 and fix any drift between the docs and the behavior. Mark every task in this file as done.

---

## Dependencies & Execution Order

- **Setup (T001–T005)** → **Foundational (T006–T009)** → **US1** → **US2** → **US3** → **Polish**.
- US2 needs US1's site (`bb site`) and a green suite; publishing a broken game is pointless.
- US3's docs describe what US1 and US2 built. T033 (the bin/acceptance hardening check) can start any time after T002.
- Inside US1:
  - T010→T011 comes before T012/T013, because the terminal relies on the syntax-line behavior.
  - T014, T015 and T012 can run in parallel.
  - T016 needs T013 and T014.
  - T017→T018→T019→T020→T021 run in order.
  - T022 can run in parallel with 3c/3d; T023→T024→T025 follow it; T026, T027 and T028 come after T025.

## Parallel Opportunities

```text
Phase 1:      T004 ‖ T005
Phase 3 (US1): T012 (terminal spec) ‖ T014 (index.html) ‖ T015 (terminal.css) ‖ T022 (puzzles spec)
Phase 6:      T036 ‖ T037
```

## Implementation Strategy

- **MVP = through T021**: the reference puzzle playable in the Matrix browser terminal, verified through the session functions. Stop and demo here if time is short.
- Then T022–T028 for replay variety, and US2 to go public. US3 and Polish harden it for the long term.
- The SwarmForge `last-train-matrix-game` card stays "done". This work runs through Spec Kit (`/speckit-implement`), not the swarm.
