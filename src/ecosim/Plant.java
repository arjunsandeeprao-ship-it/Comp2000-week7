package ecosim;

import ecosim.exceptions.OverpopulationException;
import java.awt.Color;
import java.util.List;

/**
 * A stationary food source. Plants do not move and do not eat anything;
 * they simply grow, and once mature they may spread a copy of themselves
 * into a free neighbouring cell.
 */
public class Plant extends Organism implements Edible {

    public static final int MATURITY_AGE = 4;
    private static final double SPREAD_CHANCE = 0.35;

    public Plant(Position position) {
        super(position);
    }

    @Override
    public void act(World world) throws OverpopulationException {
        incrementAge();
        if (getAge() >= MATURITY_AGE && world.getRandom().nextDouble() < SPREAD_CHANCE) {
            trySpread(world);
        }
    }

    private void trySpread(World world) {
        List<Position> freeNeighbours = world.freeNeighbours(getPosition());
        if (freeNeighbours.isEmpty()) {
            return; // no room to spread this tick -- not an error, just try again later
        }
        Position target = freeNeighbours.get(world.getRandom().nextInt(freeNeighbours.size()));
        try {
            world.addOrganism(OrganismFactory.createPlant(target));
        } catch (OverpopulationException e) {
            // Recoverable and expected: another organism claimed the last free
            // cell between us computing freeNeighbours and now. Caught here,
            // locally, because a plant failing to spread is not exceptional --
            // see OverpopulationException's Javadoc for the contrast with how
            // World.tick() and SimulationFrame handle the same exception.
        }
    }

    /** Energy value grows as the plant matures, rewarding herbivores that let plants grow. */
    @Override
    public int getEnergyValue() {
        return 8 + Math.min(getAge(), MATURITY_AGE) * 3;
    }

    @Override
    public void markConsumed() {
        die();
    }

    @Override
    public Color getColor() {
        int greenLevel = Math.min(255, 90 + getAge() * 30);
        return new Color(30, greenLevel, 30);
    }

    @Override
    public char getSymbol() {
        return '*';
    }
}
