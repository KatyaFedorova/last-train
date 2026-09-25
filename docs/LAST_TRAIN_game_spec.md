# last-train-matrix-game

> **Current rules (quick mode).** The shipped game was simplified from the design below. There are no Awake or Sleeper roles and no questions: one Agent always lies and the other three passengers are humans who always tell the truth (4 possible worlds). Each train's four lines leave exactly one possible Agent, and the player names it with one key (A–D) within 20 seconds. A run is 10 generated trains (3 plain, then lines with "or"). Score = 100 per right answer + 5 per second left; one hint per run names an honest passenger; a wrong answer shows why. The rest of this document is the original, harder design.

LAST TRAIN — a Matrix-style logic game

A short text game with 5 rounds and about 5–8 minutes of play. You're in a subway car inside the Matrix with four passengers. One of them is an Agent. You get a few questions before the next stop. Find the Agent.

The logic is based on Raymond Smullyan's knights-and-knaves puzzles (What Is the Name of This Book?), with a third character type added so it's no longer a kids' puzzle.

Core idea in one line

LLMs give the passengers their voices. Code decides what's true.

A deterministic logic engine owns the truth. Each LLM agent does one narrow job: acting, parsing, narrating or checking. That split is the multi-agent showcase. The puzzle stays fair because no LLM ever decides what's true.

Contents
Game Design
Logic Engine
Agent Architecture
Agent Prompts
State & Flow
Build Plan
Appendix: Reference Solver
Building it with SwarmForge
1. Game Design
Pitch

It's 2:47 AM on the last train through the city. You took the red pill a week ago. Now your Operator's voice crackles in your ear:

"There's an Agent in your car. It's wearing someone. You have three stops before it makes its move. Find it first."

Four passengers are sitting across from you. One is an Agent, one is Awake (a freed mind and your ally, though you don't know which one), and two are Sleepers: ordinary people still plugged in.

The three kinds (the heart of the game)
Kind	Rule	Matrix flavor
Awake	Always tells the truth	Unplugged, sees everything clearly
Agent	Always lies	Knows everything, uses it to deceive
Sleeper	Honest but blind: answers as if everyone is a Sleeper	Believes the Matrix is real. "Agents? What, like real estate agents?"

The Sleeper is what makes this harder than a kids' puzzle. A Sleeper never lies, but its answers about other people are always wrong in the same predictable way:

A Sleeper always says nobody is an Agent, and nobody is Awake.
A Sleeper always says any two people are the same kind.
A Sleeper says it's an ordinary person, so it's never Agent and never Awake.

That means a "no" can come from a truthful person, a liar, or a blind person. A "yes" to "Is X an Agent?" can never come from a Sleeper. Smart players exploit asymmetries like this one.

Fixed composition: 4 passengers = 1 Agent + 1 Awake + 2 Sleepers. That gives 12 possible worlds.

Rounds
#	Round	What happens
1	Boarding	Each passenger says one line on their own (a logical statement wrapped in small talk).
2	Station 1	You ask one yes/no question to one passenger.
3	Déjà vu	A black cat walks past twice (a glitch). You ask one question or call the Oracle once for a hint.
4	Station 2	One more question.
5	Last Stop	Accuse the Agent. Optional bonus: name your Awake ally.

The whole game is 3 questions total, and a solvable path always needs ≤ 2 of them. That leaves you one question as slack for a mistake.

Asking questions

The player types free text, for example "Hey courier, is the guy in the suit an Agent?" The Interpreter agent maps it to a formal question (see section 2). If it can't map the question cleanly, the Operator says "Signal's noisy. Rephrase." and the question isn't spent.

Supported questions in v1:

"Is X an Agent / Awake / a Sleeper?" (X can be "you")
"Are X and Y the same kind?"

In v2 (Smullyan-style): "If I asked you whether X is an Agent, would you say yes?"

The Oracle (once per game, round 3 only)

The Oracle uses up your round-3 question and gives one true hint that removes at least one possible world but never solves the puzzle alone. For example: "Cookie? … The nurse is not what you're looking for."

Win / lose
Correct Agent → "You pull the emergency brake. The Agent's face flickers… and it's gone." WIN.
Correct Agent + correct Awake ally → PERFECT RUN.
Wrong accusation → the Agent stands up, adjusts its tie: "Mister… Anderson." Screen glitches, LOSE.
Score = win (100) + ally (50) + 25 per unused question.
Modes
Blue Pill (standard): the rules above.
Red Pill (hard, stretch goal): at the déjà vu glitch the Agent may jump into a Sleeper's body. After that, the old host is a Sleeper and the new host is the Agent. Statements made before the glitch are judged by the roles at that time. You must accuse the Agent's current body.
Sample passengers (personas are random each game; roles are separate)
Seat	Persona	Voice
A	Vera, night-shift nurse	Tired, kind, blunt
B	Tomasz, bike courier	Chatty, jokes, fidgets with his helmet
C	Ilse, student with headphones	Sarcastic, short answers
D	Mr. Grey, man in a grey suit with a newspaper	Formal. Looks like an Agent, which is a deliberate red herring

Personas never hint at roles. Mr. Grey is the Agent exactly as often as anyone else.

Sample playthrough (verified by the reference solver (section 7))

Round 1: Boarding

Vera (A): "Relax, kid. There are no Agents on this train. Just us tired people."
Tomasz (B): "Vera? She's no Agent, trust me. She patched my knee last month."
Ilse (C): "Me and the courier? Same kind. Whatever that means."
Mr. Grey (D): "The young man and the young lady are, I'd say, the same kind."

Player deduction (the fun part):

Vera says there's no Agent. An Awake person knows there is one, so Vera is Agent or Sleeper.
Tomasz says Vera isn't an Agent. If Tomasz were the Agent, that would be a lie, so Vera would be an Agent too, but there's only one. So Tomasz is not the Agent.
Ilse says she and Tomasz are the same kind. If Ilse were Awake, Tomasz would be Awake too, which is impossible. So Ilse is Agent or Sleeper.
Three possible worlds remain: the Agent is Vera, Ilse, or Mr. Grey.

Round 2: the key insight is that Tomasz is never the Agent, so he either tells the truth or is blind. A blind Sleeper never says "yes, X is an Agent", so a "yes" from Tomasz is gold.

Player asks Tomasz: "Is Mr. Grey an Agent?" → "Nah." (Mr. Grey is ruled out.)

Round 3 (déjà vu):

Player asks Tomasz: "Is Ilse an Agent?" → "Ilse? No way." So Ilse is out, and the Agent is Vera.

Round 5: accuse Vera → WIN. Since Tomasz answered "no" to both, he's the blind Sleeper, and by elimination Mr. Grey is your Awake ally → PERFECT RUN (+25 for the one unused question).

Trap question example: "Vera, are you an Agent?" This is the rookie move. The Agent lies and a Sleeper is blind, so the answer is "no" in every remaining world, and the question is wasted. The UI can optionally say "You learned nothing new" in easy mode.

2. Logic Engine (deterministic, no LLM)

The engine is pure code and the single source of truth. LLM agents never evaluate logic. They only receive the engine's verdicts and phrase them.

1. Model
Role     = AGENT | AWAKE | SLEEPER
Seat     = A | B | C | D
World    = Map<Seat, Role>   // exactly 1 AGENT, 1 AWAKE, 2 SLEEPER → 12 worlds
2. Propositions (the grammar)
Prop := Is(seat, role)
      | Same(seat, seat)
      | CountEq(role, n)          // e.g. "There is no Agent here" = CountEq(AGENT, 0)
      | Not(Prop) | And(Prop, Prop) | Or(Prop, Prop)

eval(prop, world) -> bool is standard boolean evaluation.

3. Who can say what (the core rule)
perceived(world, speaker):
    AWAKE   -> world
    AGENT   -> world
    SLEEPER -> every seat = SLEEPER        // honest but blind

canSay(world, speaker, prop):
    AWAKE   -> eval(prop, world)
    AGENT   -> NOT eval(prop, world)
    SLEEPER -> eval(prop, allSleepers)

answerYesNo(world, speaker, prop) = canSay(world, speaker, prop)
    // "yes" means the speaker would assert prop
v2: meta questions ("If I asked you P, would you say yes?")

answerMeta(w, s, P) = canSay(w, s, Prop(answerYesNo(w, s, P) == true))

Awake: truth → truth. Agent: lie about a lie → truth. Sleeper: blind about blind → blind.
This is the classic Smullyan trick. Advanced players can use it to force the Agent to reveal the truth.
4. Consistent worlds
consistent(statements) = [ w in ALL_WORLDS | for all (speaker, prop) in statements: canSay(w, speaker, prop) ]

After each answer, append (speaker, prop, answer) and filter again. The engine always knows the live world set. The UI never shows it, except in debug mode.

5. Puzzle generation
Pick the true world at random (12 options).
Build a statement library (templates below), filtered to lines the speaker can say in the true world.
Sample one opening statement per seat. Accept the set only if all of these hold:
2 ≤ |consistent| ≤ 4
the remaining worlds contain ≥ 2 different Agent seats (not solved at boarding)
not solvable with 1 question (so it isn't trivial)
solvable with ≤ 2 adaptive questions (the player has 3, so 1 is slack)
Retry up to N=5000 times. On failure, fall back to a curated puzzle from puzzles/curated.json.

solvable(worlds, depth) is a minimax search over every legal question (4 speakers × 18 props = 72) and succeeds when every branch ends with a unique Agent. The search space is tiny, so it runs in milliseconds.

Statement templates (v1)
Logic	Surface example (the passenger agent rephrases it)
Is(X, AGENT) / Not(...)	"The guy in the suit? Definitely one of them." / "She's no Agent."
Is(X, AWAKE) / Not(...)	"He's seen behind the curtain."
Same(X, Y) / Not(...)	"Those two are cut from the same cloth."
CountEq(AGENT, 0)	"There are no Agents on this train."

Rule: a passenger may talk about themselves (Same(self, Y), Is(self, …)).

6. Hint generation (Oracle)

Pick a true fact Not(Is(X, AGENT)) or Is(X, SLEEPER) that removes ≥ 1 live world but leaves ≥ 2 Agent candidates when possible. If only one fact would solve the puzzle, choose the weakest hint available.

7. Red Pill mode (stretch)

Roles become time-indexed: World_t. At the glitch (between rounds 2 and 3) the engine may apply jump(agentSeat -> sleeperSeat). Statements are evaluated against the world at the time they were said. The consistent set tracks pairs (world_before, jump): 12 worlds × (2 possible jumps + "no jump") = 36 candidates. Generation constraints stay the same, applied to the Agent's final seat.

8. Verified reference puzzle
Seat	Statement	Logic
A	"There are no Agents on this train."	CountEq(AGENT,0)
B	"A is not an Agent."	Not(Is(A,AGENT))
C	"B and I are the same kind."	Same(B,C)
D	"B and C are the same kind."	Same(B,C)

Consistent worlds:

#	A	B	C	D
W1	AGENT	SLEEPER	SLEEPER	AWAKE
W2	SLEEPER	AWAKE	AGENT	SLEEPER
W3	SLEEPER	AWAKE	SLEEPER	AGENT

Optimal strategy: ask B "Is D an Agent?" (yes only in W3). If the answer is no, ask B "Is C an Agent?" (yes only in W2, no means W1). the reference solver (section 7) reproduces all of this. Run it to check.

3. Agent Architecture
Design principles
Code decides truth, LLMs perform. No agent evaluates logic.
Information isolation. Each agent sees only what its job needs. Passenger agents don't know their own role, so they can't leak it.
Verify every LLM output against a machine-checkable contract. Retry on failure, then fall back to a template.
One orchestrator, many specialists. The Director is the only agent that talks to the engine and routes messages.
Roster
                    ┌──────────────┐
   player text ───▶ │ Interpreter  │──▶ formal query (JSON)
                    └──────────────┘
                           │
                           ▼
┌──────────┐  verdicts  ┌───────────────┐  scene/narration  ┌──────────┐
│  Logic   │◀──────────▶│   Director    │──────────────────▶│  Player  │
│  Engine  │   (code)   │ (orchestrator)│◀──────────────────│    UI    │
└──────────┘            └───────────────┘                   └──────────┘
                        │      │      │
         line spec ─────┘      │      └───── hint spec
                ▼              ▼                ▼
       ┌──────────────┐  ┌──────────┐    ┌──────────┐
       │ Passenger ×4 │─▶│ Verifier │    │  Oracle  │
       └──────────────┘  └──────────┘    └──────────┘
Agent	Type	Job	Sees	Never sees
Logic Engine	Code	Generate the puzzle, answer queries, track live worlds, compute hints, score	Everything	—
Director	LLM + code	Orchestrates rounds and narrates as the Operator voice ("the Operator")	Round state, public transcript	Roles (in prod mode)
Passenger ×4	LLM	Speak one line in persona that conveys an exact logical content	Persona, the prop to convey, polarity, transcript	Its own role, other roles
Interpreter	LLM	Free text → formal query, or REJECT	Player text, seat/persona names	Roles
Verifier	LLM (judge)	Check the passenger line conveys exactly the prop, with no extra claims	Line, prop spec	Roles
Oracle	LLM	Phrase the engine's hint cryptically but unambiguously	Hint fact, persona names	Roles

Having the Director blind to roles is optional. Keep it blind anyway, because it's a stronger demo: "No LLM in the loop knows the answer."

I/O contracts (JSON)
Director → Passenger
json
{
  "persona": {"seat": "B", "name": "Tomasz", "bio": "bike courier", "voice": "chatty, jokes"},
  "task": "ANSWER",                         // or "STATEMENT"
  "question_text": "Is the guy in the suit an Agent?",
  "must_convey": {"prop": "Is(D, AGENT)", "polarity": false},
  "names": {"A": "Vera", "B": "Tomasz", "C": "Ilse", "D": "Mr. Grey"},
  "transcript_tail": ["..."]
}

polarity: false means the line must clearly communicate NO / the prop is false.

Passenger → Verifier → Director
json
{"line": "Mr. Grey? Nah. Guy just really likes newspapers."}
json
{"conveys": "Not(Is(D,AGENT))", "matches_spec": true, "extra_claims": [], "ambiguous": false}

Accept only if matches_spec && !ambiguous && extra_claims.length == 0. Otherwise retry (max 2), then use the template line: "No. Mr. Grey is not an Agent."

Player → Interpreter
json
{"status": "OK", "target_seat": "B", "prop": "Is(D, AGENT)", "kind": "YESNO"}
json
{"status": "REJECT", "reason": "multiple questions in one"}
Engine → Oracle
json
{"fact": "Not(Is(A, AGENT))", "names": {...}}
Why this is a good multi-agent showcase
Role separation: actor, parser, judge, narrator and hint-giver each have their own prompt and contract.
Information asymmetry by design: a passenger can't cheat because it doesn't know the answer.
Generator/verifier loop: passenger output is checked by a separate judge, with retries and a fallback.
Deterministic core: the game is reproducible from a seed, so you can write eval tests (see section 6).
Parallelism: the four boarding lines are generated concurrently.
4. Agent Prompts

{{var}} = injected at runtime. Every output is JSON, parsed and validated by code.

Director (the Operator)
You are the Operator: a calm, dry voice in the player's earpiece during a
Matrix-style mission on a late-night subway. You narrate scene transitions only.

Rules:
- You do NOT know which passenger is the Agent. Never guess or hint.
- Max 2 sentences per beat. Present tense. Terse, a little ominous.
- Beats you may be asked for: INTRO, STATION, GLITCH, REJECTED_QUESTION,
  ACCUSE_PROMPT, WIN, PERFECT, LOSE.
- For WIN/LOSE you'll be given the outcome; just dramatize it.

Input: {"beat": "...", "round": n, "questions_left": n, "names": {...}, "outcome": ...}
Output JSON: {"text": "..."}
Passenger
You are {{name}}, {{bio}}, riding the last train at 2:47 AM.
Voice: {{voice}}.

You will be told exactly WHAT your line must mean. Your only job is HOW you
say it, in character.

Hard rules:
1. Your line must clearly convey exactly this, and nothing more:
   {{must_convey_plain}}   (e.g. "NO — Mr. Grey is not an Agent")
2. Do not add any other claim about who is an Agent, Awake, or ordinary.
3. No hedging that changes the meaning ("maybe", "I think not" are forbidden).
4. 1–2 sentences, max 30 words. Refer to others by name.
5. Small talk and persona detail are welcome as long as they don't make any claim.

Recent conversation: {{transcript_tail}}
Question asked (if any): {{question_text}}

Output JSON: {"line": "..."}

must_convey_plain is produced by code from the prop and polarity. The passenger never sees role words about itself.

Verifier (judge)
You are a strict logic auditor. Decide what a spoken line asserts.

Vocabulary: people {{names}}; kinds: AGENT, AWAKE, SLEEPER (ordinary person).
Speaker: {{speaker_name}}. "I/me" = speaker.

Line: "{{line}}"
Required meaning: {{prop}} with polarity {{polarity}}

Tasks:
1. Translate the line's core claim into the formal grammar:
   Is(X,ROLE) | Same(X,Y) | CountEq(ROLE,n) | Not(...) | And | Or
2. List any EXTRA claims about kinds (anything beyond the required one).
3. Flag ambiguity if a reasonable listener could read it two ways.

Output JSON:
{"conveys": "...", "matches_spec": bool, "extra_claims": [...], "ambiguous": bool}
Interpreter
You translate a player's question to a passenger into a formal yes/no query.

Passengers: {{names_with_descriptions}}  (e.g. "D = Mr. Grey, the man in the grey suit")
Allowed props: Is(X, AGENT|AWAKE|SLEEPER), Same(X, Y).
Synonyms: "one of them / program / suit" -> AGENT; "unplugged / free / red pill /
awake / one of us" -> AWAKE; "normal / ordinary / plugged in / asleep" -> SLEEPER.
"you" = the passenger being addressed.

REJECT if: not yes/no, more than one question, uses and/or/if (v1),
refers to an unknown person, or asks about anything other than kinds.

Output JSON:
{"status":"OK","target_seat":"B","prop":"Is(D, AGENT)","kind":"YESNO"}
or
{"status":"REJECT","reason":"short reason the Operator can say"}
Oracle
You are the Oracle: warm, cryptic, baking cookies in a kitchen that shouldn't
exist. You deliver ONE true fact: {{fact_plain}} (e.g. "Vera is not the Agent").

- Be playful and mysterious, but the fact must be unambiguous. Use the name.
- Do not add any other information. Max 2 sentences.

Output JSON: {"text": "..."}

The Oracle's output also goes through the Verifier.

5. State & Flow
Game state
ts
type Role = "AGENT" | "AWAKE" | "SLEEPER";
type Seat = "A" | "B" | "C" | "D";

interface GameState {
  seed: number;
  mode: "BLUE_PILL" | "RED_PILL";
  round: 1 | 2 | 3 | 4 | 5;
  phase: "BOARDING" | "ASK" | "GLITCH" | "ACCUSE" | "OVER";
  questionsLeft: number;               // starts at 3
  oracleUsed: boolean;
  passengers: Record<Seat, Persona>;   // public
  secret: { trueWorld: Record<Seat, Role>; jump?: {from: Seat; to: Seat} }; // engine only
  facts: Fact[];                       // (speaker, prop, polarity, round)
  liveWorlds: World[];                 // engine only (debug view)
  transcript: Line[];                  // public
  outcome?: { win: boolean; allyCorrect: boolean; score: number };
}

secret and liveWorlds are never serialized into any LLM prompt.

State machine
INTRO
  └─▶ R1 BOARDING  (4 statements, parallel) ─────────────▶ R2 ASK
R2 ASK      ── valid question ─▶ answer ─────────────────▶ R3 GLITCH
R3 GLITCH   ── (red pill: engine may jump)
            ── question OR oracle ───────────────────────▶ R4 ASK
R4 ASK      ── valid question ─▶ answer ─────────────────▶ R5 ACCUSE
Any ASK     ── "accuse now" ─────────────────────────────▶ R5 ACCUSE (bonus for unused Qs)
R5 ACCUSE   ── pick Agent (+ optional ally) ─▶ score ────▶ OVER
Invalid question → REJECTED beat, stay in same round, no question spent
One ASK round: message sequence
Player    → UI:           "Tomasz, is the suit guy an Agent?"
UI        → Interpreter:  text + names
Interpreter→ Director:    {OK, target B, Is(D,AGENT)}
Director  → Engine:       answer(B, Is(D,AGENT))
Engine    → Director:     {polarity:false}   (+ updates liveWorlds)
Director  → Passenger B:  persona + must_convey NO
Passenger → Verifier:     {"line": "..."}
Verifier  → Director:     matches ✔  (else retry ×2 → template)
Director  → Operator:     STATION beat
Director  → UI:           passenger line + Operator line
Latency budget (text game, must feel snappy)
Step	Target
Interpreter	< 1.0 s (small/fast model)
Passenger + Verifier	< 2.5 s total
Boarding (4 lines in parallel)	< 3 s

Use a small model for Interpreter and Verifier and a stronger one for Passengers and the Operator.

Debug / demo mode

Add a toggle that shows a side panel with live worlds, the engine's verdict for each line, Verifier results and retries. This is the "look at my multi-agent system" view for your portfolio.

6. Build Plan (for a multi-agent dev team)

Tool-agnostic. The flow is test-first: each ticket starts by writing its acceptance tests. Tickets in different lanes can run in parallel across dev agents. the reference solver (section 7) is the ground truth for all engine tests.

Suggested dev-agent team
Dev agent	Owns
Architect	Interfaces and contracts (T0), reviews every PR against section 3
Engine dev	Lane E
LLM dev	Lane L
Frontend dev	Lane U
QA / eval	Lane Q, the gatekeeper for every merge
Tickets
T0 — Contracts (blocks everything)

Types from section 5 and the JSON schemas from section 3. AC: schemas compile, and sample JSONs in docs validate.

Lane E — Logic Engine (pure code)
E1 Props & eval. AC: eval matches the truth table for all props on all 12 worlds.
E2 canSay / answer. AC: Sleeper answers Is(X,AGENT) → always false, Same(X,Y) → always true. Agent answers are the negation of Awake answers in every world.
E3 Consistent worlds. AC: the reference puzzle yields exactly W1, W2, W3 (see section 2, §8).
E4 Solvability search. AC: the reference puzzle is solvable(depth=2) and not solvable(depth=1).
E5 Generator. AC: 1,000 seeds all satisfy the constraints in section 2, §5, and each takes < 200 ms.
E6 Hints. AC: a hint is always true in the true world and removes ≥ 1 live world.
E7 Scoring. AC: the table in section 1, "Win / lose".
E8 (stretch) Red Pill jump. AC: time-indexed evaluation, 36 candidates.
Lane L — LLM agents
L1 Interpreter. AC: ≥ 95% accuracy on tests/interpreter_cases.jsonl (write 60+ cases: synonyms, "you", rejects).
L2 Passenger. AC: over 200 generated lines, Verifier pass rate ≥ 90% on the first try.
L3 Verifier. AC: catches 100% of a hand-made "bad lines" set (wrong polarity, extra claims, hedges).
L4 Retry/fallback loop. AC: never emits an unverified line, and falls back to the template after 2 failures.
L5 Operator + Oracle. AC: output ≤ 2 sentences, and the Oracle passes the Verifier.
Lane U — UI (terminal first, web optional)
U1 CLI loop: rounds, input, transcript. U2 Debug side panel (live worlds, verdicts). U3 (optional) Web UI in a green-on-black terminal style.
Lane Q — Evals (the portfolio centerpiece)
Q1 Leak test: grep every LLM prompt in the logs for role words tied to seats → must be zero.
Q2 Fairness test: simulate 1,000 games with a perfect bot player; it must win 100%.
Q3 Red herring test: Mr. Grey's Agent rate ≈ 25% across seeds.
Q4 End-to-end replay: seed 42 replays the identical game (mock the LLMs).
Milestones
M	Scope	Demo
M1	T0 + E1–E4 + U1 with template lines (no LLM)	Playable logic game
M2	L1–L4	Passengers talk, Verifier loop visible in debug
M3	E5–E7, L5, Q1–Q4	Full game, eval report
M4	E8, U3	Red Pill mode, web UI

M1 matters most: the game has to be fun with no LLM at all before you add the voices.

7. Appendix: Reference Solver

Save this as reference_solver.py and run it with python reference_solver.py. It's the ground truth for the engine tests.

python
"""LAST TRAIN — reference logic engine & solver (ground truth for tests).
Run: python reference_solver.py
"""
import itertools

SEATS = ["A", "B", "C", "D"]
AGENT, AWAKE, SLEEPER = "AGENT", "AWAKE", "SLEEPER"


def all_worlds():
    for ag, aw in itertools.permutations(SEATS, 2):
        yield {s: AGENT if s == ag else AWAKE if s == aw else SLEEPER for s in SEATS}


ALL_WORLDS = list(all_worlds())
ALL_SLEEPERS = {s: SLEEPER for s in SEATS}

# --- propositions: (label, fn(world) -> bool) ---
def Is(x, r): return (f"Is({x},{r})", lambda w: w[x] == r)
def Same(x, y): return (f"Same({x},{y})", lambda w: w[x] == w[y])
def CountEq(r, n): return (f"CountEq({r},{n})", lambda w: sum(v == r for v in w.values()) == n)
def Not(p): return (f"Not({p[0]})", lambda w: not p[1](w))


def can_say(world, speaker, prop):
    role, f = world[speaker], prop[1]
    if role == AWAKE:
        return f(world)
    if role == AGENT:
        return not f(world)
    return f(ALL_SLEEPERS)  # sleeper: honest but blind


def consistent(facts, worlds=ALL_WORLDS):
    """facts: list of (speaker, prop, asserted_bool)."""
    return [w for w in worlds if all(can_say(w, s, p) == a for s, p, a in facts)]


QUESTIONS = [(s, Is(x, r)) for s in SEATS for x in SEATS for r in (AGENT, AWAKE, SLEEPER)] + \
            [(s, Same(x, y)) for s in SEATS for x, y in itertools.combinations(SEATS, 2)]


def agent_of(w): return next(s for s in SEATS if w[s] == AGENT)


def solvable(worlds, depth):
    if len({agent_of(w) for w in worlds}) <= 1:
        return True
    if depth == 0:
        return False
    for s, p in QUESTIONS:
        yes = [w for w in worlds if can_say(w, s, p)]
        no = [w for w in worlds if not can_say(w, s, p)]
        if yes and no and solvable(yes, depth - 1) and solvable(no, depth - 1):
            return True
    return False


if __name__ == "__main__":
    opening = [
        ("A", CountEq(AGENT, 0), True),       # "No Agents on this train."
        ("B", Not(Is("A", AGENT)), True),     # "Vera is no Agent."
        ("C", Same("B", "C"), True),          # "Me and the courier? Same kind."
        ("D", Same("B", "C"), True),          # "Those two are the same kind."
    ]
    ws = consistent(opening)
    print(f"Consistent worlds: {len(ws)}")
    for w in ws:
        print("  ", w)
    assert len(ws) == 3
    assert not solvable(ws, 1) and solvable(ws, 2)

    # Sample playthrough: true world = W1 (Vera is the Agent)
    q1 = ("B", Is("D", AGENT), False)
    q2 = ("B", Is("C", AGENT), False)
    final = consistent(opening + [q1, q2])
    assert len(final) == 1 and agent_of(final[0]) == "A"
    print("Sample playthrough verified -> Agent is A (Vera), ally is",
          next(s for s in SEATS if final[0][s] == AWAKE))

    # Trap question: uninformative ("Vera, are you an Agent?" -> always "no")
    trap = {can_say(w, "A", Is("A", AGENT)) for w in ws}
    assert trap == {False}
    print("Trap question 'Vera, are you an Agent?' gives no info: OK")
8. Building it with SwarmForge

SwarmForge (github.com/unclebob/swarm-forge) runs role agents in tmux and git worktrees. They hand work to each other through commits. The specifier turns intent into Gherkin, gets your approval, and hands the spec to the coder. The specs get mutation-tested, and the QA suites have to work at the user-interface level (CLI flags are allowed).

Which pack
Pack	Roles	Use it when
four-pack (recommended to start)	specifier → coder → refactorer → architect	Milestone M1–M2: a small, fast loop
six-pack	specifier → coder → cleaner → architect → hardener → QA	M3+, when you want the eval/QA story for the portfolio
bash
cd last-train && git init
get-swarm-forge four-pack
./swarm

Set the language and stack in the pack's project.prompt (e.g. Python 3.12 + pytest + behave, or TypeScript + vitest + cucumber). Put the backend (claude/codex/…) in swarmforge/swarmforge.conf.

Rules to give the swarm (add to project.prompt)
This document is the source of truth. Put it in the repo as docs/LAST_TRAIN_game_spec.md.
Build order is M1 → M4 (section 6). M1 has no LLM: template lines only.
The logic engine is pure and deterministic. It never calls an LLM.
Every game is reproducible with --seed N. LLM agents are behind an interface with a --voices=template fake. All Gherkin and QA suites use the fake, so they stay deterministic.
No role or secret data may appear in any LLM prompt (test Q1).
reference_solver.py (section 7) is the oracle for engine tests.
First message to the specifier

Read docs/LAST_TRAIN_game_spec.md. Write Gherkin for milestone M1 only: the logic engine rules (section 2) and a CLI game loop with template voices (section 5). Use the seed features in section 8 as a starting point and refine them. The QA suite should drive the CLI with --seed and --voices=template. Ask me about anything ambiguous before handing off.

Seed features (the specifier refines these)
gherkin
Feature: Passenger kinds answer by their rules
  Background:
    Given the true world is A=AGENT, B=SLEEPER, C=SLEEPER, D=AWAKE

  Scenario Outline: yes/no answers
    When I ask <speaker> "<question>"
    Then the answer is "<answer>"

    Examples:
      | speaker | question            | answer |
      | D       | Is A an Agent?      | yes    |
      | A       | Is A an Agent?      | no     |
      | B       | Is A an Agent?      | no     |
      | B       | Are C and D the same kind? | yes |
      | A       | Is D awake?         | no     |
gherkin
Feature: Opening statements narrow the possible worlds
  Scenario: reference puzzle leaves three worlds
    Given the opening statements
      | speaker | statement                  |
      | A       | CountEq(AGENT,0)           |
      | B       | Not(Is(A,AGENT))           |
      | C       | Same(B,C)                  |
      | D       | Same(B,C)                  |
    Then the possible Agents are A, C, D
    And the puzzle is not solvable in 1 question
    And the puzzle is solvable in 2 questions
gherkin
Feature: Playing a game from the CLI
  Background:
    Given I start "last-train --seed reference --voices=template"

  Scenario: perfect run
    When I ask "B" "Is D an Agent?"
    Then B says no
    When I ask "B" "Is C an Agent?"
    Then B says no
    When I accuse "A" with ally "D"
    Then I see "PERFECT RUN"
    And the score is 175

  Scenario: an unparseable question costs nothing
    When I type "what is love?"
    Then I see "Signal's noisy. Rephrase."
    And I have 3 questions left

  Scenario: wrong accusation
    When I accuse "D"
    Then I see "LOSE"

Scores: win 100 + ally 50 + 25 × 1 unused question = 175. --seed reference loads the curated puzzle from section 2 §8.
