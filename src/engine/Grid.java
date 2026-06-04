package engine;

import java.util.HashSet;
import java.util.Set;

public class Grid {
    public static final int ROWS = 8;
    public static final int COLS = 8;

    // HashSet: O(1) lookup vs array scan O(n)
    private final Set<Position> occupied = new HashSet<>();

    public boolean isOccupied(Position p) {
        return occupied.contains(p);
    }

    public boolean isInBounds(Position p) {
        return p.row() >= 0 && p.row() < ROWS
            && p.col() >= 0 && p.col() < COLS;
    }

    public boolean isValid(Position p) {
        return isInBounds(p) && !isOccupied(p);
    }

    public void occupy(Position p)  { occupied.add(p); }
    public void vacate(Position p)  { occupied.remove(p); }

    public void move(Position from, Position to) {
        vacate(from);
        occupy(to);
    }

    public Position[] getNeighbors(Position p) {
        return new Position[]{
            new Position(p.row()-1, p.col()),
            new Position(p.row()+1, p.col()),
            new Position(p.row(),   p.col()-1),
            new Position(p.row(),   p.col()+1)
        };
    }
}