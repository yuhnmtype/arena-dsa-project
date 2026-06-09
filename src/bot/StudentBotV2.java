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
 * StudentBotV2 — optimised 3-phase bot (Draft → Place → Battle).
 *
 * DSA components:
 *   Phase 1 – Greedy draft:    O(n log n) sort + O(n·k) fill + O(n) role-constraint pass
 *   Phase 2 – Placement:       O(n log n) sort by role-tier, O(n) cell assignment
 *   Phase 3 – Battle:          BFS O(V+E)=O(64) per move decision; O(n) focus/target scans
 *   Occupied set:              HashSet → O(1) membership tests throughout
 */
public class StudentBotV2 implements IBotAI {

    private static final int MAX_TEAM = 8;

    private static final Set<String> RANGED = new HashSet<>();
    private static final Set<String> HEALER = new HashSet<>();
    private static final Set<String> TANK   = new HashSet<>();   // high-def melee → centre
    private static final Set<String> STRIKER = new HashSet<>();  // high-spd melee → flanks
    static {
        RANGED .add("ARCHER");  RANGED .add("MAGE");
        RANGED .add("WARLOCK"); RANGED .add("FROST_WITCH");
        HEALER .add("CLERIC");  HEALER .add("DRUID");
        TANK   .add("GUARDIAN"); TANK  .add("PALADIN"); TANK.add("KNIGHT");
        STRIKER.add("ASSASSIN"); STRIKER.add("LANCER"); STRIKER.add("BERSERKER");
    }

    // ================================================================
    // PHASE 1: DRAFT — greedy by value-per-gold + role constraints
    // ================================================================
    @Override
    public List<String> draftTeam(int budget, List<String> availableIds) {

        // Sort descending by value-per-gold (includes skill-efficiency bonus).
        List<String> sorted = new ArrayList<>(availableIds);
        sorted.sort((a, b) -> Double.compare(
                valuePerGold(ChampionFactory.create(b, "BLUE")),
                valuePerGold(ChampionFactory.create(a, "BLUE"))));

        List<String> picks = new ArrayList<>();
        int spent = 0;

        // ── Greedy fill ────────────────────────────────────────────
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

        // ── Upgrade pass ───────────────────────────────────────────
        // Team is full; repeatedly swap the cheapest unit for the best
        // (value/gold) unit that costs more, while leftover budget allows.
        boolean upgraded = true;
        while (upgraded && !picks.isEmpty()) {
            upgraded = false;
            int minCost = Integer.MAX_VALUE, minIdx = -1;
            for (int i = 0; i < picks.size(); i++) {
                int c = ChampionFactory.getCost(picks.get(i));
                if (c < minCost) { minCost = c; minIdx = i; }
            }
            int canSpend = (budget - spent) + minCost;
            String best = null; double bestVal = -1;
            for (String id : sorted) {
                int c = ChampionFactory.getCost(id);
                if (c > minCost && c <= canSpend) {
                    double v = valuePerGold(ChampionFactory.create(id, "BLUE"));
                    if (best == null || v > bestVal) { best = id; bestVal = v; }
                }
            }
            if (best != null) {
                spent = spent - minCost + ChampionFactory.getCost(best);
                picks.set(minIdx, best);
                upgraded = true;
            }
        }

        // ── Role-constraint pass ───────────────────────────────────
        // Guarantee ≥1 healer and ≥1 ranged unit if budget allows a swap.
        picks = enforceRoleConstraint(picks, sorted, budget, spent, HEALER);
        picks = enforceRoleConstraint(picks, sorted, budget, spent, RANGED);

        // ── Fallback ───────────────────────────────────────────────
        if (picks.isEmpty() && !sorted.isEmpty()) {
            String cheapest = sorted.stream()
                .min(Comparator.comparingInt(ChampionFactory::getCost))
                .orElse(sorted.get(0));
            if (ChampionFactory.getCost(cheapest) <= budget) picks.add(cheapest);
        }
        return picks;
    }

    /**
     * If the current picks contain no champion from 'requiredRole', replace the
     * lowest-value non-role unit with the best available role unit that fits the
     * freed-up budget.  Returns picks unchanged if the constraint is already met
     * or no affordable swap exists.
     */
    private List<String> enforceRoleConstraint(List<String> picks, List<String> sorted,
                                                int budget, int spent,
                                                Set<String> requiredRole) {
        boolean alreadyHas = picks.stream().anyMatch(id -> requiredRole.contains(id));
        if (alreadyHas) return picks;

        // Best available champion of the required role
        String roleChamp = null; double roleVal = -1;
        for (String id : sorted) {
            if (!requiredRole.contains(id)) continue;
            double v = valuePerGold(ChampionFactory.create(id, "BLUE"));
            if (roleChamp == null || v > roleVal) { roleChamp = id; roleVal = v; }
        }
        if (roleChamp == null) return picks;

        int roleCost = ChampionFactory.getCost(roleChamp);
        // Find the lowest-value non-role unit we can afford to replace
        int  replIdx = -1; double replVal = Double.MAX_VALUE;
        for (int i = 0; i < picks.size(); i++) {
            String id = picks.get(i);
            if (requiredRole.contains(id)) continue;
            int costDiff = roleCost - ChampionFactory.getCost(id);
            if (budget - spent >= costDiff) {
                double v = valuePerGold(ChampionFactory.create(id, "BLUE"));
                if (v < replVal) { replVal = v; replIdx = i; }
            }
        }
        if (replIdx >= 0) picks.set(replIdx, roleChamp);
        return picks;
    }

    /**
     * Value-per-gold formula (higher = better value per gold spent).
     *
     *   durability  = effective HP after defence mitigation
     *   offense     = raw damage weighted by range (ranged fires without closing)
     *   tempo       = speed + mobility (acts early, reaches enemies fast)
     *   skillBonus  = estimated extra value from skill casts over a full match
     *                 Healers get a 1.5× multiplier — restoring HP has outsized value.
     */
    private double valuePerGold(Champion c) {
        double durability  = c.getMaxHp() * (1.0 + c.getDefense() * 0.25);
        double offense     = c.getAttack() * (1.0 + c.getRange() * 0.3);
        double tempo       = c.getSpeed() * 1.5 + c.getMoveRange() * 0.5;

        // Skill bonus: casts per 40-round match, each cast does (atk+2) effective damage/heal.
        double castsPerMatch = 40.0 / (c.getSkillCooldown() + c.getSkillManaCost() / 2.0);
        boolean isHealer     = HEALER.contains(c.getId());
        double skillMult     = isHealer ? 1.5 : 0.8;  // healing > offensive burst
        double skillBonus    = (c.getAttack() + 2) * castsPerMatch * skillMult;

        return (durability + offense * 1.6 + tempo + skillBonus) / c.getCost();
    }

    // ================================================================
    // PHASE 2: PLACEMENT — 3-tier: striker flanks, tank centre, rear back
    // ================================================================
    @Override
    public List<Position> placeTeam(List<Champion> team,
                                     List<Position> allowedCells,
                                     boolean isBlue) {
        int frontRow = maxRow(allowedCells); // BLUE row2 / RED row7 (visual home row)

        // Partition cells
        List<Position> frontAll  = new ArrayList<>();
        List<Position> backAll   = new ArrayList<>();
        for (Position p : allowedCells) {
            if (p.row() == frontRow) frontAll.add(p);
            else backAll.add(p);
        }
        frontAll.sort(Comparator.comparingInt(Position::col));
        backAll .sort(Comparator.comparingInt(Position::col));

        // Sub-divide front: centre cols for tanks, edge cols for strikers
        int mid = Grid.COLS / 2;
        List<Position> centreFront = new ArrayList<>();
        List<Position> edgeFront   = new ArrayList<>();
        for (Position p : frontAll) {
            if (p.col() >= mid - 2 && p.col() < mid + 2) centreFront.add(p);
            else                                           edgeFront  .add(p);
        }

        // Sub-divide back: centre for healers, rest for ranged
        List<Position> centreBack = new ArrayList<>();
        List<Position> edgeBack   = new ArrayList<>();
        for (Position p : backAll) {
            if (p.col() >= mid - 2 && p.col() < mid + 2) centreBack.add(p);
            else                                           edgeBack  .add(p);
        }

        // Classify each champion into one of 4 tiers (priority order for assignment)
        List<Champion> tanks    = new ArrayList<>();
        List<Champion> strikers = new ArrayList<>();
        List<Champion> ranged   = new ArrayList<>();
        List<Champion> healers  = new ArrayList<>();

        for (Champion c : team) {
            String t = templateOf(c.getId());
            if      (HEALER .contains(t)) healers .add(c);
            else if (RANGED .contains(t)) ranged  .add(c);
            else if (TANK   .contains(t)) tanks   .add(c);
            else if (STRIKER.contains(t)) strikers.add(c);
            else                          tanks   .add(c); // unknown melee → treat as tank
        }

        // Assign positions: tanks → centre front, strikers → edge front,
        //                   healers → centre back,  ranged → edge back.
        Map<String, Position> byId = new HashMap<>();
        assign(tanks,    merge(centreFront, edgeFront, backAll),  byId);
        assign(strikers, merge(edgeFront, centreFront, backAll),  byId);
        assign(healers,  merge(centreBack, edgeBack,   frontAll), byId);
        assign(ranged,   merge(edgeBack,   centreBack, frontAll), byId);

        // Remap to engine-expected order (one position per team[i])
        List<Position> out = new ArrayList<>();
        for (Champion c : team) {
            Position p = byId.get(c.getId());
            if (p != null) out.add(p);
        }
        // Safety fill for any unassigned champion
        Set<Position> used = new HashSet<>(out);
        for (Champion c : team) {
            if (byId.containsKey(c.getId())) continue;
            for (Position p : allowedCells) {
                if (!used.contains(p)) { out.add(p); used.add(p); break; }
            }
        }
        return out;
    }

    /** Assign champions to the first available position from the priority list. */
    private void assign(List<Champion> group, List<Position> priority,
                        Map<String, Position> byId) {
        Set<Position> taken = new HashSet<>(byId.values());
        int pi = 0;
        for (Champion c : group) {
            if (byId.containsKey(c.getId())) continue;
            while (pi < priority.size() && taken.contains(priority.get(pi))) pi++;
            if (pi < priority.size()) {
                byId.put(c.getId(), priority.get(pi));
                taken.add(priority.get(pi));
                pi++;
            }
        }
    }

    /** Concatenate lists without duplicates into a single priority list. */
    @SafeVarargs
    private final List<Position> merge(List<Position>... lists) {
        List<Position> result = new ArrayList<>();
        Set<Position>  seen   = new HashSet<>();
        for (List<Position> list : lists)
            for (Position p : list)
                if (seen.add(p)) result.add(p);
        return result;
    }

    // ================================================================
    // PHASE 3: BATTLE — BFS + focus-fire + retreat + smart skill timing
    // ================================================================
    @Override
    public List<BotAction> playTurn(List<Champion> allies, List<Champion> enemies,
                                     Grid grid, int round) {
        List<BotAction> actions = new ArrayList<>();
        List<Champion> aliveEnemies = aliveOnly(enemies);
        List<Champion> aliveAllies  = aliveOnly(allies);
        if (aliveEnemies.isEmpty()) {
            for (Champion a : aliveAllies) actions.add(BotAction.wait(a.getId()));
            return actions;
        }

        // Build occupied set: O(n) build, O(1) per BFS lookup.
        Set<Position> occupied = new HashSet<>();
        for (Champion c : aliveAllies)  occupied.add(c.getPosition());
        for (Champion c : aliveEnemies) occupied.add(c.getPosition());

        // Shared focus: enemy that takes fewest hits accounting for their defence.
        Champion focus = focusTarget(aliveAllies, aliveEnemies);

        for (Champion ally : aliveAllies) {
            occupied.remove(ally.getPosition());
            BotAction act = decide(ally, focus, aliveAllies, aliveEnemies, grid, occupied);
            occupied.add(act.type == BotAction.Type.MOVE && act.targetPosition != null
                    ? act.targetPosition : ally.getPosition());
            actions.add(act);
        }
        return actions;
    }

    private BotAction decide(Champion ally, Champion focus,
                              List<Champion> allies, List<Champion> enemies,
                              Grid grid, Set<Position> occupied) {
        String id   = ally.getId();
        String role = templateOf(id);

        // ── 1. Healer logic ────────────────────────────────────────
        if (HEALER.contains(role)) {
            Champion hurt = mostHurtAlly(allies);
            if (hurt != null) {
                int d = ally.getPosition().manhattanDistance(hurt.getPosition());
                if (d <= ally.getRange() + 1 && skillReady(ally)) {
                    // In range and skill ready → heal
                    return new BotAction(id, BotAction.Type.CAST_SKILL, null, hurt.getId());
                } else if (d > ally.getRange() + 1) {
                    // Out of range → chase the hurt ally
                    Position step = BFSPathfinder.advanceToward(
                            ally.getPosition(), hurt.getPosition(), grid, occupied);
                    if (step != null && !step.equals(ally.getPosition()))
                        return BotAction.move(id, step);
                }
            }
            // No one needs healing → hold position or advance cautiously
            Champion goal = approachTarget(ally, focus, enemies);
            int dist = ally.getPosition().manhattanDistance(goal.getPosition());
            if (dist > ally.getRange() + 2) {
                Position step = BFSPathfinder.advanceToward(
                        ally.getPosition(), goal.getPosition(), grid, occupied);
                if (step != null && !step.equals(ally.getPosition()))
                    return BotAction.move(id, step);
            }
            return BotAction.wait(id);
        }

        // ── 2. Retreat logic ──────────────────────────────────────
        // Critically wounded non-healer → run to nearest healer if out of their range.
        double hpFrac = (double) ally.getHp() / ally.getMaxHp();
        if (hpFrac < 0.25) {
            Champion nearHealer = closestAllyByRole(ally, allies, HEALER);
            if (nearHealer != null) {
                int d = ally.getPosition().manhattanDistance(nearHealer.getPosition());
                if (d > nearHealer.getRange() + 1) {
                    Position step = BFSPathfinder.advanceToward(
                            ally.getPosition(), nearHealer.getPosition(), grid, occupied);
                    if (step != null && !step.equals(ally.getPosition()))
                        return BotAction.move(id, step);
                }
            }
        }

        // ── 3. Attack / skill in range ────────────────────────────
        Champion inRange = bestInRange(ally, enemies);
        if (inRange != null) {
            if (skillReady(ally)) {
                int normalDmg = Math.max(1, ally.getAttack() - inRange.getDefense());
                int skillDmg  = Math.max(1, ally.getAttack() + 2 - inRange.getDefense());
                // Use skill if: it secures the kill, or target survives 3+ normal hits
                boolean skillFinishes = skillDmg >= inRange.getHp() && normalDmg < inRange.getHp();
                boolean skillWorthIt  = inRange.getHp() >= normalDmg * 3;
                if (skillFinishes || skillWorthIt)
                    return new BotAction(id, BotAction.Type.CAST_SKILL, null, inRange.getId());
            }
            return BotAction.attack(id, inRange.getId());
        }

        // ── 4. Move toward best target ───────────────────────────
        Champion goal = approachTarget(ally, focus, enemies);
        Position dest = BFSPathfinder.advanceToward(
                ally.getPosition(), goal.getPosition(), grid, occupied);
        if (dest != null && !dest.equals(ally.getPosition()))
            return BotAction.move(id, dest);

        return BotAction.wait(id);
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Focus target: enemy that takes the fewest hits to kill.
     * Uses effective damage = max(1, attacker.atk - target.def) to account for armour.
     * FIX: old version used raw attack ignoring defence, causing tanks to be incorrectly
     * prioritised when they actually take much less damage per hit.
     */
    private Champion focusTarget(List<Champion> allies, List<Champion> enemies) {
        Champion best = null;
        double bestRounds = Double.MAX_VALUE;
        for (Champion e : enemies) {
            int bestEffDmg = 1;
            for (Champion a : allies) {
                int eff = Math.max(1, a.getAttack() - e.getDefense()); // FIX: account for def
                bestEffDmg = Math.max(bestEffDmg, eff);
            }
            double rounds = (double) e.getHp() / bestEffDmg;
            if (rounds < bestRounds) { bestRounds = rounds; best = e; }
        }
        return best;
    }

    private Champion approachTarget(Champion ally, Champion focus, List<Champion> enemies) {
        Champion nearest = null; int nd = Integer.MAX_VALUE;
        for (Champion e : enemies) {
            int d = ally.getPosition().manhattanDistance(e.getPosition());
            if (d < nd) { nd = d; nearest = e; }
        }
        if (focus == null) return nearest;
        int fd = ally.getPosition().manhattanDistance(focus.getPosition());
        return (nearest != null && fd - nd >= 3) ? nearest : focus;
    }

    private Champion bestInRange(Champion attacker, List<Champion> enemies) {
        Champion best = null; double bestScore = Double.MAX_VALUE;
        for (Champion e : enemies) {
            int dist = attacker.getPosition().manhattanDistance(e.getPosition());
            if (dist > attacker.getRange()) continue;
            int dmg  = Math.max(1, attacker.getAttack() - e.getDefense());
            boolean kill = dmg >= e.getHp();
            double score = kill ? -1 : (double) e.getHp() / dmg;
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    private Champion mostHurtAlly(List<Champion> allies) {
        Champion best = null; double worst = 0.80; // heal anyone below 80% HP
        for (Champion c : allies) {
            if (!c.isAlive()) continue;
            double frac = (double) c.getHp() / c.getMaxHp();
            if (frac < worst) { worst = frac; best = c; }
        }
        return best;
    }

    /** Nearest alive ally whose template is in the given role set. */
    private Champion closestAllyByRole(Champion self, List<Champion> allies,
                                        Set<String> roleSet) {
        Champion best = null; int bd = Integer.MAX_VALUE;
        for (Champion c : allies) {
            if (!c.isAlive()) continue;
            if (c.getId().equals(self.getId())) continue;
            if (!roleSet.contains(templateOf(c.getId()))) continue;
            int d = self.getPosition().manhattanDistance(c.getPosition());
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    private boolean skillReady(Champion c) {
        return c.getMana() >= c.getSkillManaCost() && c.getRemainingCooldown() == 0;
    }

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
}