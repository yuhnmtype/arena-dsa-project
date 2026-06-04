package bot;

import engine.Grid;
import engine.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * BFSPathfinder - breadth-first shortest-path search on the 8x8 battle grid.
 *
 * The grid is an unweighted graph: each cell is a vertex, each orthogonal step is an
 * edge of equal cost. BFS is the right tool for an unweighted graph because the first
 * time it reaches a cell it has reached it by a shortest path (fewest steps), so no
 * weighting (Dijkstra) or heuristic (A*) is required.
 *
 *   Time:  O(V + E). On this grid V = ROWS * COLS = 64 cells, and every cell has at
 *          most 4 neighbours, so E <= 4V = 256. Each cell is enqueued and expanded at
 *          most once, giving a linear bound in the size of the graph.
 *   Space: O(V) for the visited set, the parent map, and the queue.
 *
 * Why BFS over a naive "step straight toward the target":
 *   The engine forbids moving onto or through an occupied cell (Grid.isValid). A naive
 *   straight-line move therefore stalls whenever a unit stands in the way - the move is
 *   rejected and the turn is wasted. BFS explores around blocked cells and is guaranteed
 *   to find a route if one exists, so it never wastes a turn to a blocked straight line.
 */
public final class BFSPathfinder {

    private BFSPathfinder() { } // static utility class

    /**
     * Shortest path (as a list of cells, excluding the start) from 'start' to 'target'.
     * 'target' itself is treated as the destination; pass an open cell. Returns an empty
     * list if no path exists. Useful when the full route is needed.
     */
    public static List<Position> findPath(Position start, Position target,
                                          Grid grid, Set<Position> occupied) {
        if (start.equals(target)) return new ArrayList<>();

        Queue<Position> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        Map<Position, Position> parent = new HashMap<>();

        queue.add(start);
        visited.add(start);
        boolean found = false;

        while (!queue.isEmpty()) {
            Position cur = queue.poll();
            if (cur.equals(target)) { found = true; break; }
            for (Position nb : grid.getNeighbors(cur)) {
                if (!grid.isInBounds(nb)) continue;
                if (visited.contains(nb)) continue;
                // The target cell is allowed as a destination; any other occupied cell is blocked.
                if (occupied.contains(nb) && !nb.equals(target)) continue;
                visited.add(nb);
                parent.put(nb, cur);
                queue.add(nb);
            }
        }
        if (!found) return new ArrayList<>();
        return reconstruct(parent, start, target);
    }

    /**
     * Best cell to MOVE to this turn when approaching 'goal'. Finds the shortest path to an
     * open cell orthogonally adjacent to 'goal' and returns the farthest cell along that
     * path. Because this engine allows a unit to move any distance to a reachable open cell
     * in one turn, the farthest reachable cell is the destination. Returns 'start' unchanged
     * if no route exists (the caller can then wait).
     */
    public static Position advanceToward(Position start, Position goal,
                                         Grid grid, Set<Position> occupied) {
        Set<Position> goalCells = new HashSet<>();
        for (Position n : grid.getNeighbors(goal)) {
            if (grid.isInBounds(n) && !occupied.contains(n)) goalCells.add(n);
        }
        if (goalCells.isEmpty()) return start;

        Queue<Position> queue = new ArrayDeque<>();
        Set<Position> visited = new HashSet<>();
        Map<Position, Position> parent = new HashMap<>();

        queue.add(start);
        visited.add(start);
        Position reached = null;

        while (!queue.isEmpty()) {
            Position cur = queue.poll();
            if (goalCells.contains(cur)) { reached = cur; break; }
            for (Position nb : grid.getNeighbors(cur)) {
                if (!grid.isInBounds(nb)) continue;
                if (visited.contains(nb)) continue;
                if (occupied.contains(nb)) continue; // cannot path through a unit
                visited.add(nb);
                parent.put(nb, cur);
                queue.add(nb);
            }
        }
        if (reached == null) return start;
        return reached; // farthest reachable cell on the shortest path; engine allows the jump
    }

    /** Walk the parent map backwards from target to start, returning cells in forward order. */
    private static List<Position> reconstruct(Map<Position, Position> parent,
                                              Position start, Position target) {
        List<Position> path = new ArrayList<>();
        Position cur = target;
        while (cur != null && !cur.equals(start)) {
            path.add(cur);
            cur = parent.get(cur);
        }
        Collections.reverse(path);
        return path;
    }
}
