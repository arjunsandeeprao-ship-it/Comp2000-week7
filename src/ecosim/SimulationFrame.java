package ecosim;

import ecosim.exceptions.OverpopulationException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;

/**
 * The top-level Swing window: a {@link WorldPanel} to watch, a small
 * sparkline of population history, and simple start/pause/step/reset
 * controls driven by a {@code javax.swing.Timer}. Only standard JRE
 * classes are used -- no external GUI libraries.
 */
public class SimulationFrame extends JFrame {

    private static final int GRID_WIDTH = 60;
    private static final int GRID_HEIGHT = 40;
    private static final int CELL_SIZE = 12;
    private static final int INITIAL_PLANTS = 140;
    private static final int INITIAL_HERBIVORES = 40;
    private static final int INITIAL_CARNIVORES = 10;

    private World world;
    private WorldPanel worldPanel;
    private SparklinePanel sparklinePanel;
    private JLabel statsLabel;
    private Timer timer;
    private JButton startPauseButton;

    public SimulationFrame() {
        super("Ecosystem Simulation - COMP2000");
        this.world = new World(GRID_WIDTH, GRID_HEIGHT, System.currentTimeMillis());
        seedWorld(world);

        setLayout(new BorderLayout());
        worldPanel = new WorldPanel(world, CELL_SIZE);
        add(worldPanel, BorderLayout.CENTER);

        sparklinePanel = new SparklinePanel(world);
        sparklinePanel.setPreferredSize(new Dimension(GRID_WIDTH * CELL_SIZE, 80));
        add(sparklinePanel, BorderLayout.SOUTH);

        add(buildControlPanel(), BorderLayout.NORTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        timer = new Timer(120, this::onTick);
    }

    private void seedWorld(World world) {
        try {
            for (int i = 0; i < INITIAL_PLANTS; i++) {
                world.addOrganism(OrganismFactory.createPlant(randomFreePosition(world)));
            }
            for (int i = 0; i < INITIAL_HERBIVORES; i++) {
                world.addOrganism(OrganismFactory.createHerbivore(randomFreePosition(world)));
            }
            for (int i = 0; i < INITIAL_CARNIVORES; i++) {
                world.addOrganism(OrganismFactory.createCarnivore(randomFreePosition(world)));
            }
        } catch (OverpopulationException e) {
            // Unlike the other two OverpopulationException call sites (see
            // its Javadoc), we let this one reach a top-level handler
            // instead of catching it locally: too many initial organisms
            // for the chosen grid size is a configuration mistake, and the
            // user should be told about it immediately rather than ending
            // up with a silently under-seeded world.
            JOptionPane.showMessageDialog(this,
                    "Could not seed the initial population: " + e.getMessage(),
                    "Overpopulation during setup", JOptionPane.WARNING_MESSAGE);
        }
    }

    private Position randomFreePosition(World world) {
        Position p;
        do {
            p = new Position(world.getRandom().nextInt(world.getWidth()),
                    world.getRandom().nextInt(world.getHeight()));
        } while (!world.isFree(p));
        return p;
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        startPauseButton = new JButton("Start");
        startPauseButton.addActionListener(this::toggleRunning);
        panel.add(startPauseButton);

        JButton stepButton = new JButton("Step");
        stepButton.addActionListener(e -> onTick(e));
        panel.add(stepButton);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> reset());
        panel.add(resetButton);

        panel.add(new JLabel("Speed:"));
        JSlider speedSlider = new JSlider(SwingConstants.HORIZONTAL, 20, 500, 120);
        speedSlider.addChangeListener(e -> timer.setDelay(speedSlider.getValue()));
        panel.add(speedSlider);

        statsLabel = new JLabel();
        statsLabel.setPreferredSize(new Dimension(420, 20));
        panel.add(statsLabel);
        updateStats();

        return panel;
    }

    private void toggleRunning(ActionEvent e) {
        if (timer.isRunning()) {
            timer.stop();
            startPauseButton.setText("Start");
        } else {
            timer.start();
            startPauseButton.setText("Pause");
        }
    }

    private void onTick(ActionEvent e) {
        world.tick();
        updateStats();
        worldPanel.repaint();
        sparklinePanel.repaint();
    }

    private void reset() {
        timer.stop();
        startPauseButton.setText("Start");
        world = new World(GRID_WIDTH, GRID_HEIGHT, System.currentTimeMillis());
        seedWorld(world);
        worldPanel.setWorld(world);
        sparklinePanel.setWorld(world);
        updateStats();
        worldPanel.repaint();
        sparklinePanel.repaint();
    }

    private void updateStats() {
        statsLabel.setText(String.format(
                "Tick %d | Plants: %d | Herbivores: %d | Carnivores: %d | Full-grid events: %d",
                world.getTickCount(),
                world.countOfType(Plant.class),
                world.countOfType(Herbivore.class),
                world.countOfType(Carnivore.class),
                world.getOverpopulationEvents()));
    }

    /** Tiny inline population-trend chart, reading straight from World's CircularBuffers. */
    private static class SparklinePanel extends JPanel {
        private World world;

        SparklinePanel(World world) {
            this.world = world;
            setBackground(Color.WHITE);
        }

        void setWorld(World world) {
            this.world = world;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            drawSeries(g, world.getPlantHistory(), w, h, new Color(30, 150, 30));
            drawSeries(g, world.getHerbivoreHistory(), w, h, new Color(60, 90, 220));
            drawSeries(g, world.getCarnivoreHistory(), w, h, new Color(210, 40, 40));
        }

        private void drawSeries(Graphics g, CircularBuffer<Integer> history, int w, int h, Color color) {
            if (history.size() < 2) {
                return;
            }
            int max = 1;
            for (int value : history) {
                max = Math.max(max, value);
            }
            g.setColor(color);
            int step = Math.max(1, w / history.capacity());
            int prevX = 0, prevY = h - (int) ((history.get(0) / (double) max) * (h - 10));
            for (int i = 1; i < history.size(); i++) {
                int x = i * step;
                int y = h - (int) ((history.get(i) / (double) max) * (h - 10));
                g.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }
    }
}
