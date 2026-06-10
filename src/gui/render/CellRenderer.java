package gui.render;

import engine.Champion;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Draws the contents of a single 80x80 board cell: the checkerboard background, a champion
 * sprite (via ChampionSprite), an HP bar, a small mana bar, and a name tag. Dead champions
 * are drawn greyed out with an X.
 *
 * This class only knows how to paint one cell; GamePanel decides which champion (if any)
 * sits in each cell and calls these methods.
 */
public class CellRenderer {

    public static final int CELL = 80;

    private static final Color DARK_A = new Color(40, 42, 54);
    private static final Color DARK_B = new Color(48, 50, 64);
    private static final Color GRID_LINE = new Color(70, 72, 90);

    /** Checkerboard background for an empty cell. */
    public void drawBackground(Graphics2D g, int row, int col, int x, int y) {
        boolean even = (row + col) % 2 == 0;
        g.setColor(even ? DARK_A : DARK_B);
        g.fillRect(x, y, CELL, CELL);
        g.setColor(GRID_LINE);
        g.drawRect(x, y, CELL, CELL);
    }

    /** Draws the champion sprite plus HP/mana bars and name tag at the cell origin (x, y). */
    public void drawChampion(Graphics2D g, Champion c, int x, int y) {
        if (c == null) return;

        if (!c.isAlive()) {
            drawDead(g, c, x, y);
            return;
        }

        // Team colour background — makes BLUE vs RED instantly readable
        boolean isBlue = c.getId().startsWith("BLUE");
        Color teamBg = isBlue
                ? new Color(30, 60, 140, 90)   // blue tint, semi-transparent
                : new Color(140, 30, 30, 90);   // red tint, semi-transparent
        g.setColor(teamBg);
        g.fillRect(x + 2, y + 2, CELL - 4, CELL - 4);

        // Team colour border
        Color teamBorder = isBlue
                ? new Color(60, 120, 220, 180)
                : new Color(220, 60, 60, 180);
        g.setColor(teamBorder);
        g.drawRect(x + 2, y + 2, CELL - 4, CELL - 4);

        ChampionSprite.draw(g, c, x, y);
        drawHpBar(g, c, x, y);
        drawManaBar(g, c, x, y);
        drawNameTag(g, c, x, y);
    }

    private void drawHpBar(Graphics2D g, Champion c, int x, int y) {
        int barW = CELL - 12;
        int barH = 6;
        int bx = x + 6;
        int by = y + 6;
        double frac = Math.max(0, Math.min(1.0, (double) c.getHp() / c.getMaxHp()));

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(bx, by, barW, barH);
        // green -> yellow -> red by remaining fraction
        Color hpColor = frac > 0.5 ? new Color(80, 200, 90)
                      : frac > 0.25 ? new Color(220, 200, 60)
                      : new Color(220, 70, 70);
        g.setColor(hpColor);
        g.fillRect(bx, by, (int) (barW * frac), barH);
        g.setColor(new Color(20, 20, 20));
        g.drawRect(bx, by, barW, barH);
    }

    private void drawManaBar(Graphics2D g, Champion c, int x, int y) {
        if (c.getMaxMana() <= 0) return;
        int barW = CELL - 12;
        int barH = 3;
        int bx = x + 6;
        int by = y + 14; // just below HP bar
        double frac = Math.max(0, Math.min(1.0, (double) c.getMana() / c.getMaxMana()));
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(bx, by, barW, barH);
        g.setColor(new Color(90, 140, 230));
        g.fillRect(bx, by, (int) (barW * frac), barH);
    }

    private void drawNameTag(Graphics2D g, Champion c, int x, int y) {
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        String name = shortName(c.getId());
        int tw = g.getFontMetrics().stringWidth(name);
        int tx = x + CELL / 2 - tw / 2;
        int ty = y + CELL - 6;
        // shadow for readability
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(name, tx + 1, ty + 1);
        g.setColor(Color.WHITE);
        g.drawString(name, tx, ty);
    }

    private void drawDead(Graphics2D g, Champion c, int x, int y) {
        g.setColor(new Color(60, 60, 60, 120));
        g.fillRect(x + 10, y + 10, CELL - 20, CELL - 20);
        g.setColor(new Color(200, 60, 60));
        g.drawLine(x + 20, y + 20, x + CELL - 20, y + CELL - 20);
        g.drawLine(x + CELL - 20, y + 20, x + 20, y + CELL - 20);
    }

    /** "BLUE_KNIGHT_0" -> "KNIGHT". */
    private String shortName(String id) {
        if (id == null) return "";
        String t = id;
        if (t.startsWith("BLUE_")) t = t.substring(5);
        else if (t.startsWith("RED_")) t = t.substring(4);
        int us = t.lastIndexOf('_');
        if (us > 0 && t.substring(us + 1).matches("\\d+")) t = t.substring(0, us);
        return t.isEmpty() ? id : t;
    }
}