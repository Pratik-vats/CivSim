package civsim.model;

/**
 * Phase 2B.4 / Phase 2B.3 -- Supernatural Rare Resource Types found in World Anomalies.
 */
public enum AnomalyResourceType {
    AETHER_CRYSTAL("Aether Crystal", 10.0, 1.0),
    PRIMORDIAL_ESSENCE("Primordial Essence", 25.0, 2.5),
    VOID_SHARD("Void Shard", 15.0, 1.5),
    CELESTIAL_ORE("Celestial Ore", 20.0, 2.0),
    SPIRIT_LOTUS("Spirit Lotus", 12.0, 1.2);

    private final String displayName;
    private final double baseUnitValueIron;
    private final double rarityMultiplier;

    AnomalyResourceType(String displayName, double baseUnitValueIron, double rarityMultiplier) {
        this.displayName = displayName;
        this.baseUnitValueIron = baseUnitValueIron;
        this.rarityMultiplier = rarityMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseUnitValueIron() {
        return baseUnitValueIron;
    }

    public double getRarityMultiplier() {
        return rarityMultiplier;
    }
}
