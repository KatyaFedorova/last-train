# Quickstart: Validate the Matrix Web Game

## Prerequisites

- Babashka (`bb`), and any browser for manual play.
- The APS tools, either at `.swarmforge/tools/Acceptance-Pipeline-Specification` or cloned elsewhere with `APS_HOME` pointing to them.
- **SwarmForge stopped**, so nothing else commits to `main` while you validate.

## 1. Unit specs and puzzle validity

```sh
bb spec
```

Expected: 0 failures. This includes `puzzles_spec` (every catalog puzzle passes `valid?`) and `terminal_spec`.

## 2. Full acceptance suite (terminal and browser)

```sh
bb acceptance
```

Expected: every scenario in every `features/*.feature` passes. That's the 56 existing scenarios plus the new catalog and web-terminal scenarios. The web scenarios call the page's session functions directly, with no browser.

## 3. Play locally

```sh
bin/last-train-web            # builds build/site/ and serves http://localhost:8080/
open "http://localhost:8080/?puzzle=reference"
```

Type these commands:

```text
ask B Is D an Agent?
accuse A ally D
```

Expected: `Tomasz (B): "No. Mr. Grey is not an Agent."`, then `PERFECT RUN`, `Score: 200` and `GAME OVER`. This is identical to:

```sh
printf 'ask B Is D an Agent?\naccuse A ally D\n' | bin/last-train --seed reference
```

Then type `new`: a different puzzle starts.

## 4. Look and accessibility check

- In DevTools device mode at 360px, 768px and 1440px, there should be no horizontal scrollbar and the prompt should stay visible.
- Turn on "Emulate CSS prefers-reduced-motion: reduce": the falling code should stop.

## 5. Publish

One-time setup: `gh api -X POST repos/KatyaFedorova/last-train/pages -f build_type=workflow`

Push to `main`, then run `gh run watch`. Expected: the `verify` then `deploy` jobs go green within 10 minutes, and <https://katyafedorova.github.io/last-train/> plays a game.
