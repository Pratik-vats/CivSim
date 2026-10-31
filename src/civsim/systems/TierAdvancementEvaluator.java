package civsim.systems;

import civsim.ProgressionConfig;
import civsim.ProgressionConfig.TierTransitionRule;
import civsim.model.AdvancementEvaluationResult;
import civsim.model.AetherAspect;
import civsim.model.AetherProfile;
import civsim.model.AttributeType;
import civsim.model.BreakthroughAttemptRecord;
import civsim.model.Person;
import civsim.model.SupernaturalClass;
import civsim.model.SupernaturalProgressionRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Stage 2A.6 — Complete Gated Tier Advancement & Breakthrough Evaluator.
 * Handles eligibility verification across the 5 required gates, failure cooldowns,
 * post-failure preparation snapshots, surplus odds calculation, and seeded idempotent execution.
 */
public class TierAdvancementEvaluator {

    /**
     * Non-destructively evaluates all gates for T→T+1 with year and month context.
     * Records the evaluation in the character's progression history.
     */
    public static AdvancementEvaluationResult evaluateAdvancementEligibility(
            Person person, ProgressionConfig config, int currentYear, int currentMonth) {

        List<String> satisfied = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (person == null || !person.isAlive()) {
            missing.add("DECEASED");
            return new AdvancementEvaluationResult(
                    false, 0, 0, satisfied, missing, currentYear,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    false, 0L, false, 0.0, 0.0);
        }

        AetherProfile ap = person.getAetherProfile();
        if (ap == null || !ap.isAwakened()) {
            missing.add("NOT_AWAKENED");
            return new AdvancementEvaluationResult(
                    false, 0, 0, satisfied, missing, currentYear,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    false, 0L, false, 0.0, 0.0);
        }

        int currentTier = ap.getPowerTier();
        int targetTier = currentTier + 1;
        int maxTier = ap.getMaxPowerTier();

        // Gate 1: Rarity Ceiling Check
        if (currentTier >= maxTier) {
            missing.add(String.format("AT_MAXIMUM_TIER_CEILING (T%d/%d for %s)",
                    currentTier, maxTier, ap.getRarity().name()));
            AdvancementEvaluationResult result = new AdvancementEvaluationResult(
                    false, currentTier, targetTier, satisfied, missing, currentYear,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    false, 0L, false, 0.0, 0.0);
            recordEvaluation(ap, result);
            return result;
        }
        satisfied.add(String.format("BELOW_TIER_CEILING (T%d < T%d)", currentTier, maxTier));

        // Gate 2: Transition Rule Existence & Status
        if (!config.hasTransitionRule(currentTier)) {
            missing.add(String.format("MISSING_APPROVED_TRANSITION_RULE (T%d→T%d)", currentTier, targetTier));
            AdvancementEvaluationResult result = new AdvancementEvaluationResult(
                    false, currentTier, targetTier, satisfied, missing, currentYear,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    false, 0L, false, 0.0, 0.0);
            recordEvaluation(ap, result);
            return result;
        }

        TierTransitionRule rule = config.getTransitionRule(currentTier);
        if (!rule.isAdvancementEnabled()) {
            missing.add(String.format("TRANSITION_RULE_DISABLED (T%d→T%d)", currentTier, targetTier));
            AdvancementEvaluationResult result = new AdvancementEvaluationResult(
                    false, currentTier, targetTier, satisfied, missing, currentYear,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    false, 0L, false, 0.0, 0.0);
            recordEvaluation(ap, result);
            return result;
        }
        satisfied.add("TRANSITION_RULE_ENABLED");

        SupernaturalProgressionRecord record = ap.getProgressionRecord();
        if (record == null) {
            missing.add("NO_PROGRESSION_RECORD");
            AdvancementEvaluationResult result = new AdvancementEvaluationResult(
                    false, currentTier, targetTier, satisfied, missing, currentYear,
                    0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    false, 0L, false, 0.0, 0.0);
            recordEvaluation(ap, result);
            return result;
        }

        // Gate 3: Failure Cooldown Check
        long currentTotalMonths = (long) currentYear * 12 + currentMonth;
        boolean activeCooldown = record.hasActiveCooldown(targetTier, currentTotalMonths);
        if (activeCooldown) {
            missing.add(String.format("ACTIVE_BREAKTHROUGH_COOLDOWN (cooldown until month %d, current month %d)",
                    record.getCooldownUntilTotalMonths(), currentTotalMonths));
        } else {
            satisfied.add("NO_ACTIVE_COOLDOWN");
        }

        // Gate 4: Post-Failure Preparation Snapshot Check
        boolean insufficientPrep = record.hasInsufficientPostFailurePreparation(targetTier);
        if (insufficientPrep) {
            missing.add(String.format("INSUFFICIENT_POST_FAILURE_PREPARATION (trn: %.1f < %.1f, exp: %.1f < %.1f)",
                    record.getTrainingProgress(), record.getRequiredRetryTrainingProgress(),
                    record.getSupernaturalExperience(), record.getRequiredRetryExperience()));
        } else {
            satisfied.add("POST_FAILURE_PREPARATION_SATISFIED");
        }

        // Gate 5A: Minimum Mastery
        if (record.getMastery() >= rule.getMinMastery()) {
            satisfied.add(String.format("MASTERY_MET (%.1f >= %.1f)", record.getMastery(), rule.getMinMastery()));
        } else {
            missing.add(String.format("MASTERY_INSUFFICIENT (%.1f < %.1f)", record.getMastery(), rule.getMinMastery()));
        }

        // Gate 5B: Minimum Class-Specific Primary Attribute
        SupernaturalClass cls = ap.getSupernaturalClass();
        AttributeType primaryType = cls.getPrimaryAttributeType();
        double effPrimary = ap.getEffectiveAttributeDouble(primaryType);
        if (effPrimary >= rule.getMinPrimaryAttribute()) {
            satisfied.add(String.format("PRIMARY_ATTR_MET (%s: %.1f >= %.1f)",
                    primaryType.name(), effPrimary, rule.getMinPrimaryAttribute()));
        } else {
            missing.add(String.format("PRIMARY_ATTR_INSUFFICIENT (%s: %.1f < %.1f)",
                    primaryType.name(), effPrimary, rule.getMinPrimaryAttribute()));
        }

        // Gate 5C: Minimum Class-Specific Weighted Aptitude Score (0.60 Primary + 0.20 Sec1 + 0.20 Sec2)
        AttributeType sec1Type = cls.getSecondaryAttribute1Type();
        AttributeType sec2Type = cls.getSecondaryAttribute2Type();
        double effSec1 = ap.getEffectiveAttributeDouble(sec1Type);
        double effSec2 = ap.getEffectiveAttributeDouble(sec2Type);
        double weightedAptitude = 0.60 * effPrimary + 0.20 * effSec1 + 0.20 * effSec2;
        if (weightedAptitude >= rule.getMinWeightedAptitude()) {
            satisfied.add(String.format("WEIGHTED_APTITUDE_MET (%.1f >= %.1f)",
                    weightedAptitude, rule.getMinWeightedAptitude()));
        } else {
            missing.add(String.format("WEIGHTED_APTITUDE_INSUFFICIENT (%.1f < %.1f)",
                    weightedAptitude, rule.getMinWeightedAptitude()));
        }

        // Gate 5D: Minimum Training Progress
        if (record.getTrainingProgress() >= rule.getMinTrainingProgress()) {
            satisfied.add(String.format("TRAINING_MET (%.1f >= %.1f)",
                    record.getTrainingProgress(), rule.getMinTrainingProgress()));
        } else {
            missing.add(String.format("TRAINING_INSUFFICIENT (%.1f < %.1f)",
                    record.getTrainingProgress(), rule.getMinTrainingProgress()));
        }

        // Gate 5E: Minimum Supernatural Experience
        if (record.getSupernaturalExperience() >= rule.getMinExperience()) {
            satisfied.add(String.format("EXPERIENCE_MET (%.1f >= %.1f)",
                    record.getSupernaturalExperience(), rule.getMinExperience()));
        } else {
            missing.add(String.format("EXPERIENCE_INSUFFICIENT (%.1f < %.1f)",
                    record.getSupernaturalExperience(), rule.getMinExperience()));
        }

        // Backward compatibility: custom minAttributes check
        Map<String, Integer> attrReqs = rule.getMinAttributes();
        for (Map.Entry<String, Integer> entry : attrReqs.entrySet()) {
            int actual = getAttributeByName(ap, entry.getKey());
            if (actual >= entry.getValue()) {
                satisfied.add(String.format("ATTR_%s_MET (%d >= %d)", entry.getKey(), actual, entry.getValue()));
            } else {
                missing.add(String.format("ATTR_%s_INSUFFICIENT (%d < %d)", entry.getKey(), actual, entry.getValue()));
            }
        }

        // --- Surplus Calculation ---
        double masterySurplus = Math.max(0.0, record.getMastery() - rule.getMinMastery());
        double primaryAttrSurplus = Math.max(0.0, effPrimary - rule.getMinPrimaryAttribute());
        double aptitudeSurplus = Math.max(0.0, weightedAptitude - rule.getMinWeightedAptitude());
        double trainingSurplus = Math.max(0.0, record.getTrainingProgress() - rule.getMinTrainingProgress());
        double experienceSurplus = Math.max(0.0, record.getSupernaturalExperience() - rule.getMinExperience());

        // --- Probability & Bounded Surplus Modifiers ---
        double baseChancePercent = rule.getBaseChancePercent();
        double bMastery = Math.min(5.0, 0.25 * masterySurplus);
        double bPrimary = Math.min(3.0, 0.10 * primaryAttrSurplus);
        double bAptitude = Math.min(3.0, 0.10 * aptitudeSurplus);

        double denom = rule.getMinTrainingProgress() + rule.getMinExperience();
        double bTraining = (denom > 0.0)
                ? Math.min(2.0, 2.0 * ((trainingSurplus + experienceSurplus) / denom))
                : 0.0;

        double bLuck = Math.max(-2.0, Math.min(4.0, (ap.getEffectiveLuckDouble() - 20.0) * 0.05));

        double rawChance = baseChancePercent + bMastery + bPrimary + bAptitude + bTraining + bLuck;
        double finalChancePercent = (rule.getBaseSuccessChance() >= 1.0)
                ? 100.0
                : Math.max(5.0, Math.min(95.0, rawChance));

        boolean eligible = missing.isEmpty();
        AdvancementEvaluationResult result = new AdvancementEvaluationResult(
                eligible, currentTier, targetTier, satisfied, missing, currentYear,
                masterySurplus, primaryAttrSurplus, aptitudeSurplus,
                trainingSurplus, experienceSurplus,
                baseChancePercent, bMastery, bPrimary, bAptitude, bTraining, bLuck,
                finalChancePercent,
                activeCooldown, record.getCooldownUntilTotalMonths(),
                insufficientPrep, record.getRequiredRetryTrainingProgress(), record.getRequiredRetryExperience());

        recordEvaluation(ap, result);
        return result;
    }

    public static AdvancementEvaluationResult evaluateAdvancementEligibility(
            Person person, ProgressionConfig config, int currentYear) {
        return evaluateAdvancementEligibility(person, config, currentYear, 1);
    }

    /**
     * Executes an atomic, seeded, idempotent breakthrough attempt.
     * Returns the BreakthroughAttemptRecord representing the outcome.
     */
    public static BreakthroughAttemptRecord attemptTierAdvancement(
            Person person, ProgressionConfig config, String attemptEventId,
            int currentYear, int currentMonth, Random random) {

        if (person == null || !person.isAlive()) {
            return null;
        }

        AetherProfile ap = person.getAetherProfile();
        if (ap == null || !ap.isAwakened()) {
            return null;
        }

        SupernaturalProgressionRecord record = ap.getProgressionRecord();
        if (record == null) {
            return null;
        }

        // Idempotency: return already processed attempt
        if (record.hasAttempted(attemptEventId)) {
            return record.getBreakthroughAttempt(attemptEventId);
        }

        // Evaluate eligibility against live authoritative stats
        AdvancementEvaluationResult eval = evaluateAdvancementEligibility(person, config, currentYear, currentMonth);

        int currentTier = eval.getCurrentTier();
        int targetTier = eval.getTargetTier();
        AetherAspect aspect = ap.isDarkAwakened() ? AetherAspect.DARK : AetherAspect.AETHERIC;

        if (!eval.isEligible()) {
            // Ineligible attempt — record without rolling RNG
            BreakthroughAttemptRecord ineligibleRecord = new BreakthroughAttemptRecord(
                    attemptEventId, person.getId(), currentYear, currentMonth,
                    currentTier, targetTier, ap.getSupernaturalClass(), aspect,
                    ap.getRarity(), false,
                    eval.getSatisfiedRequirements(), eval.getMissingRequirements(),
                    eval.getBaseChancePercent() / 100.0,
                    eval.getMasteryBonus(), eval.getPrimaryBonus(), eval.getAptitudeBonus(),
                    eval.getTrainingBonus(), eval.getLuckBonus(),
                    eval.getFinalChance(), -1.0, false,
                    record.getCooldownUntilTotalMonths(),
                    record.getRequiredRetryTrainingProgress(),
                    record.getRequiredRetryExperience());

            record.recordBreakthroughAttempt(ineligibleRecord);
            return ineligibleRecord;
        }

        // Roll seeded RNG once against finalChance
        Random rng = (random != null) ? random : new Random(((long) person.getId() << 32) ^ (attemptEventId != null ? attemptEventId.hashCode() : 42L));
        double roll = rng.nextDouble();
        double finalChance = eval.getFinalChance();
        boolean success = roll < finalChance;

        if (success) {
            // Guarded advancement
            ap.advanceTierValidated(targetTier, currentYear);
            record.clearFailureSnapshot();
            record.addMilestone(String.format("Y%d M%d: BREAKTHROUGH SUCCESS T%d→T%d (event: %s, roll: %.3f < %.3f)",
                    currentYear, currentMonth, currentTier, targetTier, attemptEventId, roll, finalChance));

            BreakthroughAttemptRecord successRecord = new BreakthroughAttemptRecord(
                    attemptEventId, person.getId(), currentYear, currentMonth,
                    currentTier, targetTier, ap.getSupernaturalClass(), aspect,
                    ap.getRarity(), true,
                    eval.getSatisfiedRequirements(), Collections.emptyList(),
                    eval.getBaseChancePercent() / 100.0,
                    eval.getMasteryBonus(), eval.getPrimaryBonus(), eval.getAptitudeBonus(),
                    eval.getTrainingBonus(), eval.getLuckBonus(),
                    finalChance, roll, true,
                    0L, 0.0, 0.0);

            record.recordBreakthroughAttempt(successRecord);
            return successRecord;
        } else {
            // Failure — apply cooldown and retry snapshot
            TierTransitionRule rule = config.getTransitionRule(currentTier);
            long currentTotalMonths = (long) currentYear * 12 + currentMonth;
            record.recordBreakthroughFailure(
                    targetTier, currentTotalMonths,
                    rule.getCooldownMonths(), rule.getRetryPreparationFraction(),
                    rule.getMinTrainingProgress(), rule.getMinExperience());

            record.addMilestone(String.format(
                    "Y%d M%d: BREAKTHROUGH FAILED T%d→T%d (event: %s, roll: %.3f >= %.3f, cd:%dmo)",
                    currentYear, currentMonth, currentTier, targetTier, attemptEventId, roll, finalChance, rule.getCooldownMonths()));

            BreakthroughAttemptRecord failureRecord = new BreakthroughAttemptRecord(
                    attemptEventId, person.getId(), currentYear, currentMonth,
                    currentTier, targetTier, ap.getSupernaturalClass(), aspect,
                    ap.getRarity(), true,
                    eval.getSatisfiedRequirements(), Collections.emptyList(),
                    eval.getBaseChancePercent() / 100.0,
                    eval.getMasteryBonus(), eval.getPrimaryBonus(), eval.getAptitudeBonus(),
                    eval.getTrainingBonus(), eval.getLuckBonus(),
                    finalChance, roll, false,
                    record.getCooldownUntilTotalMonths(),
                    record.getRequiredRetryTrainingProgress(),
                    record.getRequiredRetryExperience());

            record.recordBreakthroughAttempt(failureRecord);
            return failureRecord;
        }
    }

    public static BreakthroughAttemptRecord attemptTierAdvancement(
            Person person, String attemptEventId, int currentYear, int currentMonth) {
        long seed = ((long) person.getId() << 32) ^ (attemptEventId != null ? attemptEventId.hashCode() : 42L);
        return attemptTierAdvancement(person, ProgressionConfig.createDefault(), attemptEventId, currentYear, currentMonth, new Random(seed));
    }

    /**
     * Backward-compatible boolean advancement method for Stage 2A.4 callers.
     */
    public static boolean attemptTierAdvancement(
            Person person, ProgressionConfig config, String attemptEventId, int currentYear) {
        long seed = ((long) person.getId() << 32) ^ (attemptEventId != null ? attemptEventId.hashCode() : 42L);
        BreakthroughAttemptRecord attempt = attemptTierAdvancement(
                person, config, attemptEventId, currentYear, 1, new Random(seed));
        return attempt != null && attempt.isSuccess();
    }

    /**
     * Resolves an attribute name to its effective value on the AetherProfile.
     */
    private static int getAttributeByName(AetherProfile ap, String attrName) {
        switch (attrName.toUpperCase()) {
            case "STRENGTH": case "STR": return ap.getEffectiveStrength();
            case "AGILITY": case "AGI": return ap.getEffectiveAgility();
            case "VITALITY": case "VIT": return ap.getEffectiveVitality();
            case "ENDURANCE": case "END": return ap.getEffectiveEndurance();
            case "INTELLIGENCE": case "INT": return ap.getEffectiveIntelligence();
            case "PERCEPTION": case "PER": return ap.getEffectivePerception();
            case "WILLPOWER": case "WIL": return ap.getEffectiveWillpower();
            case "WISDOM": case "WIS": return ap.getEffectiveWisdom();
            case "LUCK": return ap.getEffectiveLuck();
            case "AETHER_CAPACITY": case "AET_CAP": return ap.getEffectiveAetherCapacity();
            case "AETHER_CONTROL": case "AET_CTRL": return ap.getEffectiveAetherControl();
            case "AETHER": case "AET": return ap.getEffectiveAether();
            default: return 0;
        }
    }

    private static void recordEvaluation(AetherProfile ap, AdvancementEvaluationResult result) {
        SupernaturalProgressionRecord record = ap.getProgressionRecord();
        if (record != null) {
            record.addEvaluationMilestone(result);
        }
    }
}
