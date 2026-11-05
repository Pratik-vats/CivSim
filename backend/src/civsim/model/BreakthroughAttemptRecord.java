package civsim.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stage 2A.6 — Immutable historical and runtime record of a tier breakthrough attempt.
 * Preserved in individual progression history and summarized upon death.
 */
public final class BreakthroughAttemptRecord {

    private final String attemptId;
    private final int personId;
    private final int simulationYear;
    private final int simulationMonth;
    private final int fromTier;
    private final int toTier;
    private final SupernaturalClass supernaturalClass;
    private final boolean darkAspect;
    private final ClassRarity rarity;

    private final boolean eligible;
    private final List<String> satisfiedRequirements;
    private final List<String> missingRequirements;

    private final double baseChance;
    private final double masteryBonus;
    private final double primaryAttrBonus;
    private final double aptitudeBonus;
    private final double trainingBonus;
    private final double luckBonus;
    private final double finalChance;
    private final double roll;

    private final boolean success;
    private final long cooldownUntilMonth;
    private final double requiredRetryTrainingProgress;
    private final double requiredRetryExperience;

    public BreakthroughAttemptRecord(
            String attemptId, int personId, int simulationYear, int simulationMonth,
            int fromTier, int toTier, SupernaturalClass supernaturalClass,
            boolean darkAspect, ClassRarity rarity, boolean eligible,
            List<String> satisfiedRequirements, List<String> missingRequirements,
            double baseChance, double masteryBonus, double primaryAttrBonus,
            double aptitudeBonus, double trainingBonus, double luckBonus,
            double finalChance, double roll, boolean success, long cooldownUntilMonth,
            double requiredRetryTrainingProgress, double requiredRetryExperience) {
        this.attemptId = Objects.requireNonNull(attemptId, "attemptId must not be null");
        this.personId = personId;
        this.simulationYear = simulationYear;
        this.simulationMonth = simulationMonth;
        this.fromTier = fromTier;
        this.toTier = toTier;
        this.supernaturalClass = supernaturalClass;
        this.darkAspect = darkAspect;
        this.rarity = rarity;
        this.eligible = eligible;
        this.satisfiedRequirements = (satisfiedRequirements != null)
                ? Collections.unmodifiableList(satisfiedRequirements)
                : Collections.emptyList();
        this.missingRequirements = (missingRequirements != null)
                ? Collections.unmodifiableList(missingRequirements)
                : Collections.emptyList();
        this.baseChance = baseChance;
        this.masteryBonus = masteryBonus;
        this.primaryAttrBonus = primaryAttrBonus;
        this.aptitudeBonus = aptitudeBonus;
        this.trainingBonus = trainingBonus;
        this.luckBonus = luckBonus;
        this.finalChance = finalChance;
        this.roll = roll;
        this.success = success;
        this.cooldownUntilMonth = cooldownUntilMonth;
        this.requiredRetryTrainingProgress = requiredRetryTrainingProgress;
        this.requiredRetryExperience = requiredRetryExperience;
    }

    public BreakthroughAttemptRecord(
            String attemptId, int personId, int simulationYear, int simulationMonth,
            int fromTier, int toTier, SupernaturalClass supernaturalClass,
            AetherAspect aspect, ClassRarity rarity, boolean eligible,
            List<String> satisfiedRequirements, List<String> missingRequirements,
            double baseChance, double masteryBonus, double primaryAttrBonus,
            double aptitudeBonus, double trainingBonus, double luckBonus,
            double finalChance, double roll, boolean success, long cooldownUntilMonth,
            double requiredRetryTrainingProgress, double requiredRetryExperience) {
        this(attemptId, personId, simulationYear, simulationMonth, fromTier, toTier,
                supernaturalClass, aspect != null && aspect.isDark(), rarity, eligible,
                satisfiedRequirements, missingRequirements, baseChance, masteryBonus,
                primaryAttrBonus, aptitudeBonus, trainingBonus, luckBonus, finalChance,
                roll, success, cooldownUntilMonth, requiredRetryTrainingProgress, requiredRetryExperience);
    }

    public BreakthroughAttemptRecord(
            String attemptId, int personId, int simulationYear, int simulationMonth,
            int fromTier, int toTier, SupernaturalClass supernaturalClass,
            boolean darkAspect, ClassRarity rarity, boolean eligible,
            List<String> satisfiedRequirements, List<String> missingRequirements,
            double baseChance, double masteryBonus, double primaryAttrBonus,
            double aptitudeBonus, double trainingBonus, double luckBonus,
            double finalChance, boolean success, long cooldownUntilMonth,
            double requiredRetryTrainingProgress, double requiredRetryExperience) {
        this(attemptId, personId, simulationYear, simulationMonth, fromTier, toTier,
                supernaturalClass, darkAspect, rarity, eligible, satisfiedRequirements,
                missingRequirements, baseChance, masteryBonus, primaryAttrBonus, aptitudeBonus,
                trainingBonus, luckBonus, finalChance, -1.0, success, cooldownUntilMonth,
                requiredRetryTrainingProgress, requiredRetryExperience);
    }

    public String getAttemptId() { return attemptId; }
    public int getPersonId() { return personId; }
    public int getSimulationYear() { return simulationYear; }
    public int getSimulationMonth() { return simulationMonth; }
    public int getFromTier() { return fromTier; }
    public int getToTier() { return toTier; }
    public SupernaturalClass getSupernaturalClass() { return supernaturalClass; }
    public boolean isDarkAspect() { return darkAspect; }
    public AetherAspect getAspect() { return darkAspect ? AetherAspect.DARK : AetherAspect.AETHERIC; }
    public ClassRarity getRarity() { return rarity; }

    public boolean isEligible() { return eligible; }
    public List<String> getSatisfiedRequirements() { return satisfiedRequirements; }
    public List<String> getMissingRequirements() { return missingRequirements; }

    public double getBaseChance() { return baseChance; }
    public double getMasteryBonus() { return masteryBonus; }
    public double getPrimaryAttrBonus() { return primaryAttrBonus; }
    public double getAptitudeBonus() { return aptitudeBonus; }
    public double getTrainingBonus() { return trainingBonus; }
    public double getLuckBonus() { return luckBonus; }
    public double getFinalChance() { return finalChance; }
    public double getRoll() { return roll; }

    public boolean isSuccess() { return success; }
    public long getCooldownUntilMonth() { return cooldownUntilMonth; }
    public double getRequiredRetryTrainingProgress() { return requiredRetryTrainingProgress; }
    public double getRequiredRetryExperience() { return requiredRetryExperience; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BreakthroughAttemptRecord)) return false;
        BreakthroughAttemptRecord that = (BreakthroughAttemptRecord) o;
        return attemptId.equals(that.attemptId);
    }

    @Override
    public int hashCode() {
        return attemptId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("BreakthroughAttempt[%s: pId=%d, T%d→T%d, eligible=%b, finalChance=%.1f%%, success=%b, Y%d-M%d]",
                attemptId, personId, fromTier, toTier, eligible, finalChance, success, simulationYear, simulationMonth);
    }
}
