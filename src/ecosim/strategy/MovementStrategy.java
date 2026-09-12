package ecosim.strategy;

import ecosim.Animal;
import ecosim.Position;
import ecosim.World;

/**
 * Strategy pattern: decides where an {@link Animal} wants to move next.
 *
 * <p>Why a design pattern was needed here rather than just an if/else in
 * {@code Animal}: herbivores and carnivores both need "move towards food,
 * away from danger, otherwise wander" behaviour, but with different
 * definitions of food and danger. Without this interface that logic would
 * either be duplicated in both subclasses or collapse into one method full
 * of {@code instanceof} checks on the animal's own type. Instead, each
 * {@code Animal} is handed a strategy object at construction time (see
 * {@code OrganismFactory}) that already knows what it flees and what it
 * seeks, and {@code Animal.move} just delegates to it -- the animal
 * doesn't need to know or care which concrete strategy it was given.</p>
 */
public interface MovementStrategy {
    /**
     * Returns the position the animal should try to move to this tick.
     * Returning the animal's current position means "stay put". The
     * returned position is only a request -- {@code World.moveOrganism}
     * still enforces bounds and collision rules and may reject it.
     */
    Position nextMove(Animal self, World world);
}
