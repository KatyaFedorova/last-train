# Contract: Web Page

## URL

- `https://katyafedorova.github.io/last-train/` opens a random puzzle.
- `…/?puzzle=<name>` opens that puzzle, e.g. `?puzzle=reference`. The acceptance tests use this.
- If `<name>` is unknown, the system line says `Unknown puzzle "<name>". Boarding a random train.` and a random puzzle starts.

## DOM hooks (stable, used by the acceptance tests)

| Selector | Element | Content |
|----------|---------|---------|
| `#transcript` | `<pre>` / `<div role="log" aria-live="polite">` | One child `.line` per transcript line, whose `textContent` is exactly the game output line. |
| `#command` | `<input type="text">` | The prompt. Enter submits it. It has `autocapitalize="off"`, `autocorrect="off"`, `spellcheck="false"` and `autocomplete="off"`, and gets focus on load and after each submit. |
| `#rain` | `<canvas aria-hidden="true">` | The decorative background. It isn't drawn when `prefers-reduced-motion: reduce` is set. |
| `noscript` | — | Shows `LAST TRAIN needs JavaScript enabled.` |
| `body[data-ready="true"]` | — | Set once Scittle has loaded and the first game has rendered. Tests wait for this. |

The page must never contain a passenger's true role (`agent`/`awake`/`sleeper` values from `:true-world`) in the DOM, in attributes or in inline data before game over (FR-003).

## Look (FR-009…012)

- Background `#000`, text `#33ff66`, monospace, with a blinking block cursor after the input.
- There is no horizontal scroll at 360px. The transcript wraps (`white-space: pre-wrap; overflow-wrap: anywhere`).
- The input stays visible when the on-screen keyboard opens. The layout is `100dvh` with the transcript scrolling above a fixed prompt row.

## Files published (`build/site/`)

`index.html`, `terminal.css`, `terminal.cljs`, `last_train/{logic,english,game,puzzles,terminal}.cljc`. Scittle is loaded from `cdn.jsdelivr.net/npm/scittle@<pinned>/dist/`. All paths are relative.
