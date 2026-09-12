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
        long herbivoreSum = 0, carnivoreSum = 0, plantSum = 0;
        int samples = 0;
        boolean herbivoresSurvived = true;
        boolean carnivoresSurvived = true;

        final int totalTicks = 400;
        for (int tick = 1; tick <= totalTicks; tick++) {
            world.tick();
            long plants = world.countOfType(Plant.class);
            long herbivores = world.countOfType(Herbivore.class);
            long carnivores = world.countOfType(Carnivore.class);

            if (tick % 20 == 0) {
                System.out.printf("%d\t%d\t%d\t%d\t%d%n",
                        tick, plants, herbivores, carnivores, world.getOverpopulationEvents());
            }

            plantSum += plants;
            herbivoreSum += herbivores;
            carnivoreSum += carnivores;
            samples++;
            if (herbivores == 0) herbivoresSurvived = false;
            if (carnivores == 0) carnivoresSurvived = false;
        }

        // Summary report: at a glance, did the ecosystem stay balanced for the
        // whole run, or collapse partway through? Useful after tuning
        // constants (e.g. AMBIENT_SEED_CHANCE in World) without having to
        // eyeball 20 rows of the tick-by-tick table above every time.
        System.out.println();
        System.out.println("=== Summary over " + totalTicks + " ticks (seed 42) ===");
        System.out.printf("Average population -- plants: %.1f, herbivores: %.1f, carnivores: %.1f%n",
                plantSum / (double) samples, herbivoreSum / (double) samples, carnivoreSum / (double) samples);
        System.out.println("Herbivores survived the full run: " + herbivoresSurvived);
        System.out.println("Carnivores survived the full run: " + carnivoresSurvived);
        System.out.println("Full-grid (OverpopulationException) events: " + world.getOverpopulationEvents());
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
