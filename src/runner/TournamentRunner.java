package runner;

import bot.IBotAI;
import bot.SimpleBot;
import bot.StudentBotV2;
import engine.BattleEngine;
import engine.MatchResult;
import java.io.*;

public class TournamentRunner {

    public static void main(String[] args) throws IOException {
        PrintWriter csv = new PrintWriter(new FileWriter("match_results.csv"));
        csv.println("match_type,budget,winner,rounds," +
                    "blue_hp,red_hp,blue_alive,red_alive," +
                    "blue_comp,red_comp");

        String[] types = {
            "V2_vs_Simple",
            "Simple_vs_V2",
            "V2_vs_V2",
            "Simple_vs_Simple"
        };

        int total = 0;
        for (int budget = 5; budget <= 100; budget += 5) {
            for (String type : types) {
                IBotAI blue, red;
                switch (type) {
                    case "V2_vs_Simple":
                        blue = new StudentBotV2(); red = new SimpleBot();    break;
                    case "Simple_vs_V2":
                        blue = new SimpleBot();    red = new StudentBotV2(); break;
                    case "V2_vs_V2":
                        blue = new StudentBotV2(); red = new StudentBotV2(); break;
                    default:
                        blue = new SimpleBot();    red = new SimpleBot();    break;
                }

                BattleEngine engine = new BattleEngine(blue, red);
                for (int i = 0; i < 10; i++) {
                    MatchResult r = engine.runMatch(budget, type);
                    csv.println(r.toCsvRow());
                    total++;
                }
            }
        }

        csv.close();
        System.out.println("Done: " + total + " matches -> match_results.csv");
    }
}