package ecosim.exceptions;

/**
 * Thrown by {@code World.addOrganism} when the grid is already at capacity
 * (every cell occupied) and a new organism -- born from reproduction, or a
 * plant spreading -- has nowhere to go.
 *
 * <p>Unlike {@link InvalidPositionException}, which is resolved by picking
 * a different cell, there is no "try again" fix for this one: the world is
 * simply full. It is used in three different ways across the codebase on
 * purpose, to show three different exception-handling strategies for the
 * same checked exception (see worksheet-7.md, question 3.2, for the
 * reasoning):
 * <ol>
 *   <li>During routine reproduction ({@code Herbivore}/{@code Carnivore}/
 *       {@code Plant}) it is caught immediately and ignored -- a full grid
 *       just means "no babies this tick", which is normal.</li>
 *   <li>During {@code World.tick()} it is caught centrally and counted, so
 *       one crowded organism cannot stop the whole tick from finishing.</li>
 *   <li>During initial seeding in {@code SimulationFrame} it is allowed to
 *       propagate out to a top-level handler that shows the user a dialog,
 *       because too many starting organisms for the chosen grid size is a
 *       configuration mistake the user should know about immediately.</li>
 * </ol>
 */
public class OverpopulationException extends SimulationException {
    public OverpopulationException(String message) {
        super(message);
    }
}
