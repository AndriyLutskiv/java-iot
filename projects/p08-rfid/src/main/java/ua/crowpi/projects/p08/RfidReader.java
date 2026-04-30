package ua.crowpi.projects.p08;

import ua.crowpi.core.exception.HardwareException;
import ua.crowpi.core.hardware.GpioFacade;
import ua.crowpi.core.mock.MockGpioFacade;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reads the UID of an RFID card presented to the MFRC522 reader module.
 *
 * <p>On real Raspberry Pi hardware the MFRC522 communicates over SPI (bus 0, CE0).
 * In mock/simulation mode (when a {@link MockGpioFacade} is injected, or the
 * {@code mockMode} flag is {@code true}) the reader cycles deterministically through
 * a list of four test UIDs at 3-second intervals, making it easy to walk through
 * all access scenarios without physical hardware.</p>
 *
 * <p><strong>SPI register map used (simplified MFRC522 protocol):</strong></p>
 * <ul>
 *   <li>{@code 0x26} — REQA command: checks whether any card is in the RF field</li>
 *   <li>{@code 0x93} — Anti-collision loop: retrieves 4-byte UID</li>
 * </ul>
 *
 * <p>For a full production driver the student should implement the complete
 * ISO/IEC 14443-A framing on top of the {@link ua.crowpi.core.hardware.SpiFacade}.</p>
 */
public class RfidReader {

    // -------------------------------------------------------------------------
    // Mock UID cycling
    // -------------------------------------------------------------------------

    /** Ordered list of UIDs that the mock implementation cycles through. */
    private static final String[] MOCK_UIDS = {
        "AA:BB:CC:DD",   // Student Ivanenko — active → GRANTED
        "11:22:33:44",   // Teacher Petrenko  — active → GRANTED
        "FF:FF:FF:FF",   // Deactivated Card  — inactive → DENIED
        "00:00:00:00"    // Unknown UID        — not in DB → UNKNOWN_CARD
    };

    // Інтервал між симульованими піднесеннями картки у мілісекундах
    private static final long MOCK_INTERVAL_MS = 3000L;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Whether this instance operates in mock (simulation) mode. */
    private final boolean mockMode;

    /** Reference to the GPIO facade (kept for future real-hardware SPI integration). */
    private final GpioFacade gpio;

    /**
     * Monotonically incrementing index into {@link #MOCK_UIDS}.
     * AtomicInteger — потокобезпечний лічильник без необхідності синхронізації
     */
    private final AtomicInteger mockIndex = new AtomicInteger(0);

    /** Timestamp of the last simulated card presentation (epoch ms). */
    private long lastMockReadMs = 0L;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates an RfidReader, automatically enabling mock mode when the supplied
     * {@link GpioFacade} is an instance of {@link MockGpioFacade}.
     *
     * <p>This constructor is the primary entry point used by {@link RfidProject}.</p>
     *
     * @param gpio the GPIO facade; if it is a {@link MockGpioFacade}, mock mode is activated
     */
    public RfidReader(GpioFacade gpio) {
        this.gpio = gpio;
        // Якщо передано MockGpioFacade — автоматично вмикаємо симуляцію
        this.mockMode = (gpio instanceof MockGpioFacade);
    }

    /**
     * Creates an RfidReader with an explicit mock-mode flag.
     *
     * <p>Useful in tests where a Mockito mock (not a {@link MockGpioFacade}) is used,
     * but simulated UID cycling is still desired.</p>
     *
     * @param gpio     the GPIO facade
     * @param mockMode {@code true} to force mock/simulation mode regardless of gpio type
     */
    public RfidReader(GpioFacade gpio, boolean mockMode) {
        this.gpio     = gpio;
        this.mockMode = mockMode;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Attempts to read the UID of a card currently in the RF field.
     *
     * <p>In <strong>real mode</strong>: sends the REQA command (0x26) via SPI to the
     * MFRC522, then performs an anti-collision loop (command 0x93) to retrieve the
     * 4-byte UID. The UID bytes are formatted as {@code "XX:XX:XX:XX"}.
     * (Simplified: currently returns {@code null} as full Pi4J SPI integration
     * requires a live Raspberry Pi.)</p>
     *
     * <p>In <strong>mock mode</strong>: returns a new UID from the cycling list
     * every {@value #MOCK_INTERVAL_MS} ms; returns {@code null} in between
     * intervals to simulate the absence of a card.</p>
     *
     * @return colon-separated hex UID string such as {@code "AA:BB:CC:DD"},
     *         or {@code null} if no card is currently detected
     * @throws HardwareException if real SPI communication fails
     */
    public String readUid() throws HardwareException {
        if (mockMode) {
            return readUidMock();
        }
        // Реальна реалізація SPI через MFRC522 — спрощена версія
        return readUidReal();
    }

    /**
     * Checks whether a card is currently present in the RF field.
     *
     * <p>In mock mode: returns {@code true} every {@value #MOCK_INTERVAL_MS} ms
     * to simulate a new card being presented at regular intervals.</p>
     *
     * @return {@code true} if a card is detected; {@code false} otherwise
     */
    public boolean isCardPresent() {
        if (mockMode) {
            // Картка "присутня" якщо минуло ≥ MOCK_INTERVAL_MS з попереднього зчитування
            long now = System.currentTimeMillis();
            return (now - lastMockReadMs) >= MOCK_INTERVAL_MS;
        }
        // На реальному обладнанні — перевірка через SPI REQA команду
        // Для спрощення повертаємо false якщо реальний SPI не підключено
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the next UID from the cycling mock list once per {@value #MOCK_INTERVAL_MS} ms.
     *
     * @return UID string or {@code null} if the interval has not elapsed since the last call
     */
    private String readUidMock() {
        long now = System.currentTimeMillis();
        if ((now - lastMockReadMs) < MOCK_INTERVAL_MS) {
            // Ще не минуло 3 секунди — картка ще не піднесена
            return null;
        }
        // Оновлюємо час останнього зчитування
        lastMockReadMs = now;

        // Циклічно отримуємо наступний UID зі списку
        // getAndIncrement() атомарно повертає поточне значення і збільшує лічильник
        int idx = mockIndex.getAndIncrement() % MOCK_UIDS.length;
        String uid = MOCK_UIDS[idx];
        System.out.printf("[RFID MOCK] Simulated card read: %s%n", uid);
        return uid;
    }

    /**
     * Performs a simplified MFRC522 SPI read to retrieve a card UID from real hardware.
     *
     * <p>Full implementation requires a live SPI bus. This stub returns {@code null}
     * until the student wires up a {@link ua.crowpi.core.hardware.SpiFacade}
     * and implements the complete ISO 14443-A anti-collision sequence.</p>
     *
     * @return {@code null} — real SPI integration pending physical hardware
     * @throws HardwareException never thrown by this stub; declared for interface compatibility
     */
    private String readUidReal() throws HardwareException {
        // Реальна SPI-реалізація потребує Pi4J SpiFacade + MFRC522 driver.
        // Студент підключає SpiFacade та реалізує:
        //   1. Відправити команду REQA (0x26) через spi.transfer(...)
        //   2. Зчитати ATQA відповідь
        //   3. Виконати AntiCollision Loop 1 (команда 0x93)
        //   4. Повернути 4 байти UID у форматі "XX:XX:XX:XX"
        System.out.println("[RFID] Real SPI read — not yet implemented (requires physical hardware)");
        return null;
    }

    /**
     * Formats a 4-byte UID byte array as a colon-separated uppercase hex string.
     *
     * <p>Example: {@code {0xAA, 0xBB, 0xCC, 0xDD}} → {@code "AA:BB:CC:DD"}</p>
     *
     * @param uidBytes 4-byte array from the MFRC522 anti-collision response
     * @return formatted UID string
     */
    static String formatUid(byte[] uidBytes) {
        // Форматуємо кожен байт як 2-символьний hex і з'єднуємо двокрапкою
        StringBuilder sb = new StringBuilder(11); // "XX:XX:XX:XX" = 11 символів
        for (int i = 0; i < uidBytes.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02X", uidBytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
