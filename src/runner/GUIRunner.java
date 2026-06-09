package runner;

import bot.SimpleBot;
import bot.StudentBotV2;
import engine.BattleEngine;
import engine.BattleLog;
import gui.core.GameWindow;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GUIRunner {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> showSetup());
    }

    private static void showSetup() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Arena Bot Simulator");
        dialog.setSize(420, 320);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(20, 20, 35));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        // Title
        JLabel title = new JLabel("ARENA BOT SIMULATOR");
        title.setForeground(new Color(100, 180, 255));
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("DSA Final Project — BFS + Greedy");
        subtitle.setForeground(new Color(130, 130, 150));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Matchup dropdown
        JLabel lblType = makeLabel("Matchup Type:");
        String[] types = {
            "V2_vs_Simple  (StudentBotV2 vs SimpleBot)",
            "Simple_vs_V2  (SimpleBot vs StudentBotV2)",
            "V2_vs_V2      (StudentBotV2 vs StudentBotV2)",
            "Simple_vs_Simple (Baseline)"
        };
        JComboBox<String> dropdown = new JComboBox<>(types);
        styleDropdown(dropdown);
        dropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        // Budget slider
        JLabel lblBudget = makeLabel("Gold Budget:");
        JLabel budgetVal = new JLabel("20");
        budgetVal.setForeground(new Color(100, 220, 100));
        budgetVal.setFont(new Font("Arial", Font.BOLD, 16));
        budgetVal.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSlider slider = new JSlider(5, 100, 20);
        slider.setBackground(new Color(20, 20, 35));
        slider.setForeground(new Color(150, 150, 150));
        slider.setMajorTickSpacing(15);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        slider.addChangeListener(e ->
            budgetVal.setText(String.valueOf(slider.getValue()))
        );

        // Run button
        JButton btnRun = new JButton("▶  Run Match");
        btnRun.setBackground(new Color(40, 140, 80));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFont(new Font("Arial", Font.BOLD, 14));
        btnRun.setFocusPainted(false);
        btnRun.setBorderPainted(false);
        btnRun.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRun.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnRun.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnRun.addActionListener(e -> {
            int budget = slider.getValue();
            int idx    = dropdown.getSelectedIndex();
            String[] keys = {
                "V2_vs_Simple","Simple_vs_V2",
                "V2_vs_V2","Simple_vs_Simple"
            };
            String matchType = keys[idx];

            btnRun.setEnabled(false);
            btnRun.setText("Running " + budget + "g ...");

            SwingWorker<BattleLog, Void> worker = new SwingWorker<>() {
                @Override protected BattleLog doInBackground() {
                    BattleEngine engine = buildEngine(matchType);
                    return engine.runMatchWithLog(budget, matchType);
                }
                @Override protected void done() {
                    try {
                        BattleLog log = get();
                        dialog.dispose();
                        GameWindow window = new GameWindow(log);
                        window.setVisible(true);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog,
                            "Error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                        btnRun.setEnabled(true);
                        btnRun.setText("▶  Run Match");
                    }
                }
            };
            worker.execute();
        });

        // Layout
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(20));
        panel.add(lblType);
        panel.add(Box.createVerticalStrut(6));
        panel.add(dropdown);
        panel.add(Box.createVerticalStrut(14));
        panel.add(lblBudget);
        panel.add(Box.createVerticalStrut(4));
        panel.add(budgetVal);
        panel.add(slider);
        panel.add(Box.createVerticalStrut(16));
        panel.add(btnRun);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private static BattleEngine buildEngine(String type) {
        switch (type) {
            case "V2_vs_Simple":
                return new BattleEngine(new StudentBotV2(), new SimpleBot());
            case "Simple_vs_V2":
                return new BattleEngine(new SimpleBot(), new StudentBotV2());
            case "V2_vs_V2":
                return new BattleEngine(new StudentBotV2(), new StudentBotV2());
            default:
                return new BattleEngine(new SimpleBot(), new SimpleBot());
        }
    }

    private static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        return l;
    }

    private static void styleDropdown(JComboBox<String> cb) {
        cb.setBackground(new Color(40, 40, 60));
        cb.setForeground(Color.WHITE);
        cb.setFont(new Font("Arial", Font.PLAIN, 12));
    }
}