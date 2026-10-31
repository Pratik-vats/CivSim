package civsim.model;

/**
 * Stage 2A.4 — Class-specific primary ability specialization.
 * Each awakened character receives a default primary ability corresponding to their class.
 * Extensible for future secondary abilities.
 */
public enum PrimaryAbility {
    /** VANGUARD: Physical reinforcement and enhancement of the body */
    BODY_REINFORCEMENT("Body Reinforcement", SupernaturalClass.VANGUARD),

    /** ELEMENTALIST: Manipulation of elemental forces */
    ELEMENTAL_MANIPULATION("Elemental Manipulation", SupernaturalClass.ELEMENTALIST),

    /** VITALIST: Restoration and acceleration of life force */
    VITAL_RESTORATION("Vital Restoration", SupernaturalClass.VITALIST),

    /** WARDEN: Projection and maintenance of protective barriers */
    BARRIER_PROJECTION("Barrier Projection", SupernaturalClass.WARDEN),

    /** SEER: Enhanced perception and probability foresight */
    CLAIRVOYANT_PERCEPTION("Clairvoyant Perception", SupernaturalClass.SEER),

    /** BEASTBINDER: Bonding with and commanding mutated creatures */
    CREATURE_BONDING("Creature Bonding", SupernaturalClass.BEASTBINDER),

    /** RIFTWALKER: Short-range spatial displacement */
    SPATIAL_BLINK("Spatial Blink", SupernaturalClass.RIFTWALKER);

    private final String displayName;
    private final SupernaturalClass associatedClass;

    PrimaryAbility(String displayName, SupernaturalClass associatedClass) {
        this.displayName = displayName;
        this.associatedClass = associatedClass;
    }

    public String getDisplayName() { return displayName; }
    public SupernaturalClass getAssociatedClass() { return associatedClass; }

    /**
     * Returns the default primary ability for a given supernatural class.
     */
    public static PrimaryAbility forClass(SupernaturalClass cls) {
        for (PrimaryAbility ability : values()) {
            if (ability.associatedClass == cls) {
                return ability;
            }
        }
        return BODY_REINFORCEMENT; // fallback
    }
}
