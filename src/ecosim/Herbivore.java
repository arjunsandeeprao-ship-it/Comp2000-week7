package ecosim;

import ecosim.exceptions.OverpopulationException;
import ecosim.strategy.MovementStrategy;
import java.awt.Color;
import java.util.List;

/** Eats plants, flees carnivores. See {@link Animal} for the shared tick logic. */
public class Herbivore extends Animal {

    public static final int INITIAL_ENERGY = 40;
    private static final int REPRODUCTION_THRESHOLD = 65;
    private static final int REPRODUCTION_COST = 30;
    private static final int MAX_AGE = 60;

    public Herbivore(Position position, int initialEnergy, MovementStrategy strategy) {
        super(position, initialEnergy, REPRODUCTION_THRESHOLD, MAX_AGE, strategy);
    }

    @Override
    protected void forage(World world) {
        // Stream form of "find the first living adjacent Plant and eat it".
        // getNeighbours() already filters out the dead; the type filter is
        // intentional and narrow -- this species' diet is Plant only.
        world.getNeighbours(getPosition()).stream()
                .filter(Plant.class::isInstance)
                .map(Plant.class::cast)
                .findFirst()
                .ifPresent(plant -> {
                    energy += plant.getEnergyValue();
                    plant.markConsumed();
                    world.removeOrganism(plant);
                });
    }

    @Override
    protected void tryReproduce(World world) throws OverpopulationException {
        List<Position> freeNeighbours = world.freeNeighbours(getPosition());
        if (freeNeighbours.isEmpty()) {
            return;
        }
        Position birthSite = freeNeighbours.get(world.getRandom().nextInt(freeNeighbours.size()));
        energy -= REPRODUCTION_COST;
        world.addOrganism(OrganismFactory.createHerbivore(birthSite, REPRODUCTION_COST));
    }

    @Override
    public Color getColor() {
        return new Color(60, 90, 220);
    }

    @Override
    public char getSymbol() {
        return 'h';
    }
}
