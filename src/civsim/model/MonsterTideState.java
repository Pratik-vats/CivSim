package civsim.model;

/**
 * Phase 2B.5 — Lifecycle states of an active MonsterTide invasion.
 */
public enum MonsterTideState {
    MOBILIZING("Mobilizing"),
    APPROACHING("Approaching"),
    DETECTED("Detected"),
    INVADING("Invading"),
    RESOLVED("Resolved");

    private final String displayName;

    MonsterTideState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isInTransit() {
        return this == APPROACHING || this == DETECTED;
    }

    public boolean isTerminal() {
        return this == RESOLVED;
    }
}

