package ua.crowpi.projects.p08;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory registry of all registered RFID cards in the door-lock system.
 *
 * <p>Cards are stored in a {@link HashMap} keyed by their normalised (upper-case) UID.
 * The database is pre-seeded at construction time with three demonstration cards;
 * additional cards can be added at runtime via {@link #addCard(KnownCard)}.</p>
 *
 * <p>All UID lookups are case-insensitive: {@code "aa:bb:cc:dd"} and
 * {@code "AA:BB:CC:DD"} resolve to the same card.</p>
 *
 * <p>This class is <strong>not</strong> thread-safe. If multiple threads access the
 * database concurrently, external synchronisation is required.</p>
 */
public class CardDatabase {

    // HashMap — O(1) пошук за UID; ключі нормалізовані до верхнього регістру
    private final Map<String, KnownCard> cards;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new CardDatabase and immediately seeds it with the three
     * demonstration cards defined in the project specification.
     */
    public CardDatabase() {
        // Ініціалізуємо з невеликою початковою ємністю — база карток мала
        cards = new HashMap<>(8);
        // Заповнюємо базу демо-картками згідно специфікації
        seed();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Populates the database with the three pre-seeded demonstration cards.
     *
     * <p>Called automatically from the constructor. Can be called again to restore
     * the database to its initial state (e.g. in tests).</p>
     */
    public void seed() {
        // Активна картка студента — має отримати GRANTED
        addCard(new KnownCard("AA:BB:CC:DD", "Student Ivanenko", true));
        // Активна картка викладача — має отримати GRANTED
        addCard(new KnownCard("11:22:33:44", "Teacher Petrenko", true));
        // Деактивована картка — має отримати DENIED, але UID відомий системі
        addCard(new KnownCard("FF:FF:FF:FF", "Deactivated Card", false));
    }

    /**
     * Looks up a card by its UID, ignoring case differences.
     *
     * <p>Example: {@code findByUid("aa:bb:cc:dd")} will match
     * a card stored under key {@code "AA:BB:CC:DD"}.</p>
     *
     * @param uid the UID string as read from the RFID reader (any case)
     * @return an {@link Optional} containing the matching {@link KnownCard},
     *         or {@link Optional#empty()} if no card with that UID exists
     */
    public Optional<KnownCard> findByUid(String uid) {
        if (uid == null) {
            // null-UID не може відповідати жодній картці — повертаємо порожній Optional
            return Optional.empty();
        }
        // Нормалізуємо до верхнього регістру для нечутливого до регістру порівняння
        String normalised = uid.toUpperCase();
        return Optional.ofNullable(cards.get(normalised));
    }

    /**
     * Evaluates an access attempt for the given UID and returns the appropriate result.
     *
     * <p>Logic:</p>
     * <ol>
     *   <li>If the UID is not in the database → {@link AccessResult#UNKNOWN_CARD}</li>
     *   <li>If the card exists but is inactive → {@link AccessResult#DENIED}</li>
     *   <li>If the card exists and is active → {@link AccessResult#GRANTED}</li>
     * </ol>
     *
     * @param uid the UID string as read from the RFID reader
     * @return the access decision for that UID
     */
    public AccessResult check(String uid) {
        Optional<KnownCard> found = findByUid(uid);

        if (!found.isPresent()) {
            // UID взагалі не зареєстрований — невідома картка
            return AccessResult.UNKNOWN_CARD;
        }

        KnownCard card = found.get();
        if (!card.isActive()) {
            // Картка відома, але деактивована адміністратором
            return AccessResult.DENIED;
        }

        // Картка відома та активна — доступ дозволено
        return AccessResult.GRANTED;
    }

    /**
     * Adds or replaces a card in the database.
     *
     * <p>If a card with the same UID (case-insensitive) already exists it is silently
     * overwritten with the new card. The UID is normalised to upper-case before storage.</p>
     *
     * @param card the {@link KnownCard} to add; must not be {@code null}
     * @throws IllegalArgumentException if {@code card} is {@code null}
     */
    public void addCard(KnownCard card) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        // Зберігаємо за нормалізованим ключем — KnownCard конструктор теж нормалізує uid
        cards.put(card.getUid(), card);
    }

    /**
     * Returns the total number of registered cards in the database.
     *
     * @return number of entries in the database (including deactivated cards)
     */
    public int size() {
        return cards.size();
    }
}
