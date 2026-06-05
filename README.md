# Arena Bot Custom Simulator

A custom turn-based battle engine built in Java for the IU Data Structures
and Algorithms final project. Two AI bots compete by drafting champion teams,
placing them on an 8x8 grid, and fighting round by round until one team is
eliminated or the 40-round limit is reached.

## DSA Components

- BFS Pathfinding: shortest path navigation on an 8x8 grid, O(V+E) = O(256)
- Greedy Draft: champion selection by value-per-gold ratio, O(n log n)
- Priority Queue: turn ordering by SPD stat, O(log n) per operation
- HashSet: O(1) occupied cell lookup during pathfinding

## Project Structure

```
src/
  engine/     BattleEngine, Champion, ChampionFactory, Grid, Position, MatchResult
  bot/        IBotAI, BotAction, StudentBotV2, SimpleBot, BFSPathfinder
  runner/     TournamentRunner
analysis.html        Live tournament dashboard (Chart.js)
match_results.csv    800 match results across 4 matchup types
compile.bat          Windows compile script
```

## How to Run

Compile:
```
.\compile.bat
```

Run tournament (800 matches):
```
java -cp out runner.TournamentRunner
```

View dashboard:
```
py -m http.server 8000
```
Then open http://localhost:8000/analysis.html

## Tournament Setup

4 matchup types x 20 budgets (5-100) x 10 matches = 800 total matches

- V2_vs_Simple: StudentBotV2 (BLUE) vs SimpleBot (RED)
- Simple_vs_V2: SimpleBot (BLUE) vs StudentBotV2 (RED)
- V2_vs_V2: StudentBotV2 vs StudentBotV2
- Simple_vs_Simple: SimpleBot vs SimpleBot (baseline)

## Key Findings

StudentBotV2 with BFS consistently outperforms SimpleBot with naive movement.
StudentBotV2 achieves 95% win rate when playing as RED against SimpleBot.
At low budgets, BFS pathfinding gives a first-mover advantage that determines
match outcomes within 10-15 rounds. At high budgets, matches reach the 40-round
limit and are decided by remaining HP.

## Team

| Student ID    | Name                  | Role                  |
|---------------|-----------------------|-----------------------|
| ITITWE24012   | Phan Nhat Huy         | Engine + Data Analyst |
| ITITWE24005   | Le Quoc Bao           | Bot AI + BFS          |
| ITITWE24068   | Mai Tran Tam          | Diagrams + Slides     |
| ITITWE24044   | Nguyen Vo Minh Huy    | Report + Presentation |

## Course

IT013IU - Algorithms and Data Structures
International University, VNU-HCM, 2025-2026 Semester 2
