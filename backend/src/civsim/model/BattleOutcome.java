package civsim.model;

/**
 * Phase 2B.1 — High-level termination status of a resolved combat encounter.
 */
public enum BattleOutcome {
    VICTORY("Victory"),
    DEFEAT("Defeat"),
    WITHDRAWAL("Organized Withdrawal"),
    PARTIAL_WITHDRAWAL("Partial Withdrawal"),
    STALEMATE("Stalemate / Inconclusive"),
    INVALID_REQUEST("Invalid Request");

    private final String displayName;

    BattleOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
