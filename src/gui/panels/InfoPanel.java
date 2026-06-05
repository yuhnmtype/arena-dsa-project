package gui.panels;

import engine.BattleLogEntry;
import gui.core.ReplayController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class InfoPanel extends JPanel {

    private JLabel labelRound;
    private JLabel labelActing;
    private JLabel labelAction;
    private JLabel labelFrom;
    private JLabel labelHP;
    private JLabel labelBFS;

    public InfoPanel(ReplayController controller) {
        setBackground(new Color(25, 25, 40));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 90), 1),
            new EmptyBorder(12, 14, 12, 14)
        ));
        setLayout(new GridLayout(7, 1, 0, 6));
        buildUI();

        controller.addListener((entry, step, total) -> updateInfo(entry));
    }

    private void buildUI() {
        JLabel header = makeLabel("ACTION INFO", new Color(100, 180, 255), 12, true);
        labelRound   = makeLabel("Round: —", Color.WHITE, 13, false);
        labelActing  = makeLabel("Acting: —", new Color(150, 255, 150), 13, false);
        labelAction  = makeLabel("Action: —", new Color(255, 200, 100), 13, false);
        labelFrom    = makeLabel("Position: —", new Color(200, 200, 200), 12, false);
        labelHP      = makeLabel("HP: —", new Color(255, 120, 120), 12, false);
        labelBFS     = makeLabel("BFS: —", new Color(100, 220, 255), 12, false);

        add(header);
        add(labelRound);
        add(labelActing);
        add(labelAction);
        add(labelFrom);
        add(labelHP);
        add(labelBFS);
    }

    private void updateInfo(BattleLogEntry entry) {
        if (entry == null) {
            labelRound.setText("Round: —");
            labelActing.setText("Acting: —");
            labelAction.setText("Action: —");
            labelFrom.setText("Position: —");
            labelHP.setText("HP: —");
            labelBFS.setText("BFS: —");
            return;
        }

        labelRound.setText("Round: " + entry.round + " / 40");
        labelActing.setText("Acting: " + entry.team + " " + entry.championName);
        labelAction.setText("Action: " + entry.actionType);

        if (entry.fromPos != null && entry.toPos != null
                && !entry.fromPos.equals(entry.toPos)) {
            labelFrom.setText("Move: (" + entry.fromPos.row()
                + "," + entry.fromPos.col() + ") → ("
                + entry.toPos.row() + "," + entry.toPos.col() + ")");
        } else if (entry.fromPos != null) {
            labelFrom.setText("At: (" + entry.fromPos.row()
                + "," + entry.fromPos.col() + ")");
        }

        if (entry.hpBefore != entry.hpAfter) {
            labelHP.setText("HP: " + entry.hpBefore
                + " → " + entry.hpAfter
                + " (-" + (entry.hpBefore - entry.hpAfter) + ")");
        } else {
            labelHP.setText("HP: " + entry.hpAfter + " (no change)");
        }

        int bfsSteps = entry.bfsPath != null ? entry.bfsPath.size() : 0;
        if (bfsSteps > 0) {
            labelBFS.setText("BFS path: " + bfsSteps + " steps");
        } else {
            labelBFS.setText("BFS: not used this action");
        }
    }

    private JLabel makeLabel(String text, Color color, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }
}