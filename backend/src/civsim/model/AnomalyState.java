package civsim.model;

/**
 * Phase 2B.3 / Phase 2B.4 -- Lifecycle state of a supernatural World Anomaly.
 * Tracks progression from hidden formation to public manifestation, active exploration,
 * securing, controlled extraction, and terminal states.
 */
public enum AnomalyState {
    FORMING("Forming"),
    REVEALED("Revealed"),
    DISCOVERED("Discovered"),
    EXPEDITION_ACTIVE("Expedition Active"),
    UNCLAIMED("Unclaimed"),
    SECURING("Securing"),
    CONTROLLED("Controlled"),
    CONTESTED("Contested"),
    UNRESOLVED_STALEMATE("Unresolved Stalemate"),
    DEPLETED("Depleted"),
    MONSTER_LAIR_PENDING("Monster Lair Pending"),
    EXPIRED("Expired");

    private final String displayName;

    AnomalyState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * An anomaly counts toward the active cap (5) only while in these non-terminal states.
     */
    public boolean isActive() {
        return this == FORMING ||
                this == REVEALED ||
                this == DISCOVERED ||
                this == EXPEDITION_ACTIVE ||
                this == UNCLAIMED ||
                this == SECURING ||
                this == CONTROLLED ||
                this == CONTESTED ||
                this == UNRESOLVED_STALEMATE;
    }

    public boolean isTerminal() {
        return this == DEPLETED || this == MONSTER_LAIR_PENDING || this == EXPIRED;
    }

    public boolean canExtract() {
        return this == CONTROLLED;
    }

    public boolean isUnderContest() {
        return this == CONTESTED || this == UNRESOLVED_STALEMATE;
    }

    public boolean isPrecognitionEligible() {
        return this == FORMING;
    }

    public boolean isPubliclySensedEligible() {
        return this == REVEALED || this == UNCLAIMED || this == DISCOVERED;
    }
}
