package ecosim.strategy;

import ecosim.Animal;
import ecosim.Organism;
import ecosim.Position;
import ecosim.World;
import java.util.List;
import java.util.Optional;

/**
 * A movement strategy parameterised, generically, by what to run from and
 * what to chase: "if a {@code threatType} is nearby, move away from it;
 * otherwise, if a {@code foodType} is nearby, move towards it; otherwise
 * wander randomly."
 *
 * <p>{@code threatType} may be {@code null} (used for {@code Carnivore},
 * which has no predator in this ecosystem). Using {@code Class<? extends
 * Organism>} tokens here -- rather than writing one strategy class for
 * "herbivore flees carnivore, seeks plant" and a near-identical one for
 * "carnivore seeks herbivore" -- is the generics decision this class
 * exists to demonstrate: the searching logic is written once, in
 * {@code World.findNearest}, and reused for any organism/threat
 * combination the ecosystem might need in future (a second predator
 * species, say) with zero new code.</p>
 */
public class FleeOrSeekStrategy implements MovementStrategy {

    private static final int SIGHT_RADIUS = 4;

    private final Class<? extends Organism> threatType;
    private final Class<? extends Organism> foodType;
    private final RandomWalkStrategy fallback = new RandomWalkStrategy();

    public FleeOrSeekStrategy(Class<? extends Organism> threatType, Class<? extends Organism> foodType) {
        this.threatType = threatType; // nullable: "nothing preys on me"
        this.foodType = foodType;
    }

    @Override
    public Position nextMove(Animal self, World world) {
        if (threatType != null) {
            Optional<? extends Organism> threat =
                    world.findNearest(self.getPosition(), threatType, SIGHT_RADIUS);
            if (threat.isPresent()) {
                return stepAwayFrom(self, world, threat.get().getPosition());
            }
        }

        Optional<? extends Organism> food =
                world.findNearest(self.getPosition(), foodType, SIGHT_RADIUS);
        if (food.isPresent()) {
            return stepToward(self, world, food.get().getPosition());
        }

        return fallback.nextMove(self, world);
    }

    private Position stepToward(Animal self, World world, Position target) {
        return bestFreeStep(self, world, target, 1);
    }

    private Position stepAwayFrom(Animal self, World world, Position threat) {
        return bestFreeStep(self, world, threat, -1);
    }

    /**
     * Picks the free neighbouring cell that best moves (direction = +1) or
     * away from (direction = -1) the given reference point.
     */
    private Position bestFreeStep(Animal self, World world, Position reference, int direction) {
        Position current = self.getPosition();
        List<Position> options = world.freeNeighbours(current);
        if (options.isEmpty()) {
            return current;
        }
        Position best = current;
        int bestScore = Integer.MIN_VALUE;
        for (Position candidate : options) {
            int score = direction * (current.distanceTo(reference) - candidate.distanceTo(reference));
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }
}
