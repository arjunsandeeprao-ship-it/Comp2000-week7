package ecosim.exceptions;

import ecosim.Position;

/**
 * Thrown when an organism tries to move to, or be placed at, a
 * {@link Position} that cannot accept it right now: either the position is
 * outside the grid, or it is already occupied by another living organism.
 *
 * <p>This is treated as a normal, recoverable event rather than a bug:
 * with dozens of organisms moving every tick it is completely expected
 * that two of them will occasionally decide to move to the same cell.
 * Callers (see {@code Animal.move}) generally catch this locally and fall
 * back to "stay where you are" rather than letting it propagate.</p>
 */
public class InvalidPositionException extends SimulationException {
    private final Position attempted;

    public InvalidPositionException(Position attempted, String reason) {
        super("Cannot use position " + attempted + ": " + reason);
        this.attempted = attempted;
    }

    public Position getAttemptedPosition() {
        return attempted;
    }
}
