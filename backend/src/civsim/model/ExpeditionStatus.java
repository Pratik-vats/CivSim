package civsim.model;

/**
 * Phase 2B.3 / Phase 2B.4 -- Lifecycle status of an active Expedition.
 */
public enum ExpeditionStatus {
    EN_ROUTE_TO_ANOMALY("En Route to Anomaly"),
    WAITING_FOR_MANIFESTATION("Waiting for Manifestation"),
    PRESENT_AT_ANOMALY("Present at Anomaly"),
    SECURING_SITE("Securing Site"),
    EXTRACTING("Extracting"),
    RETURNING("Returning Home"),
    RETURNED("Returned Home"),
    ABORTED_SUPPLY_EXHAUSTION("Aborted (Supply Exhaustion)"),
    DISBANDED("Disbanded"),
    DESTROYED("Destroyed in Combat");

    private final String displayName;

    ExpeditionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this != DISBANDED && this != DESTROYED && this != RETURNED;
    }

    public boolean isAtSite() {
        return this == PRESENT_AT_ANOMALY || this == SECURING_SITE || this == EXTRACTING;
    }

    public boolean isTravelling() {
        return this == EN_ROUTE_TO_ANOMALY || this == RETURNING || this == ABORTED_SUPPLY_EXHAUSTION;
    }
}
