package civsim.model;

/**
 * Phase 2B.1 — Categorization of entities capable of engaging in tactical combat.
 */
public enum CombatantType {
    HUMAN("Human"),
    BONDED_BEAST("Bonded Beast"),
    MONSTER("Monster"),
    ALPHA_MONSTER("Alpha Monster");

    private final String displayName;

    CombatantType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
