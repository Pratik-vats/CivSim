package civsim.model;

/**
 * Phase 2B.2 — Configurable leadership succession policies.
 */
public enum SuccessionPolicy {
    LINEAGE_AND_SENIORITY("Lineage & Seniority", 20.0, 30.0, 10.0, 30.0, 10.0),
    MASTERY_AND_TIER_MERIT("Mastery & Tier Merit", 15.0, 15.0, 45.0, 5.0, 20.0),
    COUNCIL_OR_ELDER_ELECTION("Council/Elder Election", 35.0, 25.0, 10.0, 0.0, 30.0),
    COMBAT_PROWESS_AND_EXPERIENCE("Combat Prowess & Experience", 20.0, 15.0, 40.0, 5.0, 20.0),
    AMBITION_AND_POWER("Ambition & Power", 10.0, 10.0, 50.0, 0.0, 30.0),
    SETTLEMENT_APPOINTED("Settlement Appointed", 30.0, 30.0, 15.0, 5.0, 20.0);

    private final String displayName;
    private final double weightRoleRank;
    private final double weightTenure;
    private final double weightPower;
    private final double weightKinship;
    private final double weightLeadership;

    SuccessionPolicy(String displayName,
                     double weightRoleRank,
                     double weightTenure,
                     double weightPower,
                     double weightKinship,
                     double weightLeadership) {
        this.displayName = displayName;
        this.weightRoleRank = weightRoleRank;
        this.weightTenure = weightTenure;
        this.weightPower = weightPower;
        this.weightKinship = weightKinship;
        this.weightLeadership = weightLeadership;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getWeightRoleRank() {
        return weightRoleRank;
    }

    public double getWeightTenure() {
        return weightTenure;
    }

    public double getWeightPower() {
        return weightPower;
    }

    public double getWeightKinship() {
        return weightKinship;
    }

    public double getWeightLeadership() {
        return weightLeadership;
    }
}

