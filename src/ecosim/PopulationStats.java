package ecosim;

import java.util.Collection;
import java.util.EnumMap;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Stream-based summaries of the living population, used by the GUI's stats
 * line and by {@link SmokeTest}/{@link BatchExperiment}.
 *
 * <p>Two things here go beyond a plain {@code filter().count()}:</p>
 * <ul>
 *   <li>{@link #countBySpecies} uses {@code Collectors.groupingBy} with a
 *       downstream {@code counting()} collector to produce every species'
 *       count in a <em>single</em> pass over the organism list, instead of
 *       one {@code countOfType} pass per species.</li>
 *   <li>{@link #energyCollector()} is a hand-written {@link Collector}
 *       (supplier / accumulator / combiner / finisher) that folds a stream of
 *       animals into an {@link EnergyReport}. Because it supplies a proper
 *       combiner, it is safe to use on a parallel stream -- each thread fills
 *       its own private {@link IntSummaryStatistics} and they are merged at
 *       the end, so no shared mutable state is ever touched by two threads.</li>
 * </ul>
 */
public final class PopulationStats {

    /** Species labels used as map keys, so results print in a fixed, readable order. */
    public enum Species { PLANT, HERBIVORE, CARNIVORE }

    private PopulationStats() {
    }

    /** Maps an organism to its species label. */
    static Species speciesOf(Organism organism) {
        if (organism instanceof Plant) {
            return Species.PLANT;
        }
        return organism instanceof Carnivore ? Species.CARNIVORE : Species.HERBIVORE;
    }

    /** Every species' living count, computed in one stream pass with groupingBy. */
    public static Map<Species, Long> countBySpecies(Collection<? extends Organism> organisms) {
        Map<Species, Long> counts = organisms.stream()
                .filter(Organism::isAlive)
                .collect(Collectors.groupingBy(PopulationStats::speciesOf,
                        () -> new EnumMap<>(Species.class),
                        Collectors.counting()));
        // Species with zero members are absent from groupingBy's output; fill them in explicitly.
        for (Species s : Species.values()) {
            counts.putIfAbsent(s, 0L);
        }
        return counts;
    }

    /** Energy summary for one species of animal, e.g. {@code energyOf(world, Herbivore.class)}. */
    public static <T extends Animal> EnergyReport energyOf(World world, Class<T> type) {
        return world.getOrganisms().stream()
                .filter(Organism::isAlive)
                .filter(type::isInstance)
                .map(type::cast)
                .collect(energyCollector());
    }

    /**
     * A custom collector that summarises the energy of a stream of animals.
     * Written out longhand (rather than {@code Collectors.summarizingInt})
     * to show each of the four collector roles explicitly, and because the
     * finisher produces a domain-specific {@link EnergyReport} including a
     * "how many are close to starving" figure that no built-in collector gives.
     */
    public static Collector<Animal, ?, EnergyReport> energyCollector() {
        Supplier<int[]> supplier = () -> new int[4]; // [count, sum, min, starving]
        BiConsumer<int[], Animal> accumulator = (acc, animal) -> {
            int e = animal.getEnergy();
            acc[2] = acc[0] == 0 ? e : Math.min(acc[2], e);
            acc[0]++;
            acc[1] += e;
            if (e <= EnergyReport.STARVING_THRESHOLD) {
                acc[3]++;
            }
        };
        BinaryOperator<int[]> combiner = (a, b) -> {
            if (a[0] == 0) return b;
            if (b[0] == 0) return a;
            return new int[] {a[0] + b[0], a[1] + b[1], Math.min(a[2], b[2]), a[3] + b[3]};
        };
        Function<int[], EnergyReport> finisher =
                acc -> new EnergyReport(acc[0], acc[0] == 0 ? 0.0 : acc[1] / (double) acc[0], acc[2], acc[3]);
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /** Immutable result of {@link #energyCollector()}. */
    public static final class EnergyReport {
        public static final int STARVING_THRESHOLD = 10;

        private final int count;
        private final double averageEnergy;
        private final int minEnergy;
        private final int starving;

        EnergyReport(int count, double averageEnergy, int minEnergy, int starving) {
            this.count = count;
            this.averageEnergy = averageEnergy;
            this.minEnergy = minEnergy;
            this.starving = starving;
        }

        public int getCount() {
            return count;
        }

        public double getAverageEnergy() {
            return averageEnergy;
        }

        public int getMinEnergy() {
            return minEnergy;
        }

        public int getStarving() {
            return starving;
        }

        @Override
        public String toString() {
            return String.format("n=%d avg=%.1f min=%d starving=%d", count, averageEnergy, minEnergy, starving);
        }
    }
}
