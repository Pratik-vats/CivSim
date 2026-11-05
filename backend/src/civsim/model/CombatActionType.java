package civsim.model;

/**
 * Phase 2B.1 — Tactical turn action types available to combatants.
 */
public enum CombatActionType {
    PHYSICAL_ATTACK("Physical Attack"),
    PRIMARY_ABILITY("Primary Supernatural Ability"),
    DEFEND("Defend"),
    SUPPORT("Support / Healing"),
    REPOSITION("Reposition"),
    RETREAT("Personal Retreat / Escape");

    private final String displayName;

    CombatActionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
