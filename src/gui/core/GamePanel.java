package gui.core;

import engine.BattleLog;
import engine.BattleLogEntry;
import engine.Champion;
import engine.Position;
import gui.render.BFSVisualizer;
import gui.render.CellRenderer;
import bot.BotAction;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

/**
 * The 8x8 board. Listens to the ReplayController; on each step it reconstructs the board
 * state (every champion's position and HP at that step) by replaying the log from the
 * initial team layout up to the current step, then paints the grid, the champions, and the
 * BFS overlays for the champion acting on the current step.
 *
 * Each log entry records only ONE champion's action, so to draw the whole board we replay
 * all entries up to the current step. We keep the live HP/mana/position in a small local
 * RenderUnit per champion (rather than mutating engine Champions), so this stays within the
 * GUI layer and needs no engine setters.
 */
public class GamePanel extends JPanel implements ReplayController.StepListener {

    private static final int GRID = 8;
    private static final int CELL = CellRenderer.CELL;
    private static final int ORIGIN = 0;

    /** Lightweight render-only snapshot of a champion at the current step. */
    private static final class RenderUnit {
        final Champion base;   // immutable stats + side + id (positions/hp tracked separately)
        Position pos;
        int hp;
        int mana;
        boolean alive;
        RenderUnit(Champion c) {
            this.base = c;
            this.pos = c.getPosition();
            this.hp = c.getHp();
            this.mana = c.getMana();
            this.alive = c.isAlive();
        }
    }

    private final BattleLog log;
    private final CellRenderer cellRenderer = new CellRenderer();
    private final BFSVisualizer bfs = new BFSVisualizer();

    private final Map<String, RenderUnit> state = new HashMap<>();
    private BattleLogEntry currentEntry;

    public GamePanel(ReplayController controller) {
        this.log = controller.getLog();
        Dimension boardSize = new Dimension(GRID * CELL, GRID * CELL);
        setPreferredSize(boardSize);
        setMinimumSize(boardSize);
        setMaximumSize(boardSize);
        setBackground(new Color(28, 30, 40));
        controller.addListener(this);
        rebuildState(0);
    }

    @Override
    public void onStepChanged(BattleLogEntry entry, int step, int total) {
        this.currentEntry = entry;
        rebuildState(step);
        repaint();
    }

    private void rebuildState(int step) {
        state.clear();
        if (log.initialBlueTeam == null || log.initialRedTeam == null) return;

        for (Champion c : log.initialBlueTeam) state.put(c.getId(), new RenderUnit(c));
        for (Champion c : log.initialRedTeam)  state.put(c.getId(), new RenderUnit(c));

        for (int i = 0; i <= step && i < log.totalSteps(); i++) {
            BattleLogEntry e = log.getEntry(i);
            if (e == null) continue;
            RenderUnit actor = state.get(e.championId);
            if (actor != null) {
                if (e.actionType == BotAction.Type.MOVE && e.toPos != null) {
                    actor.pos = e.toPos;
                }
                actor.hp = e.hpAfter;
                actor.mana = e.manaAfter;
                actor.alive = e.hpAfter > 0;
            }
            if (e.targetId != null) {
                RenderUnit target = state.get(e.targetId);
                if (target != null) {
                    target.hp = e.targetHpAfter;
                    target.alive = e.targetHpAfter > 0;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int r = 0; r < GRID; r++) {
            for (int c = 0; c < GRID; c++) {
                cellRenderer.drawBackground(g, r, c, ORIGIN + c * CELL, ORIGIN + r * CELL);
            }
        }

        if (currentEntry != null) {
            bfs.showExplored(g, currentEntry.occupiedSnapshot, ORIGIN, ORIGIN);
            bfs.showPath(g, currentEntry.bfsPath, ORIGIN, ORIGIN);
            bfs.showNextStep(g, currentEntry.bfsPath, ORIGIN, ORIGIN);
        }

        for (RenderUnit u : state.values()) {
            if (u.pos == null) continue;
            int x = ORIGIN + u.pos.col() * CELL;
            int y = ORIGIN + u.pos.row() * CELL;
            drawUnit(g, u, x, y);
        }

        if (currentEntry != null && currentEntry.fromPos != null) {
            int x = ORIGIN + currentEntry.fromPos.col() * CELL;
            int y = ORIGIN + currentEntry.fromPos.row() * CELL;
            g.setColor(new Color(255, 255, 255, 90));
            g.drawRect(x + 1, y + 1, CELL - 2, CELL - 2);
        }
    }

    /** Draw one render unit: sprite + HP/mana bars, or a greyed-out X if dead. */
    private void drawUnit(Graphics2D g, RenderUnit u, int x, int y) {
        if (!u.alive) {
            g.setColor(new Color(60, 60, 60, 120));
            g.fillRect(x + 10, y + 10, CELL - 20, CELL - 20);
            g.setColor(new Color(200, 60, 60));
            g.drawLine(x + 20, y + 20, x + CELL - 20, y + CELL - 20);
            g.drawLine(x + CELL - 20, y + 20, x + 20, y + CELL - 20);
            return;
        }
        gui.render.ChampionSprite.draw(g, u.base, x, y);
        drawHpBar(g, u, x, y);
        drawManaBar(g, u, x, y);
        drawNameTag(g, u.base.getId(), x, y);
    }

    private void drawHpBar(Graphics2D g, RenderUnit u, int x, int y) {
        int barW = CELL - 12, barH = 6, bx = x + 6, by = y + 6;
        double frac = Math.max(0, Math.min(1.0, (double) u.hp / u.base.getMaxHp()));
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(bx, by, barW, barH);
        Color hp = frac > 0.5 ? new Color(80, 200, 90)
                 : frac > 0.25 ? new Color(220, 200, 60) : new Color(220, 70, 70);
        g.setColor(hp);
        g.fillRect(bx, by, (int) (barW * frac), barH);
        g.setColor(new Color(20, 20, 20));
        g.drawRect(bx, by, barW, barH);
    }

    private void drawManaBar(Graphics2D g, RenderUnit u, int x, int y) {
        if (u.base.getMaxMana() <= 0) return;
        int barW = CELL - 12, barH = 3, bx = x + 6, by = y + 14;
        double frac = Math.max(0, Math.min(1.0, (double) u.mana / u.base.getMaxMana()));
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(bx, by, barW, barH);
        g.setColor(new Color(90, 140, 230));
        g.fillRect(bx, by, (int) (barW * frac), barH);
    }

    private String templateOf(String id) {
        if (id == null) return "";
        String t = id;
        if (t.startsWith("BLUE_")) t = t.substring(5);
        else if (t.startsWith("RED_")) t = t.substring(4);
        int us = t.lastIndexOf('_');
        if (us > 0 && t.substring(us + 1).matches("\\d+")) t = t.substring(0, us);
        return t.isEmpty() ? id : t;
    }

    private void drawNameTag(Graphics2D g, String id, int x, int y) {
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
        String name = templateOf(id);
        int tw = g.getFontMetrics().stringWidth(name);
        int tx = x + CELL / 2 - tw / 2, ty = y + CELL - 6;
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(name, tx + 1, ty + 1);
        g.setColor(Color.WHITE);
        g.drawString(name, tx, ty);
    }
}
