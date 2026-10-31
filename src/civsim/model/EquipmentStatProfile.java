package civsim.model;

/**
 * Phase 2C.1 -- Numerical stat modifiers provided by an equipment item.
 */
public class EquipmentStatProfile {

    private final double attackBonus;
    private final double defenseBonus;
    private final double accuracyBonus;
    private final double evasionBonus;
    private final double aetherPotencyBonus;
    private final double aetherCostEfficiency;
    private final double barrierOrResistanceBonus;

    public EquipmentStatProfile(
            double attackBonus,
            double defenseBonus,
            double accuracyBonus,
            double evasionBonus,
            double aetherPotencyBonus,
            double aetherCostEfficiency,
            double barrierOrResistanceBonus) {
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.accuracyBonus = accuracyBonus;
        this.evasionBonus = evasionBonus;
        this.aetherPotencyBonus = aetherPotencyBonus;
        this.aetherCostEfficiency = aetherCostEfficiency;
        this.barrierOrResistanceBonus = barrierOrResistanceBonus;
    }

    public static EquipmentStatProfile zero() {
        return new EquipmentStatProfile(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    public double getAttackBonus() { return attackBonus; }
    public double getDefenseBonus() { return defenseBonus; }
    public double getAccuracyBonus() { return accuracyBonus; }
    public double getEvasionBonus() { return evasionBonus; }
    public double getAetherPotencyBonus() { return aetherPotencyBonus; }
    public double getAetherCostEfficiency() { return aetherCostEfficiency; }
    public double getBarrierOrResistanceBonus() { return barrierOrResistanceBonus; }

    public EquipmentStatProfile scaled(double multiplier) {
        if (multiplier <= 0.0) {
            return zero();
        }
        return new EquipmentStatProfile(
                attackBonus * multiplier,
                defenseBonus * multiplier,
                accuracyBonus * multiplier,
                evasionBonus * multiplier,
                aetherPotencyBonus * multiplier,
                aetherCostEfficiency * multiplier,
                barrierOrResistanceBonus * multiplier
        );
    }

    public EquipmentStatProfile combine(EquipmentStatProfile other) {
        if (other == null) return this;
        return new EquipmentStatProfile(
                this.attackBonus + other.attackBonus,
                this.defenseBonus + other.defenseBonus,
                this.accuracyBonus + other.accuracyBonus,
                this.evasionBonus + other.evasionBonus,
                this.aetherPotencyBonus + other.aetherPotencyBonus,
                this.aetherCostEfficiency + other.aetherCostEfficiency,
                this.barrierOrResistanceBonus + other.barrierOrResistanceBonus
        );
    }
}

