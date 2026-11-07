package ecosim;

import ecosim.strategy.FleeOrSeekStrategy;

/**
 * Simple Factory: centralises the "how do you build a fresh organism of
 * species X" knowledge (starting energy, which movement strategy it gets,
 * what it flees and seeks) in one place.
 *
 * <p>Without this, the code that spawns a newborn ({@code Herbivore.
 * tryReproduce}, {@code Carnivore.tryReproduce}) and the code that seeds
 * the initial population ({@code SimulationFrame}) would each need to know
 * these construction details, and they would drift apart the moment
 * someone tuned one constant but not the other. Centralising it here means
 * "what a baby herbivore looks like" is defined exactly once.</p>
 */
public final class OrganismFactory {

    private OrganismFactory() {
        // static utility -- never instantiated
    }

    public static Plant createPlant(Position position) {
        return new Plant(position);
    }

    /** A herbivore for the initial population, with the standard starting energy. */
    public static Herbivore createHerbivore(Position position) {
        return createHerbivore(position, Herbivore.INITIAL_ENERGY);
    }

    /**
     * A newborn herbivore carrying exactly {@code energy} -- the amount its
     * parent paid to reproduce. Energy is conserved across birth: earlier
     * versions gave every newborn the full INITIAL_ENERGY regardless of what
     * the parent paid, which silently created energy from nothing on every
     * birth and drove the boom-and-crash collapse (see BatchExperiment).
     */
    public static Herbivore createHerbivore(Position position, int energy) {
        return new Herbivore(position, energy,
                new FleeOrSeekStrategy(Carnivore.class, Plant.class));
    }

    /** A carnivore for the initial population, with the standard starting energy. */
    public static Carnivore createCarnivore(Position position) {
        return createCarnivore(position, Carnivore.INITIAL_ENERGY);
    }

    /** A newborn carnivore carrying the energy its parent paid (see createHerbivore). */
    public static Carnivore createCarnivore(Position position, int energy) {
        return new Carnivore(position, energy,
                new FleeOrSeekStrategy(null, Herbivore.class));
    }
}
