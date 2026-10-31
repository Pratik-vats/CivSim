package civsim.model;

/**
 * Diplomatic status between settlements (Phase 1) and organizations (Phase 2B.2).
 */
public enum DiplomaticStatus {
    NEUTRAL("Neutral"),
    FRIENDLY("Friendly"),
    ALLIED("Allied"),
    RIVAL("Rival"),
    HOSTILE("Hostile"),
    CEASEFIRE("Ceasefire"),
    AT_WAR("At War");

    private final String displayName;

    DiplomaticStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTreatyActive() {
        return this == ALLIED || this == CEASEFIRE;
    }

    public boolean isHostile() {
        return this == HOSTILE || this == AT_WAR;
    }

    /**
     * Maps a continuous relationship score [-100.0, +100.0] to a default diplomatic status band.
     */
    public static DiplomaticStatus fromScore(double score) {
        if (score >= 75.0) return ALLIED;
        if (score >= 30.0) return FRIENDLY;
        if (score > -30.0) return NEUTRAL;
        if (score > -75.0) return RIVAL;
        return HOSTILE;
    }
}
