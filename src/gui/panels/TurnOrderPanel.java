package gui.panels;

import engine.BattleLog;
import engine.BattleLogEntry;
import engine.Champion;
import gui.core.ReplayController;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Shows the current round state in simultaneous-resolution mode.
 * Units no longer act sequentially, so the old speed-order list is
 * replaced with a BLUE / RED team summary showing who is still alive.
 */
public class TurnOrderPanel extends JPanel {

    private final BattleLog log;
    private JPanel listPanel;
    private JLabel headerLabel;
    private int currentRound = 0;
    private String currentPhase = "";

    public TurnOrderPanel(ReplayController controller) {
        this.log = controller.getLog();
        setBackground(new Color(25, 25, 40));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 90), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        setLayout(new BorderLayout());
        buildUI();
        controller.addListener((entry, step, total) -> updatePanel(entry));
    }

    private void buildUI() {
        headerLabel = new JLabel("ROUND INFO");
        headerLabel.setForeground(new Color(100, 180, 255));
        headerLabel.setFont(new Font("Arial", Font.BOLD, 12));
        headerLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        listPanel = new JPanel();
        listPanel.setBackground(new Color(25, 25, 40));
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        add(headerLabel, BorderLayout.NORTH);
        add(listPanel, BorderLayout.CENTER);
    }

    private void updatePanel(BattleLogEntry entry) {
        listPanel.removeAll();

        if (entry == null) {
            // Initial state — show starting rosters
            headerLabel.setText("ROUND INFO  —  Start");
            addTeamSection("BLUE TEAM", log.initialBlueTeam,
                           new Color(60, 120, 220), null);
            addTeamSection("RED TEAM",  log.initialRedTeam,
                           new Color(220, 60, 60),  null);
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }

        currentRound = entry.round;
        // tick=0 → MOVE phase, tick=1 → ATTACK phase
        currentPhase = entry.tick == 0 ? "Move" : "Attack";
        headerLabel.setText("ROUND " + currentRound + " / 40  —  " + currentPhase + " phase");

        // Highlight unit that just acted in this tick group
        String actingId = entry.championId;

        addTeamSection("BLUE TEAM", log.initialBlueTeam,
                       new Color(60, 120, 220), actingId);
        addSeparator();
        addTeamSection("RED TEAM",  log.initialRedTeam,
                       new Color(220, 60, 60),  actingId);

        listPanel.revalidate();
        listPanel.repaint();
    }

    private void addTeamSection(String title, List<Champion> team,
                                 Color teamColor, String actingId) {
        if (team == null) return;

        // Team header row
        JLabel lbl = new JLabel(title);
        lbl.setForeground(teamColor);
        lbl.setFont(new Font("Arial", Font.BOLD, 11));
        lbl.setBorder(new EmptyBorder(2, 0, 4, 0));
        listPanel.add(lbl);

        for (Champion c : team) {
            String name     = templateName(c.getId());
            boolean acting  = c.getId().equals(actingId);
            JPanel row      = makeUnitRow(name, c, teamColor, acting);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(3));
        }
    }

    private JPanel makeUnitRow(String name, Champion c,
                                Color teamColor, boolean acting) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        Color bg = acting
            ? new Color(60, 120, 60)
            : new Color(35, 35, 55);
        row.setBackground(bg);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                acting ? new Color(100, 200, 100) : new Color(50, 50, 70), 1),
            new EmptyBorder(3, 8, 3, 8)
        ));

        // Name
        String prefix = acting ? ">> " : "   ";
        JLabel nameLabel = new JLabel(prefix + name);
        nameLabel.setForeground(c.isAlive() ? teamColor : new Color(80, 80, 80));
        nameLabel.setFont(new Font("Arial",
            acting ? Font.BOLD : Font.PLAIN, 11));

        // Status: HP or "DEAD"
        JLabel statusLabel;
        if (c.isAlive()) {
            statusLabel = new JLabel(c.getHp() + "hp");
            statusLabel.setForeground(new Color(120, 220, 120));
        } else {
            statusLabel = new JLabel("DEAD");
            statusLabel.setForeground(new Color(120, 50, 50));
        }
        statusLabel.setFont(new Font("Arial", Font.BOLD, 10));

        row.add(nameLabel,   BorderLayout.WEST);
        row.add(statusLabel, BorderLayout.EAST);
        return row;
    }

    private void addSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(60, 60, 90));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(100, 1));
        listPanel.add(Box.createVerticalStrut(4));
        listPanel.add(sep);
        listPanel.add(Box.createVerticalStrut(4));
    }

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