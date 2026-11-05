package civsim.model;

/**
 * Phase 2C.1 -- Enchantment categorization families.
 * Strict stacking rule: at most one enchantment from each family per item.
 */
public enum EnchantmentFamily {
    // Offensive
    PHYSICAL_EDGE("Physical Edge", FamilyGroup.OFFENSIVE),
    AETHERIC_EDGE("Aetheric Edge", FamilyGroup.OFFENSIVE),
    ELEMENTAL_AFFINITY("Elemental Affinity", FamilyGroup.OFFENSIVE),
    MONSTER_BANE("Monster Bane", FamilyGroup.OFFENSIVE),

    // Defensive
    AETHERIC_WARD("Aetheric Ward", FamilyGroup.DEFENSIVE),
    ELEMENTAL_RESISTANCE("Elemental Resistance", FamilyGroup.DEFENSIVE),
    REINFORCEMENT("Reinforcement", FamilyGroup.DEFENSIVE),
    IMPACT_ABSORPTION("Impact Absorption", FamilyGroup.DEFENSIVE),

    // Utility
    AETHER_CHANNELING("Aether Channeling", FamilyGroup.UTILITY),
    FOCUS_ENHANCEMENT("Focus Enhancement", FamilyGroup.UTILITY),
    PERCEPTION_ENHANCEMENT("Perception Enhancement", FamilyGroup.UTILITY),
    CONTROLLED_MOVEMENT("Controlled Movement", FamilyGroup.UTILITY);

    public enum FamilyGroup {
        OFFENSIVE,
        DEFENSIVE,
        UTILITY
    }

    private final String displayName;
    private final FamilyGroup group;

    EnchantmentFamily(String displayName, FamilyGroup group) {
        this.displayName = displayName;
        this.group = group;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FamilyGroup getGroup() {
        return group;
    }

    public boolean isOffensive() {
        return group == FamilyGroup.OFFENSIVE;
    }

    public boolean isDefensive() {
        return group == FamilyGroup.DEFENSIVE;
    }

    public boolean isUtility() {
        return group == FamilyGroup.UTILITY;
    }
}

