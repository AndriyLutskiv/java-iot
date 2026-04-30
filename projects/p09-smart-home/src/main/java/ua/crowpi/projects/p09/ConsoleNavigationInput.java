package ua.crowpi.projects.p09;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * {@link NavigationInput} implementation for mock (laptop) mode that reads keystrokes
 * from {@code System.in}.
 *
 * <p>Key mapping:</p>
 * <pre>
 *   w, u, ↑  → UP
 *   s, d, ↓  → DOWN
 *   Enter, e → SELECT
 *   q, b     → BACK
 * </pre>
 *
 * <p>Runs a background reader thread that enqueues actions from stdin so the main
 * menu thread is not blocked inside {@link Scanner#nextLine()}.</p>
 */
public class ConsoleNavigationInput implements NavigationInput {

    private static final Logger LOG = LogManager.getLogger(ConsoleNavigationInput.class);

    /** Instructions printed to stdout so the student knows what keys to press. */
    private static final String KEY_HELP =
            "[MOCK INPUT]  w=UP  s=DOWN  e=SELECT  q=BACK  (Enter after each key)";

    private final BlockingQueue<NavAction> queue = new LinkedBlockingQueue<>(32);
    private final Thread readerThread;
    private volatile boolean running = true;

    /**
     * Creates a ConsoleNavigationInput and starts the background stdin reader thread.
     */
    public ConsoleNavigationInput() {
        System.out.println(KEY_HELP);
        readerThread = new Thread(this::readLoop, "console-nav-input");
        // Daemon-потік — не перешкоджає завершенню JVM після виходу з меню
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Blocks until the student types a command or the timeout expires.</p>
     */
    @Override
    public NavAction poll(long timeoutMs) {
        try {
            return queue.poll(Math.max(timeoutMs, 0), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Stops the stdin reader thread.
     * Should be called when the menu exits.
     */
    public void stop() {
        running = false;
        readerThread.interrupt();
    }

    // -------------------------------------------------------------------------

    /**
     * Background loop that reads lines from stdin and enqueues NavActions.
     */
    private void readLoop() {
        Scanner scanner = new Scanner(System.in);
        while (running && scanner.hasNextLine()) {
            String line = scanner.nextLine().trim().toLowerCase();
            NavAction action = parseAction(line);
            if (action != null) {
                boolean offered = queue.offer(action);
                if (!offered) {
                    LOG.warn("Console input queue full — discarding '{}'", line);
                }
            } else if (!line.isEmpty()) {
                System.out.println(KEY_HELP);
            }
        }
    }

    /**
     * Converts a raw input string to a {@link NavAction}.
     *
     * @param input trimmed, lowercase input line
     * @return corresponding NavAction, or {@code null} for unrecognised input
     */
    private NavAction parseAction(String input) {
        switch (input) {
            case "w":
            case "u":
            case "up":
                return NavAction.UP;
            case "s":
            case "d":
            case "down":
                return NavAction.DOWN;
            case "e":
            case "":
            case "enter":
            case "select":
                return NavAction.SELECT;
            case "q":
            case "b":
            case "back":
            case "exit":
                return NavAction.BACK;
            default:
                return null;
        }
    }
}
