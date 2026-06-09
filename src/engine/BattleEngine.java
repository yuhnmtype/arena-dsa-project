package engine;

import bot.BFSPathfinder;
import bot.BotAction;
import bot.IBotAI;
import java.util.*;

public class BattleEngine {

    public static final int MAX_ROUNDS = 40;
    public static final int MAX_TEAM   = 8;

    private final IBotAI blueBot;
    private final IBotAI redBot;

    public BattleEngine(IBotAI blueBot, IBotAI redBot) {
        this.blueBot = blueBot;
        this.redBot  = redBot;
    }

    public MatchResult runMatch(int budget) {
        return runMatch(budget, "Unknown");
    }

    public MatchResult runMatch(int budget, String matchType) {
        Grid grid = new Grid();

        List<String> pool      = ChampionFactory.getAllIds();
        List<String> blueDraft = blueBot.draftTeam(budget, pool);
        List<String> redDraft  = redBot.draftTeam(budget, pool);

        List<Champion> blueTeam = new ArrayList<>();
        List<Champion> redTeam  = new ArrayList<>();
        for (int i = 0; i < blueDraft.size(); i++)
            blueTeam.add(ChampionFactory.createWithUniqueId(blueDraft.get(i), "BLUE", i));
        for (int i = 0; i < redDraft.size(); i++)
            redTeam.add(ChampionFactory.createWithUniqueId(redDraft.get(i), "RED", i));

        List<Position> blueCells  = getAllowedCells("BLUE");
        List<Position> redCells   = getAllowedCells("RED");
        List<Position> bluePlaces = blueBot.placeTeam(blueTeam, blueCells, true);
        List<Position> redPlaces  = redBot.placeTeam(redTeam, redCells, false);

        for (int i = 0; i < blueTeam.size(); i++) {
            Position p = bluePlaces.get(i);
            blueTeam.get(i).setPosition(p);
            grid.occupy(p);
        }
        for (int i = 0; i < redTeam.size(); i++) {
            Position p = redPlaces.get(i);
            redTeam.get(i).setPosition(p);
            grid.occupy(p);
        }

        for (int round = 1; round <= MAX_ROUNDS; round++) {
            PriorityQueue<Champion> turnOrder = new PriorityQueue<>(
                Comparator.comparingInt(Champion::getSpeed).reversed()
            );
            blueTeam.stream().filter(Champion::isAlive).forEach(turnOrder::add);
            redTeam.stream().filter(Champion::isAlive).forEach(turnOrder::add);

            List<BotAction> blueActions = blueBot.playTurn(
                aliveOnly(blueTeam), aliveOnly(redTeam), grid, round);
            List<BotAction> redActions  = redBot.playTurn(
                aliveOnly(redTeam), aliveOnly(blueTeam), grid, round);

            Map<String, BotAction> actionMap = new HashMap<>();
            for (BotAction a : blueActions) actionMap.put(a.championId, a);
            for (BotAction a : redActions)  actionMap.put(a.championId, a);

            while (!turnOrder.isEmpty()) {
                Champion actor = turnOrder.poll();
                if (!actor.isAlive()) continue;
                BotAction action = actionMap.get(actor.getId());
                if (action != null)
                    executeAction(actor, action, blueTeam, redTeam, grid);
            }

            blueTeam.forEach(c -> { if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); } });
            redTeam.forEach(c  -> { if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); } });

            boolean blueAlive = blueTeam.stream().anyMatch(Champion::isAlive);
            boolean redAlive  = redTeam.stream().anyMatch(Champion::isAlive);

            if (!blueAlive || !redAlive) {
                String winner = blueAlive ? "BLUE" : "RED";
                return buildResult(matchType, winner, round, budget,
                                   blueTeam, redTeam, blueDraft, redDraft);
            }
        }

        int blueHp = blueTeam.stream().filter(Champion::isAlive)
                              .mapToInt(Champion::getHp).sum();
        int redHp  = redTeam.stream().filter(Champion::isAlive)
                             .mapToInt(Champion::getHp).sum();
        String winner = blueHp >= redHp ? "BLUE" : "RED";
        return buildResult(matchType, winner, MAX_ROUNDS, budget,
                           blueTeam, redTeam, blueDraft, redDraft);
    }

    // ── GUI version — returns BattleLog ──────────────────────────
    public BattleLog runMatchWithLog(int budget, String matchType) {
        BattleLog log = new BattleLog(matchType, budget);
        Grid grid = new Grid();

        List<String> pool      = ChampionFactory.getAllIds();
        List<String> blueDraft = blueBot.draftTeam(budget, pool);
        List<String> redDraft  = redBot.draftTeam(budget, pool);

        List<Champion> blueTeam = new ArrayList<>();
        List<Champion> redTeam  = new ArrayList<>();
        for (int i = 0; i < blueDraft.size(); i++)
            blueTeam.add(ChampionFactory.createWithUniqueId(blueDraft.get(i), "BLUE", i));
        for (int i = 0; i < redDraft.size(); i++)
            redTeam.add(ChampionFactory.createWithUniqueId(redDraft.get(i), "RED", i));

        List<Position> blueCells  = getAllowedCells("BLUE");
        List<Position> redCells   = getAllowedCells("RED");
        List<Position> bluePlaces = blueBot.placeTeam(blueTeam, blueCells, true);
        List<Position> redPlaces  = redBot.placeTeam(redTeam, redCells, false);

        for (int i = 0; i < blueTeam.size(); i++) {
            blueTeam.get(i).setPosition(bluePlaces.get(i));
            grid.occupy(bluePlaces.get(i));
        }
        for (int i = 0; i < redTeam.size(); i++) {
            redTeam.get(i).setPosition(redPlaces.get(i));
            grid.occupy(redPlaces.get(i));
        }

        log.initialBlueTeam = deepCopy(blueTeam);
        log.initialRedTeam  = deepCopy(redTeam);

        for (int round = 1; round <= MAX_ROUNDS; round++) {
            PriorityQueue<Champion> turnOrder = new PriorityQueue<>(
                Comparator.comparingInt(Champion::getSpeed).reversed()
            );
            blueTeam.stream().filter(Champion::isAlive).forEach(turnOrder::add);
            redTeam.stream().filter(Champion::isAlive).forEach(turnOrder::add);

            List<BotAction> blueActions = blueBot.playTurn(
                aliveOnly(blueTeam), aliveOnly(redTeam), grid, round);
            List<BotAction> redActions  = redBot.playTurn(
                aliveOnly(redTeam), aliveOnly(blueTeam), grid, round);

            Map<String, BotAction> actionMap = new HashMap<>();
            for (BotAction a : blueActions) actionMap.put(a.championId, a);
            for (BotAction a : redActions)  actionMap.put(a.championId, a);

            while (!turnOrder.isEmpty()) {
                Champion actor = turnOrder.poll();
                if (!actor.isAlive()) continue;
                BotAction action = actionMap.get(actor.getId());
                if (action == null) continue;

                // Snapshot before
                int hpBefore       = actor.getHp();
                int manaBefore     = actor.getMana();
                Position fromPos   = actor.getPosition();
                String targetId    = action.targetChampionId;
                int targetHpBefore = -1;
                Champion targetChamp = findById(targetId, blueTeam, redTeam);
                if (targetChamp != null) targetHpBefore = targetChamp.getHp();

                // Execute
                executeAction(actor, action, blueTeam, redTeam, grid);

                // Snapshot after
                int hpAfter    = actor.getHp();
                int manaAfter  = actor.getMana();
                Position toPos = actor.getPosition();
                int targetHpAfter = targetChamp != null ? targetChamp.getHp() : -1;

                // Occupied snapshot
                Set<Position> snap = new HashSet<>();
                blueTeam.stream().filter(Champion::isAlive)
                        .forEach(c -> snap.add(c.getPosition()));
                redTeam.stream().filter(Champion::isAlive)
                       .forEach(c -> snap.add(c.getPosition()));

                // ── BFS path for MOVE actions ─────────────────
                List<Position> bfsPath = new ArrayList<>();
                if (action.type == BotAction.Type.MOVE
                        && fromPos != null && toPos != null
                        && !fromPos.equals(toPos)) {
                    bfsPath = BFSPathfinder.findPath(fromPos, toPos, grid, snap);
                }

                // Extract template name
                String[] parts = actor.getId().split("_");
                String name = parts.length >= 2
                    ? parts[parts.length - 2] : actor.getId();
                String team = actor.getId().startsWith("BLUE") ? "BLUE" : "RED";

                log.add(new BattleLogEntry(
                    round, actor.getId(), name, team,
                    action.type,
                    fromPos, toPos,
                    hpBefore, hpAfter,
                    manaBefore, manaAfter,
                    targetId, targetHpBefore, targetHpAfter,
                    bfsPath, snap   // ← bfsPath thay vì null
                ));
            }

            blueTeam.forEach(c -> { if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); } });
            redTeam.forEach(c  -> { if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); } });

            boolean blueAlive = blueTeam.stream().anyMatch(Champion::isAlive);
            boolean redAlive  = redTeam.stream().anyMatch(Champion::isAlive);

            if (!blueAlive || !redAlive) {
                log.winner          = blueAlive ? "BLUE" : "RED";
                log.totalRounds     = round;
                log.blueHpRemaining = blueTeam.stream()
                    .filter(Champion::isAlive).mapToInt(Champion::getHp).sum();
                log.redHpRemaining  = redTeam.stream()
                    .filter(Champion::isAlive).mapToInt(Champion::getHp).sum();
                return log;
            }
        }

        int blueHp = blueTeam.stream().filter(Champion::isAlive)
                              .mapToInt(Champion::getHp).sum();
        int redHp  = redTeam.stream().filter(Champion::isAlive)
                             .mapToInt(Champion::getHp).sum();
        log.winner          = blueHp >= redHp ? "BLUE" : "RED";
        log.totalRounds     = MAX_ROUNDS;
        log.blueHpRemaining = blueHp;
        log.redHpRemaining  = redHp;
        return log;
    }

    // ── Helpers ──────────────────────────────────────────────────
    private MatchResult buildResult(String matchType, String winner,
                                    int rounds, int budget,
                                    List<Champion> blueTeam,
                                    List<Champion> redTeam,
                                    List<String> blueDraft,
                                    List<String> redDraft) {
        int blueHp    = blueTeam.stream().filter(Champion::isAlive)
                                 .mapToInt(Champion::getHp).sum();
        int redHp     = redTeam.stream().filter(Champion::isAlive)
                                .mapToInt(Champion::getHp).sum();
        int blueAlive = (int) blueTeam.stream()
                                       .filter(Champion::isAlive).count();
        int redAlive  = (int) redTeam.stream()
                                      .filter(Champion::isAlive).count();
        return new MatchResult(matchType, winner, rounds, budget,
                               blueHp, redHp, blueAlive, redAlive,
                               String.join("+", blueDraft),
                               String.join("+", redDraft));
    }

    private List<Champion> deepCopy(List<Champion> list) {
        List<Champion> copy = new ArrayList<>();
        for (Champion c : list) copy.add(c.copy());
        return copy;
    }

    private void executeAction(Champion actor, BotAction action,
                                List<Champion> blueTeam,
                                List<Champion> redTeam,
                                Grid grid) {
        switch (action.type) {
            case MOVE:
                if (action.targetPosition != null
                        && grid.isValid(action.targetPosition)) {
                    grid.move(actor.getPosition(), action.targetPosition);
                    actor.setPosition(action.targetPosition);
                }
                break;
            case ATTACK:
                Champion target = findById(action.targetChampionId,
                                           blueTeam, redTeam);
                if (target != null && target.isAlive())
                    target.takeDamage(actor.getAttack());
                break;
            case CAST_SKILL:
                if (actor.getMana() >= actor.getSkillManaCost()
                        && actor.getRemainingCooldown() == 0) {
                    actor.useSkill();
                    Champion skillTarget = findById(action.targetChampionId,
                                                    blueTeam, redTeam);
                    if (skillTarget != null && skillTarget.isAlive())
                        skillTarget.takeDamage(actor.getAttack() + 2);
                }
                break;
            default:
                break;
        }
    }

    private Champion findById(String id,
                               List<Champion> blue,
                               List<Champion> red) {
        if (id == null) return null;
        for (Champion c : blue) if (c.getId().equals(id)) return c;
        for (Champion c : red)  if (c.getId().equals(id)) return c;
        return null;
    }

    private List<Champion> aliveOnly(List<Champion> list) {
        List<Champion> result = new ArrayList<>();
        for (Champion c : list) if (c.isAlive()) result.add(c);
        return result;
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
}