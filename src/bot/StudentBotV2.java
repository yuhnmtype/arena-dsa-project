package bot;

import engine.Champion;
import engine.ChampionFactory;
import engine.Grid;
import engine.Position;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StudentBotV2 — tuned for the simultaneous-resolution engine.
 *
 * Under simultaneous resolution, all attacks resolve from a single HP snapshot:
 * you cannot prevent retaliation by killing first, and retreating wastes your attack
 * without reducing incoming damage. The optimal strategy is therefore maximum aggression:
 * every unit should deal damage every round if possible.
 *
 * DSA components:
 *   BFSPathfinder.advanceToward : O(V+E) pathfinding around obstacles
 *   Greedy draft (sort + fill)  : O(n log n) + O(n*k)
 *   HashSet for occupied cells   : O(1) membership
 */
public class StudentBotV2 implements IBotAI {

    private static final int MAX_TEAM = 8;

    private static final Set<String> RANGED = new HashSet<>();
    private static final Set<String> HEALER = new HashSet<>();
    static {
        RANGED.add("ARCHER"); RANGED.add("MAGE"); RANGED.add("WARLOCK"); RANGED.add("FROST_WITCH");
        HEALER.add("CLERIC"); HEALER.add("DRUID");
    }

    // ================================================================
    // DRAFT — greedy by value-per-gold
    // ================================================================
    @Override
    public List<String> draftTeam(int budget, List<String> availableIds) {
        List<String> sorted = new ArrayList<>(availableIds);
        sorted.sort((a, b) -> Double.compare(
                valuePerGold(ChampionFactory.create(b, "BLUE")),
                valuePerGold(ChampionFactory.create(a, "BLUE"))));

        List<String> picks = new ArrayList<>();
        int spent = 0;
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
            for (String id : sorted)
                if (ChampionFactory.getCost(id) < ChampionFactory.getCost(cheapest)) cheapest = id;
            if (ChampionFactory.getCost(cheapest) <= budget) picks.add(cheapest);
        }
        // Low-budget override: if we could only draft 1 unit, pick the best 1v1 fighter
        // (highest HP * ATK * DEF product). At budget 5 this picks KNIGHT over ARCHER.
        if (picks.size() == 1) {
            String best1v1 = null;
            double bestCombat = -1;
            for (String id : availableIds) {
                if (ChampionFactory.getCost(id) > budget) continue;
                Champion c = ChampionFactory.create(id, "BLUE");
                double combat = c.getMaxHp() * c.getAttack() * Math.max(1, c.getDefense());
                if (combat > bestCombat) { bestCombat = combat; best1v1 = id; }
            }
            if (best1v1 != null) { picks.clear(); picks.add(best1v1); }
        }
        return picks;
    }

    private double valuePerGold(Champion c) {
        double durability = c.getMaxHp() * (1.0 + c.getDefense() * 0.3);
        double offense    = c.getAttack() * (1.0 + c.getRange() * 0.5);
        double tempo      = c.getSpeed() * 1.2 + c.getMoveRange() * 0.3;
        // In simultaneous, range is king (kite without retaliation) and healing offsets
        // unavoidable damage. Healers get a flat bonus so at least one is drafted.
        double healBonus  = (c.getId().equals("CLERIC") || c.getId().equals("DRUID")) ? 8.0 : 0;
        return (durability * 0.8 + offense * 2.0 + tempo + healBonus) / c.getCost();
    }

    // ================================================================
    // PLACEMENT — melee front, ranged/healers back
    // ================================================================
    @Override
    public List<Position> placeTeam(List<Champion> team,
                                    List<Position> allowedCells,
                                    boolean isBlue) {
        int frontRow = isBlue ? maxRow(allowedCells) : minRow(allowedCells);

        List<Position> front = new ArrayList<>();
        List<Position> back = new ArrayList<>();
        for (Position p : allowedCells) {
            if (p.row() == frontRow) front.add(p);
            else back.add(p);
        }
        front.sort(Comparator.comparingInt(Position::col));
        back.sort(Comparator.comparingInt(Position::col));

        List<Champion> ordered = new ArrayList<>(team);
        ordered.sort(Comparator.comparingInt(c -> isRear(c.getId()) ? 1 : 0));

        List<Position> result = new ArrayList<>();
        int fi = 0, bi = 0;
        for (Champion c : ordered) {
            Position p;
            if (isRear(c.getId())) {
                p = bi < back.size() ? back.get(bi++) : (fi < front.size() ? front.get(fi++) : null);
            } else {
                p = fi < front.size() ? front.get(fi++) : (bi < back.size() ? back.get(bi++) : null);
            }
            if (p != null) result.add(p);
        }
        if (result.size() < team.size()) {
            for (Position p : allowedCells) {
                if (result.size() >= team.size()) break;
                if (!result.contains(p)) result.add(p);
            }
        }
        return remapToTeamOrder(team, ordered, result);
    }

    private List<Position> remapToTeamOrder(List<Champion> team,
                                            List<Champion> ordered,
                                            List<Position> orderedPlaces) {
        Map<String, Position> byId = new HashMap<>();
        for (int i = 0; i < ordered.size() && i < orderedPlaces.size(); i++)
            byId.put(ordered.get(i).getId(), orderedPlaces.get(i));
        List<Position> out = new ArrayList<>();
        for (Champion c : team) {
            Position p = byId.get(c.getId());
            if (p != null) out.add(p);
        }
        return out;
    }

    // ================================================================
    // BATTLE — simultaneous-optimised: maximum aggression
    //
    // Key principle: under simultaneous resolution, retreating wastes
    // your attack without reducing incoming damage (it's from a snapshot).
    // Every unit should deal damage every round if it can.
    // ================================================================
    @Override
    public List<BotAction> playTurn(List<Champion> allies,
                                    List<Champion> enemies,
                                    Grid grid, int round) {
        List<BotAction> actions = new ArrayList<>();
        List<Champion> aliveEnemies = aliveOnly(enemies);
        List<Champion> aliveAllies  = aliveOnly(allies);

        if (aliveEnemies.isEmpty()) {
            for (Champion a : aliveAllies) actions.add(BotAction.wait(a.getId()));
            return actions;
        }

        Set<Position> occupied = new HashSet<>();
        for (Champion c : aliveAllies) occupied.add(c.getPosition());
        for (Champion c : aliveEnemies) occupied.add(c.getPosition());

        Champion focus = focusTarget(aliveAllies, aliveEnemies);

        for (Champion ally : aliveAllies) {
            occupied.remove(ally.getPosition());
            BotAction act = decide(ally, focus, aliveAllies, aliveEnemies, grid, occupied);
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
        String id   = ally.getId();
        String role = templateOf(id);

        // ── HEALER: heal if anyone is hurt, otherwise ATTACK (never idle) ──
        if (HEALER.contains(role)) {
            Champion hurt = mostHurtAlly(allies);
            if (hurt != null && skillReady(ally)) {
                int d = ally.getPosition().manhattanDistance(hurt.getPosition());
                if (d <= ally.getRange() + 1) {
                    return new BotAction(id, BotAction.Type.CAST_SKILL, null, hurt.getId());
                }
                // Hurt ally out of range: move toward them
                Position step = BFSPathfinder.advanceToward(
                        ally.getPosition(), hurt.getPosition(), grid, occupied);
                if (step != null && !step.equals(ally.getPosition()))
                    return BotAction.move(id, step);
            }
            // No one needs healing or skill not ready: ATTACK like everyone else
            Champion inRange = bestInRange(ally, enemies);
            if (inRange != null) {
                if (skillReady(ally))
                    return new BotAction(id, BotAction.Type.CAST_SKILL, null, inRange.getId());
                return BotAction.attack(id, inRange.getId());
            }
            // No target in range: advance toward focus
            Champion goal = approachTarget(ally, focus, enemies);
            Position dest = BFSPathfinder.advanceToward(
                    ally.getPosition(), goal.getPosition(), grid, occupied);
            if (dest != null && !dest.equals(ally.getPosition()))
                return BotAction.move(id, dest);
            return BotAction.wait(id);
        }

        // ── RANGED UNITS: kite (stay at max range, never let melee close) ──
        if (RANGED.contains(role) && ally.getRange() >= 2) {
            Champion inRange = bestInRange(ally, enemies);
            if (inRange != null) {
                // Attack, but if a melee enemy is dangerously close, kite away first
                Champion closestMelee = closestMeleeEnemy(ally, enemies);
                if (closestMelee != null) {
                    int mDist = ally.getPosition().manhattanDistance(closestMelee.getPosition());
                    if (mDist <= 1) {
                        // Too close to melee — kite away
                        Position away = kiteAway(ally.getPosition(), closestMelee.getPosition(), grid, occupied);
                        if (away != null && !away.equals(ally.getPosition()))
                            return BotAction.move(id, away);
                    }
                }
                // Safe to attack
                if (skillReady(ally))
                    return new BotAction(id, BotAction.Type.CAST_SKILL, null, inRange.getId());
                return BotAction.attack(id, inRange.getId());
            }
            // Not in range: advance toward target
            Champion goal = approachTarget(ally, focus, enemies);
            Position dest = BFSPathfinder.advanceToward(
                    ally.getPosition(), goal.getPosition(), grid, occupied);
            if (dest != null && !dest.equals(ally.getPosition()))
                return BotAction.move(id, dest);
            return BotAction.wait(id);
        }

        // ── MELEE UNITS: pure aggression, close and attack ──
        Champion inRange = bestInRange(ally, enemies);
        if (inRange != null) {
            if (skillReady(ally))
                return new BotAction(id, BotAction.Type.CAST_SKILL, null, inRange.getId());
            return BotAction.attack(id, inRange.getId());
        }

        Champion goal = approachTarget(ally, focus, enemies);
        Position dest = BFSPathfinder.advanceToward(
                ally.getPosition(), goal.getPosition(), grid, occupied);
        if (dest != null && !dest.equals(ally.getPosition()))
            return BotAction.move(id, dest);

        return BotAction.wait(id);
    }

    // ================================================================
    // TARGETING
    // ================================================================

    // Focus: the enemy that dies in the fewest total damage hits from ALL our units.
    // Killing one enemy fast reduces their team's damage output next round.
    private Champion focusTarget(List<Champion> allies, List<Champion> enemies) {
        Champion best = null;
        double bestScore = Double.MAX_VALUE;
        for (Champion e : enemies) {
            int totalDmgPerRound = 0;
            for (Champion a : allies) {
                int d = a.getPosition().manhattanDistance(e.getPosition());
                if (d <= a.getRange() + 2) // within striking distance next round
                    totalDmgPerRound += Math.max(1, a.getAttack() - e.getDefense());
            }
            if (totalDmgPerRound == 0) totalDmgPerRound = 1;
            double roundsToKill = (double) e.getHp() / totalDmgPerRound;
            if (roundsToKill < bestScore) { bestScore = roundsToKill; best = e; }
        }
        return best;
    }

    private Champion approachTarget(Champion ally, Champion focus, List<Champion> enemies) {
        Champion nearest = null;
        int nd = Integer.MAX_VALUE;
        for (Champion e : enemies) {
            int d = ally.getPosition().manhattanDistance(e.getPosition());
            if (d < nd) { nd = d; nearest = e; }
        }
        if (focus == null) return nearest;
        int fd = ally.getPosition().manhattanDistance(focus.getPosition());
        // Follow focus unless a different enemy is much closer
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
            double score = kill ? -1 : (double) e.getHp() / dmg;
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    // ================================================================
    // HELPERS
    // ================================================================

    private Champion mostHurtAlly(List<Champion> allies) {
        Champion best = null;
        double worst = 0.80; // heal at 80% — more aggressive than before
        for (Champion c : allies) {
            if (!c.isAlive()) continue;
            double frac = (double) c.getHp() / c.getMaxHp();
            if (frac < worst) { worst = frac; best = c; }
        }
        return best;
    }

    private boolean skillReady(Champion c) {
        return c.getMana() >= c.getSkillManaCost() && c.getRemainingCooldown() == 0;
    }

    private boolean isRear(String id) {
        String t = templateOf(id);
        return RANGED.contains(t) || HEALER.contains(t);
    }

    private String templateOf(String id) {
        if (id == null) return "";
        String t = id;
        if (t.startsWith("BLUE_")) t = t.substring(5);
        else if (t.startsWith("RED_")) t = t.substring(4);
        int us = t.lastIndexOf('_');
        if (us > 0 && t.substring(us + 1).matches("\\d+")) t = t.substring(0, us);
        return t.isEmpty() ? id : t;
    }

    private Champion closestAllyByRole(Champion me, List<Champion> allies, Set<String> roles) {
        Champion best = null;
        int bd = Integer.MAX_VALUE;
        for (Champion a : allies) {
            if (!a.isAlive() || a.getId().equals(me.getId())) continue;
            if (!roles.contains(templateOf(a.getId()))) continue;
            int d = me.getPosition().manhattanDistance(a.getPosition());
            if (d < bd) { bd = d; best = a; }
        }
        return best;
    }

    private List<Champion> aliveOnly(List<Champion> list) {
        List<Champion> out = new ArrayList<>();
        for (Champion c : list) if (c.isAlive()) out.add(c);
        return out;
    }

    private int maxRow(List<Position> cells) {
        int m = 0; for (Position p : cells) m = Math.max(m, p.row()); return m;
    }

    private int minRow(List<Position> cells) {
        int m = Integer.MAX_VALUE; for (Position p : cells) m = Math.min(m, p.row()); return m;
    }

    private Champion closestMeleeEnemy(Champion ally, List<Champion> enemies) {
        Champion best = null;
        int bd = Integer.MAX_VALUE;
        for (Champion e : enemies) {
            if (e.getRange() >= 2) continue; // not melee
            int d = ally.getPosition().manhattanDistance(e.getPosition());
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    // Move away from a threat: pick the open neighbour that maximises distance from threat.
    private Position kiteAway(Position from, Position threat, Grid grid, Set<Position> occupied) {
        Position best = null;
        int bestDist = from.manhattanDistance(threat);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                if (Math.abs(dr) + Math.abs(dc) != 1) continue; // only orthogonal
                Position p = new Position(from.row() + dr, from.col() + dc);
                if (!grid.isInBounds(p) || occupied.contains(p)) continue;
                int d = p.manhattanDistance(threat);
                if (d > bestDist) { bestDist = d; best = p; }
            }
        }
        return best;
    }
}
