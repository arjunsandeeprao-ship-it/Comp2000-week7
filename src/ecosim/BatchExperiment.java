package ecosim;

import ecosim.exceptions.OverpopulationException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Headless "how robust is this ecosystem?" experiment: runs the same
 * simulation from many different random seeds <b>in parallel</b> and reports
 * how often each species survives, and when extinctions tend to happen.
 *
 * <p>A single run can't tell you whether a tuning change made the ecosystem
 * more stable or just got lucky with one seed. Running 200 seeds one after
 * another is slow; but every run is completely independent (its own
 * {@link World}, its own {@link java.util.Random}, no static mutable state),
 * which makes it an ideal fit for a parallel stream.</p>
 *
 * <p><b>Thread safety:</b> nothing is shared between threads. Each seed
 * builds and mutates only its own World inside {@link #runOne(long)}, and the
 * results are combined only by stream collectors ({@code toList},
 * {@code groupingBy}, {@code averagingLong}), which merge per-thread
 * partial results themselves. There is no {@code synchronized}, no shared
 * counter and no {@code forEach} writing into an outside collection.</p>
 *
 * <p>Usage: {@code java -cp bin ecosim.BatchExperiment [runs] [ticks]}. Add
 * {@code -Decosim.migration=0} to switch off migration and compare.</p>
 */
public final class BatchExperiment {

    private static final int DEFAULT_RUNS = 100;
    private static final int DEFAULT_TICKS = 600;

    private BatchExperiment() {
    }

    /** Outcome of one seeded run. Immutable, so it is safe to hand between threads. */
    static final class RunResult {
        final long seed;
        final List<Long> herbivoreCrashes;   // ticks at which herbivores hit zero
        final List<Long> carnivoreCrashes;   // ticks at which carnivores hit zero
        final double coexistenceFraction;    // share of ticks with all three species alive
        final boolean animalsAliveAtEnd;

        RunResult(long seed, List<Long> herbivoreCrashes, List<Long> carnivoreCrashes,
                  double coexistenceFraction, boolean animalsAliveAtEnd) {
            this.seed = seed;
            this.herbivoreCrashes = List.copyOf(herbivoreCrashes);
            this.carnivoreCrashes = List.copyOf(carnivoreCrashes);
            this.coexistenceFraction = coexistenceFraction;
            this.animalsAliveAtEnd = animalsAliveAtEnd;
        }
    }

    public static void main(String[] args) {
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_RUNS;
        int ticks = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_TICKS;

        long start = System.nanoTime();
        List<RunResult> results = IntStream.range(0, runs)
                .parallel()
                .mapToObj(seed -> runOne(seed, ticks))
                .collect(Collectors.toList());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("Ran %d seeds x %d ticks in %d ms on %d cores (migration chance %.2f)%n%n",
                runs, ticks, elapsedMs, Runtime.getRuntime().availableProcessors(), World.MIGRATION_CHANCE);

        long aliveAtEnd = results.stream().filter(r -> r.animalsAliveAtEnd).count();
        System.out.printf("Runs with animals still alive at tick %d: %d / %d%n", ticks, aliveAtEnd, runs);

        double avgCoexistence = results.stream()
                .mapToDouble(r -> r.coexistenceFraction)
                .average().orElse(0);
        System.out.printf("Average share of ticks with all three species alive: %.0f%%%n", 100 * avgCoexistence);

        double avgHerbCrashes = results.stream().mapToInt(r -> r.herbivoreCrashes.size()).average().orElse(0);
        double avgCarnCrashes = results.stream().mapToInt(r -> r.carnivoreCrashes.size()).average().orElse(0);
        System.out.printf("Average crashes per run -- herbivores: %.1f, carnivores: %.1f%n",
                avgHerbCrashes, avgCarnCrashes);

        // Which season do herbivore crashes happen in? Nothing in the code says
        // "herbivores die in spring" -- any pattern here is emergent.
        Map<Season, Long> crashSeason = results.stream()
                .flatMap(r -> r.herbivoreCrashes.stream())
                .collect(Collectors.groupingBy(BatchExperiment::seasonAt,
                        () -> new EnumMap<>(Season.class), Collectors.counting()));
        System.out.println("Herbivore crashes by season: " + crashSeason);

        Map<Season, Long> carnCrashSeason = results.stream()
                .flatMap(r -> r.carnivoreCrashes.stream())
                .collect(Collectors.groupingBy(BatchExperiment::seasonAt,
                        () -> new EnumMap<>(Season.class), Collectors.counting()));
        System.out.println("Carnivore crashes by season: " + carnCrashSeason);
    }

    /** The season the world is in at a given tick (mirrors World.tick's rollover rule). */
    static Season seasonAt(long tick) {
        Season[] all = Season.values();
        return all[(int) ((tick / World.SEASON_LENGTH) % all.length)];
    }

    /** One complete, independent run. Touches nothing outside its own World. */
    static RunResult runOne(long seed, int ticks) {
        World world = new World(60, 40, seed);
        try {
            seed(world, 140, OrganismFactory::createPlant);
            seed(world, 40, OrganismFactory::createHerbivore);
            seed(world, 10, OrganismFactory::createCarnivore);
        } catch (OverpopulationException e) {
            throw new IllegalStateException("Grid too small for initial population", e);
        }

        // A per-run observer. Its lists are created inside this method, so they are
        // confined to whichever single thread is running this seed -- never shared.
        List<Long> herbCrashes = new ArrayList<>();
        List<Long> carnCrashes = new ArrayList<>();
        int[] coexistTicks = {0};
        world.addListener(new SimulationListener() {
            @Override
            public void onTickCompleted(World w) {
                Map<PopulationStats.Species, Long> c = PopulationStats.countBySpecies(w.getOrganisms());
                if (c.values().stream().allMatch(n -> n > 0)) {
                    coexistTicks[0]++;
                }
            }

            @Override
            public void onExtinction(World w, Class<? extends Organism> species) {
                if (species == Herbivore.class) herbCrashes.add(w.getTickCount());
                if (species == Carnivore.class) carnCrashes.add(w.getTickCount());
            }
        });

        for (int t = 0; t < ticks; t++) {
            world.tick();
        }
        boolean animalsAlive = world.countOfType(Animal.class) > 0;
        return new RunResult(seed, herbCrashes, carnCrashes, coexistTicks[0] / (double) ticks, animalsAlive);
    }

    private static void seed(World world, int count, java.util.function.Function<Position, ? extends Organism> factory)
            throws OverpopulationException {
        for (int i = 0; i < count; i++) {
            Position p;
            do {
                p = new Position(world.getRandom().nextInt(world.getWidth()),
                        world.getRandom().nextInt(world.getHeight()));
            } while (!world.isFree(p));
            world.addOrganism(factory.apply(p));
        }
    }
}
