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

    public static Herbivore createHerbivore(Position position) {
        return new Herbivore(position, Herbivore.INITIAL_ENERGY,
                new FleeOrSeekStrategy(Carnivore.class, Plant.class));
    }

    public static Carnivore createCarnivore(Position position) {
        return new Carnivore(position, Carnivore.INITIAL_ENERGY,
                new FleeOrSeekStrategy(null, Herbivore.class));
    }
}
