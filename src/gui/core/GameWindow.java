package gui.core;

import engine.BattleLog;
import gui.panels.ControlPanel;
import gui.panels.InfoPanel;
import gui.panels.StatsPanel;
import gui.panels.TurnOrderPanel;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Main application window (1200x800). Left side: the 8x8 GamePanel (the board). Right side:
 * the info / turn-order / stats panels stacked vertically, with the playback ControlPanel
 * along the bottom. A single ReplayController drives the whole window: every panel registers
 * as a listener, so pressing Next/Prev/Auto updates the board and all panels together.
 */
public class GameWindow extends JFrame {

    private static final Color BG = new Color(18, 19, 28);

    public GameWindow(BattleLog log) {
        super("Arena Bot - Battle Replay");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        // One controller shared by every panel.
        ReplayController controller = new ReplayController(log);

        // ── LEFT: the board ───────────────────────────────
        GamePanel board = new GamePanel(controller);
        JPanel boardWrap = new JPanel(new java.awt.BorderLayout());
        boardWrap.setBackground(BG);
        boardWrap.setPreferredSize(new Dimension(660, 800));
        // padding around the board so it sits nicely; board keeps its 640x640 preferred size
        boardWrap.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
        boardWrap.add(board, java.awt.BorderLayout.NORTH);
        add(boardWrap, BorderLayout.WEST);

        // ── RIGHT: info + turn order + stats stacked ──────
        JPanel right = new JPanel();
        right.setBackground(BG);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(520, 800));

        right.add(new InfoPanel(controller));
        right.add(new TurnOrderPanel(controller));
        right.add(new StatsPanel(controller));

        JScrollPane rightScroll = new JScrollPane(right);
        rightScroll.setBorder(null);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(rightScroll, BorderLayout.CENTER);

        // ── BOTTOM: playback controls ─────────────────────
        add(new ControlPanel(controller), BorderLayout.SOUTH);

        // Initialize all panels to step 0.
        controller.reset();
    }
}
