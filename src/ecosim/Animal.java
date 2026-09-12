package ecosim;

import ecosim.exceptions.InvalidPositionException;
import ecosim.exceptions.OverpopulationException;
import ecosim.strategy.MovementStrategy;

/**
 * Common base for {@link Herbivore} and {@link Carnivore}: anything that
 * moves, spends energy, eats, ages and can starve to death.
 *
 * <p>{@link #act(World)} is a <b>Template Method</b> (deliberately marked
 * {@code final} -- subclasses may not override the sequence of steps,
 * only the two hooks {@link #forage(World)} and
 * {@link #tryReproduce(World)}). Every animal ages, spends upkeep energy,
 * moves and then gets a chance to forage and reproduce in exactly the same
 * order; only <em>what counts as food</em> and <em>what a baby looks
 * like</em> differ between species, and those are exactly the two things
 * left abstract. This avoids Herbivore and Carnivore each re-implementing
 * (and risking drifting out of sync on) the ageing/starvation/movement
 * bookkeeping.</p>
 */
public abstract class Animal extends Organism implements Edible {

    private static final int UPKEEP_COST = 1;

    protected int energy;
    private final int reproductionThreshold;
    private final int maxAge;
    private final MovementStrategy movementStrategy;

    protected Animal(Position position, int initialEnergy, int reproductionThreshold,
                      int maxAge, MovementStrategy movementStrategy) {
        super(position);
        this.energy = initialEnergy;
        this.reproductionThreshold = reproductionThreshold;
        this.maxAge = maxAge;
        this.movementStrategy = movementStrategy;
    }

    @Override
    public final void act(World world) throws OverpopulationException {
        incrementAge();
        energy -= UPKEEP_COST;

        if (energy <= 0 || getAge() > maxAge) {
            die();
            return;
        }

        move(world);
        if (isAlive()) {
            forage(world);
        }
        if (isAlive() && energy >= reproductionThreshold) {
            tryReproduce(world);
        }
    }

    private void move(World world) {
        Position target = movementStrategy.nextMove(this, world);
        if (target.equals(getPosition())) {
            return;
        }
        try {
            world.moveOrganism(this, target);
        } catch (InvalidPositionException e) {
            // Recoverable: the strategy's chosen cell was taken by another
            // organism earlier in this tick's iteration, or is out of
            // bounds. Standing still for one tick is an acceptable
            // fallback, so we swallow this locally rather than letting it
            // interrupt act() -- see worksheet-7.md 3.2 for the reasoning.
        }
    }

    /** Species-specific: look for adjacent food and eat it if present. */
    protected abstract void forage(World world);

    /** Species-specific: attempt to place a newborn nearby, if energy allows. */
    protected abstract void tryReproduce(World world) throws OverpopulationException;

    public int getEnergy() {
        return energy;
    }

    @Override
    public int getEnergyValue() {
        // A predator gains roughly half of a prey animal's remaining energy.
        return Math.max(5, energy / 2);
    }

    @Override
    public void markConsumed() {
        die();
    }
}
