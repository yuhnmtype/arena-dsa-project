package gui.render;

import engine.Champion;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches champion sprite images from assets/images/champions/.
 *
 * File naming: TEMPLATE_SIDE.png  (e.g. "KNIGHT_BLUE.png", "ARCHER_RED.png").
 * Champions carry a unique id of the form "SIDE_TEMPLATE_index" (e.g. "BLUE_KNIGHT_0"),
 * so we extract the template and side to build the file name.
 *
 * If a PNG is missing, a colored-circle fallback with the champion's initial is drawn
 * instead, so the GUI still works before all art is final.
 *
 * Images are cached in a HashMap so each file is read from disk only once.
 */
public class ChampionSprite {

    private static final String DIR = "src/gui/images/champions/";
    private static final int SIZE = 80;

    // cache: file key -> image (null value means "tried and missing", use fallback)
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    /** Returns the sprite for this champion, or null if no PNG exists (caller draws fallback). */
    public static BufferedImage get(Champion c) {
        String template = templateOf(c.getId());
        String side = c.getTeamSide(); // "BLUE" or "RED"
        String key = template + "_" + side;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        BufferedImage img = null;
        try {
            File f = new File(DIR + key + ".png");
            if (f.exists()) {
                img = ImageIO.read(f);
            }
        } catch (Exception e) {
            img = null; // fall through to fallback
        }
        CACHE.put(key, img);
        return img;
    }

    /**
     * Draws the champion at (x, y) in an 80x80 cell. Uses the PNG if available, otherwise a
     * colored circle (blue/red by side) with the champion's initial letter.
     */
    public static void draw(Graphics2D g, Champion c, int x, int y) {
        BufferedImage img = get(c);
        if (img != null) {
            g.drawImage(img, x, y, SIZE, SIZE, null);
            return;
        }
        drawFallback(g, c, x, y);
    }

    private static void drawFallback(Graphics2D g, Champion c, int x, int y) {
        boolean blue = "BLUE".equals(c.getTeamSide());
        Color fill = blue ? new Color(70, 130, 220) : new Color(210, 80, 80);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = 12;
        g.setColor(fill);
        g.fillOval(x + pad, y + pad, SIZE - 2 * pad, SIZE - 2 * pad);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        String letter = templateOf(c.getId()).substring(0, 1);
        int tw = g.getFontMetrics().stringWidth(letter);
        g.drawString(letter, x + SIZE / 2 - tw / 2, y + SIZE / 2 + 8);
    }

    /**
     * "BLUE_FROST_WITCH_0" -> "FROST_WITCH". The id is SIDE_TEMPLATE_index, but the template
     * itself may contain underscores (FROST_WITCH), so we strip the leading side token and the
     * trailing numeric index rather than naively taking one split segment.
     */
    private static String templateOf(String id) {
        if (id == null) return "?";
        String s = id;
        // strip leading "BLUE_" or "RED_"
        if (s.startsWith("BLUE_")) s = s.substring(5);
        else if (s.startsWith("RED_")) s = s.substring(4);
        // strip trailing "_<number>"
        int us = s.lastIndexOf('_');
        if (us > 0 && s.substring(us + 1).matches("\\d+")) {
            s = s.substring(0, us);
        }
        return s.isEmpty() ? id : s;
    }
}
