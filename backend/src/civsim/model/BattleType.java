package civsim.model;

/**
 * Phase 2B.1 — Categories of combat encounters submitted to the tactical combat engine.
 */
public enum BattleType {
    SETTLEMENT_WARFARE("Settlement Warfare"),
    ANOMALY_SKIRMISH("Anomaly Skirmish"),
    MONSTER_INVASION("Monster Invasion"),
    FACTION_CONFLICT("Faction Conflict"),
    DUEL("Duel");

    private final String displayName;

    BattleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
