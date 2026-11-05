package civsim.model;

/**
 * Phase 2B.1 — Common contract for all tactical combatants (human soldiers, awakened champions,
 * bonded beasts, and monsters).
 */
public interface Combatant {

    /** Unique combatant entity identifier */
    String getCombatantId();

    /** Entity categorization */
    CombatantType getEntityType();

    /** Display name */
    String getName();

    /** Allegiance side in the current encounter */
    String getSideId();

    void setSideId(String sideId);

    /** Alive and persistent existence check */
    boolean isAlive();

    /** Active combat capability: alive, not incapacitated, not fled/escaped, currentHealth > 0 */
    boolean canActInCombat();

    /** Authoritative combat state */
    CombatState getCombatState();

    // --- Attributes ---
    double getEffectiveStrength();
    double getEffectiveAgility();
    double getEffectiveVitality();
    double getEffectiveEndurance();
    double getEffectiveIntelligence();
    double getEffectivePerception();
    double getEffectiveWillpower();
    double getEffectiveLuck();
    double getEffectiveAetherCapacity();
    double getEffectiveAetherControl();

    // --- Combat Ratings & Equipment ---
    double getPhysicalAttackRating();
    double getPhysicalDefenseRating();
    double getEquipmentAttackBonus();
    double getEquipmentDefenseBonus();
    double getEquipmentEvasionBonus();
    default double getEquipmentAccuracyBonus() { return 0.0; }
    default double getEquipmentAetherPotencyBonus() { return 0.0; }
    default double getEquipmentAetherCostEfficiency() { return 0.0; }
    default double getEquipmentBarrierBonus() { return 0.0; }

    // --- Supernatural State ---
    boolean isAwakened();
    SupernaturalClass getSupernaturalClass();
    AetherAspect getAspect();
    int getPowerTier();
    double getMastery();
    DerivedPowerProfile getDerivedPowerProfile();

    /** Underlying Person instance if human, null if beast/monster adapter */
    Person getPerson();
}
