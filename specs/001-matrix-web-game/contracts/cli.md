# Contract: CLI (`bin/last-train`)

| Option | Values | Default |
|--------|--------|---------|
| `--seed` | any catalog name (`reference`, `commuters`, `night-shift`, `terminus`, `red-eye`) or `random` | `reference`, which is unchanged so the existing CLI features stay valid |
| `--voices` | `template` | `template` |

An unknown seed prints `Unknown seed: <x>. Supported: <names…>, random` to stderr and exits with code 2.

`bin/last-train-web [--port N]` now serves the static site from `build/site/` (building it first) at `http://localhost:N/`. The default port is 8080.
