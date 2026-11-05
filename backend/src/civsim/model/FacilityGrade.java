package civsim.model;

/**
 * Phase 2C.1 -- Forge and workshop facility tiers.
 * Era gating: BASIC and ADVANCED operate in Mundane Era (Year 0+).
 * SUPERNATURAL, MASTERWORK, and LEGENDARY operate only in Awakening Era (Year 50+).
 */
public enum FacilityGrade {
    BASIC_FORGE("Basic Forge", 1, 0, false, 1.0),
    ADVANCED_FORGE("Advanced Forge", 2, 2, false, 1.5),
    SUPERNATURAL_FORGE("Supernatural Forge", 4, 3, true, 2.5),
    MASTERWORK_FORGE("Masterwork Forge", 7, 5, true, 4.0),
    LEGENDARY_FORGE("Legendary Forge", 10, 7, true, 6.0);

    private final String displayName;
    private final int maxEquipmentTier;
    private final int minTechLevel;
    private final boolean supernatural;
    private final double facilityScore;

    FacilityGrade(String displayName, int maxEquipmentTier, int minTechLevel, boolean supernatural, double facilityScore) {
        this.displayName = displayName;
        this.maxEquipmentTier = maxEquipmentTier;
        this.minTechLevel = minTechLevel;
        this.supernatural = supernatural;
        this.facilityScore = facilityScore;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxEquipmentTier() {
        return maxEquipmentTier;
    }

    public int getMinTechLevel() {
        return minTechLevel;
    }

    public boolean isSupernatural() {
        return supernatural;
    }

    public double getFacilityScore() {
        return facilityScore;
    }

    public boolean canOperateInEra(int currentYear, int awakeningStartYear) {
        if (!supernatural) return true;
        return currentYear >= awakeningStartYear;
    }
}

