package civsim.model;

/**
 * Stage 2A.4 — Multidimensional supernatural power assessment.
 * Computed dynamically from live state by PowerAssessmentCalculator.
 * Never stored as stale hardcoded integers.
 */
public final class DerivedPowerProfile {
    private final double overallEffectivePower;
    private final double offensivePotency;
    private final double defensiveResilience;
    private final double mobilityAndEvasion;
    private final double perceptionAndDetection;
    private final double sustainedEfficiency;

    public DerivedPowerProfile(double overallEffectivePower, double offensivePotency,
                                double defensiveResilience, double mobilityAndEvasion,
                                double perceptionAndDetection, double sustainedEfficiency) {
        this.overallEffectivePower = overallEffectivePower;
        this.offensivePotency = offensivePotency;
        this.defensiveResilience = defensiveResilience;
        this.mobilityAndEvasion = mobilityAndEvasion;
        this.perceptionAndDetection = perceptionAndDetection;
        this.sustainedEfficiency = sustainedEfficiency;
    }

    public double getOverallEffectivePower() { return overallEffectivePower; }
    public double getOffensivePotency() { return offensivePotency; }
    public double getDefensiveResilience() { return defensiveResilience; }
    public double getMobilityAndEvasion() { return mobilityAndEvasion; }
    public double getPerceptionAndDetection() { return perceptionAndDetection; }
    public double getSustainedEfficiency() { return sustainedEfficiency; }

    /** Compact one-line summary for reporting */
    public String toCompactString() {
        return String.format("PWR:%.0f OFF:%.0f DEF:%.0f MOB:%.0f PER:%.0f SUS:%.0f",
                overallEffectivePower, offensivePotency, defensiveResilience,
                mobilityAndEvasion, perceptionAndDetection, sustainedEfficiency);
    }

    @Override
    public String toString() {
        return String.format("DerivedPowerProfile[Overall=%.1f, Off=%.1f, Def=%.1f, Mob=%.1f, Per=%.1f, Sus=%.1f]",
                overallEffectivePower, offensivePotency, defensiveResilience,
                mobilityAndEvasion, perceptionAndDetection, sustainedEfficiency);
    }
}
