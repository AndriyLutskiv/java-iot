package ua.crowpi.core.mock;

import ua.crowpi.core.hardware.SpiFacade;

import java.util.Arrays;

/**
 * Mock implementation of {@link SpiFacade} for running on a laptop (no RPi required).
 *
 * <p>Simulates SPI full-duplex transfers by returning a configurable response pattern.
 * By default every transfer returns a zero-filled array of the same length as the
 * input. A custom response can be supplied via {@link #setNextResponse(byte[])} for
 * deterministic unit tests (e.g. simulating a known RFID UID).</p>
 */
public class MockSpiFacade implements SpiFacade {

    // Наступна відповідь, яку поверне transfer() — null означає "повернути нулі"
    private byte[] nextResponse = null;

    /**
     * Sets the byte array that will be returned by the next call to {@link #transfer(byte[])}.
     *
     * <p>After a single use the response is cleared back to {@code null} (zero-fill mode).
     * Call this method again before each transfer that requires a specific response.</p>
     *
     * @param response bytes to return as the simulated MISO data
     */
    public void setNextResponse(byte[] response) {
        // Захисна копія — щоб зовнішній код не міг змінити відповідь після встановлення
        this.nextResponse = Arrays.copyOf(response, response.length);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: logs the transmitted bytes in hex, then returns either the
     * configured {@code nextResponse} or a zero-filled array of the same length.</p>
     */
    @Override
    public byte[] transfer(byte[] data) {
        System.out.printf("[MOCK SPI] TX → %s%n", toHexString(data));

        byte[] response;
        if (nextResponse != null && nextResponse.length == data.length) {
            // Використовуємо попередньо встановлену відповідь для детермінованих тестів
            response = Arrays.copyOf(nextResponse, nextResponse.length);
            nextResponse = null;  // одноразове використання — скидаємо після передачі
        } else {
            // За замовчуванням повертаємо нулі — безпечна заглушка для SPI-читання
            response = new byte[data.length];
        }

        System.out.printf("[MOCK SPI] RX ← %s%n", toHexString(response));
        return response;
    }

    /**
     * {@inheritDoc}
     *
     * <p>In mock mode: logs the close operation.</p>
     */
    @Override
    public void close() {
        nextResponse = null;
        System.out.println("[MOCK SPI] closed");
    }

    // -------------------------------------------------------------------------
    // Допоміжні методи
    // -------------------------------------------------------------------------

    /**
     * Converts a byte array to a space-separated uppercase hex string for logging.
     *
     * @param bytes byte array to format
     * @return formatted string like {@code "AA BB CC DD"}
     */
    private static String toHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (byte b : bytes) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
