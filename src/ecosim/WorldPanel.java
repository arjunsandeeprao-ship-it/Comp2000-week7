package ecosim;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Draws the grid. Deliberately built on plain {@code JPanel} +
 * {@code paintComponent}, per the assignment brief: this is not a polished
 * custom-widget GUI, just enough to make the simulation watchable.
 *
 * <p>Notice there is no {@code instanceof Plant}/{@code instanceof
 * Herbivore}/{@code instanceof Carnivore} chain here at all -- every
 * organism knows how to draw itself via the polymorphic
 * {@code getColor()} method it inherits/overrides from {@code Organism}.
 * Adding a fourth species later would need zero changes to this class.</p>
 */
public class WorldPanel extends JPanel {

    private World world;
    private final int cellSize;

    public WorldPanel(World world, int cellSize) {
        this.world = world;
        this.cellSize = cellSize;
        setBackground(new Color(235, 235, 225));
        setPreferredSize(new Dimension(world.getWidth() * cellSize, world.getHeight() * cellSize));
    }

    /** Swaps in a new World (used by the Reset button) without recreating the panel. */
    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(210, 210, 200));
        for (int x = 0; x <= world.getWidth(); x++) {
            g2.drawLine(x * cellSize, 0, x * cellSize, world.getHeight() * cellSize);
        }
        for (int y = 0; y <= world.getHeight(); y++) {
            g2.drawLine(0, y * cellSize, world.getWidth() * cellSize, y * cellSize);
        }

        for (Organism organism : world.getOrganisms()) {
            if (!organism.isAlive()) {
                continue;
            }
            g2.setColor(organism.getColor());
            int px = organism.getPosition().getX() * cellSize;
            int py = organism.getPosition().getY() * cellSize;
            if (organism instanceof Plant) {
                g2.fillRect(px + 2, py + 2, cellSize - 4, cellSize - 4);
            } else {
                g2.fillOval(px + 1, py + 1, cellSize - 2, cellSize - 2);
            }
        }
    }
}
