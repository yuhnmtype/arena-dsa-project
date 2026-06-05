package gui.panels;

import bot.SimpleBot;
import bot.StudentBotV2;
import engine.BattleEngine;
import engine.BattleLog;
import gui.core.ReplayController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class SetupPanel extends JPanel {

    private JComboBox<String> matchTypeDropdown;
    private JSlider budgetSlider;
    private JLabel budgetLabel;
    private JButton btnRun;
    private Consumer<BattleLog> onMatchReady;

    public SetupPanel(Consumer<BattleLog> onMatchReady) {
        this.onMatchReady = onMatchReady;
        setBackground(new Color(20, 20, 35));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 90), 1),
            new EmptyBorder(12, 14, 12, 14)
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
    }

    private void buildUI() {
        // Header
        JLabel header = makeLabel("MATCH SETUP", new Color(100, 180, 255), 13, true);

        // Matchup dropdown
        JLabel lblType = makeLabel("Matchup Type:", Color.WHITE, 12, false);
        String[] types = {
            "V2_vs_Simple",
            "Simple_vs_V2",
            "V2_vs_V2",
            "Simple_vs_Simple"
        };
        matchTypeDropdown = new JComboBox<>(types);
        matchTypeDropdown.setBackground(new Color(40, 40, 60));
        matchTypeDropdown.setForeground(Color.WHITE);
        matchTypeDropdown.setFont(new Font("Arial", Font.PLAIN, 12));
        matchTypeDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Budget slider
        JLabel lblBudget = makeLabel("Budget:", Color.WHITE, 12, false);
        budgetLabel = makeLabel("20", new Color(100, 220, 100), 13, true);
        budgetSlider = new JSlider(5, 100, 20);
        budgetSlider.setMajorTickSpacing(15);
        budgetSlider.setMinorTickSpacing(5);
        budgetSlider.setPaintTicks(true);
        budgetSlider.setBackground(new Color(20, 20, 35));
        budgetSlider.setForeground(new Color(150, 150, 150));
        budgetSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        budgetSlider.addChangeListener(e ->
            budgetLabel.setText(String.valueOf(budgetSlider.getValue()))
        );

        // Budget row
        JPanel budgetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        budgetRow.setBackground(new Color(20, 20, 35));
        budgetRow.add(lblBudget);
        budgetRow.add(budgetLabel);
        budgetRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // Run button
        btnRun = new JButton("▶  Run Match");
        btnRun.setBackground(new Color(40, 140, 80));
        btnRun.setForeground(Color.WHITE);
        btnRun.setFont(new Font("Arial", Font.BOLD, 14));
        btnRun.setFocusPainted(false);
        btnRun.setBorderPainted(false);
        btnRun.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRun.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnRun.addActionListener(e -> runMatch());

        // Layout
        add(center(header));
        add(Box.createVerticalStrut(12));
        add(lblType);
        add(Box.createVerticalStrut(4));
        add(matchTypeDropdown);
        add(Box.createVerticalStrut(10));
        add(budgetRow);
        add(budgetSlider);
        add(Box.createVerticalStrut(12));
        add(btnRun);
    }

    private void runMatch() {
        btnRun.setEnabled(false);
        btnRun.setText("Running...");

        String type   = (String) matchTypeDropdown.getSelectedItem();
        int    budget = budgetSlider.getValue();

        // Run in background thread để không freeze UI
        SwingWorker<BattleLog, Void> worker = new SwingWorker<>() {
            @Override
            protected BattleLog doInBackground() {
                BattleEngine engine = buildEngine(type);
                return engine.runMatchWithLog(budget, type);
            }

            @Override
            protected void done() {
                try {
                    BattleLog log = get();
                    onMatchReady.accept(log);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SetupPanel.this,
                        "Error: " + ex.getMessage(),
                        "Match Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnRun.setEnabled(true);
                    btnRun.setText("▶  Run Match");
                }
            }
        };
        worker.execute();
    }

    private BattleEngine buildEngine(String type) {
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

    private JLabel makeLabel(String text, Color color, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }

    private JPanel center(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setBackground(new Color(20, 20, 35));
        p.add(comp);
        return p;
    }
}