package civsim.model;

/**
 * Phase 2B.5 — Threat ranking of individual monsters, independent of human power rarity and tiers.
 */
public enum MonsterThreatRank {
    MINOR("Minor", 1.0, 0.03, ResourceGrade.COMMON),
    STANDARD("Standard", 2.2, 0.15, ResourceGrade.COMMON),
    ELITE("Elite", 4.5, 0.45, ResourceGrade.UNCOMMON),
    ALPHA("Alpha", 10.0, 0.85, ResourceGrade.RARE),
    ANCIENT("Ancient", 25.0, 1.00, ResourceGrade.LEGENDARY);

    private final String displayName;
    private final double powerMultiplier;
    private final double baseCoreDropChance;
    private final ResourceGrade baseCoreGrade;

    MonsterThreatRank(String displayName, double powerMultiplier, double baseCoreDropChance, ResourceGrade baseCoreGrade) {
        this.displayName = displayName;
        this.powerMultiplier = powerMultiplier;
        this.baseCoreDropChance = baseCoreDropChance;
        this.baseCoreGrade = baseCoreGrade;
    }

    public String getDisplayName() { return displayName; }
    public double getPowerMultiplier() { return powerMultiplier; }
    public double getBaseCoreDropChance() { return baseCoreDropChance; }
    public ResourceGrade getBaseCoreGrade() { return baseCoreGrade; }

    public boolean isApexOrAncient() {
        return this == ALPHA || this == ANCIENT;
    }
}

