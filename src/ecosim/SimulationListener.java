package ecosim;

/**
 * Observer pattern: anything that wants to react when the {@link World}
 * finishes a tick (the GUI repainting, a console logger printing a row, an
 * extinction alert) implements this and registers itself with
 * {@link World#addListener(SimulationListener)}.
 *
 * <p>Before this existed, {@code SimulationFrame.onTick} had to remember to
 * call {@code world.tick()} <em>and then</em> manually refresh every view
 * that depended on it, and {@code SmokeTest} duplicated its own "after each
 * tick, print a row" logic inline. With the World acting as the subject,
 * it no longer needs to know who is watching -- it just announces "tick N
 * is done" and every observer decides for itself what to do.</p>
 *
 * <p>It is a single-method interface, so it is annotated
 * {@link FunctionalInterface} and observers can be registered as lambdas,
 * e.g. {@code world.addListener(w -> System.out.println(w.getTickCount()));}.</p>
 */
@FunctionalInterface
public interface SimulationListener {

    /** Called by {@link World#tick()} after all organisms have acted and history is recorded. */
    void onTickCompleted(World world);

    /**
     * Called once, at the moment a species' population first drops to zero.
     * Default no-op so lambda-based listeners only have to implement
     * {@link #onTickCompleted(World)}.
     */
    default void onExtinction(World world, Class<? extends Organism> species) {
    }
}
