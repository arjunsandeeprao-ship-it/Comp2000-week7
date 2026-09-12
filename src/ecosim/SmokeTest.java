package ecosim;

import ecosim.exceptions.OverpopulationException;

/**
 * A headless, GUI-free smoke test: seeds a world identical in scale to
 * {@code SimulationFrame} and runs several hundred ticks, printing
 * population snapshots. Not a unit test framework (JUnit was intentionally
 * left out to keep the "no external libraries" requirement trivially true)
 * but enough to confirm the ecosystem behaves sensibly (plants recover,
 * herbivores and carnivores rise and fall, nothing throws unexpectedly)
 * without needing eyes on the Swing window. Useful during development and
 * handy evidence in the log book / commit history.
 */
public final class SmokeTest {
    public static void main(String[] args) throws OverpopulationException {
        World world = new World(60, 40, 42L);
        for (int i = 0; i < 140; i++) {
            world.addOrganism(OrganismFactory.createPlant(randomFree(world)));
        }
        for (int i = 0; i < 40; i++) {
            world.addOrganism(OrganismFactory.createHerbivore(randomFree(world)));
        }
        for (int i = 0; i < 10; i++) {
            world.addOrganism(OrganismFactory.createCarnivore(randomFree(world)));
        }

        System.out.println("tick\tplants\therbivores\tcarnivores\toverpopEvents");
        for (int tick = 1; tick <= 400; tick++) {
            world.tick();
            if (tick % 20 == 0) {
                System.out.printf("%d\t%d\t%d\t%d\t%d%n",
                        tick,
                        world.countOfType(Plant.class),
                        world.countOfType(Herbivore.class),
                        world.countOfType(Carnivore.class),
                        world.getOverpopulationEvents());
            }
        }
    }

    private static Position randomFree(World world) {
        Position p;
        do {
            p = new Position(world.getRandom().nextInt(world.getWidth()),
                    world.getRandom().nextInt(world.getHeight()));
        } while (!world.isFree(p));
        return p;
    }
}
