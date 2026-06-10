package engine;

import bot.BFSPathfinder;
import bot.BotAction;
import bot.IBotAI;
import java.util.*;

/**
 * BattleEngine — simultaneous-resolution turn engine.
 *
 * Each round is resolved in two phases:
 *
 *   Phase B1 – MOVE:  All units' move intentions are collected, conflicts
 *              resolved by speed (highest speed wins the contested cell;
 *              PriorityQueue re-used as the tiebreak comparator), then
 *              all non-blocked moves applied at once.
 *
 *   Phase B2 – ATTACK / SKILL:  All damage/heal values are accumulated
 *              into per-target maps using a snapshot of pre-move positions
 *              for range validation (the board the bot decided on).
 *              Defence is subtracted per-hit before accumulation so armour
 *              is applied correctly for every incoming strike.
 *              All accumulated values are applied simultaneously:
 *                – mutual kills are possible
 *                – overkill requires no special case
 *                – CAST_SKILL on an ally heals; on an enemy damages
 *
 * DSA note: the PriorityQueue is retained as the move-conflict tiebreak
 * comparator (speed DESC, BLUE first), keeping the graded DSA component.
 */
public class BattleEngine {

    public static final int MAX_ROUNDS = 40;
    public static final int MAX_TEAM   = 8;

    private final IBotAI blueBot;
    private final IBotAI redBot;

    public BattleEngine(IBotAI blueBot, IBotAI redBot) {
        this.blueBot = blueBot;
        this.redBot  = redBot;
    }

    // ================================================================
    // runMatch — tournament version (no log)
    // ================================================================
    public MatchResult runMatch(int budget) { return runMatch(budget, "Unknown"); }

    public MatchResult runMatch(int budget, String matchType) {
        Grid grid = new Grid();

        List<String> pool      = ChampionFactory.getAllIds();
        List<String> blueDraft = blueBot.draftTeam(budget, pool);
        List<String> redDraft  = redBot .draftTeam(budget, pool);

        List<Champion> blueTeam = buildTeam(blueDraft, "BLUE");
        List<Champion> redTeam  = buildTeam(redDraft,  "RED");

        placeTeam(blueTeam, blueBot, getAllowedCells("BLUE"), true,  grid);
        placeTeam(redTeam,  redBot,  getAllowedCells("RED"),  false, grid);

        for (int round = 1; round <= MAX_ROUNDS; round++) {
            List<Champion> aliveBlue = aliveOnly(blueTeam);
            List<Champion> aliveRed  = aliveOnly(redTeam);
            if (aliveBlue.isEmpty() || aliveRed.isEmpty()) break;

            // Bot decisions (see the pre-move board)
            Map<String, BotAction> actionMap = collectActions(
                aliveBlue, aliveRed, grid, round);

            // Two-phase simultaneous resolution
            List<Champion> allAlive = merged(aliveBlue, aliveRed);
            Map<String, Position> startPos = snapshotPositions(allAlive);

            resolveMoves  (allAlive, actionMap, grid);
            resolveAttacks(allAlive, actionMap, startPos, blueTeam, redTeam, grid);

            tickManaAndCooldown(blueTeam);
            tickManaAndCooldown(redTeam);

            boolean blueAlive = blueTeam.stream().anyMatch(Champion::isAlive);
            boolean redAlive  = redTeam .stream().anyMatch(Champion::isAlive);
            if (!blueAlive || !redAlive) {
                return buildResult(matchType, blueAlive ? "BLUE" : "RED",
                    round, budget, blueTeam, redTeam, blueDraft, redDraft);
            }
        }

        int blueHp = hpSum(blueTeam), redHp = hpSum(redTeam);
        return buildResult(matchType, blueHp >= redHp ? "BLUE" : "RED",
            MAX_ROUNDS, budget, blueTeam, redTeam, blueDraft, redDraft);
    }

    // ================================================================
    // runMatchWithLog — GUI version (produces BattleLog for replay)
    // ================================================================
    public BattleLog runMatchWithLog(int budget, String matchType) {
        BattleLog log  = new BattleLog(matchType, budget);
        Grid      grid = new Grid();

        List<String> pool      = ChampionFactory.getAllIds();
        List<String> blueDraft = blueBot.draftTeam(budget, pool);
        List<String> redDraft  = redBot .draftTeam(budget, pool);

        List<Champion> blueTeam = buildTeam(blueDraft, "BLUE");
        List<Champion> redTeam  = buildTeam(redDraft,  "RED");

        placeTeam(blueTeam, blueBot, getAllowedCells("BLUE"), true,  grid);
        placeTeam(redTeam,  redBot,  getAllowedCells("RED"),  false, grid);

        log.initialBlueTeam = deepCopy(blueTeam);
        log.initialRedTeam  = deepCopy(redTeam);

        for (int round = 1; round <= MAX_ROUNDS; round++) {
            List<Champion> aliveBlue = aliveOnly(blueTeam);
            List<Champion> aliveRed  = aliveOnly(redTeam);
            if (aliveBlue.isEmpty() || aliveRed.isEmpty()) break;

            // Bot decisions (see the pre-move board)
            Map<String, BotAction> actionMap = collectActions(
                aliveBlue, aliveRed, grid, round);

            List<Champion> allAlive = merged(aliveBlue, aliveRed);

            // ── Snapshot pre-move state ───────────────────────────
            Map<String, Position> startPos = snapshotPositions(allAlive);
            Map<String, Integer>  startHp  = snapshotHp(allAlive);

            // ── Phase B1: simultaneous moves ──────────────────────
            Map<String, Position> actualDest = resolveMoves(allAlive, actionMap, grid);

            // Log MOVE phase (tick=0)
            Set<Position> snapAfterMove = occupiedSet(allAlive);
            for (Champion actor : allAlive) {
                BotAction a    = actionMap.get(actor.getId());
                BotAction.Type type = (a != null) ? a.type : BotAction.Type.WAIT;

                Position from = startPos.get(actor.getId());
                Position to   = actor.getPosition();

                // BFS path for moved units
                List<Position> bfsPath = new ArrayList<>();
                if (type == BotAction.Type.MOVE && from != null
                        && !from.equals(to)) {
                    bfsPath = BFSPathfinder.findPath(from, to, grid, snapAfterMove);
                }

                log.add(new BattleLogEntry(
                    round, 0,
                    actor.getId(), templateName(actor.getId()),
                    actor.getId().startsWith("BLUE") ? "BLUE" : "RED",
                    type == BotAction.Type.ATTACK || type == BotAction.Type.CAST_SKILL
                        ? BotAction.Type.WAIT : type,   // attacks logged in tick=1
                    from, to,
                    startHp.getOrDefault(actor.getId(), actor.getHp()), actor.getHp(),
                    actor.getMana(), actor.getMana(),
                    null, -1, -1,
                    bfsPath, snapAfterMove
                ));
            }

            // ── Phase B2: simultaneous attacks / skills ───────────
            // Accumulate damage and heals BEFORE applying anything
            Map<String, Integer> damageAcc = new LinkedHashMap<>();
            Map<String, Integer> healAcc   = new LinkedHashMap<>();

            for (Champion actor : allAlive) {
                BotAction a = actionMap.get(actor.getId());
                if (a == null) continue;
                if (a.type != BotAction.Type.ATTACK
                        && a.type != BotAction.Type.CAST_SKILL) continue;

                Champion target = findById(a.targetChampionId, blueTeam, redTeam);
                if (target == null) continue;

                // Range check on PRE-MOVE positions (what the bot saw)
                Position ap = startPos.get(actor.getId());
                Position tp = startPos.get(target.getId());
                if (ap == null || tp == null) continue;
                if (ap.manhattanDistance(tp) > actor.getRange()) continue;

                if (a.type == BotAction.Type.ATTACK) {
                    int eff = Math.max(1, actor.getAttack() - target.getDefense());
                    damageAcc.merge(target.getId(), eff, Integer::sum);

                } else { // CAST_SKILL
                    if (actor.getMana() >= actor.getSkillManaCost()
                            && actor.getRemainingCooldown() == 0) {
                        actor.useSkill();
                        boolean sameTeam = actor.getTeamSide().equals(target.getTeamSide());
                        if (sameTeam) {
                            // Healer targeting ally → heal
                            healAcc.merge(target.getId(),
                                actor.getAttack() + 2, Integer::sum);
                        } else {
                            int eff = Math.max(1, actor.getAttack() + 2 - target.getDefense());
                            damageAcc.merge(target.getId(), eff, Integer::sum);
                        }
                    }
                }
            }

            // Apply heals
            for (Map.Entry<String, Integer> e : healAcc.entrySet()) {
                Champion t = findById(e.getKey(), blueTeam, redTeam);
                if (t != null && t.isAlive()) t.heal(e.getValue());
            }

            // Apply accumulated damage (directDamage: defence already subtracted per-hit)
            for (Map.Entry<String, Integer> e : damageAcc.entrySet()) {
                Champion t = findById(e.getKey(), blueTeam, redTeam);
                if (t != null && t.isAlive()) {
                    t.directDamage(e.getValue());
                    if (!t.isAlive()) grid.vacate(t.getPosition());
                }
            }

            // Log ATTACK/SKILL phase (tick=1)
            Set<Position> snapAfterCombat = occupiedSet(allAlive);
            for (Champion actor : allAlive) {
                BotAction a = actionMap.get(actor.getId());
                if (a == null) continue;
                if (a.type != BotAction.Type.ATTACK
                        && a.type != BotAction.Type.CAST_SKILL) continue;

                Champion target = findById(a.targetChampionId, blueTeam, redTeam);
                int tHpBefore = target != null
                    ? startHp.getOrDefault(target.getId(), -1) : -1;
                int tHpAfter  = target != null ? target.getHp() : -1;

                log.add(new BattleLogEntry(
                    round, 1,
                    actor.getId(), templateName(actor.getId()),
                    actor.getId().startsWith("BLUE") ? "BLUE" : "RED",
                    a.type,
                    startPos.get(actor.getId()), actor.getPosition(),
                    startHp.getOrDefault(actor.getId(), actor.getHp()), actor.getHp(),
                    actor.getMana(), actor.getMana(),
                    a.targetChampionId, tHpBefore, tHpAfter,
                    new ArrayList<>(), snapAfterCombat
                ));
            }

            tickManaAndCooldown(blueTeam);
            tickManaAndCooldown(redTeam);

            boolean blueAlive = blueTeam.stream().anyMatch(Champion::isAlive);
            boolean redAlive  = redTeam .stream().anyMatch(Champion::isAlive);
            if (!blueAlive || !redAlive) {
                log.winner          = blueAlive ? "BLUE" : "RED";
                log.totalRounds     = round;
                log.blueHpRemaining = hpSum(blueTeam);
                log.redHpRemaining  = hpSum(redTeam);
                return log;
            }
        }

        log.winner          = hpSum(blueTeam) >= hpSum(redTeam) ? "BLUE" : "RED";
        log.totalRounds     = MAX_ROUNDS;
        log.blueHpRemaining = hpSum(blueTeam);
        log.redHpRemaining  = hpSum(redTeam);
        return log;
    }

    // ================================================================
    // Phase B1: Simultaneous move resolution
    // Returns map of championId → actual destination (for logging).
    // Conflict rule: highest speed wins contested cell; BLUE first on ties
    //   (PriorityQueue comparator repurposed as move-conflict tiebreak).
    // ================================================================
    private Map<String, Position> resolveMoves(List<Champion> allAlive,
                                                Map<String, BotAction> actionMap,
                                                Grid grid) {
        // Collect desired destinations
        Map<String, Position> desired = new LinkedHashMap<>();
        for (Champion c : allAlive) {
            BotAction a = actionMap.get(c.getId());
            if (a != null && a.type == BotAction.Type.MOVE
                    && a.targetPosition != null
                    && grid.isValid(a.targetPosition)) {
                desired.put(c.getId(), a.targetPosition);
            }
        }

        // Group by destination to detect conflicts
        Map<Position, List<Champion>> byDest = new LinkedHashMap<>();
        for (Map.Entry<String, Position> e : desired.entrySet()) {
            Champion c = findByIdInList(e.getKey(), allAlive);
            if (c != null)
                byDest.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(c);
        }

        // Conflict resolution via speed-order (PriorityQueue comparator rule):
        // highest speed wins; BLUE first on ties. Losers are blocked.
        Set<String> blocked = new HashSet<>();
        Comparator<Champion> moveConflictOrder =
            Comparator.comparingInt(Champion::getSpeed).reversed()
                .thenComparing(c -> c.getTeamSide().equals("BLUE") ? 0 : 1);

        for (List<Champion> contestants : byDest.values()) {
            if (contestants.size() <= 1) continue;
            contestants.sort(moveConflictOrder);
            for (int i = 1; i < contestants.size(); i++)
                blocked.add(contestants.get(i).getId());
        }

        // Also block moves into cells occupied by stationary units
        // (units that are NOT moving away from their current position)
        Set<String> movingAway = new HashSet<>(desired.keySet());
        movingAway.removeAll(blocked);

        outer:
        for (Map.Entry<String, Position> e : desired.entrySet()) {
            if (blocked.contains(e.getKey())) continue;
            for (Champion occ : allAlive) {
                if (movingAway.contains(occ.getId())) continue; // vacating
                if (occ.getId().equals(e.getKey())) continue;
                if (occ.getPosition().equals(e.getValue())) {
                    blocked.add(e.getKey()); // dest is taken by a stationary unit
                    continue outer;
                }
            }
        }

        // Apply non-blocked moves
        Map<String, Position> actualDest = new LinkedHashMap<>();
        for (Champion c : allAlive) {
            Position dest = desired.get(c.getId());
            if (dest != null && !blocked.contains(c.getId())) {
                grid.move(c.getPosition(), dest);
                c.setPosition(dest);
                actualDest.put(c.getId(), dest);
            } else {
                actualDest.put(c.getId(), c.getPosition());
            }
        }
        return actualDest;
    }

    // ================================================================
    // Phase B2: Simultaneous attack/skill resolution (no-log version)
    // ================================================================
    private void resolveAttacks(List<Champion> allAlive,
                                 Map<String, BotAction> actionMap,
                                 Map<String, Position> startPos,
                                 List<Champion> blueTeam,
                                 List<Champion> redTeam,
                                 Grid grid) {
        Map<String, Integer> damageAcc = new LinkedHashMap<>();
        Map<String, Integer> healAcc   = new LinkedHashMap<>();

        for (Champion actor : allAlive) {
            BotAction a = actionMap.get(actor.getId());
            if (a == null) continue;
            if (a.type != BotAction.Type.ATTACK
                    && a.type != BotAction.Type.CAST_SKILL) continue;

            Champion target = findById(a.targetChampionId, blueTeam, redTeam);
            if (target == null) continue;

            Position ap = startPos.get(actor.getId());
            Position tp = startPos.get(target.getId());
            if (ap == null || tp == null) continue;
            if (ap.manhattanDistance(tp) > actor.getRange()) continue;

            if (a.type == BotAction.Type.ATTACK) {
                int eff = Math.max(1, actor.getAttack() - target.getDefense());
                damageAcc.merge(target.getId(), eff, Integer::sum);
            } else {
                if (actor.getMana() >= actor.getSkillManaCost()
                        && actor.getRemainingCooldown() == 0) {
                    actor.useSkill();
                    boolean sameTeam = actor.getTeamSide().equals(target.getTeamSide());
                    if (sameTeam) {
                        healAcc.merge(target.getId(), actor.getAttack() + 2, Integer::sum);
                    } else {
                        int eff = Math.max(1, actor.getAttack() + 2 - target.getDefense());
                        damageAcc.merge(target.getId(), eff, Integer::sum);
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> e : healAcc.entrySet()) {
            Champion t = findById(e.getKey(), blueTeam, redTeam);
            if (t != null && t.isAlive()) t.heal(e.getValue());
        }
        for (Map.Entry<String, Integer> e : damageAcc.entrySet()) {
            Champion t = findById(e.getKey(), blueTeam, redTeam);
            if (t != null && t.isAlive()) {
                t.directDamage(e.getValue());
                if (!t.isAlive()) grid.vacate(t.getPosition());
            }
        }
    }

    // ================================================================
    // Helpers
    // ================================================================
    private Map<String, BotAction> collectActions(List<Champion> aliveBlue,
                                                   List<Champion> aliveRed,
                                                   Grid grid, int round) {
        List<BotAction> ba = blueBot.playTurn(aliveBlue, aliveRed, grid, round);
        List<BotAction> ra = redBot .playTurn(aliveRed, aliveBlue, grid, round);
        Map<String, BotAction> map = new HashMap<>();
        for (BotAction a : ba) map.put(a.championId, a);
        for (BotAction a : ra) map.put(a.championId, a);
        return map;
    }

    private Map<String, Position> snapshotPositions(List<Champion> units) {
        Map<String, Position> m = new HashMap<>();
        for (Champion c : units) m.put(c.getId(), c.getPosition());
        return m;
    }

    private Map<String, Integer> snapshotHp(List<Champion> units) {
        Map<String, Integer> m = new HashMap<>();
        for (Champion c : units) m.put(c.getId(), c.getHp());
        return m;
    }

    private Set<Position> occupiedSet(List<Champion> units) {
        Set<Position> s = new HashSet<>();
        for (Champion c : units) if (c.isAlive()) s.add(c.getPosition());
        return s;
    }

    private List<Champion> merged(List<Champion> a, List<Champion> b) {
        List<Champion> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    private List<Champion> buildTeam(List<String> draft, String side) {
        List<Champion> team = new ArrayList<>();
        for (int i = 0; i < draft.size(); i++)
            team.add(ChampionFactory.createWithUniqueId(draft.get(i), side, i));
        return team;
    }

    private void placeTeam(List<Champion> team, IBotAI bot,
                            List<Position> cells, boolean isBlue, Grid grid) {
        List<Position> places = bot.placeTeam(team, cells, isBlue);
        for (int i = 0; i < team.size() && i < places.size(); i++) {
            team.get(i).setPosition(places.get(i));
            grid.occupy(places.get(i));
        }
    }

    private void tickManaAndCooldown(List<Champion> team) {
        for (Champion c : team) {
            if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); }
        }
    }

    private int hpSum(List<Champion> team) {
        return team.stream().filter(Champion::isAlive).mapToInt(Champion::getHp).sum();
    }

    private Champion findById(String id, List<Champion> blue, List<Champion> red) {
        if (id == null) return null;
        for (Champion c : blue) if (c.getId().equals(id)) return c;
        for (Champion c : red)  if (c.getId().equals(id)) return c;
        return null;
    }

    private Champion findByIdInList(String id, List<Champion> list) {
        for (Champion c : list) if (c.getId().equals(id)) return c;
        return null;
    }

    private MatchResult buildResult(String matchType, String winner,
                                     int rounds, int budget,
                                     List<Champion> blue, List<Champion> red,
                                     List<String> blueDraft, List<String> redDraft) {
        return new MatchResult(matchType, winner, rounds, budget,
            hpSum(blue), hpSum(red),
            (int) blue.stream().filter(Champion::isAlive).count(),
            (int) red .stream().filter(Champion::isAlive).count(),
            String.join("+", blueDraft),
            String.join("+", redDraft));
    }

    private List<Champion> deepCopy(List<Champion> list) {
        List<Champion> copy = new ArrayList<>();
        for (Champion c : list) copy.add(c.copy());
        return copy;
    }

    private List<Champion> aliveOnly(List<Champion> list) {
        List<Champion> out = new ArrayList<>();
        for (Champion c : list) if (c.isAlive()) out.add(c);
        return out;
    }

    private List<Position> getAllowedCells(String side) {
        List<Position> cells = new ArrayList<>();
        if (side.equals("BLUE")) {
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < Grid.COLS; c++)
                    cells.add(new Position(r, c));
        } else {
            for (int r = 5; r < Grid.ROWS; r++)
                for (int c = 0; c < Grid.COLS; c++)
                    cells.add(new Position(r, c));
        }
        return cells;
    }

    private static String templateName(String id) {
        if (id == null) return "";
        String t = id;
        if (t.startsWith("BLUE_"))     t = t.substring(5);
        else if (t.startsWith("RED_")) t = t.substring(4);
        int u = t.lastIndexOf('_');
        if (u > 0 && t.substring(u + 1).matches("\\d+"))
            t = t.substring(0, u);
        return t.isEmpty() ? id : t;
    }
}