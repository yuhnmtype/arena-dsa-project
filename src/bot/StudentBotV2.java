package bot;

import engine.Champion;
import engine.ChampionFactory;
import engine.Grid;
import engine.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * StudentBotV2 - optimized bot with BFS pathfinding.
 *
 * DSA components used:
 *   draftTeam  : greedy selection by value-per-gold, O(n log n)
 *   BFSPathfinder.advanceToward : breadth-first search around obstacles, O(V + E)
 *   occupied   : HashSet membership test, O(1) per lookup
 *   turn loop  : linear scan of allies, O(n)
 *
 * Movement note for THIS engine: BattleEngine executes a MOVE by teleporting the
 * champion straight to the requested cell as long as it is in bounds and not
 * occupied (Grid.isValid). There is no per-turn move-range cap. A naive bot that
 * steps one tile toward the target therefore crawls and, worse, stalls when the
 * straight-line cell is blocked by another unit. BFS solves both problems: it finds
 * the shortest route around occupied cells, and because moves are unbounded we jump
 * to the farthest cell along that route in a single turn.
 */
public class StudentBotV2 implements IBotAI {

    private static final int MAX_TEAM = 8;

    private static final Set<String> RANGED = new HashSet<>();
    private static final Set<String> HEALER = new HashSet<>();
    static {
        RANGED.add("ARCHER");
        RANGED.add("MAGE");
        RANGED.add("WARLOCK");
        RANGED.add("FROST_WITCH");
        HEALER.add("CLERIC");
        HEALER.add("DRUID");
    }

    // ============================================================
    // PHASE 1: DRAFT - greedy by value-per-gold, O(n log n)
    // ============================================================
    @Override
    public List<String> draftTeam(int budget, List<String> availableIds) {
        List<String> sorted = new ArrayList<>(availableIds);
        sorted.sort((a, b) -> Double.compare(
                valuePerGold(ChampionFactory.create(b, "BLUE")),
                valuePerGold(ChampionFactory.create(a, "BLUE"))));

        List<String> picks = new ArrayList<>();
        int spent = 0;
        // Greedy fill: best value-per-gold first, duplicates allowed, respect budget + cap.
        boolean progress = true;
        while (picks.size() < MAX_TEAM && progress) {
            progress = false;
            for (String id : sorted) {
                if (picks.size() >= MAX_TEAM) break;
                int cost = ChampionFactory.getCost(id);
                if (spent + cost <= budget) {
                    picks.add(id);
                    spent += cost;
                    progress = true;
                }
            }
        }
        if (picks.isEmpty() && !sorted.isEmpty()) {
            String cheapest = sorted.get(0);
            for (String id : sorted) {
                if (ChampionFactory.getCost(id) < ChampionFactory.getCost(cheapest)) cheapest = id;
            }
            if (ChampionFactory.getCost(cheapest) <= budget) picks.add(cheapest);
        }
        return picks;
    }

    // Value-per-gold: reward durability (survives the damage floor) and damage output,
    // with a small bonus for range and speed. Defense is weighted because damage is
    // reduced by defense (min 1), so each point of defense saves damage every hit.
    private double valuePerGold(Champion c) {
        double durability = c.getMaxHp() * (1.0 + c.getDefense() * 0.25);
        double offense = c.getAttack() * (1.0 + c.getRange() * 0.3);
        double tempo = c.getSpeed() * 1.5 + c.getMoveRange() * 0.5;
        return (durability * 1.0 + offense * 1.6 + tempo) / c.getCost();
    }

    // ============================================================
    // PHASE 2: PLACEMENT - melee front, ranged/healers back
    // ============================================================
    @Override
    public List<Position> placeTeam(List<Champion> team,
                                    List<Position> allowedCells,
                                    boolean isBlue) {
        // Front row = the one closest to the enemy. BLUE owns rows 0-2 (front = highest row),
        // RED owns rows 5-7 (front = lowest row).
        int frontRow = isBlue ? maxRow(allowedCells) : minRow(allowedCells);

        List<Position> front = new ArrayList<>();
        List<Position> back = new ArrayList<>();
        for (Position p : allowedCells) {
            if (p.row() == frontRow) front.add(p);
            else back.add(p);
        }
        front.sort(Comparator.comparingInt(Position::col));
        back.sort(Comparator.comparingInt(Position::col));

        // Melee take front cells, ranged/healers take back cells.
        List<Champion> ordered = new ArrayList<>(team);
        ordered.sort(Comparator.comparingInt(c -> rear(c.getId()) ? 1 : 0));

        List<Position> result = new ArrayList<>();
        int fi = 0, bi = 0;
        for (Champion c : ordered) {
            Position p;
            if (rear(c.getId())) {
                p = bi < back.size() ? back.get(bi++) : (fi < front.size() ? front.get(fi++) : null);
            } else {
                p = fi < front.size() ? front.get(fi++) : (bi < back.size() ? back.get(bi++) : null);
            }
            if (p != null) result.add(p);
        }
        // Safety: if anything is unassigned, fill from remaining allowed cells in order.
        if (result.size() < team.size()) {
            for (Position p : allowedCells) {
                if (result.size() >= team.size()) break;
                if (!result.contains(p)) result.add(p);
            }
        }
        // placeTeam must return one position per champion, in team order. Re-map:
        return remapToTeamOrder(team, ordered, result);
    }

    // The engine assigns placement[i] to team[i], but we sorted into 'ordered'. Rebuild a
    // team-indexed list so each champion gets the cell we chose for it.
    private List<Position> remapToTeamOrder(List<Champion> team,
                                            List<Champion> ordered,
                                            List<Position> orderedPlaces) {
        Map<String, Position> byId = new HashMap<>();
        for (int i = 0; i < ordered.size() && i < orderedPlaces.size(); i++) {
            byId.put(ordered.get(i).getId(), orderedPlaces.get(i));
        }
        List<Position> out = new ArrayList<>();
        for (Champion c : team) {
            Position p = byId.get(c.getId());
            if (p != null) out.add(p);
        }
        return out;
    }

    // ============================================================
    // PHASE 3: BATTLE - BFS pathfinding + focus fire
    // ============================================================
    @Override
    public List<BotAction> playTurn(List<Champion> allies,
                                    List<Champion> enemies,
                                    Grid grid, int round) {
        List<BotAction> actions = new ArrayList<>();

        List<Champion> aliveEnemies = aliveOnly(enemies);
        List<Champion> aliveAllies = aliveOnly(allies);
        if (aliveEnemies.isEmpty()) {
            for (Champion a : aliveAllies) actions.add(BotAction.wait(a.getId()));
            return actions;
        }

        // occupied set for pathfinding: O(n) build, O(1) per lookup during BFS.
        Set<Position> occupied = new HashSet<>();
        for (Champion c : aliveAllies) occupied.add(c.getPosition());
        for (Champion c : aliveEnemies) occupied.add(c.getPosition());

        // Shared focus target: the enemy that takes the fewest hits to kill (lowest hp/dmg).
        Champion focus = focusTarget(aliveAllies, aliveEnemies);

        for (Champion ally : aliveAllies) {
            // The ally vacates its own cell while it decides (it can move out of it).
            occupied.remove(ally.getPosition());
            BotAction act = decide(ally, focus, aliveAllies, aliveEnemies, grid, occupied);
            // Reserve the resulting cell so later-acting allies do not path into it.
            if (act.type == BotAction.Type.MOVE && act.targetPosition != null) {
                occupied.add(act.targetPosition);
            } else {
                occupied.add(ally.getPosition());
            }
            actions.add(act);
        }
        return actions;
    }

    private BotAction decide(Champion ally, Champion focus,
                             List<Champion> allies, List<Champion> enemies,
                             Grid grid, Set<Position> occupied) {
        String id = ally.getId();
        String role = templateOf(id); // unique id is "SIDE_TEMPLATE_index"; role checks use the template

        // Healer: heal the most-hurt ally in range; do NOT wander off when nobody needs healing.
        if (HEALER.contains(role) && skillReady(ally)) {
            Champion hurt = mostHurtAlly(allies);
            if (hurt != null
                    && ally.getPosition().manhattanDistance(hurt.getPosition()) <= ally.getRange() + 1) {
                return new BotAction(id, BotAction.Type.CAST_SKILL, null, hurt.getId());
            }
        }

        // Attack the best target already in range (prefer one we can kill this turn).
        Champion inRange = bestInRange(ally, enemies);
        if (inRange != null) {
            if (skillReady(ally) && !HEALER.contains(role)) {
                return new BotAction(id, BotAction.Type.CAST_SKILL, null, inRange.getId());
            }
            return BotAction.attack(id, inRange.getId());
        }

        // Choose who to approach: the shared focus, unless a different enemy is much closer.
        Champion goalEnemy = approachTarget(ally, focus, enemies);

        // BFS to the best open cell adjacent to the target, then (because this engine allows
        // unbounded moves) jump to the FARTHEST cell along that shortest path. If a healer has
        // nothing to do and no enemy is close, it holds near the team instead of running in.
        if (HEALER.contains(role)) {
            int d = ally.getPosition().manhattanDistance(goalEnemy.getPosition());
            if (d > ally.getRange() + 2) {
                Position step = BFSPathfinder.advanceToward(ally.getPosition(), goalEnemy.getPosition(), grid, occupied);
                if (step != null && !step.equals(ally.getPosition())) {
                    return BotAction.move(id, step);
                }
            }
            return BotAction.wait(id);
        }

        Position dest = BFSPathfinder.advanceToward(ally.getPosition(), goalEnemy.getPosition(), grid, occupied);
        if (dest != null && !dest.equals(ally.getPosition())) {
            return BotAction.move(id, dest);
        }
        return BotAction.wait(id);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private Champion focusTarget(List<Champion> allies, List<Champion> enemies) {
        Champion best = null;
        double bestRounds = Double.MAX_VALUE;
        for (Champion e : enemies) {
            // estimate rounds to kill using our strongest attacker's damage
            int bestDmg = 1;
            for (Champion a : allies) {
                bestDmg = Math.max(bestDmg, Math.max(1, a.getAttack() - e.getDefense()));
            }
            double rounds = (double) e.getHp() / bestDmg;
            if (rounds < bestRounds) { bestRounds = rounds; best = e; }
        }
        return best;
    }

    private Champion approachTarget(Champion ally, Champion focus, List<Champion> enemies) {
        // Follow the team focus unless a different enemy is meaningfully closer to this unit.
        Champion nearest = null;
        int nd = Integer.MAX_VALUE;
        for (Champion e : enemies) {
            int d = ally.getPosition().manhattanDistance(e.getPosition());
            if (d < nd) { nd = d; nearest = e; }
        }
        if (focus == null) return nearest;
        int fd = ally.getPosition().manhattanDistance(focus.getPosition());
        return (nearest != null && fd - nd >= 3) ? nearest : focus;
    }

    private Champion bestInRange(Champion attacker, List<Champion> enemies) {
        Champion best = null;
        double bestScore = Double.MAX_VALUE;
        for (Champion e : enemies) {
            int dist = attacker.getPosition().manhattanDistance(e.getPosition());
            if (dist > attacker.getRange()) continue;
            int dmg = Math.max(1, attacker.getAttack() - e.getDefense());
            boolean kill = dmg >= e.getHp();
            double score = kill ? -1 : (double) e.getHp() / dmg; // prefer a kill, else fastest kill
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    private Champion mostHurtAlly(List<Champion> allies) {
        Champion best = null;
        double worst = 0.65; // only heal allies below 65% hp
        for (Champion c : allies) {
            double frac = (double) c.getHp() / c.getMaxHp();
            if (frac < worst) { worst = frac; best = c; }
        }
        return best;
    }

    private boolean skillReady(Champion c) {
        return c.getMana() >= c.getSkillManaCost() && c.getRemainingCooldown() == 0;
    }

    private boolean rear(String id) {
        String t = templateOf(id);
        return RANGED.contains(t) || HEALER.contains(t);
    }

    // Champions carry a unique id of the form "SIDE_TEMPLATE_index" (e.g. "BLUE_CLERIC_0").
    // Role lookups use the TEMPLATE portion, so extract it. Falls back to the raw id if the
    // format is unexpected, so a plain "CLERIC" id still works.
    private String templateOf(String id) {
        if (id == null) return "";
        String[] parts = id.split("_");
        return parts.length >= 2 ? parts[parts.length - 2] : id;
    }

    private List<Champion> aliveOnly(List<Champion> list) {
        List<Champion> out = new ArrayList<>();
        for (Champion c : list) if (c.isAlive()) out.add(c);
        return out;
    }

    private int maxRow(List<Position> cells) {
        int m = 0;
        for (Position p : cells) m = Math.max(m, p.row());
        return m;
    }

    private int minRow(List<Position> cells) {
        int m = Integer.MAX_VALUE;
        for (Position p : cells) m = Math.min(m, p.row());
        return m;
    }
}