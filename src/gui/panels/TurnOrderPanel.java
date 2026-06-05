package gui.panels;

import engine.BattleLog;
import engine.BattleLogEntry;
import engine.Champion;
import gui.core.ReplayController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TurnOrderPanel extends JPanel {

    private final BattleLog log;
    private JPanel listPanel;
    private String currentActingId = "";

    public TurnOrderPanel(ReplayController controller) {
        this.log = controller.getLog();
        setBackground(new Color(25, 25, 40));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 90), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        setLayout(new BorderLayout());
        buildUI();

        controller.addListener((entry, step, total) -> updateOrder(entry));
    }

    private void buildUI() {
        JLabel header = new JLabel("TURN ORDER  (SPD ↓)");
        header.setForeground(new Color(100, 180, 255));
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        listPanel = new JPanel();
        listPanel.setBackground(new Color(25, 25, 40));
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        add(header, BorderLayout.NORTH);
        add(listPanel, BorderLayout.CENTER);
    }

    private void updateOrder(BattleLogEntry entry) {
        if (entry == null) return;
        currentActingId = entry.championId;

        // Collect alive champions từ initial teams
        List<Champion> all = new ArrayList<>();
        if (log.initialBlueTeam != null)
            all.addAll(log.initialBlueTeam);
        if (log.initialRedTeam != null)
            all.addAll(log.initialRedTeam);

        // Sort by SPD descending
        all.sort((a, b) -> b.getSpeed() - a.getSpeed());

        listPanel.removeAll();

        for (Champion c : all) {
            String[] parts = c.getId().split("_");
            String name = parts.length >= 2 ? parts[parts.length - 2] : c.getId();
            String team = c.getId().startsWith("BLUE") ? "BLUE" : "RED";
            boolean isActing = c.getId().equals(currentActingId);

            JPanel row = makeRow(name, team, c.getSpeed(), isActing);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(4));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel makeRow(String name, String team,
                            int spd, boolean isActing) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        // Colors
        Color bg = isActing
            ? new Color(60, 120, 60)
            : new Color(35, 35, 55);
        Color teamColor = team.equals("BLUE")
            ? new Color(80, 140, 255)
            : new Color(255, 80, 80);

        row.setBackground(bg);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                isActing ? new Color(100, 200, 100) : new Color(50, 50, 70), 1),
            new EmptyBorder(4, 8, 4, 8)
        ));

        // Acting indicator
        String prefix = isActing ? "► " : "  ";

        JLabel nameLabel = new JLabel(prefix + name);
        nameLabel.setForeground(teamColor);
        nameLabel.setFont(new Font("Arial",
            isActing ? Font.BOLD : Font.PLAIN, 12));

        JLabel spdLabel = new JLabel("⚡" + spd);
        spdLabel.setForeground(new Color(255, 220, 80));
        spdLabel.setFont(new Font("Arial", Font.BOLD, 12));

        row.add(nameLabel, BorderLayout.WEST);
        row.add(spdLabel, BorderLayout.EAST);

        return row;
    }
}