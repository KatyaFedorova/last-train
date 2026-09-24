# Contract: Player Commands (terminal and web)

The grammar is the same on both surfaces. It is case-insensitive, and leading and trailing spaces are ignored. `<p>` is a seat letter `A`–`D` or a passenger's persona name.

| Input | Effect | Costs a question |
|-------|--------|------------------|
| `ask <p> <question>` or `ask <p>, <question>` | The passenger answers `Yes. …` or `No. …`, and the round advances. | yes |
| `ask <p> <unsupported>` | `Operator: "Signal's noisy. Rephrase."` and `Questions left: n` | no |
| `ask …` with 0 questions left | `Operator: "No more questions. Accuse the Agent."` | no |
| `accuse <p>` | Win (`WIN`) or loss (`LOSE`), then `Score: n` and `GAME OVER` | — |
| `accuse <p> ally <p>` | Same as above, with `PERFECT RUN` if the ally is also right | — |
| anything else (**new**) | The noisy line, then the syntax line `Ask: ask <passenger> <question>   Accuse: accuse <passenger> [ally <passenger>]` | no |

Supported questions (`english/parse-question`):
- `Is <p> an Agent|Awake|a Sleeper?`
- `Are <p> and <p> the same kind?`

## Web-only commands (`last-train.terminal`)

| Input | Effect |
|-------|--------|
| `new` / `restart` / `play again` | Starts a new game with a different puzzle. It works at any time. |
| `help` | Shows the syntax line. |

## Score

`win ? 100 + (ally ? 50 : 0) + 25 × questions-left : 0`. This is unchanged.
