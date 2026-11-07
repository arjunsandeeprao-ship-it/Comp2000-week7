package ecosim;

import java.awt.Color;

/**
 * The four seasons of the simulated year. The {@link World} advances through
 * them every {@link World#SEASON_LENGTH} ticks; each season scales how
 * readily plants spread and how often new plants sprout from wind-blown seed.
 *
 * <p>An enum with behaviour (fields + methods) rather than a set of
 * {@code if (season == ...)} checks scattered through {@code Plant} and
 * {@code World}: the "how fertile is this season" knowledge lives on the
 * season itself, so adding a fifth season, or retuning winter, is a
 * one-line change here.</p>
 */
public enum Season {
    SPRING(1.4, 1.5, new Color(245, 250, 235)),
    SUMMER(1.0, 1.0, new Color(250, 248, 225)),
    AUTUMN(0.7, 0.6, new Color(245, 235, 220)),
    WINTER(0.25, 0.2, new Color(232, 238, 245));

    private final double spreadMultiplier;
    private final double seedingMultiplier;
    private final Color background;

    Season(double spreadMultiplier, double seedingMultiplier, Color background) {
        this.spreadMultiplier = spreadMultiplier;
        this.seedingMultiplier = seedingMultiplier;
        this.background = background;
    }

    /** Scales a plant's base spread chance for this season, capped at 1.0. */
    public double adjustSpreadChance(double baseChance) {
        return Math.min(1.0, baseChance * spreadMultiplier);
    }

    /** Scales the world's ambient (wind-blown seed) sprouting chance for this season. */
    public double adjustSeedingChance(double baseChance) {
        return Math.min(1.0, baseChance * seedingMultiplier);
    }

    /** Background tint for the GUI, so the season is visible at a glance. */
    public Color getBackground() {
        return background;
    }

    /** The season after this one, wrapping WINTER back round to SPRING. */
    public Season next() {
        Season[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
