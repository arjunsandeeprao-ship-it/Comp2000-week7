package ecosim;

import javax.swing.SwingUtilities;

/** Entry point: launches the Swing GUI on the Event Dispatch Thread, as Swing requires. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulationFrame frame = new SimulationFrame();
            frame.setVisible(true);
        });
    }
}
