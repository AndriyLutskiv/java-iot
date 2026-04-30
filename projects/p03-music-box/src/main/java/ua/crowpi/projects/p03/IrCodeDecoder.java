package ua.crowpi.projects.p03;

import java.util.HashMap;
import java.util.Map;

/**
 * Decodes raw NEC-protocol IR remote codes to melody indices 0–9.
 *
 * <p>The NEC protocol transmits a 32-bit frame. The codes below correspond to
 * a standard CrowPi IR remote where buttons 0–9 are mapped to specific hex
 * values. Any unrecognised code returns {@code -1}.</p>
 *
 * <p>In <em>mock mode</em> (enabled when the decoder is constructed with
 * {@code mockMode=true}) no real GPIO decoding occurs; instead, successive calls
 * to {@link #decode(int)} return melody indices 1–9 in a repeating cycle,
 * useful for automated demo runs.</p>
 */
public final class IrCodeDecoder {

    // -------------------------------------------------------------------------
    // NEC protocol button codes (CrowPi standard remote)
    // -------------------------------------------------------------------------

    /** IR code for button 0 (stop). */
    private static final int CODE_0 = 0xFF6897;
    /** IR code for button 1. */
    private static final int CODE_1 = 0xFF30CF;
    /** IR code for button 2. */
    private static final int CODE_2 = 0xFF18E7;
    /** IR code for button 3. */
    private static final int CODE_3 = 0xFF7A85;
    /** IR code for button 4. */
    private static final int CODE_4 = 0xFF10EF;
    /** IR code for button 5. */
    private static final int CODE_5 = 0xFF38C7;
    /** IR code for button 6. */
    private static final int CODE_6 = 0xFF5AA5;
    /** IR code for button 7. */
    private static final int CODE_7 = 0xFF42BD;
    /** IR code for button 8. */
    private static final int CODE_8 = 0xFF4AB5;
    /** IR code for button 9. */
    private static final int CODE_9 = 0xFF52AD;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** Lookup table: raw IR code → digit 0–9. */
    private final Map<Integer, Integer> codeToDigit;

    /** Whether this decoder operates in mock mode (no real GPIO). */
    private final boolean mockMode;

    /**
     * Counter for mock mode: tracks which melody index to return next (cycles 1–9).
     * Not volatile because mock decode is typically called from a single thread.
     */
    private int mockCounter = 1;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a decoder that uses real NEC IR code lookup.
     *
     * <p>Use this constructor when running on a Raspberry Pi with a real IR receiver.</p>
     */
    public IrCodeDecoder() {
        this(false);
    }

    /**
     * Creates a decoder with an explicit mock-mode flag.
     *
     * <p>In mock mode {@link #decode(int)} ignores the raw code and returns
     * sequential values 1–9, cycling back to 1 after 9.</p>
     *
     * @param mockMode {@code true} to enable sequential mock output
     */
    public IrCodeDecoder(boolean mockMode) {
        this.mockMode = mockMode;

        // Заповнюємо таблицю пошуку один раз під час конструювання
        codeToDigit = new HashMap<>(16);
        codeToDigit.put(CODE_0, 0);
        codeToDigit.put(CODE_1, 1);
        codeToDigit.put(CODE_2, 2);
        codeToDigit.put(CODE_3, 3);
        codeToDigit.put(CODE_4, 4);
        codeToDigit.put(CODE_5, 5);
        codeToDigit.put(CODE_6, 6);
        codeToDigit.put(CODE_7, 7);
        codeToDigit.put(CODE_8, 8);
        codeToDigit.put(CODE_9, 9);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Decodes a raw 32-bit NEC IR code to a digit 0–9.
     *
     * <p>In mock mode the {@code rawCode} parameter is ignored and the method
     * returns the next value in the cycle 1–9 (wraps after 9).</p>
     *
     * @param rawCode raw 32-bit NEC frame value received from the IR sensor
     * @return digit 0–9 if the code is recognised; {@code -1} if unknown
     */
    public int decode(int rawCode) {
        if (mockMode) {
            // У mock-режимі ігноруємо реальний код і повертаємо послідовні значення
            int result = mockCounter;
            // Циклічне збільшення: після 9 повертаємося до 1
            mockCounter = (mockCounter % 9) + 1;
            return result;
        }

        // Шукаємо код у таблиці; якщо не знайдено — повертаємо -1
        Integer digit = codeToDigit.get(rawCode);
        return (digit != null) ? digit : -1;
    }

    /**
     * Returns the NEC IR code associated with the given digit button.
     *
     * <p>Primarily used for documentation and testing purposes.</p>
     *
     * @param digit button digit 0–9
     * @return the corresponding NEC code, or {@code -1} if the digit is out of range
     */
    public int getCodeForDigit(int digit) {
        // Зворотний пошук: за цифрою знаходимо код
        for (Map.Entry<Integer, Integer> entry : codeToDigit.entrySet()) {
            if (entry.getValue() == digit) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * Returns whether this decoder is operating in mock mode.
     *
     * @return {@code true} if mock mode is active
     */
    public boolean isMockMode() {
        return mockMode;
    }
}
