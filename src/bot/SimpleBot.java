package bot;

import engine.Champion;
import engine.ChampionFactory;
import engine.Grid;
import engine.Position;
import java.util.*;

/**
 * Placeholder bot — Bảo sẽ thay bằng StudentBotV2.java
 * SimpleBot chỉ draft rẻ nhất, đứng yên, tấn công gần nhất
 */
public class SimpleBot implements IBotAI {

    @Override
    public List<String> draftTeam(int budget, List<String> available) {
        List<String> picks = new ArrayList<>();
        int spent = 0;
        for (String id : available) {
            if (picks.size() >= 8) break;
            int cost = ChampionFactory.getCost(id);
            if (spent + cost <= budget) {
                picks.add(id);
                spent += cost;
            }
        }
        return picks;
    }

    @Override
    public List<Position> placeTeam(List<Champion> team,
                                     List<Position> cells,
                                     boolean isBlue) {
        List<Position> result = new ArrayList<>();
        for (int i = 0; i < team.size() && i < cells.size(); i++)
            result.add(cells.get(i));
        return result;
    }

    @Override
    public List<BotAction> playTurn(List<Champion> allies,
                                     List<Champion> enemies,
                                     Grid grid, int round) {
        List<BotAction> actions = new ArrayList<>();
        for (Champion ally : allies) {
            if (!ally.isAlive()) continue;

            // Tìm enemy gần nhất trong range
            Champion target = null;
            int minDist = Integer.MAX_VALUE;
            for (Champion e : enemies) {
                if (!e.isAlive()) continue;
                int dist = ally.getPosition().manhattanDistance(e.getPosition());
                if (dist < minDist) { minDist = dist; target = e; }
            }

            if (target == null) {
                actions.add(BotAction.wait(ally.getId()));
                continue;
            }

            if (minDist <= ally.getRange()) {
                // Attack nếu trong range
                actions.add(BotAction.attack(ally.getId(), target.getId()));
            } else {
                // Move về phía enemy
                Position dest = stepToward(ally.getPosition(),
                                           target.getPosition(), grid);
                actions.add(BotAction.move(ally.getId(), dest));
            }
        }
        return actions;
    }

    private Position stepToward(Position from, Position to, Grid grid) {
        int dr = Integer.signum(to.row() - from.row());
        int dc = Integer.signum(to.col() - from.col());
        Position next = new Position(from.row() + dr, from.col() + dc);
        if (grid.isValid(next)) return next;
        // thử hướng khác nếu bị chặn
        Position r1 = new Position(from.row() + dr, from.col());
        Position r2 = new Position(from.row(), from.col() + dc);
        if (grid.isValid(r1)) return r1;
        if (grid.isValid(r2)) return r2;
        return from;
    }
}