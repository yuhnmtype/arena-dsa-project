# Arena Bot Custom Simulator

A custom turn-based battle engine built from scratch in Java for the IU Data Structures & Algorithms final project (IT013IU). Two AI bots compete across three phases — **Draft**, **Placement**, and **Battle** — each implemented using a distinct DSA technique studied in the course.

> **No external framework.** Engine, bot logic, pathfinding, and GUI are all hand-written.

-----

## Team

|Student ID |Name              |Responsibility                             |
|-----------|------------------|-------------------------------------------|
|ITITWE24012|Phan Nhật Huy     |Engine architecture, bot logic, coordinator|
|ITITWE24005|Lê Quốc Bảo       |GUI rendering, BFS implementation          |
|ITITWE24068|Mai Trần Tâm      |Diagrams, slides, Q&A prep                 |
|ITITWE24044|Nguyễn Võ Minh Huy|Assets, report, presentation               |

-----

## DSA Components

### 1. BFS Pathfinding — `BFSPathfinder.java`

**Problem:** Find shortest path on an 8×8 grid with dynamically blocked cells (occupied by living units).

**Algorithm:** Breadth-First Search using `ArrayDeque` as the FIFO queue and `HashSet<Position>` for O(1) visited lookup.

```
Time:  O(V + E) = O(64 + 256) ≈ O(1) for this fixed grid size
Space: O(V) = O(64) for visited set + queue
```

**Why BFS and not A*?** BFS is optimal for unweighted grids. A* adds heuristic overhead with no benefit at 8×8 — BFS is already near-instant. The grid is fully re-evaluated each turn because occupied cells change every step.

**Key design choices:**

- `ArrayDeque` instead of `LinkedList` → better cache locality
- `HashSet<Position>` for occupied cells → O(1) membership vs O(n) array scan
- Dead units are vacated from the grid immediately on kill → pathfinding always reflects true board state

-----

### 2. Greedy Draft — `StudentBotV2.draftTeam()`

**Problem:** Select up to 8 champions from a pool of 12 types, within a gold budget, to maximise team combat power.

**This is a Bounded Knapsack problem** (slots = 8, item costs = 3–5g, duplicates allowed). Pure DP is feasible here (small n), but the Greedy approach by value-density is used to demonstrate the classic approximation strategy.

**Value-per-gold formula:**

```
durability  = maxHp × (1 + defense × 0.25)
offense     = attack × (1 + range × 0.3)        // range units attack without closing
tempo       = speed × 1.5 + moveRange × 0.5
castsPerMatch = 40 / (skillCooldown + skillMana / 2)
skillBonus  = (attack + 2) × castsPerMatch × mult  // mult = 1.5 for healers, 0.8 for others

valuePerGold = (durability + offense×1.6 + tempo + skillBonus) / cost
```

**Three-pass algorithm:**

|Pass          |Purpose                                               |Complexity                       |
|--------------|------------------------------------------------------|---------------------------------|
|1. Sort       |Rank all champions by value/gold                      |O(n log n)                       |
|2. Greedy fill|Add best-fitting champion each iteration              |O(n × k), k = team slots         |
|3. Upgrade    |Swap cheapest unit for more expensive if budget allows|O(n²) worst case, n=8 in practice|

**Role constraint:** After the greedy fill, a post-processing pass guarantees ≥1 healer and ≥1 ranged unit — pure value/gold optimisation without role constraints produces unbalanced teams (e.g. 8 GUARDIANs, no ranged damage).

-----

### 3. Priority Queue — Turn Order — `BattleEngine.java`

**Problem:** Each round, all living units must act in descending speed order. Units enter and leave (die) dynamically.

**Algorithm:** `PriorityQueue<Champion>` (Java’s binary min-heap, inverted to max-heap via reversed comparator). Rebuilt each round from living units only — ensures dead units are never scheduled.

```
Comparator: speed DESC, then BLUE before RED on ties (teacher requirement)

Build:    O(n log n)
Poll all: O(n log n)   (n ≤ 16 units total)
```

**Why rebuild each round instead of a single persistent heap?** Simpler correctness guarantee — no need to track removals mid-heap. At n ≤ 16, rebuild cost is negligible.

-----

### 4. HashSet — Occupied Cell Lookup — `Grid.java`, `BFSPathfinder.java`

**Problem:** BFS must check whether each neighbour cell is blocked. Called hundreds of times per match.

**Structure:** `HashSet<Position>` keyed by `(row, col)` pair.

```
Lookup:  O(1) average  vs  O(n) for array/list scan
Insert:  O(1) average
Delete:  O(1) average   ← called on unit death to free the cell
```

**Critical bug fixed:** Previously `grid.vacate()` was never called on unit death, permanently blocking cells for the rest of the match. BFS treated corpse positions as walls, trapping living units. Fixed in `BattleEngine.executeAction()`.

-----

### 5. Placement — Role-Tiered Assignment — `StudentBotV2.placeTeam()`

**Problem:** Assign starting positions to 8 champions across 3 allowed rows to maximise team formation quality.

**Algorithm:** 4-tier role sort + greedy positional assignment.

```
Tier 1 – TANK    (GUARDIAN, PALADIN, KNIGHT)   → centre front columns
Tier 2 – STRIKER (ASSASSIN, LANCER, BERSERKER) → edge front columns (flanks)
Tier 3 – HEALER  (CLERIC, DRUID)               → centre back columns
Tier 4 – RANGED  (ARCHER, MAGE, WARLOCK, ...)  → edge back columns

Complexity: O(n log n) sort + O(n) assignment
```

**Rationale:** Tanks absorb damage from the centre. Strikers flank for early engagement. Healers are centred to maximise heal reach. Ranged units spread to avoid splash targeting.

-----

## Battle Decision Flow

Each unit follows a priority decision tree per turn:

```
HEALER?  →  hurt ally in range?  →  CAST_SKILL (heal)
             hurt ally too far?   →  MOVE toward hurt ally
             no one hurt?         →  hold / cautious advance

OTHER    →  HP < 25%?            →  MOVE toward nearest healer (retreat)
         →  enemy in range?      →  SKILL? (if secures kill or target survives 3+ hits)
                                  →  ATTACK otherwise
         →  no enemy in range   →  BFS MOVE toward focus target
```

**Focus target selection** uses effective damage `= max(1, atk - target.def)` to correctly account for armour. A GUARDIAN with def=4 facing atk=5 attackers takes only 1 dmg/hit — it should NOT be the focus target despite low effective HP.

-----

## Project Structure

```
src/
  engine/          BattleEngine, Champion, ChampionFactory, Grid, Position
                   BattleLog, BattleLogEntry, MatchResult
  bot/             IBotAI, BotAction, BFSPathfinder
                   StudentBotV2   ← all DSA algorithms live here
                   SimpleBot      ← sequential baseline (no BFS, no greedy)
  runner/          TournamentRunner, GUIRunner
  gui/
    core/          GameWindow, GamePanel, ReplayController
    panels/        ControlPanel, StatsPanel, TurnOrderPanel,
                   InfoPanel, EndMatchPanel, SetupPanel
    render/        ChampionSprite, CellRenderer, BFSVisualizer
assets/
  images/champions/   24 champion PNG sprites (96×96, RGBA)
analysis.html          Tournament dashboard (Chart.js, loads from CSV)
match_results.csv      800-match dataset
compile.bat            Windows one-click compile
```

-----

## How to Run

**Compile:**

```bat
.\compile.bat
```

**GUI (single match with replay):**

```bat
java -cp out runner.GUIRunner
```

**Tournament runner (800 matches → CSV):**

```bat
java -cp out runner.TournamentRunner
```

**Analysis dashboard:**

```bat
py -m http.server 8000
```

Then open `http://localhost:8000/analysis.html`

-----

## Tournament Setup

800 matches: 4 matchup types × 20 budget levels (5–100g, step 5) × 10 matches each.

|Matchup         |BLUE        |RED                 |
|----------------|------------|--------------------|
|V2_vs_Simple    |StudentBotV2|SimpleBot           |
|Simple_vs_V2    |SimpleBot   |StudentBotV2        |
|V2_vs_V2        |StudentBotV2|StudentBotV2        |
|Simple_vs_Simple|SimpleBot   |SimpleBot (baseline)|

-----

## Algorithm Complexity Summary

|Component  |Algorithm           |Time            |Space|
|-----------|--------------------|----------------|-----|
|Pathfinding|BFS                 |O(V+E) = O(320) |O(64)|
|Draft sort |Comparison sort     |O(n log n), n=12|O(n) |
|Draft fill |Greedy              |O(n·k), k≤8     |O(k) |
|Turn order |Priority Queue      |O(n log n), n≤16|O(n) |
|Cell lookup|HashSet             |O(1) avg        |O(64)|
|Placement  |Tiered sort + assign|O(n log n)      |O(n) |

-----

## Course

IT013IU — Algorithms and Data Structures  
International University, VNU-HCM — Semester 2, 2025–2026