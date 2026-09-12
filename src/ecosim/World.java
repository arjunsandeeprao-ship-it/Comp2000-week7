package ecosim;

import ecosim.exceptions.InvalidPositionException;
import ecosim.exceptions.OverpopulationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * The grid and the rules for what is allowed to happen on it. {@code World}
 * is deliberately <em>not</em> generic (it is not {@code World<T extends
 * Organism>}) even though it is the most obvious candidate for it in this
 * codebase: it manages a heterogeneous mix of {@code Plant}, {@code
 * Herbivore} and {@code Carnivore} objects at the same time, so a single
 * type parameter would not describe what it actually holds. Where
 * type-safety for a *specific* species is genuinely useful -- "find the
 * nearest Carnivore", "how many Herbivores are alive" -- that is provided
 * instead by small generic <em>methods</em> ({@link #findNearest} and
 * {@link #countOfType}) parameterised with a {@code Class<T>} token. That
 * targeted use, rather than a generic class, is the deliberate-avoidance
 * decision discussed in worksheet-7.md question 3.1.
 */
public class World {

    private static final int HISTORY_LENGTH = 120;
    /**
     * Per-tick chance of a new plant sprouting at a random free cell,
     * independent of any existing plant. Real plant populations are
     * replenished by wind- and animal-carried seed from well beyond one
     * grid cell's neighbourhood; {@link Plant#act} alone (spreading only
     * into its own free neighbours) cannot model that, and without some
     * form of it a single wave of overgrazing drives plants to total
     * extinction with no possible recovery -- which then starves out
     * every herbivore and carnivore in turn and ends the simulation
     * early. This constant is the one tuning knob that keeps the
     * predator-prey-plant cycle oscillating for a long, watchable run
     * instead of collapsing after a couple of hundred ticks.
     */
    private static final double AMBIENT_SEED_CHANCE = 0.6;
    private static final int[][] EIGHT_DIRECTIONS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0},           {1, 0},
            {-1, 1},  {0, 1},  {1, 1}
    };

    private final int width;
    private final int height;
    private final Map<Position, Organism> grid = new HashMap<>();
    private final List<Organism> organisms = new ArrayList<>();
    private final Random random;

    private final CircularBuffer<Integer> plantHistory = new CircularBuffer<>(HISTORY_LENGTH);
    private final CircularBuffer<Integer> herbivoreHistory = new CircularBuffer<>(HISTORY_LENGTH);
    private final CircularBuffer<Integer> carnivoreHistory = new CircularBuffer<>(HISTORY_LENGTH);

    private long tickCount = 0;
    private long overpopulationEvents = 0;

    public World(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.random = new Random(seed);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Random getRandom() {
        return random;
    }

    public long getTickCount() {
        return tickCount;
    }

    public long getOverpopulationEvents() {
        return overpopulationEvents;
    }

    public boolean isInBounds(Position p) {
        return p.getX() >= 0 && p.getX() < width && p.getY() >= 0 && p.getY() < height;
    }

    public boolean isFree(Position p) {
        return isInBounds(p) && !grid.containsKey(p);
    }

    public void addOrganism(Organism organism) throws OverpopulationException {
        Position position = organism.getPosition();
        if (!isInBounds(position)) {
            throw new OverpopulationException(
                    "Cannot add organism outside the grid at " + position);
        }
        if (grid.containsKey(position)) {
            throw new OverpopulationException(
                    "Grid cell " + position + " is already occupied; no room to add organism");
        }
        grid.put(position, organism);
        organisms.add(organism);
    }

    public void removeOrganism(Organism organism) {
        grid.remove(organism.getPosition());
    }

    /**
     * Attempts to move {@code organism} onto {@code target}. Throws a
     * checked {@link InvalidPositionException} rather than silently
     * failing, so every caller has to decide how to react (see
     * {@code Animal.move} for the standard "stay put" recovery).
     */
    public void moveOrganism(Organism organism, Position target) throws InvalidPositionException {
        if (!isInBounds(target)) {
            throw new InvalidPositionException(target, "outside the grid");
        }
        if (grid.containsKey(target) && grid.get(target) != organism) {
            throw new InvalidPositionException(target, "already occupied");
        }
        grid.remove(organism.getPosition());
        grid.put(target, organism);
        organism.setPosition(target);
    }

    public List<Organism> getNeighbours(Position center) {
        List<Organism> result = new ArrayList<>();
        for (int[] d : EIGHT_DIRECTIONS) {
            Position p = center.translate(d[0], d[1]);
            Organism o = grid.get(p);
            if (o != null && o.isAlive()) {
                result.add(o);
            }
        }
        return result;
    }

    public List<Position> freeNeighbours(Position center) {
        List<Position> result = new ArrayList<>();
        for (int[] d : EIGHT_DIRECTIONS) {
            Position p = center.translate(d[0], d[1]);
            if (isFree(p)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Generic method: finds the closest living organism of type {@code T}
     * within {@code radius} cells of {@code from}, or {@link Optional#empty()}
     * if none exists. The {@code Class<T>} token lets this one method serve
     * "find nearest Plant" and "find nearest Carnivore" alike with full
     * type safety at the call site -- see {@code FleeOrSeekStrategy}, which
     * is the reason this method exists.
     */
    public <T extends Organism> Optional<T> findNearest(Position from, Class<T> type, int radius) {
        return organisms.stream()
                .filter(Organism::isAlive)
                .filter(type::isInstance)
                .filter(o -> from.distanceTo(o.getPosition()) <= radius)
                .min(Comparator.comparingInt(o -> from.distanceTo(o.getPosition())))
                .map(type::cast);
    }

    /** Generic method: counts living organisms of a given species without an instanceof chain. */
    public <T extends Organism> long countOfType(Class<T> type) {
        return organisms.stream()
                .filter(Organism::isAlive)
                .filter(type::isInstance)
                .count();
    }

    private void maybeSeedAmbientPlant() {
        if (random.nextDouble() >= AMBIENT_SEED_CHANCE) {
            return;
        }
        Position candidate = new Position(random.nextInt(width), random.nextInt(height));
        if (isFree(candidate)) {
            try {
                addOrganism(OrganismFactory.createPlant(candidate));
            } catch (OverpopulationException e) {
                // Grid filled up between the isFree check and now -- fine, skip this tick.
            }
        }
    }

    public List<Organism> getOrganisms() {
        return Collections.unmodifiableList(organisms);
    }

    public CircularBuffer<Integer> getPlantHistory() {
        return plantHistory;
    }

    public CircularBuffer<Integer> getHerbivoreHistory() {
        return herbivoreHistory;
    }

    public CircularBuffer<Integer> getCarnivoreHistory() {
        return carnivoreHistory;
    }

    /**
     * Advances the simulation by one tick: every living organism acts once,
     * the dead are cleared out, and population history is recorded.
     *
     * <p>We iterate over a snapshot ({@code new ArrayList<>(organisms)})
     * rather than {@code organisms} directly, because {@code act()} can add
     * (reproduction) or remove (predation) elements from that very list --
     * iterating the live list would throw {@code ConcurrentModificationException}.</p>
     */
    public void tick() {
        tickCount++;
        List<Organism> snapshot = new ArrayList<>(organisms);
        for (Organism organism : snapshot) {
            if (!organism.isAlive()) {
                continue;
            }
            try {
                organism.act(this);
            } catch (OverpopulationException e) {
                // Caught centrally here (contrast with Plant.trySpread, which
                // catches its own OverpopulationException locally): a full
                // grid during reproduction is common enough that we do not
                // want it logged per-organism, but we do want an aggregate
                // count so SimulationFrame can show how crowded the world
                // has been -- see worksheet-7.md 3.2.
                overpopulationEvents++;
            }
        }
        organisms.removeIf(o -> !o.isAlive());
        grid.values().removeIf(o -> !o.isAlive());

        maybeSeedAmbientPlant();

        plantHistory.add((int) countOfType(Plant.class));
        herbivoreHistory.add((int) countOfType(Herbivore.class));
        carnivoreHistory.add((int) countOfType(Carnivore.class));
    }
}
