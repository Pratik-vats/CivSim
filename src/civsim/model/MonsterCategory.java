package civsim.model;

/**
 * Phase 2B.5 — Categorization of supernatural and mundane monsters in the ecology.
 */
public enum MonsterCategory {
    BEAST("Beast"),
    ABERRATION("Aberration"),
    CONSTRUCT("Construct"),
    SPIRIT("Spirit"),
    SIEGE_CREATURE("Siege Creature"),
    ALPHA("Alpha"),
    ANCIENT("Ancient");

    private final String displayName;

    MonsterCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isApexOrAncient() {
        return this == ALPHA || this == ANCIENT;
    }

    public boolean isSiegeCapable() {
        return this == SIEGE_CREATURE || this == ANCIENT;
    }
}

