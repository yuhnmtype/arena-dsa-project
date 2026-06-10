package gui.panels;

import gui.core.ReplayController;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ControlPanel extends JPanel {

    private final ReplayController controller;

    private JButton btnReset;
    private JButton btnPrev;
    private JButton btnNext;
    private JButton btnAuto;
    private JButton btnPause;
    private JButton btnResults;
    private JButton btnNewMatch;
    private JSlider speedSlider;
    private JLabel labelStep;
    private JLabel labelRound;
    private JLabel labelAction;

    public ControlPanel(ReplayController controller) {
        this.controller = controller;
        setBackground(new Color(20, 20, 35));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
        registerListener();
    }

    private void buildUI() {
        // ── Labels ──────────────────────────────────────────
        labelStep   = makeLabel("Step: 0 / 0",       Color.WHITE,              13);
        labelRound  = makeLabel("Round: — / 40",      new Color(100, 200, 255), 13);
        labelAction = makeLabel("Action: —",           new Color(200, 200, 200), 12);

        // ── Playback buttons ─────────────────────────────────
        btnReset   = makeButton("<< Reset",   new Color(80,  80,  100));
        btnPrev    = makeButton("< Prev",     new Color(60,  100, 140));
        btnNext    = makeButton("Next >",     new Color(60,  140, 100));
        btnAuto    = makeButton("Auto >>",    new Color(140, 100, 60));
        btnPause   = makeButton("|| Pause",   new Color(140, 60,  60));

        btnReset.addActionListener(e -> controller.reset());
        btnPrev .addActionListener(e -> controller.prev());
        btnNext .addActionListener(e -> controller.next());
        btnAuto .addActionListener(e -> controller.auto());
        btnPause.addActionListener(e -> controller.pause());

        // ── Utility buttons ──────────────────────────────────
        btnResults  = makeButton("Results",    new Color(80,  60,  140));
        btnNewMatch = makeButton("New Match",  new Color(30,  100, 130));

        // Show EndMatchPanel with cost breakdown
        btnResults.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(ControlPanel.this);
            EndMatchPanel.show(frame, controller.getLog());
        });

        // Close GameWindow → reopen setup dialog with new budget
        btnNewMatch.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                ControlPanel.this,
                "Close this match and start a new one?",
                "New Match",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(ControlPanel.this);
                if (frame != null) frame.dispose();
                runner.GUIRunner.showSetup();
            }
        });

        // ── Speed slider ─────────────────────────────────────
        JLabel labelSpeed = makeLabel("Speed:",  new Color(180, 180, 180), 12);
        JLabel labelSlow  = makeLabel("Slow",    new Color(150, 150, 150), 11);
        JLabel labelFast  = makeLabel("Fast",    new Color(150, 150, 150), 11);

        speedSlider = new JSlider(100, 1500, 800);
        speedSlider.setInverted(true); // left = fast, right = slow
        speedSlider.setBackground(new Color(20, 20, 35));
        speedSlider.addChangeListener(e ->
            controller.setSpeed(speedSlider.getValue())
        );

        // ── Playback button row ──────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnPanel.setBackground(new Color(20, 20, 35));
        btnPanel.add(btnReset);
        btnPanel.add(btnPrev);
        btnPanel.add(btnNext);
        btnPanel.add(btnAuto);
        btnPanel.add(btnPause);

        // ── Utility button row ────────────────────────────────
        JPanel utilPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        utilPanel.setBackground(new Color(20, 20, 35));
        utilPanel.add(btnResults);
        utilPanel.add(btnNewMatch);

        // ── Speed row ────────────────────────────────────────
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        speedPanel.setBackground(new Color(20, 20, 35));
        speedPanel.add(labelSpeed);
        speedPanel.add(labelSlow);
        speedPanel.add(speedSlider);
        speedPanel.add(labelFast);

        // ── Add all ──────────────────────────────────────────
        add(Box.createVerticalStrut(6));
        add(center(labelStep));
        add(Box.createVerticalStrut(4));
        add(center(labelRound));
        add(Box.createVerticalStrut(4));
        add(center(labelAction));
        add(Box.createVerticalStrut(10));
        add(btnPanel);
        add(Box.createVerticalStrut(6));
        add(utilPanel);
        add(Box.createVerticalStrut(8));
        add(speedPanel);
        add(Box.createVerticalStrut(6));
    }

    private void registerListener() {
        controller.addListener((entry, step, total) -> {
            int grp   = controller.getCurrentGroup() + 1; // 1-indexed display
            int grpTot = controller.getTotalGroups();
            labelStep.setText("Step: " + grp + " / " + grpTot);
            if (entry != null) {
                labelRound.setText("Round: " + entry.round + " / 40");
                labelAction.setText(entry.team + " " + entry.championName
                    + " \u2192 " + entry.actionType);
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel makeLabel(String text, Color color, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.PLAIN, size));
        return label;
    }

    private JPanel center(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setBackground(new Color(20, 20, 35));
        p.add(comp);
        return p;
    }
}