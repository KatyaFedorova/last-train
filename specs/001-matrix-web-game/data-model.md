# Data Model: Matrix Web Game

Everything is in-memory Clojure data. There is no storage. The shapes match the existing code, and new fields are marked **new**.

## Puzzle (`last-train.puzzles`)

| Field | Type | Rule |
|-------|------|------|
| `:name` **new** | string, kebab-case | Unique within the catalog. `"reference"` must exist. |
| `:true-world` | `{seat role}` | Exactly one `:agent`, one `:awake` and two `:sleeper`. Seats are `:A`–`:D`. |
| `:personas` | `{seat {:name str :bio str}}` | All 4 seats. The names are distinct and none is a seat letter or a pronoun (I/me/you), since the parser resolves those words. |
| `:opening` | `[[seat prop polarity]]` ×4 | One per seat. Every entry is sayable in the true world: `(logic/can-say? true-world seat prop) = polarity`. |

**Validity** (`puzzles/valid?`, FR-007a; `docs/LAST_TRAIN_game_spec.md` §2.5). All of these must hold:
- the true world is one of `W = (logic/consistent opening)`
- `2 ≤ |W| ≤ 4`
- `|agent-seats W| ≥ 2`
- `(not (logic/solvable? W 1))`
- `(logic/solvable? W 2)`

**Catalog**: `puzzles/catalog` is a vector of at least 5 puzzles. `puzzles/by-name` looks one up by name.
- `puzzles/pick [rand-int previous-name]`: returns a random puzzle whose name is not `previous-name`. It is pure: the random source is passed in.

## Game state (`last-train.game`, unchanged shape)

`{:puzzle :round :questions-left :facts :live-worlds :over? :outcome}`

State transitions (existing):

```
start → round 2 (Station 1), questions-left 3
ask (valid) → round+1, questions-left−1, fact appended, live-worlds filtered
ask (invalid) → unchanged + "Signal's noisy. Rephrase."
ask (0 left) → unchanged + "No more questions. Accuse the Agent."
accuse → over? true, outcome {:win? :ally? :score}
any input when over? → unchanged + "The game is over."
```

**New transition**: input that is neither `ask …` nor `accuse …` → state unchanged. The output is the noisy line plus the command-syntax line. The CLI has the same behavior, so FR-001 holds.

## Terminal session (`last-train.terminal`, **new**, pure `.cljc`)

| Field | Type | Meaning |
|-------|------|---------|
| `:game` | game state | The current game. |
| `:lines` | vector of `{:text str :kind kw}` | The transcript. `:kind` is one of `:system`, `:operator`, `:passenger`, `:player` or `:outcome`, and sets the CSS class. |
| `:previous` | string or nil | The last puzzle name, passed to `pick` so a replay gets a different puzzle. |

Functions (all pure):
- `(boot {:puzzle-param str-or-nil :rand-int f})`: returns a session.
  - An unknown puzzle param produces a `:system` line saying so, then a random puzzle starts.
- `(submit session input rand-int)`: returns a session.
  - Echoes the input as a `:player` line (`> …`).
  - `new`, `restart` or `play again` start a new game with a different puzzle, and are accepted at any time.
  - `help` shows the command syntax.
  - Anything else is passed to `game/handle`.
  - After game over, it adds `Type "new" to play again.`
- `(prompt-enabled? session)`: always true, because `new` is valid after game over.

## Seat, Role, Proposition

These are unchanged (`last-train.logic` / `last-train.english`). Seats are `:A :B :C :D`, and roles are `:agent :awake :sleeper`. Propositions are `[:is s r] [:same s s] [:count-eq r n] [:not p] [:and p q] [:or p q]`.
