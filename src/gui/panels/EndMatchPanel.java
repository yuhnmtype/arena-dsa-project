package gui.panels;

import engine.BattleLog;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EndMatchPanel extends JDialog {

    public EndMatchPanel(JFrame parent, BattleLog log) {
        super(parent, "Match Result", true);
        setSize(360, 280);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(new Color(20, 20, 35));
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        buildUI(log);
    }

    private void buildUI(BattleLog log) {
        // Winner banner
        boolean blueWin = "BLUE".equals(log.winner);
        Color winColor  = blueWin
            ? new Color(60, 120, 255)
            : new Color(255, 60, 60);
        String winText  = blueWin ? "BLUE WINS!" : "RED WINS!";

        JLabel lblWinner = makeLabel(winText, winColor, 26, true);
        JLabel lblType   = makeLabel(log.matchType, new Color(180,180,180), 12, false);

        // Stats
        JPanel statsPanel = new JPanel(new GridLayout(4, 2, 10, 8));
        statsPanel.setBackground(new Color(30, 30, 50));
        statsPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        addRow(statsPanel, "Total Rounds:",
            log.totalRounds + " / 40");
        addRow(statsPanel, "BLUE HP Left:",
            String.valueOf(log.blueHpRemaining));
        addRow(statsPanel, "RED HP Left:",
            String.valueOf(log.redHpRemaining));
        addRow(statsPanel, "Total Actions:",
            String.valueOf(log.totalSteps()));

        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        // Buttons
        JButton btnAgain = makeButton("▶ Play Again", new Color(40, 140, 80));
        JButton btnClose = makeButton("✕ Close",      new Color(140, 40, 40));

        btnAgain.addActionListener(e -> dispose());
        btnClose.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(new Color(20, 20, 35));
        btnRow.add(btnAgain);
        btnRow.add(btnClose);

        // Layout
        add(Box.createVerticalStrut(16));
        add(center(lblWinner));
        add(Box.createVerticalStrut(4));
        add(center(lblType));
        add(Box.createVerticalStrut(12));
        add(statsPanel);
        add(Box.createVerticalStrut(12));
        add(btnRow);
        add(Box.createVerticalStrut(12));
    }

    private void addRow(JPanel panel, String key, String value) {
        JLabel k = new JLabel(key);
        k.setForeground(new Color(180, 180, 180));
        k.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel v = new JLabel(value);
        v.setForeground(Color.WHITE);
        v.setFont(new Font("Arial", Font.BOLD, 12));

        panel.add(k);
        panel.add(v);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 36));
        return btn;
    }

    private JLabel makeLabel(String text, Color color,
                              int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial",
            bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }

    private JPanel center(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setBackground(new Color(20, 20, 35));
        p.add(comp);
        return p;
    }

    // Static helper để mở dialog
    public static void show(JFrame parent, BattleLog log) {
        EndMatchPanel dialog = new EndMatchPanel(parent, log);
        dialog.setVisible(true);
    }
}