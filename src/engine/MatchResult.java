package engine;

public class MatchResult {
    public final String winner;
    public final int rounds;
    public final int budget;
    public final String blueComp;
    public final String redComp;

    public MatchResult(String winner, int rounds, int budget,
                       String blueComp, String redComp) {
        this.winner   = winner;
        this.rounds   = rounds;
        this.budget   = budget;
        this.blueComp = blueComp;
        this.redComp  = redComp;
    }

    public String toCsvRow() {
        return budget + "," + winner + "," + rounds + ","
             + blueComp + "," + redComp;
    }

    @Override
    public String toString() {
        return "Winner: " + winner + " | Rounds: " + rounds
             + " | Budget: " + budget;
    }
}