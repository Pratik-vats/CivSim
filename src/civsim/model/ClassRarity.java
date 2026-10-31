package civsim.model;

/**
 * Rarity tiers for supernatural class manifestation.
 * Higher rarity grants higher starting attribute bonuses and unlocks higher power tier ceilings.
 *
 * Distribution (approximate):
 * - COMMON:    40%
 * - UNCOMMON:  25%
 * - RARE:      18%
 * - EPIC:      10%
 * - LEGENDARY:  5%
 * - MYTHIC:     2% (global cap: max 2 living Mythic beings worldwide)
 */
public enum ClassRarity {
    COMMON("Common", 0.40, 1, 5),
    UNCOMMON("Uncommon", 0.25, 1, 6),
    RARE("Rare", 0.18, 1, 7),
    EPIC("Epic", 0.10, 1, 8),
    LEGENDARY("Legendary", 0.05, 1, 8),
    MYTHIC("Mythic", 0.02, 1, 10);

    private final String displayName;
    private final double baseProbability;
    private final int startingTier;
    private final int maxPowerTier;

    ClassRarity(String displayName, double baseProbability, int startingTier, int maxPowerTier) {
        this.displayName = displayName;
        this.baseProbability = baseProbability;
        this.startingTier = startingTier;
        this.maxPowerTier = maxPowerTier;
    }

    public String getDisplayName() { return displayName; }
    public double getBaseProbability() { return baseProbability; }
    public int getStartingTier() { return startingTier; }
    public int getMaxPowerTier() { return maxPowerTier; }

    /**
     * Roll a rarity from a 0.0–1.0 roll value using cumulative distribution.
     * Lower rolls = more common.
     */
    public static ClassRarity fromRoll(double roll) {
        double cumulative = 0.0;
        for (ClassRarity rarity : values()) {
            cumulative += rarity.baseProbability;
            if (roll < cumulative) {
                return rarity;
            }
        }
        return COMMON; // fallback
    }
}
