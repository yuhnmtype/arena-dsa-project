package engine;

import java.util.ArrayList;
import java.util.List;

public class ChampionFactory {

    public static Champion create(String id, String side) {
        switch (id) {
            case "KNIGHT":      return new Champion(id, side, 4, 18, 6, 3, 1, 2, 2, 10, 3, 3);
            case "ARCHER":      return new Champion(id, side, 3, 12, 5, 1, 3, 2, 3, 10, 3, 2);
            case "MAGE":        return new Champion(id, side, 4, 10, 6, 1, 3, 2, 2, 10, 4, 3);
            case "ASSASSIN":    return new Champion(id, side, 5, 11, 7, 1, 1, 3, 4, 10, 3, 3);
            case "CLERIC":      return new Champion(id, side, 4, 13, 4, 2, 2, 2, 2, 10, 4, 3);
            case "GUARDIAN":    return new Champion(id, side, 3, 16, 4, 4, 1, 1, 1, 10, 3, 3);
            case "PALADIN":     return new Champion(id, side, 5, 17, 6, 3, 1, 2, 2, 10, 4, 3);
            case "WARLOCK":     return new Champion(id, side, 4, 11, 6, 1, 3, 2, 2, 10, 4, 3);
            case "DRUID":       return new Champion(id, side, 4, 13, 4, 2, 2, 2, 2, 10, 4, 3);
            case "LANCER":      return new Champion(id, side, 4, 14, 6, 2, 1, 3, 3, 10, 3, 3);
            case "FROST_WITCH": return new Champion(id, side, 4, 10, 5, 1, 3, 2, 2, 10, 4, 3);
            case "BERSERKER":   return new Champion(id, side, 5, 15, 7, 1, 1, 2, 3, 10, 3, 3);
            default: throw new IllegalArgumentException("Unknown champion: " + id);
        }
    }

    public static List<String> getAllIds() {
        return new ArrayList<>(List.of(
            "KNIGHT", "ARCHER", "MAGE", "ASSASSIN", "CLERIC",
            "GUARDIAN", "PALADIN", "WARLOCK", "DRUID",
            "LANCER", "FROST_WITCH", "BERSERKER"
        ));
    }

    public static int getCost(String id) {
        return create(id, "BLUE").getCost();
    }
}