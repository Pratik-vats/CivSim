package civsim.model;

import java.util.Random;

/**
 * Phase 2B.3 -- Supernatural Resource Categories found in rare World Anomalies.
 * Tracks base deposit quantities, spawn weights, and base economic valuations.
 */
public enum SupernaturalResourceType {
    RAW_AETHER_CRYSTAL("Raw Aether Crystal", 40, 100, 0.50, 10.0),
    ANOMALOUS_ORE("Anomalous Ore", 25, 70, 0.30, 20.0),
    CONDENSED_AETHER_ESSENCE("Condensed Aether Essence", 10, 30, 0.20, 25.0);

    private final String displayName;
    private final int minBaseQuantity;
    private final int maxBaseQuantity;
    private final double spawnWeight;
    private final double baseValueIron;

    SupernaturalResourceType(
            String displayName,
            int minBaseQuantity,
            int maxBaseQuantity,
            double spawnWeight,
            double baseValueIron) {
        this.displayName = displayName;
        this.minBaseQuantity = minBaseQuantity;
        this.maxBaseQuantity = maxBaseQuantity;
        this.spawnWeight = spawnWeight;
        this.baseValueIron = baseValueIron;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinBaseQuantity() {
        return minBaseQuantity;
    }

    public int getMaxBaseQuantity() {
        return maxBaseQuantity;
    }

    public double getSpawnWeight() {
        return spawnWeight;
    }

    public double getBaseValueIron() {
        return baseValueIron;
    }

    /**
     * Deterministically samples a base quantity in [minBaseQuantity, maxBaseQuantity].
     */
    public int sampleBaseQuantity(Random random) {
        if (random == null) return minBaseQuantity;
        return minBaseQuantity + random.nextInt(maxBaseQuantity - minBaseQuantity + 1);
    }

    /**
     * Weighted sampling of a SupernaturalResourceType based on spawn weights.
     */
    public static SupernaturalResourceType sampleByWeight(Random random) {
        if (random == null) return RAW_AETHER_CRYSTAL;
        double roll = random.nextDouble();
        if (roll < 0.50) {
            return RAW_AETHER_CRYSTAL;
        } else if (roll < 0.80) {
            return ANOMALOUS_ORE;
        } else {
            return CONDENSED_AETHER_ESSENCE;
        }
    }

    /**
     * Converts to legacy AnomalyResourceType for backward compatibility.
     */
    public AnomalyResourceType toLegacyType() {
        switch (this) {
            case RAW_AETHER_CRYSTAL:
                return AnomalyResourceType.AETHER_CRYSTAL;
            case ANOMALOUS_ORE:
                return AnomalyResourceType.CELESTIAL_ORE;
            case CONDENSED_AETHER_ESSENCE:
                return AnomalyResourceType.PRIMORDIAL_ESSENCE;
            default:
                return AnomalyResourceType.AETHER_CRYSTAL;
        }
    }

    /**
     * Converts from legacy AnomalyResourceType.
     */
    public static SupernaturalResourceType fromLegacyType(AnomalyResourceType legacy) {
        if (legacy == null) return RAW_AETHER_CRYSTAL;
        switch (legacy) {
            case AETHER_CRYSTAL:
            case SPIRIT_LOTUS:
                return RAW_AETHER_CRYSTAL;
            case CELESTIAL_ORE:
            case VOID_SHARD:
                return ANOMALOUS_ORE;
            case PRIMORDIAL_ESSENCE:
                return CONDENSED_AETHER_ESSENCE;
            default:
                return RAW_AETHER_CRYSTAL;
        }
    }
}

