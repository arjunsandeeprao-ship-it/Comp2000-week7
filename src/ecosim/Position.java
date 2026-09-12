package ecosim;

import java.util.Objects;

/**
 * An immutable grid coordinate.
 *
 * <p>Immutability is the encapsulation decision worth noting here: every
 * field is {@code final} and every "mutating" operation ({@link #translate})
 * returns a brand-new {@code Position} rather than changing this one. That
 * means a {@code Position} handed out by {@code Organism.getPosition()} can
 * never be modified by the caller to secretly move the organism -- the only
 * way to move an organism is through {@code World.moveOrganism}, which is
 * exactly where we want move-legality (bounds checks, collision checks) to
 * be enforced.</p>
 */
public final class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /** Returns a new Position offset from this one; this instance is untouched. */
    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /** Chebyshev (chessboard) distance -- matches 8-directional adjacency used by World. */
    public int distanceTo(Position other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
