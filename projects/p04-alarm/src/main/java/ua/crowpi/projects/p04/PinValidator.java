package ua.crowpi.projects.p04;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Validates user-entered PIN codes using SHA-256 hashing.
 *
 * <p>The plain-text PIN is never stored; only its SHA-256 hex digest is kept.
 * This mirrors real-world security practice where passwords are stored as hashes
 * rather than plain text, making the stored configuration safe to commit to VCS.</p>
 *
 * <p>After three consecutive failed attempts {@link #isLockedOut()} returns
 * {@code true}. The caller ({@link AlarmProject}) is responsible for transitioning
 * the FSM to LOCKED and scheduling a timer to call {@link #reset()} after the
 * configured lockout duration.</p>
 */
public class PinValidator {

    /** Maximum consecutive wrong attempts before lockout. */
    private static final int MAX_FAILS = 3;

    // Очікуваний SHA-256 хеш (hex-рядок) еталонного PIN-коду
    private final String expectedHash;

    // Лічильник невдалих спроб — скидається при правильному PIN
    private int failCount;

    /**
     * Creates a PinValidator that compares entered PINs against the given SHA-256 hash.
     *
     * @param expectedHash the SHA-256 hex digest of the correct PIN (64 hex characters)
     */
    public PinValidator(String expectedHash) {
        // Нормалізуємо до нижнього регістру для стабільного порівняння
        this.expectedHash = expectedHash.toLowerCase();
        this.failCount = 0;
    }

    /**
     * Validates an entered PIN by comparing its SHA-256 hash to the expected hash.
     *
     * <p>On success the fail counter is reset to zero.
     * On failure the fail counter is incremented.
     * When {@link #isLockedOut()} becomes {@code true} the caller should
     * stop accepting PIN entry until the lockout timer expires.</p>
     *
     * @param pin the plain-text PIN string entered by the user
     * @return {@code true} if the PIN is correct; {@code false} otherwise
     */
    public boolean validate(String pin) {
        // Обчислюємо хеш введеного PIN і порівнюємо з очікуваним
        String hash = sha256(pin);
        boolean correct = hash.equals(expectedHash);

        if (correct) {
            // Правильний PIN — скидаємо лічильник, щоб дати новий "кредит" довіри
            failCount = 0;
        } else {
            // Невірний PIN — нарощуємо лічильник для відслідковування блокування
            failCount++;
        }

        return correct;
    }

    /**
     * Returns the number of consecutive failed PIN attempts since the last reset or success.
     *
     * @return current fail count; 0 after a correct PIN or after {@link #reset()}
     */
    public int getFailCount() {
        return failCount;
    }

    /**
     * Returns {@code true} if the fail counter has reached the lockout threshold (3 or more).
     *
     * <p>When this returns {@code true} the caller must transition the FSM to LOCKED
     * and schedule a timer to call {@link #reset()} after the lockout duration.</p>
     *
     * @return {@code true} if the keypad should be locked out
     */
    public boolean isLockedOut() {
        // Перевіряємо чи перевищено поріг невдалих спроб
        return failCount >= MAX_FAILS;
    }

    /**
     * Resets the fail counter to zero, removing the lockout condition.
     *
     * <p>Called by {@link AlarmProject} when the lockout timer expires.</p>
     */
    public void reset() {
        // Скидаємо лічильник — дозволяємо знову вводити PIN
        failCount = 0;
    }

    /**
     * Computes the SHA-256 hash of the given input string (UTF-8 encoded) and returns
     * the result as a lowercase hex string of exactly 64 characters.
     *
     * @param input the string to hash; must not be {@code null}
     * @return lowercase 64-character hex digest
     * @throws IllegalStateException if the JVM does not support SHA-256 (never in practice)
     */
    public static String sha256(String input) {
        try {
            // SHA-256 гарантовано доступний у всіх JVM, що відповідають Java SE
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Конвертуємо байти у hex-рядок вручну для уникнення залежностей від сторонніх бібліотек
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                // %02x — форматуємо кожен байт як два hex-символи з ведучим нулем при потребі
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // Ця гілка недосяжна на будь-якій стандартній JVM
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
