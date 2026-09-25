# LAST TRAIN

A fast, deterministic logic game set on a late night subway inside the Matrix. Each train has four passengers, and one of them is the Agent. The Agent always lies; everyone else always tells the truth. Read their four lines and name the Agent before the clock runs out.

**Play it now: <https://katyafedorova.github.io/last-train/>**

## How to play

- Each train shows four passengers, one line each. The lines leave exactly one possible Agent.
- Tap the Agent or press their letter: **A**, **B**, **C** or **D**. You have 1 minute; the clock starts when you press **Start**.
- Wrong answer or time up: the game shows why and which line was the lie.
- A run is 3 trains. The first 2 are easy; the last adds lines like "A or C is the Agent."
- **H** gives one tip per run (it names an honest passenger). **Enter** goes to the next train.
- Score: 100 per right answer, plus 5 per second left on the clock.

Tip: if two passengers contradict each other, one of them is the Agent, so the other two are honest.

In the terminal, type a letter, `accuse <letter>`, `hint`, or `time` (give up on this train).

## Play in the browser locally

Install [Babashka](https://babashka.org/), then run:

```sh
bin/last-train-web            # builds build/site/ and serves http://localhost:8080/
bin/last-train-web --port 9000
```

Open `http://localhost:8080/?seed=42` to replay a specific run. Without `?seed=`, the run is random.

The page is static: `web/` plus the shared game core in `src/last_train/*.cljc`, run in the browser by [Scittle](https://github.com/babashka/scittle). The browser runs the exact same rules as the terminal game.

## Play in a terminal

```sh
bin/last-train --seed 42
bin/last-train --seed random
```

`--seed` accepts a number (the same number replays the same run) or `random`, the default.

## Development

Prerequisites:

- [Babashka](https://babashka.org/).
- Node (`npx`), optional. It is used by `bin/cljs-smoke` to check the core as ClojureScript.
- The [Acceptance Pipeline tools](https://github.com/unclebob/Acceptance-Pipeline-Specification). Either put `gherkin-parser` on `PATH`, or clone the repo and point `APS_HOME` at it. It defaults to `.swarmforge/tools/Acceptance-Pipeline-Specification`.

```sh
bb spec              # unit specifications
bb acceptance        # ClojureScript smoke check + every Gherkin feature in features/
bb acceptance-spec   # specs for the acceptance runtime and scripts
bb hardening         # focused hardening specs
bb property          # property tests (kept out of the other runs)
bb site              # build the static site into build/site/
```

Trains are generated, not hand-written: `puzzles/generate` draws a line for each passenger, A to D, until `puzzles/valid?` holds (the lines leave exactly one possible Agent). A seeded Park-Miller generator makes runs replay identically in Babashka and in the browser.

The logic engine is deterministic and does not call an LLM. See [`docs/LAST_TRAIN_game_spec.md`](docs/LAST_TRAIN_game_spec.md) for the original design. The shipped rules are summarised at its top.

## Deployment

Every push to `main` runs [`.github/workflows/pages.yml`](.github/workflows/pages.yml). It runs `bb spec` and `bb acceptance`, and if both pass it publishes `build/site/` to GitHub Pages. Nothing is published when a test fails.
