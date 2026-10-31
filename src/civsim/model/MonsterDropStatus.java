package civsim.model;

/**
 * Phase 2B.5 — Lifecycle status of monster drops from creation to physical inventory delivery.
 */
public enum MonsterDropStatus {
    GENERATED("Generated"),
    RECOVERABLE("Recoverable"),
    RECOVERED("Recovered"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered");

    private final String displayName;

    MonsterDropStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAvailableForPickup() {
        return this == GENERATED || this == RECOVERABLE;
    }

    public boolean isDelivered() {
        return this == DELIVERED;
    }
}

