package runner;

import bot.SimpleBot;
import bot.StudentBotV2;
import engine.BattleEngine;
import engine.BattleLog;
import gui.core.GameWindow;

public class GUIRunner {
    public static void main(String[] args) {
        // Test với budget=20, V2 vs Simple
        BattleEngine engine = new BattleEngine(
            new StudentBotV2(),
            new SimpleBot()
        );

        BattleLog log = engine.runMatchWithLog(20, "V2_vs_Simple");
        System.out.println("Log loaded: " + log.totalSteps() + " steps");
        System.out.println("Winner: " + log.winner);

        // Mở GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(log);
            window.setVisible(true);
        });
    }
}