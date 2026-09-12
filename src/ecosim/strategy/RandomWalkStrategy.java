package ecosim.strategy;

import ecosim.Animal;
import ecosim.Position;
import ecosim.World;
import java.util.List;

/** Wanders to a random free neighbouring cell, or stays put if boxed in. */
public class RandomWalkStrategy implements MovementStrategy {

    @Override
    public Position nextMove(Animal self, World world) {
        List<Position> options = world.freeNeighbours(self.getPosition());
        if (options.isEmpty()) {
            return self.getPosition();
        }
        return options.get(world.getRandom().nextInt(options.size()));
    }
}
