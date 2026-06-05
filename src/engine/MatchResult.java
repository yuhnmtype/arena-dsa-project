package engine;

public class MatchResult {
    public final String matchType;
    public final String winner;
    public final int rounds;
    public final int budget;
    public final int blueHpRemaining;
    public final int redHpRemaining;
    public final int blueAliveCount;
    public final int redAliveCount;
    public final String blueComp;
    public final String redComp;

    public MatchResult(String matchType, String winner, int rounds,
                       int budget, int blueHp, int redHp,
                       int blueAlive, int redAlive,
                       String blueComp, String redComp) {
        this.matchType       = matchType;
        this.winner          = winner;
        this.rounds          = rounds;
        this.budget          = budget;
        this.blueHpRemaining = blueHp;
        this.redHpRemaining  = redHp;
        this.blueAliveCount  = blueAlive;
        this.redAliveCount   = redAlive;
        this.blueComp        = blueComp;
        this.redComp         = redComp;
    }

    public String toCsvRow() {
        return matchType + "," + budget + "," + winner + "," + rounds + ","
             + blueHpRemaining + "," + redHpRemaining + ","
             + blueAliveCount  + "," + redAliveCount  + ","
             + blueComp + "," + redComp;
    }
}