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
import java.util.Map;

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
    private String lastCrash = "none yet";

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
        attachListeners(world);
    }

    /**
     * Observer registration. The frame no longer refreshes its views by hand
     * after calling world.tick(); it subscribes to the World instead, so the
     * views update whenever the World announces a completed tick -- whoever
     * triggered it (the Timer, the Step button, or anything added later).
     */
    private void attachListeners(World w) {
        w.addListener(new SimulationListener() {
            @Override
            public void onTickCompleted(World world) {
                updateStats();
                worldPanel.repaint();
                sparklinePanel.repaint();
            }

            @Override
            public void onExtinction(World world, Class<? extends Organism> species) {
                // A local crash is now routine (migrants recolonise later), so it is
                // reported in the status bar rather than with a modal dialog.
                lastCrash = species.getSimpleName() + "s crashed at tick " + world.getTickCount()
                        + " (" + world.getSeason() + ")";
            }
        });
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
        resetButton.addActionListener(this::onResetRequested);
        panel.add(resetButton);

        panel.add(new JLabel("Speed:"));
        JSlider speedSlider = new JSlider(SwingConstants.HORIZONTAL, 20, 500, 120);
        speedSlider.addChangeListener(e -> timer.setDelay(speedSlider.getValue()));
        panel.add(speedSlider);

        // Stats get their own row underneath the buttons so the window stays as wide as the grid.
        statsLabel = new JLabel();
        updateStats();
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsRow.add(statsLabel);
        wrapper.add(statsRow, BorderLayout.SOUTH);
        return wrapper;
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
        world.tick(); // views refresh via the SimulationListener registered in attachListeners()
    }

    /**
     * Confirms before resetting: a long-running simulation (hundreds of ticks
     * of population history) is easy to lose with one misplaced click, and
     * unlike Start/Pause/Step, Reset is not undoable.
     */
    private void onResetRequested(ActionEvent e) {
        boolean wasRunning = timer.isRunning();
        if (wasRunning) {
            timer.stop();
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Reset will discard the current population and history (tick " + world.getTickCount() + "). Continue?",
                "Confirm reset", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            reset();
        } else if (wasRunning) {
            timer.start();
        }
    }

    private void reset() {
        timer.stop();
        startPauseButton.setText("Start");
        world = new World(GRID_WIDTH, GRID_HEIGHT, System.currentTimeMillis());
        seedWorld(world);
        lastCrash = "none yet";
        attachListeners(world);
        worldPanel.setWorld(world);
        sparklinePanel.setWorld(world);
        updateStats();
        worldPanel.repaint();
        sparklinePanel.repaint();
    }

    private void updateStats() {
        Map<PopulationStats.Species, Long> counts = PopulationStats.countBySpecies(world.getOrganisms());
        PopulationStats.EnergyReport herbEnergy = PopulationStats.energyOf(world, Herbivore.class);
        statsLabel.setText(String.format(
                "Tick %d (%s) | Plants: %d | Herbivores: %d (avg energy %.0f) | Carnivores: %d | Full-grid: %d",
                world.getTickCount(),
                world.getSeason(),
                counts.get(PopulationStats.Species.PLANT),
                counts.get(PopulationStats.Species.HERBIVORE),
                herbEnergy.getAverageEnergy(),
                counts.get(PopulationStats.Species.CARNIVORE),
                world.getOverpopulationEvents()));
        setTitle("Ecosystem Simulation - COMP2000   |   last crash: " + lastCrash
                + "   |   migrants: " + world.getMigrations());
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
