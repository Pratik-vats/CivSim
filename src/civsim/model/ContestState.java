package civsim.model;

/**
 * Phase 2B.4 -- State transitions for an AnomalyContestRecord.
 */
public enum ContestState {
    PENDING_RESOLUTION("Pending Resolution"),
    STAGING_MUSTER("Staging Muster"),
    NEGOTIATING("Negotiating"),
    BATTLE_SCHEDULED("Battle Scheduled"),
    BATTLE_IN_PROGRESS("Battle In Progress"),
    VICTORY_PENDING_SECURING("Victory Pending Securing"),
    RESOLVED_PEACEFULLY("Resolved Peacefully"),
    RESOLVED_BY_VICTORY("Resolved By Victory"),
    UNRESOLVED_STALEMATE("Unresolved Stalemate"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");

    private final String displayName;

    ContestState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isResolved() {
        return this == RESOLVED_PEACEFULLY || this == RESOLVED_BY_VICTORY;
    }

    public boolean isTerminal() {
        return isResolved() || this == UNRESOLVED_STALEMATE || this == EXPIRED || this == CANCELLED;
    }

    public boolean isPendingBattle() {
        return this == BATTLE_SCHEDULED || this == BATTLE_IN_PROGRESS;
    }
}
