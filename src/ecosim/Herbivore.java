package ecosim;

import ecosim.exceptions.OverpopulationException;
import ecosim.strategy.MovementStrategy;
import java.awt.Color;
import java.util.List;
import java.util.Optional;

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
        List<Organism> neighbours = world.getNeighbours(getPosition());
        for (Organism neighbour : neighbours) {
            // instanceof check is intentional and narrow: a herbivore's diet
            // is plants only, even though other Edible things (other
            // herbivores are not Edible in this model, but future species
            // could be) might be adjacent.
            if (neighbour instanceof Plant && neighbour.isAlive()) {
                Plant plant = (Plant) neighbour;
                energy += plant.getEnergyValue();
                plant.markConsumed();
                world.removeOrganism(plant);
                return;
            }
        }
    }

    @Override
    protected void tryReproduce(World world) throws OverpopulationException {
        List<Position> freeNeighbours = world.freeNeighbours(getPosition());
        if (freeNeighbours.isEmpty()) {
            return;
        }
        Position birthSite = freeNeighbours.get(world.getRandom().nextInt(freeNeighbours.size()));
        energy -= REPRODUCTION_COST;
        world.addOrganism(OrganismFactory.createHerbivore(birthSite));
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
