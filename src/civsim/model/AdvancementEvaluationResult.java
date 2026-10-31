package civsim.model;

import java.util.Collections;
import java.util.List;

/**
 * Stage 2A.4 / 2A.6 — Immutable result of a tier advancement eligibility evaluation.
 * Separates eligibility inspection from advancement execution.
 * Tracks surpluses, base probability, preparation bonuses, and cooldown/retry constraints.
 */
public final class AdvancementEvaluationResult {
    private final boolean eligible;
    private final int currentTier;
    private final int targetTier;
    private final List<String> satisfiedRequirements;
    private final List<String> missingRequirements;
    private final int evaluationYear;

    // Stage 2A.6: Surplus tracking
    private final double masterySurplus;
    private final double primaryAttrSurplus;
    private final double aptitudeSurplus;
    private final double trainingSurplus;
    private final double experienceSurplus;

    // Stage 2A.6: Probabilities and bounded bonuses (percentage points)
    private final double baseChancePercent;
    private final double masteryBonus;
    private final double primaryBonus;
    private final double aptitudeBonus;
    private final double trainingBonus;
    private final double luckBonus;
    private final double finalChancePercent;

    // Stage 2A.6: Failure cooldown and retry status
    private final boolean activeCooldown;
    private final long cooldownUntilMonths;
    private final boolean insufficientPreparation;
    private final double requiredRetryTraining;
    private final double requiredRetryExp;

    public AdvancementEvaluationResult(
            boolean eligible, int currentTier, int targetTier,
            List<String> satisfiedRequirements,
            List<String> missingRequirements,
            int evaluationYear,
            double masterySurplus, double primaryAttrSurplus, double aptitudeSurplus,
            double trainingSurplus, double experienceSurplus,
            double baseChancePercent, double masteryBonus, double primaryBonus,
            double aptitudeBonus, double trainingBonus, double luckBonus,
            double finalChancePercent,
            boolean activeCooldown, long cooldownUntilMonths,
            boolean insufficientPreparation, double requiredRetryTraining, double requiredRetryExp) {
        this.eligible = eligible;
        this.currentTier = currentTier;
        this.targetTier = targetTier;
        this.satisfiedRequirements = Collections.unmodifiableList(satisfiedRequirements);
        this.missingRequirements = Collections.unmodifiableList(missingRequirements);
        this.evaluationYear = evaluationYear;
        this.masterySurplus = Math.max(0.0, masterySurplus);
        this.primaryAttrSurplus = Math.max(0.0, primaryAttrSurplus);
        this.aptitudeSurplus = Math.max(0.0, aptitudeSurplus);
        this.trainingSurplus = Math.max(0.0, trainingSurplus);
        this.experienceSurplus = Math.max(0.0, experienceSurplus);
        this.baseChancePercent = baseChancePercent;
        this.masteryBonus = masteryBonus;
        this.primaryBonus = primaryBonus;
        this.aptitudeBonus = aptitudeBonus;
        this.trainingBonus = trainingBonus;
        this.luckBonus = luckBonus;
        this.finalChancePercent = finalChancePercent;
        this.activeCooldown = activeCooldown;
        this.cooldownUntilMonths = cooldownUntilMonths;
        this.insufficientPreparation = insufficientPreparation;
        this.requiredRetryTraining = requiredRetryTraining;
        this.requiredRetryExp = requiredRetryExp;
    }

    /**
     * Backward-compatible constructor for Stage 2A.4 callers.
     */
    public AdvancementEvaluationResult(boolean eligible, int currentTier, int targetTier,
                                       List<String> satisfiedRequirements,
                                       List<String> missingRequirements,
                                       int evaluationYear) {
        this(eligible, currentTier, targetTier, satisfiedRequirements, missingRequirements, evaluationYear,
                0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                false, 0L, false, 0.0, 0.0);
    }

    public boolean isEligible() { return eligible; }
    public int getCurrentTier() { return currentTier; }
    public int getTargetTier() { return targetTier; }
    public List<String> getSatisfiedRequirements() { return satisfiedRequirements; }
    public List<String> getMissingRequirements() { return missingRequirements; }
    public int getEvaluationYear() { return evaluationYear; }

    public double getMasterySurplus() { return masterySurplus; }
    public double getPrimaryAttrSurplus() { return primaryAttrSurplus; }
    public double getAptitudeSurplus() { return aptitudeSurplus; }
    public double getTrainingSurplus() { return trainingSurplus; }
    public double getExperienceSurplus() { return experienceSurplus; }

    public double getBaseChancePercent() { return baseChancePercent; }
    public double getMasteryBonus() { return masteryBonus; }
    public double getPrimaryBonus() { return primaryBonus; }
    public double getAptitudeBonus() { return aptitudeBonus; }
    public double getTrainingBonus() { return trainingBonus; }
    public double getLuckBonus() { return luckBonus; }
    public double getFinalChancePercent() { return finalChancePercent; }
    public double getFinalChance() { return finalChancePercent / 100.0; }

    public boolean hasActiveCooldown() { return activeCooldown; }
    public long getCooldownUntilMonths() { return cooldownUntilMonths; }
    public boolean hasInsufficientPostFailurePreparation() { return insufficientPreparation; }
    public double getRequiredRetryTraining() { return requiredRetryTraining; }
    public double getRequiredRetryExp() { return requiredRetryExp; }

    @Override
    public String toString() {
        return String.format("AdvancementEvaluation[T%d→T%d, eligible=%b, met=%d, missing=%d, chance=%.1f%%]",
                currentTier, targetTier, eligible,
                satisfiedRequirements.size(), missingRequirements.size(), finalChancePercent);
    }
}
