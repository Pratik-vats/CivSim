package civsim.model;

/**
 * Phase 2B.5 — Final tactical and strategic outcome of a MonsterTide invasion encounter.
 */
public enum MonsterTideOutcome {
    SUCCESSFUL_DEFENSE("Successful Defense"),
    COSTLY_VICTORY("Costly Victory"),
    MONSTER_RETREAT("Monster Retreat"),
    PARTIAL_BREACH("Partial Breach"),
    SETTLEMENT_OVERRUN("Settlement Overrun"),
    SETTLEMENT_ABANDONED("Settlement Abandoned");

    private final String displayName;

    MonsterTideOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefended() {
        return this == SUCCESSFUL_DEFENSE || this == COSTLY_VICTORY || this == MONSTER_RETREAT;
    }

    public boolean isBreached() {
        return this == PARTIAL_BREACH || this == SETTLEMENT_OVERRUN || this == SETTLEMENT_ABANDONED;
    }
}

