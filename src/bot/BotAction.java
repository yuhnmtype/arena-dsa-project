package bot;

import engine.Position;

public class BotAction {
    public enum Type { MOVE, ATTACK, CAST_SKILL, DEFEND, WAIT }

    public final String championId;
    public final Type type;
    public final Position targetPosition;
    public final String targetChampionId;

    public BotAction(String championId, Type type,
                     Position targetPosition, String targetChampionId) {
        this.championId       = championId;
        this.type             = type;
        this.targetPosition   = targetPosition;
        this.targetChampionId = targetChampionId;
    }

    public static BotAction wait(String championId) {
        return new BotAction(championId, Type.WAIT, null, null);
    }

    public static BotAction move(String championId, Position target) {
        return new BotAction(championId, Type.MOVE, target, null);
    }

    public static BotAction attack(String championId, String targetId) {
        return new BotAction(championId, Type.ATTACK, null, targetId);
    }
}