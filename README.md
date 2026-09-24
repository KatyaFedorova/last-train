# LAST TRAIN

A deterministic, text based logic game set on a late night subway. Four passengers are aboard: one Agent, one Awake ally, and two Sleepers. Read their statements, ask questions, and identify the Agent.

## Play in a terminal

Install [Babashka](https://babashka.org/), then run:

```sh
bin/last-train --seed reference --voices=template
```

The reference game is repeatable. Ask a passenger a yes or no question, for example:

```text
ask B Is D an Agent?
```

You can also accuse the Agent and optionally name the Awake ally:

```text
accuse A ally D
```

## Play in a browser

Start the local web server, then open <http://localhost:8080/>:

```sh
bin/last-train-web
```

Use `--port 9000` to serve on another port. The page plays the same reference game: choose a passenger and type a yes or no question, or accuse a passenger and optionally name the Awake ally. Its game flow is specified in [`features/web-game-start.feature`](features/web-game-start.feature) and the related `features/web-game-*.feature` files.

## Development

Run the unit specifications with:

```sh
bb spec
```

Run the Gherkin acceptance suite with:

```sh
bb acceptance
```

Run the property tests (kept out of the unit, coverage and mutation runs) with:

```sh
bb property
```

The logic engine is deterministic and does not call an LLM. See [`docs/LAST_TRAIN_game_spec.md`](docs/LAST_TRAIN_game_spec.md) for the game rules, architecture, and reference puzzle.
