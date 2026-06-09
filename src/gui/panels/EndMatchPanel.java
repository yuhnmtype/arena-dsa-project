package gui.panels;

import engine.BattleLog;
import engine.Champion;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class EndMatchPanel extends JDialog {

    public EndMatchPanel(JFrame parent, BattleLog log) {
        super(parent, "Match Result", true);
        setSize(420, 440);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(new Color(20, 20, 35));
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        buildUI(log);
    }

    private void buildUI(BattleLog log) {
        // ── Winner banner ──────────────────────────────────────
        boolean blueWin = "BLUE".equals(log.winner);
        Color winColor  = blueWin
            ? new Color(60, 120, 255)
            : new Color(255, 60, 60);
        String winText  = blueWin ? "BLUE WINS!" : "RED WINS!";

        JLabel lblWinner = makeLabel(winText, winColor, 26, true);
        JLabel lblType   = makeLabel(log.matchType + "  |  Budget: " + log.budget + "g",
                                     new Color(180, 180, 180), 12, false);

        // ── Match stats ───────────────────────────────────────
        JPanel statsPanel = new JPanel(new GridLayout(4, 2, 10, 6));
        statsPanel.setBackground(new Color(30, 30, 50));
        statsPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        addRow(statsPanel, "Total Rounds:",  log.totalRounds + " / 40");
        addRow(statsPanel, "BLUE HP Left:",  String.valueOf(log.blueHpRemaining));
        addRow(statsPanel, "RED HP Left:",   String.valueOf(log.redHpRemaining));
        addRow(statsPanel, "Total Actions:", String.valueOf(log.totalSteps()));
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // ── Team cost breakdown ───────────────────────────────
        JLabel costHeader = makeLabel("TEAM COMPOSITION & COST",
                                      new Color(100, 180, 255), 12, true);

        JPanel costSection = new JPanel(new GridLayout(1, 2, 10, 0));
        costSection.setBackground(new Color(25, 25, 42));
        costSection.setBorder(new EmptyBorder(8, 12, 8, 12));

        costSection.add(buildTeamCostPanel(
            "BLUE TEAM", new Color(60, 120, 220),
            log.initialBlueTeam, log.budget));
        costSection.add(buildTeamCostPanel(
            "RED TEAM", new Color(220, 60, 60),
            log.initialRedTeam, log.budget));

        costSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        // ── Buttons ───────────────────────────────────────────
        JButton btnAgain  = makeButton("New Match",  new Color(40, 140, 80));
        JButton btnClose  = makeButton("Close",      new Color(140, 40, 40));

        btnAgain.addActionListener(e -> {
            dispose();
            // Close GameWindow parent and reopen setup
            if (getOwner() != null) getOwner().dispose();
            runner.GUIRunner.showSetup();
        });
        btnClose.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setBackground(new Color(20, 20, 35));
        btnRow.add(btnAgain);
        btnRow.add(btnClose);

        // ── Assemble ──────────────────────────────────────────
        add(Box.createVerticalStrut(14));
        add(center(lblWinner));
        add(Box.createVerticalStrut(4));
        add(center(lblType));
        add(Box.createVerticalStrut(10));
        add(statsPanel);
        add(Box.createVerticalStrut(10));
        add(center(costHeader));
        add(Box.createVerticalStrut(6));
        add(costSection);
        add(Box.createVerticalStrut(12));
        add(btnRow);
        add(Box.createVerticalStrut(12));
    }

    // ── Build one team's cost column ──────────────────────────
    private JPanel buildTeamCostPanel(String title, Color teamColor,
                                      List<Champion> team, int budget) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(25, 25, 42));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Team header
        JLabel header = new JLabel(title);
        header.setForeground(teamColor);
        header.setFont(new Font("Arial", Font.BOLD, 11));
        header.setBorder(new EmptyBorder(0, 0, 4, 0));
        panel.add(header);

        int totalCost = 0;

        if (team != null) {
            for (Champion c : team) {
                String name = templateName(c.getId());
                int cost    = c.getCost();
                totalCost  += cost;

                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 1));
                row.setBackground(new Color(25, 25, 42));

                JLabel nameLbl = new JLabel(name);
                nameLbl.setForeground(new Color(210, 210, 210));
                nameLbl.setFont(new Font("Arial", Font.PLAIN, 11));
                nameLbl.setPreferredSize(new Dimension(90, 16));

                JLabel costLbl = new JLabel(cost + "g");
                costLbl.setForeground(new Color(255, 200, 50));
                costLbl.setFont(new Font("Arial", Font.BOLD, 11));

                row.add(nameLbl);
                row.add(costLbl);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                panel.add(row);
            }
        }

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(70, 70, 100));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(4));

        // Total cost row
        String totalText = "Total: " + totalCost + "g";
        if (budget > 0) totalText += " / " + budget + "g";
        JLabel totalLbl = new JLabel(totalText);
        totalLbl.setForeground(teamColor.brighter());
        totalLbl.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(totalLbl);

        // Budget usage bar
        if (budget > 0) {
            double ratio = Math.min(1.0, (double) totalCost / budget);
            JPanel barBg = new JPanel(new BorderLayout());
            barBg.setBackground(new Color(50, 50, 50));
            barBg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            barBg.setPreferredSize(new Dimension(120, 6));

            JPanel barFill = new JPanel();
            barFill.setBackground(ratio < 0.9
                ? teamColor
                : new Color(255, 80, 80)); // overspend warning
            barFill.setPreferredSize(new Dimension((int)(120 * ratio), 6));
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(new Color(50, 50, 50));
            wrapper.add(barFill, BorderLayout.WEST);
            barBg.add(wrapper, BorderLayout.CENTER);

            panel.add(Box.createVerticalStrut(4));
            panel.add(barBg);
        }

        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────
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

    // "BLUE_FROST_WITCH_0" → "FROST_WITCH"
    private static String templateName(String id) {
        if (id == null) return "";
        String t = id;
        if (t.startsWith("BLUE_"))     t = t.substring(5);
        else if (t.startsWith("RED_")) t = t.substring(4);
        int u = t.lastIndexOf('_');
        if (u > 0 && t.substring(u + 1).matches("\\d+"))
            t = t.substring(0, u);
        return t.isEmpty() ? id : t;
    }

    // Static helper to open dialog
    public static void show(JFrame parent, BattleLog log) {
        EndMatchPanel dialog = new EndMatchPanel(parent, log);
        dialog.setVisible(true);
    }
}