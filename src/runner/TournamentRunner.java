package runner;

import bot.IBotAI;
import bot.SimpleBot;
import engine.BattleEngine;
import engine.MatchResult;
import java.io.*;

public class TournamentRunner {

    public static void main(String[] args) throws IOException {
        IBotAI botA = new SimpleBot();
        IBotAI botB = new SimpleBot();

        PrintWriter csv = new PrintWriter(new FileWriter("match_results.csv"));
        csv.println("budget,winner,rounds,blue_comp,red_comp");

        BattleEngine engine = new BattleEngine(botA, botB);

        int total = 0;
        for (int budget = 5; budget <= 100; budget += 5) {
            for (int i = 0; i < 10; i++) {
                MatchResult r = engine.runMatch(budget);
                csv.println(r.toCsvRow());
                total++;
            }
        }

        csv.close();
        System.out.println("Done: " + total + " matches → match_results.csv");
    }
}