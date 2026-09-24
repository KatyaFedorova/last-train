# Feature Specification: Matrix Web Game on GitHub Pages

**Feature Branch**: `001-matrix-web-game`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "This is not a new project, it's an old one, some feature are already created, but I'm not sure if they working as they should be if the game is firing, and I wanted the game firing on a web page with a kinda matrix terminal, matrix like with the green letters and black background. So I want it to be en hosted on GitHub pages. help me to finish this game because March Forge doesn't yup thank you very much."

## Context

LAST TRAIN already exists as a deterministic terminal game: one reference puzzle, four passengers (Agent, Awake ally, two Sleepers), up to 3 yes/no questions, accusation with optional ally, score = 100 win + 50 ally + 25 per unused question. Unit specs pass (66 examples, 0 failures). The terminal game plays end to end.

Gaps this feature closes:

- There is no browser version. Its behavior is already specified in `features/web-game-*.feature` (start, question, invalid question, accusation, wrong accusation), but no web page exists.
- The acceptance suite cannot run on this machine: `bb acceptance` fails with `gherkin-parser not on PATH`, and no generated acceptance tests exist. So nobody can currently confirm the existing features behave as specified.
- The game is not published anywhere. The repository has no remote.
- SwarmForge stalled: its handoff to the coder was rejected because the implementation card was marked done (`tmp/web-handoff-blocked.txt`).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Play the full game in a Matrix-style browser terminal (Priority: P1)

A player opens the game page and sees a black screen with green monospaced text, like a Matrix terminal. The Operator's intro and the four passengers' opening statements appear. The player asks yes/no questions of chosen passengers, reads answers, and accuses a passenger (optionally naming the ally). The game ends with the same outcome and score the terminal version would give for the same moves.

**Why this priority**: This is the product the user asked for. Everything else supports it.

**Independent Test**: Open the page locally with the reference puzzle and play: ask B "Is D an Agent?", then accuse A with ally D. The page shows "PERFECT RUN" and score 200, matching the terminal game.

**Acceptance Scenarios** (mirror the committed `features/web-game-*.feature` files):

1. **Given** a new reference game in the web page, **When** it loads, **Then** I see the four passengers' opening statements and "3 questions remain", I do not see any passenger's true role, and I can choose a passenger, enter a yes/no question, and accuse a passenger with an optional ally.
2. **Given** a started reference game, **When** I ask passenger B "Is D an Agent?", **Then** B answers no, 2 questions remain, the possible Agent seats are A and C, and the game advances to the déjà vu round.
3. **Given** a started reference game, **When** I enter "what is love?" as a question for B, **Then** I see "Signal's noisy. Rephrase.", 3 questions still remain, and I stay in Station 1.
4. **Given** a started reference game, **When** I ask B whether D and then C are Agents and accuse A naming D as ally, **Then** I see "PERFECT RUN", the score is 175, and the game is over.
5. **Given** a started reference game, **When** I accuse passenger D, **Then** I see "LOSE" and the game is over.
6. **Given** a finished game, **When** I choose to play again, **Then** a fresh game starts without reloading the page, with a different puzzle from the one just played.

---

### User Story 2 - Play the game from a public GitHub Pages link (Priority: P2)

The player gets a public URL on GitHub Pages, opens it in any modern browser with no install or login, and plays the full game.

**Why this priority**: Publishing is the user's goal, but it only helps once Story 1 works.

**Independent Test**: Open the published URL in a private window on desktop and on a phone and complete one game on each.

**Acceptance Scenarios**:

1. **Given** the published URL, **When** a visitor opens it, **Then** the Matrix terminal loads and a game starts with no install, account, or server-side component.
2. **Given** a change merged to the main branch, **When** publishing completes, **Then** the published page reflects that change.
3. **Given** a phone-width screen (360px), **When** the page loads, **Then** all text is readable, there is no horizontal scrolling, and the player can ask and accuse.

---

### User Story 3 - Prove the existing and new features work as specified (Priority: P3)

A developer runs one documented command and gets a pass/fail result for every `.feature` file, the terminal ones and the web ones. The result doesn't depend on SwarmForge tool wrappers being installed.

**Why this priority**: The user doesn't know whether the existing features work. A runnable acceptance suite answers that and prevents regressions, but players can use the game without it.

**Independent Test**: On a clean checkout, run the documented acceptance command. Every scenario in `features/` is reported, and all of them pass.

**Acceptance Scenarios**:

1. **Given** a clean checkout with the documented prerequisites, **When** the developer runs the acceptance command, **Then** every scenario in every `.feature` file runs and reports pass or fail.
2. **Given** a web-game rule is broken (for example, the invalid-question message changes), **When** the acceptance command runs, **Then** the matching web scenario fails.
3. **Given** the README, **When** a developer follows it, **Then** they can play in the terminal, play in the browser locally, run all tests, and find the published URL.

---

### Edge Cases

- **Empty input**: an empty question or accusation is rejected with a message and costs nothing.
- **Unknown passenger**: a question or accusation naming a seat other than A-D is rejected and costs nothing.
- **Multiple questions in one**: rejected like an unsupported question ("Signal's noisy. Rephrase."), with no question used.
- **No questions left**: the player can still accuse, but further questions are refused.
- **Naming the Agent as ally, or the same seat twice**: this follows the existing game rules and matches the terminal outcome.
- **Input after game over**: it is ignored or offers a restart. The game never continues past game over.
- **Page reload mid-game**: a fresh game starts. No progress is kept. If the page was opened through a specific-puzzle link, that same puzzle restarts.
- **Unknown puzzle in a link**: the page says the puzzle is unknown and starts a random one.
- **Unrecognised command** (e.g. `hello`): the page shows the command syntax again, and no question is used.
- **Phone keyboards**: typing on a phone must work. The input stays visible above the on-screen keyboard, and auto-correct and auto-capitalise don't mangle commands.
- **Reduced motion**: if the visitor's system asks for reduced motion, the decorative Matrix effects (falling characters, typing animation) are turned off. Play is unaffected.
- **JavaScript disabled**: a plain message says the game needs scripts enabled.

## Requirements *(mandatory)*

### Functional Requirements

**Game in the browser**

- **FR-001**: The web page MUST run the existing LAST TRAIN game rules and reference puzzle. For any sequence of moves, it MUST produce the same answers, round progression, outcome text, and score as the terminal game.
- **FR-002**: The page MUST show the Operator intro, the four passengers' names, seats, and opening statements, the current round, and the number of questions remaining.
- **FR-003**: The page MUST never reveal any passenger's true role before the game ends.
- **FR-004**: The player MUST be able to ask a chosen passenger a yes/no question and accuse a passenger with an optional ally by typing terminal commands at a prompt, using the same command syntax as the terminal game (`ask B Is D an Agent?`, `accuse A ally D`). There are no clickable game controls. The page MUST show the command syntax, and MUST also show it again after an unrecognised command.
- **FR-005**: Unsupported or malformed questions MUST be rejected with "Signal's noisy. Rephrase." and cost no question.
- **FR-006**: The page MUST show a win ("PERFECT RUN" when the ally is also correct), a loss ("LOSE"), the final score, and a game-over state. It MUST then offer a new game without a page reload.
- **FR-007**: The game MUST include at least 5 fixed puzzles, including the existing reference puzzle. Each new game MUST pick one at random, and a replay MUST NOT give the same puzzle twice in a row.
- **FR-007a**: Every puzzle MUST have exactly one consistent assignment of Agent and Awake ally, given the opening statements, and MUST be solvable within the 3-question limit. This MUST be checked automatically for every puzzle.
- **FR-007b**: A player or test MUST be able to start a specific puzzle directly (for example, the reference puzzle through a shareable link), so the committed reference-game scenarios stay repeatable.
- **FR-007c**: The terminal game MUST offer the same puzzle set (by name, and at random), so the web and terminal versions stay equivalent.
- **FR-008**: Passenger dialogue MUST use the existing template voices. The page MUST NOT need any AI service, API key, or network call after it loads.

**Matrix terminal look**

- **FR-009**: The page MUST use a black background with green monospaced text that reads like a terminal, with a blinking cursor at the input line.
- **FR-010**: The page MAY show decorative Matrix effects (falling green characters behind the terminal, typed-out text). These MUST NOT reduce text legibility and MUST switch off when the system requests reduced motion.
- **FR-011**: Text MUST meet WCAG AA contrast (at least 4.5:1) against the background.
- **FR-012**: The page MUST work with keyboard alone and on touch screens from 360px wide upward, without horizontal scrolling.

**Hosting**

- **FR-013**: The game MUST be published as a static site on GitHub Pages, with no server-side component.
- **FR-014**: Publishing MUST happen automatically when changes reach the main branch. It MUST NOT publish if the unit or acceptance suite fails.

**Verification and docs**

- **FR-015**: A single documented command MUST run every scenario in `features/`, the terminal and web ones, and report pass/fail. It MUST work without the SwarmForge tool wrappers.
- **FR-016**: The web scenarios MUST check what the player actually sees in a real browser, not only the underlying game logic.
- **FR-017**: Existing unit specs (66 examples) and terminal behavior MUST keep passing unchanged.
- **FR-018**: The README MUST explain how to play in the terminal, play in the browser locally, run all tests, and reach the published game. It MUST also replace the "The browser version is being added" note.

### Key Entities

- **Game session**: one playthrough, holding the puzzle, current round, questions remaining, question/answer log, possible Agent seats, and the outcome and score once over. It lives only in the open page.
- **Passenger**: seat (A-D), display name, opening statement, and hidden role (Agent, Awake, Sleeper).
- **Puzzle**: a named, fixed set of four passengers with roles and opening statements that has exactly one solution. The reference puzzle already exists, and at least 4 more are added.
- **Terminal transcript**: the ordered lines shown to the player (Operator, passengers, system messages, player input).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can start playing from the published URL within 5 seconds on a typical broadband connection, with no install or login.
- **SC-002**: For every scripted move sequence in the terminal and web `.feature` files, the web game and the terminal game produce identical answers, outcomes, and scores (100% agreement).
- **SC-003**: The acceptance command runs every feature file (18 today, plus any added by this feature) on a clean checkout, and 100% of scenarios pass.
- **SC-004**: A full game (up to 3 questions plus an accusation) can be finished in under 8 minutes, as the game design intends.
- **SC-005**: The page is fully playable (by typing commands) at 360px, 768px and 1440px widths, with no horizontal scrolling and all text at AA contrast.
- **SC-006**: A change merged to main appears on the published site within 10 minutes, with no manual steps.
- **SC-007**: 100% of shipped puzzles pass the automatic check for exactly one solution. Across 10 consecutive new games, at least 3 different puzzles appear.

## Assumptions

- The web version uses the existing rules. It adds new puzzles but does not redesign the game. The Oracle and LLM voices from `docs/LAST_TRAIN_game_spec.md` are out of scope because the terminal game doesn't implement them either.
- No passenger voices come from an AI model. A public static page can't safely hold an API key, so template voices are used.
- Game progress is not saved between page loads. There are no accounts, leaderboards, or analytics.
- The user will create a GitHub repository for this project and push to it. There is currently **no git remote**, and GitHub Pages needs one. The repository must be public, or on a plan that allows Pages for private repositories.
- The terminal game (`bin/last-train`) stays and continues to work.
- The SwarmForge coder handoff is not needed to deliver this feature. Work proceeds through Spec Kit (`/speckit-plan` → `/speckit-tasks` → `/speckit-implement`).
- The target browsers are current versions of Chrome, Firefox, Safari and Edge, on desktop and mobile.
