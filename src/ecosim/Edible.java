package ecosim;

/**
 * Something another organism can eat. Only {@code Plant} and the two
 * {@code Animal} subclasses implement this -- eating is expressed as an
 * interface rather than a check against a concrete class so that a
 * predator's forage logic reads as "find something Edible nearby" instead
 * of a chain of {@code instanceof Herbivore || instanceof Plant} checks.
 */
public interface Edible {
    /** Energy a predator gains from consuming this organism. */
    int getEnergyValue();

    /** Marks this organism as consumed (it dies immediately). */
    void markConsumed();
}
