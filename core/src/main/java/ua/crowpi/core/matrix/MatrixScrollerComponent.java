package ua.crowpi.core.matrix;

import ua.crowpi.core.hardware.SpiFacade;
import ua.crowpi.core.mock.MockSpiFacade;
import ua.crowpi.core.pi4j.Pi4jSpiFacade;

/**
 * Self-contained LED-matrix scrolling component for CrowPi projects.
 *
 * <p>Owns the full lifecycle of the SPI bus, MAX7219 matrix driver, and
 * scrolling thread. All projects use this component to scroll the shared
 * "F7    Computer Engineering" ticker on the 8×8 LED matrix (SPI0 CE1,
 * physical pin 26) in a dedicated daemon thread.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In run(boolean mockMode):
 * matrixScroller = new MatrixScrollerComponent(mockMode);
 * matrixScroller.start();
 *
 * // In shutdown():
 * if (matrixScroller != null) {
 *     matrixScroller.stop();
 *     matrixScroller = null;
 * }
 * }</pre>
 */
public final class MatrixScrollerComponent {

    /** Text scrolled on the LED matrix across all projects. */
    private static final String SCROLL_TEXT = "F7    Computer Engineering";

    private final SpiFacade     spi;
    private final Max7219Matrix matrix;
    private final MatrixScroller scroller;

    /**
     * Creates the component, initialises the MAX7219, and prepares the scroll buffer.
     * Does not start the scroll thread yet — call {@link #start()}.
     *
     * @param mockMode {@code true} to use {@link MockSpiFacade} (no real SPI hardware needed)
     */
    public MatrixScrollerComponent(boolean mockMode) {
        spi      = mockMode ? new MockSpiFacade() : new Pi4jSpiFacade(1);
        matrix   = new Max7219Matrix(spi);
        scroller = new MatrixScroller(matrix, SCROLL_TEXT);
    }

    /**
     * Starts the scrolling daemon thread (non-blocking).
     * Has no effect if already running.
     */
    public void start() {
        scroller.start();
    }

    /**
     * Stops the scroll thread (with interrupt + join), clears the matrix,
     * puts the MAX7219 into sleep mode, and closes the SPI device.
     *
     * <p>Safe to call multiple times.</p>
     */
    public void stop() {
        scroller.stop();
        matrix.shutdown();
        spi.close();
    }
}
