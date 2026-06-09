package gui.render;

import engine.Position;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Set;

/**
 * Draws the BFS overlays on top of the board so the search is visible during replay:
 *
 *   showExplored : light-blue tint on every cell BFS could have expanded (the occupied
 *                  snapshot is drawn faintly so viewers see what blocked the search).
 *   showPath     : yellow tint on the shortest path BFS found (entry.bfsPath).
 *   showNextStep : green outline on the cell the champion actually moves to this step
 *                  (the last cell of bfsPath, since this engine allows any-distance moves).
 *
 * The colors match the roadmap: explored = light blue, path = yellow, next = green.
 */
public class BFSVisualizer {

    private static final int CELL = CellRenderer.CELL;

    private static final Color EXPLORED = new Color(80, 160, 230, 70);   // light blue, transparent
    private static final Color PATH      = new Color(235, 215, 70, 110);  // yellow, transparent
    private static final Color NEXT      = new Color(80, 220, 110, 180);  // green

    /** Faint tint on cells that were occupied (the obstacles BFS had to route around). */
    public void showExplored(Graphics2D g, Set<Position> occupied, int originX, int originY) {
        if (occupied == null) return;
        g.setColor(EXPLORED);
        for (Position p : occupied) {
            int x = originX + p.col() * CELL;
            int y = originY + p.row() * CELL;
            g.fillRect(x, y, CELL, CELL);
        }
    }

    /** Yellow tint along the shortest path BFS returned for this step. */
    public void showPath(Graphics2D g, List<Position> path, int originX, int originY) {
        if (path == null || path.isEmpty()) return;
        g.setColor(PATH);
        for (Position p : path) {
            int x = originX + p.col() * CELL;
            int y = originY + p.row() * CELL;
            g.fillRect(x, y, CELL, CELL);
        }
    }

    /** Green outline on the destination cell (the move target this step). */
    public void showNextStep(Graphics2D g, List<Position> path, int originX, int originY) {
        if (path == null || path.isEmpty()) return;
        Position next = path.get(path.size() - 1); // farthest cell = where the unit moves
        int x = originX + next.col() * CELL;
        int y = originY + next.row() * CELL;
        g.setColor(NEXT);
        g.drawRect(x + 2, y + 2, CELL - 4, CELL - 4);
        g.drawRect(x + 3, y + 3, CELL - 6, CELL - 6);
    }
}
