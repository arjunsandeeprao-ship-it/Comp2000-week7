package ecosim;

import ecosim.exceptions.OverpopulationException;
import java.awt.Color;

/**
 * Common base for everything that can live on the grid: {@link Plant},
 * and (via {@link Animal}) {@link ecosim.Herbivore} and
 * {@link ecosim.Carnivore}.
 *
 * <p>What every organism shares, regardless of species: it occupies a
 * {@link Position}, it can be alive or dead, it ages, and once per tick the
 * {@link World} asks it to {@link #act(World)}. What differs between
 * species -- how it moves, what it eats, how it reproduces, how it should
 * be drawn -- is left abstract here and supplied by subclasses. That is
 * the inheritance hierarchy in one sentence: {@code Organism} defines the
 * shape of "a thing that lives on the grid", {@code Animal} adds the
 * shape of "a thing that also moves and eats", and the leaf classes fill
 * in species-specific behaviour.</p>
 */
public abstract class Organism implements Locatable {

    private Position position;
    private boolean alive = true;
    private int age = 0;

    protected Organism(Position position) {
        this.position = position;
    }

    /**
     * Called once per tick by {@code World.tick()}. May throw
     * {@link OverpopulationException} if the organism tries to reproduce
     * or spread into a full grid; see that exception's Javadoc for why
     * this one is allowed to propagate while others are caught locally.
     */
    public abstract void act(World world) throws OverpopulationException;

    /** The colour this organism should be drawn with -- polymorphic rendering, see WorldPanel. */
    public abstract Color getColor();

    /** A one-character label, used by the text-mode smoke test and tooltips. */
    public abstract char getSymbol();

    @Override
    public Position getPosition() {
        return position;
    }

    void setPosition(Position position) {
        this.position = position;
    }

    public boolean isAlive() {
        return alive;
    }

    public void die() {
        this.alive = false;
    }

    public int getAge() {
        return age;
    }

    protected void incrementAge() {
        age++;
    }
}
