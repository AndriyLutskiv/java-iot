package ua.crowpi.projects.p03;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.crowpi.core.matrix.Max7219Matrix;
import ua.crowpi.core.matrix.MatrixScroller;
import ua.crowpi.core.mock.MockSpiFacade;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the p03 IR Remote Music Box module.
 *
 * <p>Tests cover the melody library structure, note frequency validity, and
 * the LCD progress bar rendering logic. No real GPIO hardware is required —
 * all tested classes are pure Java with no hardware dependencies.</p>
 */
class MusicBoxTest {

    // =========================================================================
    // MelodyLibrary tests
    // =========================================================================

    /**
     * Verifies that {@link MelodyLibrary#getMelodies()} returns exactly nine melodies.
     *
     * <p>Each IR button 1–9 maps to one melody by list index (0–8), so there must
     * be exactly nine entries.</p>
     */
    @Test
    void testMelodyLibrary_hasNineMelodies() {
        // Act: отримуємо список мелодій з бібліотеки
        List<Melody> melodies = MelodyLibrary.getMelodies();

        // Assert: рівно дев'ять мелодій — по одній на кнопку 1–9
        assertNotNull(melodies, "getMelodies() must not return null");
        assertEquals(9, melodies.size(),
                "MelodyLibrary must contain exactly 9 melodies for IR buttons 1–9");
    }

    /**
     * Verifies that every note in every melody has a frequency of either
     * exactly {@code 0} (rest) or in the audible range 20–20 000 Hz.
     *
     * <p>Frequencies outside this range would produce no audible sound or could
     * damage the CrowPi buzzer, so the library must not contain them.</p>
     */
    @Test
    void testNote_validFrequency() {
        // Перевіряємо кожну ноту в кожній мелодії бібліотеки
        for (Melody melody : MelodyLibrary.getMelodies()) {
            for (Note note : melody.getNotes()) {
                int freq = note.getFrequencyHz();
                // Допустимі значення: 0 (пауза) або 20–20000 Гц (чутний діапазон)
                boolean valid = (freq == 0) || (freq >= 20 && freq <= 20_000);
                assertTrue(valid,
                        "Melody '" + melody.getName() + "' contains note with invalid "
                        + "frequency: " + freq + " Hz (must be 0 or 20–20000)");
            }
        }
    }

    /**
     * Verifies that every melody has a total duration greater than zero.
     *
     * <p>A melody with zero total duration would result in an immediate "done"
     * state on the LCD with no audio output — this indicates an empty note list
     * which should not occur in the library.</p>
     */
    @Test
    void testMelody_totalDuration_positive() {
        // Кожна мелодія повинна мати щонайменше одну ноту зі стненою тривалістю
        for (Melody melody : MelodyLibrary.getMelodies()) {
            int total = melody.getTotalDurationMs();
            assertTrue(total > 0,
                    "Melody '" + melody.getName() + "' must have totalDurationMs > 0, "
                    + "got: " + total);
        }
    }

    // =========================================================================
    // LcdProgressRenderer tests
    // =========================================================================

    /**
     * Verifies that a fully completed bar (10/10) renders as ten filled blocks.
     *
     * <p>Expected: {@code "██████████"} (10 × U+2588 FULL BLOCK).</p>
     */
    @Test
    void testLcdProgressRenderer_full() {
        // Act: 10 з 10 — повністю заповнений бар
        String bar = LcdProgressRenderer.renderBar(10, 10);

        // Assert: рівно 10 заповнених символів
        assertEquals(10, bar.length(), "Bar must be exactly 10 characters");
        String expected = "\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588";
        assertEquals(expected, bar,
                "100% bar must consist of 10 full-block characters (U+2588)");
    }

    /**
     * Verifies that a half-full bar (5/10) renders as five filled and five empty blocks.
     *
     * <p>Expected: {@code "█████░░░░░"} (5 × U+2588 + 5 × U+2591).</p>
     */
    @Test
    void testLcdProgressRenderer_half() {
        // Act: 5 з 10 — рівно половина
        String bar = LcdProgressRenderer.renderBar(5, 10);

        // Assert: рівно 5 заповнених + 5 порожніх
        assertEquals(10, bar.length(), "Bar must be exactly 10 characters");
        String expected = "\u2588\u2588\u2588\u2588\u2588\u2591\u2591\u2591\u2591\u2591";
        assertEquals(expected, bar,
                "50% bar must be 5 full-block + 5 light-shade characters");
    }

    /**
     * Verifies that an empty bar (0/10) renders as ten empty blocks.
     *
     * <p>Expected: {@code "░░░░░░░░░░"} (10 × U+2591 LIGHT SHADE).</p>
     */
    @Test
    void testLcdProgressRenderer_zero() {
        // Act: 0 з 10 — порожній бар на початку мелодії
        String bar = LcdProgressRenderer.renderBar(0, 10);

        // Assert: рівно 10 порожніх символів
        assertEquals(10, bar.length(), "Bar must be exactly 10 characters");
        String expected = "\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591\u2591";
        assertEquals(expected, bar,
                "0% bar must consist of 10 light-shade characters (U+2591)");
    }

    // =========================================================================
    // IrCodeDecoder tests
    // =========================================================================

    /**
     * Verifies that {@link IrCodeDecoder#decode(int)} correctly maps known NEC codes
     * to their corresponding digit values 0–9.
     */
    @Test
    void testIrCodeDecoder_knownCodes() {
        // Використовуємо стандартний decoder (не mock-режим)
        IrCodeDecoder dec = new IrCodeDecoder(false);

        // Перевіряємо кілька відомих кодів — кнопки 0, 1, 5, 9
        assertEquals(0, dec.decode(0xFF6897), "CODE_0 must decode to 0");
        assertEquals(1, dec.decode(0xFF30CF), "CODE_1 must decode to 1");
        assertEquals(5, dec.decode(0xFF38C7), "CODE_5 must decode to 5");
        assertEquals(9, dec.decode(0xFF52AD), "CODE_9 must decode to 9");
    }

    /**
     * Verifies that an unrecognised IR code returns {@code -1}.
     */
    @Test
    void testIrCodeDecoder_unknownCode() {
        IrCodeDecoder dec = new IrCodeDecoder(false);

        // 0xDEADBEEF — не є жодним із зареєстрованих NEC-кодів
        assertEquals(-1, dec.decode(0xDEADBEEF),
                "Unrecognised IR code must return -1");
    }

    // =========================================================================
    // NecIrReceiver tests
    // =========================================================================

    /**
     * Controllable fake clock for NecIrReceiver tests.
     * Advancing {@code fakeNanos} simulates elapsed time between GPIO edges
     * without any real {@link Thread#sleep}.
     */
    private long fakeNanos;
    private NecIrReceiver receiver;

    @BeforeEach
    void setupReceiver() {
        fakeNanos = 0;
        receiver  = new NecIrReceiver(() -> fakeNanos);
    }

    /**
     * Advances the fake clock by {@code micros} microseconds.
     * Mirrors the passage of time between two GPIO edges.
     */
    private void advanceUs(long micros) {
        fakeNanos += micros * 1_000;
    }

    /**
     * Sends a complete NEC frame to the receiver by simulating the GPIO edge sequence.
     *
     * <p>NEC sends each byte LSB first; our accumulator shifts left so the first
     * received bit ends in position 31 of the result.  Therefore we iterate the
     * bit positions from 31 down to 0 — position 31 of {@code frame} is sent first
     * and is collected into bit 31 of the decoded value.</p>
     *
     * @param pin   the BCM pin number passed to {@link NecIrReceiver#onPinChange}
     * @param frame the 32-bit NEC value to transmit (e.g. {@code 0xFF30CF} for button 1)
     */
    private void sendNecFrame(int pin, int frame) {
        // Лідер-мітка: 9 000 µs LOW
        receiver.onPinChange(pin, false);
        advanceUs(9_000);
        // Лідер-пауза: 4 500 µs HIGH
        receiver.onPinChange(pin, true);
        advanceUs(4_500);

        // 32 біти даних (позиція 31 = перший отриманий = MSB результату)
        for (int i = 31; i >= 0; i--) {
            int bit = (frame >> i) & 1;
            // Мітка даних: 562 µs LOW (однакова для bit 0 і bit 1)
            receiver.onPinChange(pin, false);
            advanceUs(562);
            // Пауза даних: 562 µs = bit 0, 1 687 µs = bit 1
            receiver.onPinChange(pin, true);
            advanceUs(bit == 0 ? 562 : 1_687);
        }

        // Стоп-мітка: 562 µs LOW, потім IDLE HIGH
        receiver.onPinChange(pin, false);
        advanceUs(562);
        receiver.onPinChange(pin, true);
    }

    /**
     * Verifies that a well-formed NEC frame for button 1 (0xFF30CF) is decoded
     * to the correct 32-bit value.
     */
    @Test
    void testNecReceiver_validFrame_decodedCorrectly() {
        sendNecFrame(24, 0xFF30CF);

        int decoded = receiver.poll(0);
        assertEquals(0xFF30CF, decoded,
                "NEC frame for button 1 must decode to 0xFF30CF");
    }

    /**
     * Verifies that a leader mark that is far too short (1 ms instead of 9 ms)
     * is rejected and no frame is placed in the queue.
     */
    @Test
    void testNecReceiver_shortLeaderMark_ignored() {
        // Занадто коротка лідер-мітка (1 ms) — не є валідним NEC-лідером
        receiver.onPinChange(24, false);
        advanceUs(1_000);
        receiver.onPinChange(24, true);
        advanceUs(4_500);

        int decoded = receiver.poll(0);
        assertEquals(-1, decoded,
                "Invalid leader mark must not produce a decoded frame");
    }

    /**
     * Verifies that sending two consecutive valid frames produces two correct
     * decoded values in FIFO order.
     */
    @Test
    void testNecReceiver_twoFrames_bothDecoded() {
        sendNecFrame(24, 0xFF30CF);   // button 1
        advanceUs(40_000);             // міжкадрова пауза
        sendNecFrame(24, 0xFF18E7);   // button 2

        assertEquals(0xFF30CF, receiver.poll(0), "First frame must be 0xFF30CF (button 1)");
        assertEquals(0xFF18E7, receiver.poll(0), "Second frame must be 0xFF18E7 (button 2)");
    }

    /**
     * Integration test: verifies that a raw NEC frame decoded by {@link NecIrReceiver}
     * maps to the correct digit via {@link IrCodeDecoder}.
     *
     * <p>Simulates pressing button 5 on the CrowPi IR remote and confirms
     * the full receive → decode pipeline produces digit {@code 5}.</p>
     */
    @Test
    void testNecReceiver_integrateWithIrCodeDecoder_button5() {
        IrCodeDecoder dec = new IrCodeDecoder(false);

        sendNecFrame(24, 0xFF38C7);   // кнопка 5

        int rawCode = receiver.poll(0);
        int digit   = dec.decode(rawCode);

        assertEquals(5, digit,
                "NEC frame 0xFF38C7 must decode to digit 5 via IrCodeDecoder");
    }

    // =========================================================================
    // MatrixScroller tests
    // =========================================================================

    /**
     * Verifies that the pixel buffer for a known string has the correct length.
     *
     * <p>Formula: 8 leading blanks + (chars × 6) + 8 trailing blanks.
     * "AB" → 8 + 12 + 8 = 28.</p>
     */
    @Test
    void testMatrixScroller_pixelBufferLength() {
        // "AB" = 2 символи × 6 стовпців + 8 + 8 = 28
        byte[] buf = MatrixScroller.buildPixelBuffer("AB");
        assertEquals(28, buf.length,
                "Pixel buffer for 2-char string must be 8 + 2*6 + 8 = 28");
    }

    /**
     * Verifies that leading blank columns are truly zero (no pixel data bleeds
     * into the padding region).
     */
    @Test
    void testMatrixScroller_leadingBlanksAreZero() {
        byte[] buf = MatrixScroller.buildPixelBuffer("X");
        for (int i = 0; i < 8; i++) {
            assertEquals(0, buf[i],
                    "Leading blank column " + i + " must be 0x00");
        }
    }

    /**
     * Verifies that {@link Max7219Matrix#setFrame} does not throw when given
     * a valid 8-byte frame array and uses a MockSpiFacade.
     */
    @Test
    void testMax7219Matrix_setFrame_noException() {
        Max7219Matrix mat = new Max7219Matrix(new MockSpiFacade());
        // Рамка з усіма нулями — матриця повинна прийняти без винятків
        byte[] frame = new byte[8];
        mat.setFrame(frame);  // must not throw
    }

    /**
     * Verifies that {@link Max7219Matrix#setFrame} throws for a wrongly-sized array.
     */
    @Test
    void testMax7219Matrix_setFrame_wrongSize_throws() {
        Max7219Matrix mat = new Max7219Matrix(new MockSpiFacade());
        boolean threw = false;
        try {
            mat.setFrame(new byte[5]); // must throw
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw, "setFrame with non-8 array must throw IllegalArgumentException");
    }

    /**
     * Verifies that MatrixScroller starts and stops cleanly using MockSpiFacade,
     * with no real hardware required.
     */
    @Test
    void testMatrixScroller_startStop() throws InterruptedException {
        Max7219Matrix mat = new Max7219Matrix(new MockSpiFacade());
        MatrixScroller sc = new MatrixScroller(mat, "Hi");
        sc.start();
        Thread.sleep(120);  // дати потоку зробити щонайменше 2 кроки
        sc.stop();
        // Після stop() матриця очищена — метод не повинен кидати виняток
    }
}
