package ecosim;

import ecosim.exceptions.OverpopulationException;
import ecosim.strategy.MovementStrategy;
import java.awt.Color;
import java.util.List;

/** Eats herbivores, has no predator of its own. See {@link Animal} for shared tick logic. */
public class Carnivore extends Animal {

    public static final int INITIAL_ENERGY = 55;
    private static final int REPRODUCTION_THRESHOLD = 90;
    private static final int REPRODUCTION_COST = 45;
    private static final int MAX_AGE = 80;

    public Carnivore(Position position, int initialEnergy, MovementStrategy strategy) {
        super(position, initialEnergy, REPRODUCTION_THRESHOLD, MAX_AGE, strategy);
    }

    @Override
    protected void forage(World world) {
        List<Organism> neighbours = world.getNeighbours(getPosition());
        for (Organism neighbour : neighbours) {
            if (neighbour instanceof Herbivore && neighbour.isAlive()) {
                Herbivore prey = (Herbivore) neighbour;
                energy += prey.getEnergyValue();
                prey.markConsumed();
                world.removeOrganism(prey);
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
        world.addOrganism(OrganismFactory.createCarnivore(birthSite));
    }

    @Override
    public Color getColor() {
        return new Color(210, 40, 40);
    }

    @Override
    public char getSymbol() {
        return 'C';
    }
}
