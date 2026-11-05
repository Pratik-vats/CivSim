package civsim.model;

/**
 * Phase 2B.2 — Political and ideological alignment of an Organization.
 * Decoupled strictly from individual personal supernatural aspect.
 */
public enum OrganizationAlignment {
    AETHERIC_ALIGNED("Aetheric Aligned", true),
    DARK_ALIGNED("Dark Aligned", true),
    NEUTRAL("Neutral", false),
    MIXED("Mixed", false);

    private final String displayName;
    private final boolean supernatural;

    OrganizationAlignment(String displayName, boolean supernatural) {
        this.displayName = displayName;
        this.supernatural = supernatural;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Whether this alignment represents a supernatural ideology requiring the Supernatural Era (Year 50+).
     */
    public boolean isSupernatural() {
        return supernatural;
    }

    public boolean isDark() {
        return this == DARK_ALIGNED;
    }
}

