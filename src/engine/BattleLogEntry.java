package engine;

import bot.BotAction;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class BattleLogEntry {
    public final int    round;
    /**
     * Sub-phase within the round for simultaneous resolution.
     *   tick = 0 → MOVE phase  (all units move at once)
     *   tick = 1 → ATTACK phase (all damage applied at once)
     * GUI steps through entries sequentially; tick lets it group
     * actions that happened simultaneously in the same round.
     */
    public final int    tick;
    public final String championId;
    public final String championName;
    public final String team;
    public final BotAction.Type actionType;
    public final Position fromPos;
    public final Position toPos;
    public final int hpBefore;
    public final int hpAfter;
    public final int manaBefore;
    public final int manaAfter;
    public final String targetId;
    public final int targetHpBefore;
    public final int targetHpAfter;
    public final List<Position> bfsPath;
    public final Set<Position>  occupiedSnapshot;

    public BattleLogEntry(
            int round,
            int tick,
            String championId,
            String championName,
            String team,
            BotAction.Type actionType,
            Position fromPos,
            Position toPos,
            int hpBefore,
            int hpAfter,
            int manaBefore,
            int manaAfter,
            String targetId,
            int targetHpBefore,
            int targetHpAfter,
            List<Position> bfsPath,
            Set<Position>  occupiedSnapshot) {
        this.round            = round;
        this.tick             = tick;
        this.championId       = championId;
        this.championName     = championName;
        this.team             = team;
        this.actionType       = actionType;
        this.fromPos          = fromPos;
        this.toPos            = toPos;
        this.hpBefore         = hpBefore;
        this.hpAfter          = hpAfter;
        this.manaBefore       = manaBefore;
        this.manaAfter        = manaAfter;
        this.targetId         = targetId;
        this.targetHpBefore   = targetHpBefore;
        this.targetHpAfter    = targetHpAfter;
        this.bfsPath          = bfsPath != null ? bfsPath : new ArrayList<>();
        this.occupiedSnapshot = occupiedSnapshot != null ? occupiedSnapshot : new HashSet<>();
    }

    @Override
    public String toString() {
        return "[R" + round + "t" + tick + "] " + team + " " + championName
             + " → " + actionType
             + " | HP: " + hpBefore + "→" + hpAfter;
    }
}