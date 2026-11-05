package civsim.model;

import java.util.Random;

/**
 * Phase 2B.3 -- Resource Grade Distribution and Quantity/Value Multipliers.
 * Resource grade is independent of character power rarity.
 */
public enum ResourceGrade {
    COMMON("Common", 0.50, 1.00, 1.0, 0),
    UNCOMMON("Uncommon", 0.27, 0.80, 1.8, 1),
    RARE("Rare", 0.14, 0.55, 3.2, 2),
    LEGENDARY("Legendary", 0.07, 0.30, 6.0, 3),
    MYTHIC("Mythic", 0.02, 0.15, 12.0, 4);

    private final String displayName;
    private final double spawnWeight;
    private final double quantityMultiplier;
    private final double valueMultiplier;
    private final int gradeStep;

    ResourceGrade(
            String displayName,
            double spawnWeight,
            double quantityMultiplier,
            double valueMultiplier,
            int gradeStep) {
        this.displayName = displayName;
        this.spawnWeight = spawnWeight;
        this.quantityMultiplier = quantityMultiplier;
        this.valueMultiplier = valueMultiplier;
        this.gradeStep = gradeStep;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getSpawnWeight() {
        return spawnWeight;
    }

    public double getQuantityMultiplier() {
        return quantityMultiplier;
    }

    public double getValueMultiplier() {
        return valueMultiplier;
    }

    public int getGradeStep() {
        return gradeStep;
    }

    /**
     * Computes the initialized deposit quantity given base quantity:
     * InitializedQuantity = max(1, round(SampledBaseQuantity * GradeQuantityMultiplier))
     */
    public int calculateInitializedQuantity(int baseQuantity) {
        long qty = Math.round(baseQuantity * quantityMultiplier);
        return (int) Math.max(1L, qty);
    }

    /**
     * Deterministically samples a ResourceGrade based on configured spawn weights:
     * COMMON (50%), UNCOMMON (27%), RARE (14%), LEGENDARY (7%), MYTHIC (2%).
     */
    public static ResourceGrade sampleGrade(Random random) {
        if (random == null) return COMMON;
        double roll = random.nextDouble();
        if (roll < 0.50) {
            return COMMON;
        } else if (roll < 0.77) {
            return UNCOMMON;
        } else if (roll < 0.91) {
            return RARE;
        } else if (roll < 0.98) {
            return LEGENDARY;
        } else {
            return MYTHIC;
        }
    }
}

