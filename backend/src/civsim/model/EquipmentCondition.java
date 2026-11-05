package civsim.model;

/**
 * Phase 2C.1 -- Equipment durability status and effective stat multipliers.
 * Broken gear (durability == 0) yields 0.00x combat bonus, but is never deleted.
 */
public enum EquipmentCondition {
    EXCELLENT("Excellent", 1.00, 0.80),
    GOOD("Good", 0.95, 0.50),
    WORN("Worn", 0.85, 0.25),
    DAMAGED("Damaged", 0.65, 0.0001),
    BROKEN("Broken", 0.00, 0.0);

    private final String displayName;
    private final double statMultiplier;
    private final double minRatio;

    EquipmentCondition(String displayName, double statMultiplier, double minRatio) {
        this.displayName = displayName;
        this.statMultiplier = statMultiplier;
        this.minRatio = minRatio;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getStatMultiplier() {
        return statMultiplier;
    }

    public double getMinRatio() {
        return minRatio;
    }

    /**
     * Determines the condition band from current durability and max durability.
     */
    public static EquipmentCondition fromDurability(double currentDurability, double maxDurability) {
        if (maxDurability <= 0.0 || currentDurability <= 0.0) {
            return BROKEN;
        }
        double ratio = currentDurability / maxDurability;
        if (ratio >= 0.80) {
            return EXCELLENT;
        } else if (ratio >= 0.50) {
            return GOOD;
        } else if (ratio >= 0.25) {
            return WORN;
        } else if (ratio > 0.0) {
            return DAMAGED;
        } else {
            return BROKEN;
        }
    }
}

