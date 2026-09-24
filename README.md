# LAST TRAIN

A deterministic, text based logic game set on a late night subway inside the Matrix. Four passengers are aboard: one Agent, one Awake ally, and two Sleepers. Read their statements, ask up to three yes or no questions, and identify the Agent.

**Play it now: <https://katyafedorova.github.io/last-train/>**

## How to play

Type commands at the green prompt, or in the terminal version:

| Command | What it does |
|---------|--------------|
| `ask B Is D an Agent?` | Ask a passenger a yes or no question (costs one of your 3 questions) |
| `ask C Are A and B the same kind?` | The other supported question form |
| `accuse A` | Accuse the Agent. This ends the game |
| `accuse A ally D` | Accuse the Agent and name the Awake ally for a bonus |
| `help` | Show the command syntax (web) |
| `new` | Start a new game with a different puzzle (web) |

Passengers can be named by seat letter (`A`–`D`) or by name. The Awake always tell the truth, and the Agent always lies. A Sleeper is honest but blind: they answer as if everyone were a Sleeper.

Score: 100 for the right Agent, plus 50 for the right ally, plus 25 for each unused question.

## Play in the browser locally

Install [Babashka](https://babashka.org/), then run:

```sh
bin/last-train-web            # builds build/site/ and serves http://localhost:8080/
bin/last-train-web --port 9000
```

Open `http://localhost:8080/?puzzle=reference` to play a specific puzzle. The puzzles are `reference`, `commuters`, `night-shift`, `terminus` and `red-eye`. Without `?puzzle=`, a random one is picked.

The page is static: `web/` plus the shared game core in `src/last_train/*.cljc`, run in the browser by [Scittle](https://github.com/babashka/scittle). The browser runs the exact same rules as the terminal game.

## Play in a terminal

```sh
bin/last-train --seed reference --voices=template
bin/last-train --seed random
```

`--seed` accepts any puzzle name, or `random`. The default is `reference`.

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
bb puzzle-search B C # find opening statements for a new puzzle (Agent B, Awake C)
```

A new puzzle must pass `puzzles/valid?`, which enforces the puzzle rules in [`docs/LAST_TRAIN_game_spec.md`](docs/LAST_TRAIN_game_spec.md) §2.5. It then goes into `catalog` in `src/last_train/puzzles.cljc` and into the examples in `features/puzzle-catalog.feature`.

The logic engine is deterministic and does not call an LLM. See [`docs/LAST_TRAIN_game_spec.md`](docs/LAST_TRAIN_game_spec.md) for the game rules and the reference puzzle.

## Deployment

Every push to `main` runs [`.github/workflows/pages.yml`](.github/workflows/pages.yml). It runs `bb spec` and `bb acceptance`, and if both pass it publishes `build/site/` to GitHub Pages. Nothing is published when a test fails.
