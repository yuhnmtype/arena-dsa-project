package engine;

import bot.IBotAI;
import bot.BotAction;
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
        Grid grid = new Grid();

        // ── DRAFT ────────────────────────────────────────────────
        List<String> pool      = ChampionFactory.getAllIds();
        List<String> blueDraft = blueBot.draftTeam(budget, pool);
        List<String> redDraft  = redBot.draftTeam(budget, pool);

        List<Champion> blueTeam = new ArrayList<>();
        List<Champion> redTeam  = new ArrayList<>();
        for (int i = 0; i < blueDraft.size(); i++)
            blueTeam.add(ChampionFactory.createWithUniqueId(blueDraft.get(i), "BLUE", i));
        for (int i = 0; i < redDraft.size(); i++)
            redTeam.add(ChampionFactory.createWithUniqueId(redDraft.get(i), "RED", i));

        // ── PLACEMENT ────────────────────────────────────────────
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

        // ── BATTLE ───────────────────────────────────────────────
        for (int round = 1; round <= MAX_ROUNDS; round++) {

            // Priority Queue: sort by SPD — O(log n) per insert/extract
            PriorityQueue<Champion> turnOrder = new PriorityQueue<>(
                Comparator.comparingInt(Champion::getSpeed).reversed()
            );
            blueTeam.stream().filter(Champion::isAlive).forEach(turnOrder::add);
            redTeam.stream().filter(Champion::isAlive).forEach(turnOrder::add);

            // Get actions from each bot
            List<BotAction> blueActions = blueBot.playTurn(
                aliveOnly(blueTeam), aliveOnly(redTeam), grid, round);
            List<BotAction> redActions  = redBot.playTurn(
                aliveOnly(redTeam), aliveOnly(blueTeam), grid, round);

            // Execute actions in SPD order
            Map<String, BotAction> actionMap = new HashMap<>();
            for (BotAction a : blueActions) actionMap.put(a.championId, a);
            for (BotAction a : redActions)  actionMap.put(a.championId, a);

            while (!turnOrder.isEmpty()) {
                Champion actor = turnOrder.poll();
                if (!actor.isAlive()) continue;
                BotAction action = actionMap.get(actor.getId());
                if (action != null) executeAction(actor, action, blueTeam, redTeam, grid);
            }

            // Gain mana + tick cooldown each round
            blueTeam.forEach(c -> { if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); } });
            redTeam.forEach(c  -> { if (c.isAlive()) { c.gainMana(2); c.tickCooldown(); } });

            // Check win condition
            boolean blueAlive = blueTeam.stream().anyMatch(Champion::isAlive);
            boolean redAlive  = redTeam.stream().anyMatch(Champion::isAlive);

            if (!blueAlive || !redAlive) {
                String winner = blueAlive ? "BLUE" : "RED";
                return new MatchResult(winner, round, budget,
                    String.join("+", blueDraft),
                    String.join("+", redDraft));
            }
        }

        // Round limit: winner by remaining HP
        int blueHp = blueTeam.stream().filter(Champion::isAlive)
                              .mapToInt(Champion::getHp).sum();
        int redHp  = redTeam.stream().filter(Champion::isAlive)
                             .mapToInt(Champion::getHp).sum();
        String winner = blueHp >= redHp ? "BLUE" : "RED";
        return new MatchResult(winner, MAX_ROUNDS, budget,
            String.join("+", blueDraft),
            String.join("+", redDraft));
    }

    private void executeAction(Champion actor, BotAction action,
                                List<Champion> blueTeam, List<Champion> redTeam,
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
                if (target != null && target.isAlive()) {
                    target.takeDamage(actor.getAttack());
                }
                break;

            case CAST_SKILL:
                if (actor.getMana() >= actor.getSkillManaCost()
                        && actor.getRemainingCooldown() == 0) {
                    actor.useSkill();
                    Champion skillTarget = findById(action.targetChampionId,
                                                    blueTeam, redTeam);
                    if (skillTarget != null && skillTarget.isAlive()) {
                        skillTarget.takeDamage(actor.getAttack() + 2);
                    }
                }
                break;

            case WAIT:
            case DEFEND:
            default:
                break;
        }
    }

    private Champion findById(String id, List<Champion> blue, List<Champion> red) {
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