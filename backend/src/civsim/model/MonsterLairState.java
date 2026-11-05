package civsim.model;

/**
 * Phase 2B.5 — Lifecycle state of a persistent Monster Lair.
 */
public enum MonsterLairState {
    FORMING("Forming"),
    ACTIVE("Active"),
    DORMANT("Dormant"),
    SUPPRESSED("Suppressed"),
    DESTROYED("Destroyed"),
    COLLAPSED("Collapsed");

    private final String displayName;

    MonsterLairState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canSpawn() {
        return this == ACTIVE || this == SUPPRESSED;
    }

    public boolean isTerminal() {
        return this == DESTROYED || this == COLLAPSED;
    }
}

