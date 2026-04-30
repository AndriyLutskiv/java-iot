package ua.crowpi.projects.p03;

import ua.crowpi.core.hardware.PinChangeListener;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Decodes NEC IR protocol frames from a GPIO pin-change stream.
 *
 * <p>Implements {@link PinChangeListener} so it can be registered directly with
 * {@link ua.crowpi.core.hardware.GpioFacade#addListener(int, PinChangeListener)}.
 * Each complete 32-bit NEC frame is placed into an internal {@link LinkedBlockingQueue};
 * the main loop retrieves frames via {@link #poll(long)}.</p>
 *
 * <h2>NEC frame structure</h2>
 * <pre>
 *  9 000 µs LOW  — leader mark
 *  4 500 µs HIGH — leader space
 *  32 × (562 µs LOW mark  +  562 µs or 1 687 µs HIGH space)
 *          └─ short space = bit 0, long space = bit 1
 *    562 µs LOW  — stop mark
 * </pre>
 *
 * <p>The VS1838B demodulator output is <em>active-LOW</em>: idle = HIGH,
 * carrier burst = LOW. Each 32-bit frame carries (LSB first per byte):</p>
 * <ol>
 *   <li>8-bit device address</li>
 *   <li>8-bit inverted address</li>
 *   <li>8-bit command</li>
 *   <li>8-bit inverted command</li>
 * </ol>
 *
 * <p>Internally the bits are accumulated with a left-shift so the first-received
 * bit (address LSB) ends up in bit 31 of the Java {@code int}.  This matches the
 * hex constants in {@link IrCodeDecoder} (e.g. {@code 0xFF30CF} for button 1).</p>
 *
 * <h2>State machine</h2>
 * <pre>
 *  IDLE ──falling──► LEADER_MARK ──rising──► LEADER_SPACE
 *                                                  │
 *                                              falling
 *                                                  │
 *                                              DATA ──32 bits complete──► IDLE
 * </pre>
 *
 * <p>Any pulse outside its tolerance window resets the machine to IDLE.</p>
 */
public final class NecIrReceiver implements PinChangeListener {

    // -------------------------------------------------------------------------
    // Timing limits (microseconds, ±30 % tolerance)
    // -------------------------------------------------------------------------

    private static final long LEADER_MARK_MIN_US  =  6_300;   // 9 000 µs − 30 %
    private static final long LEADER_MARK_MAX_US  = 11_700;   // 9 000 µs + 30 %

    private static final long LEADER_SPACE_MIN_US =  3_150;   // 4 500 µs − 30 %
    private static final long LEADER_SPACE_MAX_US =  5_850;   // 4 500 µs + 30 %

    /**
     * Threshold separating a bit-0 space (~562 µs) from a bit-1 space (~1 687 µs).
     * Any space shorter than this is decoded as 0; longer as 1.
     */
    private static final long BIT_SPACE_THRESHOLD_US = 1_000;

    /**
     * Maximum plausible data-space duration.  Anything longer indicates a lost
     * pulse and triggers an IDLE reset.
     */
    private static final long DATA_SPACE_MAX_US = 2_800;

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    private enum State { IDLE, LEADER_MARK, LEADER_SPACE, DATA }

    /** Current decoder state — written and read only from the listener thread. */
    private State state = State.IDLE;

    /** Nanosecond timestamp of the most recent GPIO edge. */
    private long edgeNs = 0;

    /** Accumulated frame bits (MSB = first received bit). */
    private int bits = 0;

    /** Number of data bits collected so far in the current frame. */
    private int bitCount = 0;

    // -------------------------------------------------------------------------
    // Clock supplier (replaceable in unit tests)
    // -------------------------------------------------------------------------

    private final LongSupplier clockNs;

    // -------------------------------------------------------------------------
    // Output queue
    // -------------------------------------------------------------------------

    /** Completed NEC frames waiting to be consumed by the main loop. */
    private final LinkedBlockingQueue<Integer> frames = new LinkedBlockingQueue<>(16);

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates a receiver that uses {@link System#nanoTime()} as its clock.
     * Use this constructor in production code.
     */
    public NecIrReceiver() {
        this(System::nanoTime);
    }

    /**
     * Creates a receiver with a custom clock supplier.
     *
     * <p>Intended for unit tests: pass a controllable {@code LongSupplier} so
     * tests can advance simulated time without {@link Thread#sleep}.</p>
     *
     * @param clockNs supplier of the current time in nanoseconds
     */
    NecIrReceiver(LongSupplier clockNs) {
        this.clockNs = clockNs;
    }

    // -------------------------------------------------------------------------
    // PinChangeListener
    // -------------------------------------------------------------------------

    /**
     * Called by the GPIO facade on every pin state transition.
     *
     * <p>This method is invoked from a background GPIO-listener thread managed
     * by Pi4J (or the mock facade). It must return quickly.</p>
     *
     * @param pin  BCM GPIO pin that changed (not used — receiver is on one pin)
     * @param high {@code true} = rising edge (LOW → HIGH); {@code false} = falling edge
     */
    @Override
    public void onPinChange(int pin, boolean high) {
        long now = clockNs.getAsLong();

        switch (state) {

            case IDLE:
                // ── Falling edge starts the leader mark ──────────────────────
                if (!high) {
                    edgeNs = now;
                    state  = State.LEADER_MARK;
                }
                break;

            case LEADER_MARK:
                // ── Rising edge ends the leader mark; validate 9 ms ─────────
                if (high) {
                    long us = (now - edgeNs) / 1_000;
                    if (us >= LEADER_MARK_MIN_US && us <= LEADER_MARK_MAX_US) {
                        edgeNs = now;
                        state  = State.LEADER_SPACE;
                    } else {
                        state = State.IDLE;
                    }
                }
                break;

            case LEADER_SPACE:
                // ── Falling edge ends the leader space; validate 4.5 ms ─────
                if (!high) {
                    long us = (now - edgeNs) / 1_000;
                    if (us >= LEADER_SPACE_MIN_US && us <= LEADER_SPACE_MAX_US) {
                        bits     = 0;
                        bitCount = 0;
                        edgeNs   = now;
                        state    = State.DATA;
                    } else {
                        state = State.IDLE;
                    }
                }
                break;

            case DATA:
                if (high) {
                    // ── Rising edge: end of data mark ─────────────────────────
                    edgeNs = now;

                } else {
                    // ── Falling edge: end of data space → extract bit ─────────
                    long us = (now - edgeNs) / 1_000;

                    if (us > DATA_SPACE_MAX_US) {
                        state = State.IDLE;
                        break;
                    }

                    // Threshold decode: short space = 0, long space = 1
                    int bit = (us < BIT_SPACE_THRESHOLD_US) ? 0 : 1;

                    // Accumulate LSB-first: first received bit ends in MSB position
                    bits = (bits << 1) | bit;
                    bitCount++;
                    edgeNs = now;

                    if (bitCount == 32) {
                        // Frame complete — hand off to the main thread via the queue
                        frames.offer(bits);
                        state = State.IDLE;
                    }
                }
                break;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Retrieves the next decoded NEC frame, waiting at most {@code timeoutMs} ms.
     *
     * <p>A timeout of {@code 0} returns immediately with whatever is queued
     * (non-blocking check).</p>
     *
     * @param timeoutMs maximum wait time in milliseconds; {@code 0} = no wait
     * @return 32-bit NEC frame, or {@code -1} if no frame arrived within the timeout
     */
    public int poll(long timeoutMs) {
        try {
            Integer frame = frames.poll(timeoutMs, TimeUnit.MILLISECONDS);
            return (frame != null) ? frame : -1;
        } catch (InterruptedException e) {
            // Відновлюємо прапорець переривання — не гасимо його мовчки
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
