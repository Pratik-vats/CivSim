package civsim.model;

/**
 * Phase 2C.1 -- Immutable persistent enchantment attached to an EquipmentItem.
 */
public final class EnchantmentRecord {

    private final String enchantmentId;
    private final String displayName;
    private final EnchantmentFamily family;
    private final double primaryBonus;
    private final double secondaryBonus;
    private final int potencyTier;
    private final String catalystUsed;

    public EnchantmentRecord(
            String enchantmentId,
            String displayName,
            EnchantmentFamily family,
            double primaryBonus,
            double secondaryBonus,
            int potencyTier,
            String catalystUsed) {
        if (enchantmentId == null || family == null) {
            throw new IllegalArgumentException("Enchantment ID and Family cannot be null.");
        }
        this.enchantmentId = enchantmentId;
        this.displayName = displayName != null ? displayName : family.getDisplayName();
        this.family = family;
        this.primaryBonus = primaryBonus;
        this.secondaryBonus = secondaryBonus;
        this.potencyTier = Math.max(1, Math.min(10, potencyTier));
        this.catalystUsed = catalystUsed != null ? catalystUsed : "None";
    }

    public String getEnchantmentId() {
        return enchantmentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EnchantmentFamily getFamily() {
        return family;
    }

    public double getPrimaryBonus() {
        return primaryBonus;
    }

    public double getSecondaryBonus() {
        return secondaryBonus;
    }

    public int getPotencyTier() {
        return potencyTier;
    }

    public String getCatalystUsed() {
        return catalystUsed;
    }

    @Override
    public String toString() {
        return String.format("%s (Tier %d, %+1.1f)", displayName, potencyTier, primaryBonus);
    }
}

