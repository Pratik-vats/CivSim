package civsim.model;

/**
 * Phase 2C.1 -- Single authoritative derived profile aggregated from all
 * currently equipped, compatible, non-broken items on a combatant.
 * Enforces hard global bonus caps to prevent 100% dodge, 100% mitigation, or infinite Aether.
 */
public class DerivedEquipmentProfile {

    public static final double MAX_ATTACK_BONUS = 75.0;
    public static final double MAX_DEFENSE_BONUS = 75.0;
    public static final double MAX_ACCURACY_BONUS = 35.0;
    public static final double MAX_EVASION_BONUS = 35.0;
    public static final double MAX_AETHER_POTENCY_BONUS = 50.0;
    public static final double MAX_AETHER_COST_EFFICIENCY = 50.0;
    public static final double MAX_BARRIER_RESISTANCE_BONUS = 50.0;

    private final double attackBonus;
    private final double defenseBonus;
    private final double accuracyBonus;
    private final double evasionBonus;
    private final double aetherPotencyBonus;
    private final double aetherCostEfficiency;
    private final double barrierOrResistanceBonus;

    private final int totalEquippedItems;
    private final int brokenItemsCount;

    public DerivedEquipmentProfile(
            double rawAttack,
            double rawDefense,
            double rawAccuracy,
            double rawEvasion,
            double rawPotency,
            double rawEfficiency,
            double rawBarrier,
            int totalEquippedItems,
            int brokenItemsCount) {
        this.attackBonus = Math.min(MAX_ATTACK_BONUS, Math.max(0.0, rawAttack));
        this.defenseBonus = Math.min(MAX_DEFENSE_BONUS, Math.max(0.0, rawDefense));
        this.accuracyBonus = Math.min(MAX_ACCURACY_BONUS, Math.max(0.0, rawAccuracy));
        this.evasionBonus = Math.min(MAX_EVASION_BONUS, Math.max(0.0, rawEvasion));
        this.aetherPotencyBonus = Math.min(MAX_AETHER_POTENCY_BONUS, Math.max(0.0, rawPotency));
        this.aetherCostEfficiency = Math.min(MAX_AETHER_COST_EFFICIENCY, Math.max(0.0, rawEfficiency));
        this.barrierOrResistanceBonus = Math.min(MAX_BARRIER_RESISTANCE_BONUS, Math.max(0.0, rawBarrier));
        this.totalEquippedItems = totalEquippedItems;
        this.brokenItemsCount = brokenItemsCount;
    }

    public static DerivedEquipmentProfile empty() {
        return new DerivedEquipmentProfile(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0);
    }

    public double getAttackBonus() { return attackBonus; }
    public double getDefenseBonus() { return defenseBonus; }
    public double getAccuracyBonus() { return accuracyBonus; }
    public double getEvasionBonus() { return evasionBonus; }
    public double getAetherPotencyBonus() { return aetherPotencyBonus; }
    public double getAetherCostEfficiency() { return aetherCostEfficiency; }
    public double getBarrierOrResistanceBonus() { return barrierOrResistanceBonus; }

    public int getTotalEquippedItems() { return totalEquippedItems; }
    public int getBrokenItemsCount() { return brokenItemsCount; }
}

