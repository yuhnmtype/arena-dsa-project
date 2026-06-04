package bot;

import engine.Champion;
import engine.Grid;
import engine.Position;
import java.util.List;

public interface IBotAI {

    /** Trả về list champion IDs muốn draft, không vượt quá budget */
    List<String> draftTeam(int budget, List<String> availableIds);

    /** Trả về positions cho từng champion đã draft */
    List<Position> placeTeam(List<Champion> team,
                              List<Position> allowedCells,
                              boolean isBlue);

    /** Trả về actions cho tất cả champion còn sống trong round này */
    List<BotAction> playTurn(List<Champion> allies,
                              List<Champion> enemies,
                              Grid grid,
                              int round);
}