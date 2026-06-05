package engine;

import java.util.ArrayList;
import java.util.List;

public class BattleLog {

    public final String matchType;
    public final int budget;
    public final List<BattleLogEntry> entries = new ArrayList<>();

    // Initial state — để GUI biết vị trí ban đầu
    public List<Champion> initialBlueTeam;
    public List<Champion> initialRedTeam;

    // Result
    public String winner;
    public int totalRounds;
    public int blueHpRemaining;
    public int redHpRemaining;

    public BattleLog(String matchType, int budget) {
        this.matchType = matchType;
        this.budget    = budget;
    }

    // Thêm entry vào log
    public void add(BattleLogEntry entry) {
        entries.add(entry);
    }

    // Lấy tất cả entries trong 1 round
    public List<BattleLogEntry> getRound(int round) {
        List<BattleLogEntry> result = new ArrayList<>();
        for (BattleLogEntry e : entries)
            if (e.round == round) result.add(e);
        return result;
    }

    // Lấy entry theo index
    public BattleLogEntry getEntry(int index) {
        if (index < 0 || index >= entries.size()) return null;
        return entries.get(index);
    }

    public int totalSteps() {
        return entries.size();
    }

    @Override
    public String toString() {
        return matchType + " | Budget:" + budget
             + " | Winner:" + winner
             + " | Rounds:" + totalRounds
             + " | Steps:" + entries.size();
    }
}