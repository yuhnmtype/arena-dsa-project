package gui.panels;

import engine.BattleLog;
import engine.BattleLogEntry;
import engine.Champion;
import gui.core.ReplayController;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class StatsPanel extends JPanel {

    private final BattleLog log;
    private final Map<String, Integer> currentHp = new HashMap<>();
    private JPanel bluePanel;
    private JPanel redPanel;

    // Cost summary labels (persistent, updated in refreshPanels)
    private JLabel blueCostLabel;
    private JLabel redCostLabel;

    public StatsPanel(ReplayController controller) {
        this.log = controller.getLog();
        setBackground(new Color(25, 25, 40));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 90), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        setLayout(new GridLayout(1, 2, 10, 0));
        initHp();
        buildUI();

        controller.addListener((entry, step, total) -> updateStats(entry));
    }

    private void initHp() {
        if (log.initialBlueTeam != null)
            for (Champion c : log.initialBlueTeam)
                currentHp.put(c.getId(), c.getMaxHp());
        if (log.initialRedTeam != null)
            for (Champion c : log.initialRedTeam)
                currentHp.put(c.getId(), c.getMaxHp());
    }

    private void buildUI() {
        bluePanel = makeTeamPanel("BLUE TEAM", new Color(60, 100, 180));
        redPanel  = makeTeamPanel("RED TEAM",  new Color(180, 60, 60));
        add(bluePanel);
        add(redPanel);
        refreshPanels();
    }

    private JPanel makeTeamPanel(String title, Color headerColor) {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(25, 25, 40));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel header = new JLabel(title);
        header.setForeground(headerColor);
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(header);
        return panel;
    }

    private void updateStats(BattleLogEntry entry) {
        if (entry == null) return;

        if (entry.hpAfter >= 0)
            currentHp.put(entry.championId, entry.hpAfter);
        if (entry.targetId != null && entry.targetHpAfter >= 0)
            currentHp.put(entry.targetId, entry.targetHpAfter);

        refreshPanels();
    }

    private void refreshPanels() {
        // Clear existing champion rows (keep header at index 0)
        while (bluePanel.getComponentCount() > 1)
            bluePanel.remove(bluePanel.getComponentCount() - 1);
        while (redPanel.getComponentCount() > 1)
            redPanel.remove(redPanel.getComponentCount() - 1);

        // ── BLUE team rows ────────────────────────────────────
        int blueTotalCost = 0;
        if (log.initialBlueTeam != null) {
            for (Champion c : log.initialBlueTeam) {
                int hp = currentHp.getOrDefault(c.getId(), c.getMaxHp());
                bluePanel.add(makeChampionRow(c, hp, new Color(60, 120, 220)));
                bluePanel.add(Box.createVerticalStrut(4));
                blueTotalCost += c.getCost();
            }
        }

        // ── RED team rows ─────────────────────────────────────
        int redTotalCost = 0;
        if (log.initialRedTeam != null) {
            for (Champion c : log.initialRedTeam) {
                int hp = currentHp.getOrDefault(c.getId(), c.getMaxHp());
                redPanel.add(makeChampionRow(c, hp, new Color(220, 60, 60)));
                redPanel.add(Box.createVerticalStrut(4));
                redTotalCost += c.getCost();
            }
        }

        // ── Cost summary footers ──────────────────────────────
        bluePanel.add(Box.createVerticalStrut(6));
        bluePanel.add(makeSeparator());
        bluePanel.add(Box.createVerticalStrut(4));
        bluePanel.add(makeCostSummary(blueTotalCost, log.budget, new Color(60, 120, 220)));

        redPanel.add(Box.createVerticalStrut(6));
        redPanel.add(makeSeparator());
        redPanel.add(Box.createVerticalStrut(4));
        redPanel.add(makeCostSummary(redTotalCost, log.budget, new Color(220, 60, 60)));

        bluePanel.revalidate();
        bluePanel.repaint();
        redPanel.revalidate();
        redPanel.repaint();
    }

    // ── Champion row with name, cost badge, and HP bar ───────
    private JPanel makeChampionRow(Champion c, int curHp, Color barColor) {
        String name  = templateName(c.getId());
        int maxHp    = c.getMaxHp();
        int cost     = c.getCost();
        boolean dead = curHp <= 0;

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(new Color(25, 25, 40));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // ── Name + cost badge on same line ────────────────────
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nameRow.setBackground(new Color(25, 25, 40));

        JLabel nameLabel = new JLabel(name + (dead ? " ✝" : ""));
        nameLabel.setForeground(dead ? new Color(100, 100, 100) : Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        // Gold cost badge
        JLabel costBadge = new JLabel(" " + cost + "g");
        costBadge.setForeground(dead
            ? new Color(90, 80, 40)
            : new Color(255, 200, 50));
        costBadge.setFont(new Font("Arial", Font.BOLD, 10));

        nameRow.add(nameLabel);
        nameRow.add(costBadge);
        row.add(nameRow);

        // ── HP bar ────────────────────────────────────────────
        JPanel barBg = new JPanel(new BorderLayout());
        barBg.setBackground(new Color(50, 50, 50));
        barBg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 7));
        barBg.setPreferredSize(new Dimension(100, 7));

        if (!dead && maxHp > 0) {
            double ratio = (double) curHp / maxHp;
            JPanel barFill = new JPanel();
            barFill.setBackground(ratio > 0.5
                ? barColor
                : ratio > 0.25
                    ? new Color(255, 165, 0)
                    : new Color(255, 50, 50));
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(new Color(50, 50, 50));
            wrapper.add(barFill, BorderLayout.WEST);
            barFill.setPreferredSize(new Dimension((int)(100 * ratio), 7));
            barBg.add(wrapper, BorderLayout.CENTER);
        }

        row.add(Box.createVerticalStrut(2));
        row.add(barBg);

        // ── HP text ───────────────────────────────────────────
        JLabel hpText = new JLabel(curHp + " / " + maxHp);
        hpText.setForeground(new Color(140, 140, 140));
        hpText.setFont(new Font("Arial", Font.PLAIN, 10));
        row.add(hpText);

        return row;
    }

    // ── "Spent: Xg / Budget: Yg" footer label ────────────────
    private JLabel makeCostSummary(int spent, int budget, Color teamColor) {
        String text;
        if (budget > 0) {
            text = "Spent: " + spent + "g / Budget: " + budget + "g";
        } else {
            text = "Spent: " + spent + "g";
        }
        JLabel lbl = new JLabel(text);
        lbl.setForeground(teamColor.brighter());
        lbl.setFont(new Font("Arial", Font.BOLD, 11));
        return lbl;
    }

    // ── Thin separator line ───────────────────────────────────
    private JPanel makeSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(60, 60, 90));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(100, 1));
        return sep;
    }

    // ── Extract template name from unique ID ──────────────────
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
}