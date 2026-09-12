package ecosim.exceptions;

/**
 * Base checked exception for every problem that can occur while the
 * simulation is running. It exists so that code which genuinely does not
 * care *which* simulation-specific thing went wrong (for example a very
 * defensive top-level handler) can catch one type instead of an
 * ever-growing list of siblings.
 *
 * <p>Deliberately a <em>checked</em> exception (extends {@link Exception},
 * not {@link RuntimeException}): a failure to place or move an organism is
 * an expected, recoverable part of running a crowded grid, not a
 * programming error. Making it checked forces every caller in the model
 * layer to make a conscious decision about how to recover, instead of
 * letting a bad tick silently propagate all the way up and crash the
 * Swing event thread.</p>
 */
public abstract class SimulationException extends Exception {
    protected SimulationException(String message) {
        super(message);
    }
}
