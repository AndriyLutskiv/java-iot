package ua.crowpi.projects.p08;

/**
 * Immutable value object that represents a registered RFID card in the access control system.
 *
 * <p>Each card is identified by its UID (a colon-separated hex string such as
 * {@code "AA:BB:CC:DD"}) and carries the owner's name and an active/inactive flag.
 * Deactivated cards are still known to the system but result in {@link AccessResult#DENIED}
 * instead of {@link AccessResult#GRANTED}.</p>
 *
 * <p>Instances of this class are stored in {@link CardDatabase} and are never mutated
 * after creation.</p>
 */
public class KnownCard {

    /** Colon-separated uppercase hex UID, e.g. {@code "AA:BB:CC:DD"}. */
    private final String uid;

    /** Human-readable name of the card owner, e.g. {@code "Student Ivanenko"}. */
    private final String ownerName;

    /**
     * Whether the card is currently allowed to open the door.
     * {@code false} causes {@link AccessResult#DENIED} regardless of UID match.
     */
    private final boolean active;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new KnownCard with all fields supplied explicitly.
     *
     * @param uid       colon-separated hex UID string (case-insensitive, normalised to upper-case)
     * @param ownerName human-readable name of the card holder
     * @param active    {@code true} if the card may grant access; {@code false} to deactivate
     */
    public KnownCard(String uid, String ownerName, boolean active) {
        // Нормалізуємо до верхнього регістру, щоб порівняння в CardDatabase були консистентні
        this.uid       = uid != null ? uid.toUpperCase() : "";
        this.ownerName = ownerName != null ? ownerName : "";
        this.active    = active;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the normalised (upper-case) UID string for this card.
     *
     * @return UID in format {@code "XX:XX:XX:XX"}
     */
    public String getUid() {
        return uid;
    }

    /**
     * Returns the human-readable name of the card owner.
     *
     * @return owner name, never {@code null}
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Indicates whether this card is currently permitted to open the door.
     *
     * @return {@code true} if access is granted; {@code false} if the card is deactivated
     */
    public boolean isActive() {
        return active;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable representation useful for logging and debugging.
     *
     * <p>Format: {@code KnownCard{uid='XX:XX:XX:XX', owner='Name', active=true}}</p>
     *
     * @return formatted string representation of this card
     */
    @Override
    public String toString() {
        // Стандартний формат для швидкого читання у логах
        return "KnownCard{uid='" + uid + "', owner='" + ownerName + "', active=" + active + "}";
    }
}
