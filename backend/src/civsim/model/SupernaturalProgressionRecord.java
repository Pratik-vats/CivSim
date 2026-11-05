package civsim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 2A.4 & 2A.6 — Per-person supernatural progression tracking.
 * Attached to each awakened person's AetherProfile.
 *
 * Tracks mastery, training progress, experience, processed event IDs (for idempotency),
 * significant milestones, breakthrough attempts, failure snapshots, and aggregate counters.
 *
 * When a character dies, createDeathSummary() produces a compact immutable summary
 * and the full event set can be cleared for heap savings.
 */
public class SupernaturalProgressionRecord {

    // --- Core Progression State ---
    private double mastery;                     // 0.0–100.0
    private double trainingProgress;            // 0.0+
    private double supernaturalExperience;      // 0.0+
    private PrimaryAbility primaryAbility;
    private double aptitudeModifier;            // cached, [0.50–2.00]

    // --- Awakening Snapshot ---
    private final int awakeningYear;
    private final int awakeningAge;
    private final AwakeningPathway awakeningPathway;
    private final SupernaturalClass supernaturalClass;
    private final ClassRarity rarity;
    private final int maxPermittedTier;
    private final double initialMastery;

    // --- Idempotency ---
    private final Set<String> processedEventIds;

    // --- Milestone Log (bounded) ---
    private final List<String> milestones;
    private static final int MAX_MILESTONES = 50;

    // --- Stage 2A.6: Breakthrough Failure Snapshot & Cooldown ---
    private int failedTargetTier = 0;
    private long cooldownUntilTotalMonths = 0;
    private double requiredRetryTrainingProgress = 0.0;
    private double requiredRetryExperience = 0.0;

    // --- Stage 2A.6: Breakthrough Attempts History ---
    private final List<BreakthroughAttemptRecord> breakthroughAttempts = new ArrayList<>();
    private final Map<String, BreakthroughAttemptRecord> breakthroughAttemptMap = new HashMap<>();
    private static final int MAX_BREAKTHROUGH_HISTORY = 30;
    private int totalBreakthroughAttempts = 0;
    private int totalBreakthroughSuccesses = 0;
    private int totalBreakthroughFailures = 0;

    // --- Aggregate Counters ---
    private int totalTrainingEvents;
    private int totalAbilityUseEvents;
    private int totalProblemSolvingEvents;
    private int totalDemandingEvents;
    private double maxMasteryReached;

    // --- Milestone Tracking ---
    private boolean crossed25;
    private boolean crossed50;
    private boolean crossed75;
    private boolean crossed90;
    private boolean crossed100;

    public SupernaturalProgressionRecord(int awakeningYear, int awakeningAge,
                                          AwakeningPathway pathway, SupernaturalClass cls,
                                          ClassRarity rarity, double initialMastery,
                                          PrimaryAbility primaryAbility) {
        this.awakeningYear = awakeningYear;
        this.awakeningAge = awakeningAge;
        this.awakeningPathway = pathway;
        this.supernaturalClass = cls;
        this.rarity = rarity;
        this.maxPermittedTier = rarity.getMaxPowerTier();
        this.initialMastery = initialMastery;
        this.mastery = initialMastery;
        this.primaryAbility = primaryAbility;
        this.trainingProgress = 0.0;
        this.supernaturalExperience = 0.0;
        this.aptitudeModifier = 1.0; // set externally after creation

        this.processedEventIds = new HashSet<>();
        this.milestones = new ArrayList<>();

        this.totalTrainingEvents = 0;
        this.totalAbilityUseEvents = 0;
        this.totalProblemSolvingEvents = 0;
        this.totalDemandingEvents = 0;
        this.maxMasteryReached = initialMastery;

        this.crossed25 = initialMastery >= 25.0;
        this.crossed50 = initialMastery >= 50.0;
        this.crossed75 = initialMastery >= 75.0;
        this.crossed90 = initialMastery >= 90.0;
        this.crossed100 = initialMastery >= 100.0;

        addMilestone(String.format("Y%d: Awakened as %s %s (%s) via %s — initial mastery %.1f",
                awakeningYear, rarity.getDisplayName(), cls.getDisplayName(),
                primaryAbility.getDisplayName(), pathway.getDisplayName(), initialMastery));
    }

    // --- Event Idempotency ---

    /** Returns true if this eventId has already been processed */
    public boolean hasProcessedEvent(String eventId) {
        return processedEventIds.contains(eventId);
    }

    /** Marks an eventId as processed. Returns false if already processed (duplicate). */
    public boolean markEventProcessed(String eventId) {
        return processedEventIds.add(eventId);
    }

    // --- Mastery Updates ---

    /**
     * Applies mastery gain with clamping. Tracks milestone crossings.
     * @return the actual delta applied
     */
    public double applyMasteryGain(double delta, int currentYear) {
        double oldMastery = this.mastery;
        this.mastery = Math.max(0.0, Math.min(100.0, this.mastery + delta));
        double actualDelta = this.mastery - oldMastery;

        if (this.mastery > this.maxMasteryReached) {
            this.maxMasteryReached = this.mastery;
        }

        // Check milestone crossings
        checkMilestoneCrossing(oldMastery, 25.0, "Beginner→Developing", currentYear);
        checkMilestoneCrossing(oldMastery, 50.0, "Developing→Reliable", currentYear);
        checkMilestoneCrossing(oldMastery, 75.0, "Advanced→Exceptional", currentYear);
        checkMilestoneCrossing(oldMastery, 90.0, "Exceptional→Near-Perfect", currentYear);
        if (!crossed100 && this.mastery >= 100.0) {
            crossed100 = true;
            addMilestone(String.format("Y%d: Reached mastery ceiling (100.0)", currentYear));
        }

        return actualDelta;
    }

    private void checkMilestoneCrossing(double oldMastery, double threshold, String label, int year) {
        boolean wasBelow = oldMastery < threshold;
        boolean isAtOrAbove = this.mastery >= threshold;
        if (wasBelow && isAtOrAbove) {
            switch ((int) threshold) {
                case 25: if (!crossed25) { crossed25 = true; break; } else return;
                case 50: if (!crossed50) { crossed50 = true; break; } else return;
                case 75: if (!crossed75) { crossed75 = true; break; } else return;
                case 90: if (!crossed90) { crossed90 = true; break; } else return;
                default: return;
            }
            addMilestone(String.format("Y%d: Mastery crossed %.0f (%s), now %.1f",
                    year, threshold, label, this.mastery));
        }
    }

    // --- Training & Experience ---

    public void addTrainingProgress(double amount) {
        if (amount > 0) this.trainingProgress += amount;
    }

    public void addExperience(double amount) {
        if (amount > 0) this.supernaturalExperience += amount;
    }

    public void incrementEventCounter(ProgressionEventType type) {
        switch (type) {
            case DELIBERATE_TRAINING: totalTrainingEvents++; break;
            case OCCUPATIONAL_ABILITY_USE: totalAbilityUseEvents++; break;
            case PROBLEM_SOLVING: totalProblemSolvingEvents++; break;
            case DEMANDING_EVENT_SURVIVAL: totalDemandingEvents++; break;
        }
    }

    // --- Milestones ---

    public void addMilestone(String milestone) {
        if (milestones.size() < MAX_MILESTONES) {
            milestones.add(milestone);
        }
    }

    public void addEvaluationMilestone(AdvancementEvaluationResult result) {
        addMilestone(String.format("Y%d: Advancement eval T%d→T%d: %s (met:%d, missing:%d)",
                result.getEvaluationYear(), result.getCurrentTier(), result.getTargetTier(),
                result.isEligible() ? "ELIGIBLE" : "BLOCKED",
                result.getSatisfiedRequirements().size(),
                result.getMissingRequirements().size()));
    }

    private String deathSummary;

    // --- Death Summary (for heap pruning) ---

    /**
     * Creates a compact immutable summary for historical archives.
     * After calling this, processedEventIds can be cleared to save heap.
     */
    public String createDeathSummary(int deathYear, int finalTier) {
        this.deathSummary = String.format(
                "ProgSummary[%s %s %s, awakened Y%d age %d, died Y%d, " +
                "finalTier=T%d/%d, maxMastery=%.1f, training=%.1f, exp=%.1f, " +
                "breakthroughs(att:%d suc:%d fail:%d), " +
                "events(trn:%d use:%d prob:%d dmnd:%d)]",
                rarity.getDisplayName(), supernaturalClass.getDisplayName(),
                primaryAbility.getDisplayName(),
                awakeningYear, awakeningAge, deathYear,
                finalTier, maxPermittedTier, maxMasteryReached,
                trainingProgress, supernaturalExperience,
                totalBreakthroughAttempts, totalBreakthroughSuccesses, totalBreakthroughFailures,
                totalTrainingEvents, totalAbilityUseEvents,
                totalProblemSolvingEvents, totalDemandingEvents);
        return this.deathSummary;
    }

    public String createDeathSummary(int deathYear, int finalTier, int finalLongevityOffset,
                                     int naturalCeiling, String cause, boolean naturalDeath) {
        this.deathSummary = String.format(
                "ProgSummary[%s %s %s, awakened Y%d age %d, died Y%d, " +
                "finalTier=T%d/%d (offset=+%dy, ceil=%d), maxMastery=%.1f, training=%.1f, exp=%.1f, " +
                "breakthroughs(att:%d suc:%d fail:%d), cause='%s' (natural=%b), events(trn:%d use:%d prob:%d dmnd:%d)]",
                rarity.getDisplayName(), supernaturalClass.getDisplayName(),
                primaryAbility.getDisplayName(),
                awakeningYear, awakeningAge, deathYear,
                finalTier, maxPermittedTier, finalLongevityOffset, naturalCeiling, maxMasteryReached,
                trainingProgress, supernaturalExperience,
                totalBreakthroughAttempts, totalBreakthroughSuccesses, totalBreakthroughFailures,
                cause != null ? cause : "unknown", naturalDeath,
                totalTrainingEvents, totalAbilityUseEvents,
                totalProblemSolvingEvents, totalDemandingEvents);
        return this.deathSummary;
    }

    public String getDeathSummary() { return deathSummary; }

    /** Clears unbounded per-event sets to free heap memory after death */
    public void clearEventIdsForHeapPruning() {
        processedEventIds.clear();
        breakthroughAttemptMap.clear();
    }

    // --- Stage 2A.6: Breakthrough Failure & Snapshot ---

    public void recordBreakthroughFailure(int targetTier, long currentTotalMonths,
                                          int cooldownMonths, double retryFraction,
                                          double minTargetTraining, double minTargetExp) {
        this.failedTargetTier = targetTier;
        this.cooldownUntilTotalMonths = currentTotalMonths + cooldownMonths;
        this.requiredRetryTrainingProgress = this.trainingProgress + Math.ceil(minTargetTraining * retryFraction);
        this.requiredRetryExperience = this.supernaturalExperience + Math.ceil(minTargetExp * retryFraction);
    }

    public void clearFailureSnapshot() {
        this.failedTargetTier = 0;
        this.cooldownUntilTotalMonths = 0;
        this.requiredRetryTrainingProgress = 0.0;
        this.requiredRetryExperience = 0.0;
    }

    public boolean hasActiveCooldown(int targetTier, long currentTotalMonths) {
        return (targetTier == failedTargetTier) && (currentTotalMonths < cooldownUntilTotalMonths);
    }

    public boolean hasInsufficientPostFailurePreparation(int targetTier) {
        if (targetTier != failedTargetTier) return false;
        return (this.trainingProgress < this.requiredRetryTrainingProgress) ||
               (this.supernaturalExperience < this.requiredRetryExperience);
    }

    public int getFailedTargetTier() { return failedTargetTier; }
    public long getCooldownUntilTotalMonths() { return cooldownUntilTotalMonths; }
    public double getRequiredRetryTrainingProgress() { return requiredRetryTrainingProgress; }
    public double getRequiredRetryExperience() { return requiredRetryExperience; }

    public void recordBreakthroughAttempt(BreakthroughAttemptRecord attempt) {
        if (attempt == null) return;
        totalBreakthroughAttempts++;
        if (attempt.isSuccess()) {
            totalBreakthroughSuccesses++;
        } else {
            totalBreakthroughFailures++;
        }
        if (breakthroughAttempts.size() < MAX_BREAKTHROUGH_HISTORY) {
            breakthroughAttempts.add(attempt);
        }
        breakthroughAttemptMap.put(attempt.getAttemptId(), attempt);
        processedEventIds.add(attempt.getAttemptId());
    }

    public BreakthroughAttemptRecord getBreakthroughAttempt(String attemptId) {
        return breakthroughAttemptMap.get(attemptId);
    }

    public boolean hasAttempted(String attemptId) {
        return breakthroughAttemptMap.containsKey(attemptId) || processedEventIds.contains(attemptId);
    }

    public List<BreakthroughAttemptRecord> getBreakthroughAttempts() {
        return Collections.unmodifiableList(breakthroughAttempts);
    }

    public int getTotalBreakthroughAttempts() { return totalBreakthroughAttempts; }
    public int getTotalBreakthroughSuccesses() { return totalBreakthroughSuccesses; }
    public int getTotalBreakthroughFailures() { return totalBreakthroughFailures; }

    // --- Getters ---

    public double getMastery() { return mastery; }
    public double getTrainingProgress() { return trainingProgress; }
    public double getSupernaturalExperience() { return supernaturalExperience; }
    public PrimaryAbility getPrimaryAbility() { return primaryAbility; }
    public double getAptitudeModifier() { return aptitudeModifier; }
    public void setAptitudeModifier(double mod) { this.aptitudeModifier = mod; }
    public void setMastery(double mastery) { this.mastery = Math.max(0.0, Math.min(100.0, mastery)); }

    public int getAwakeningYear() { return awakeningYear; }
    public int getAwakeningAge() { return awakeningAge; }
    public AwakeningPathway getAwakeningPathway() { return awakeningPathway; }
    public SupernaturalClass getSupernaturalClass() { return supernaturalClass; }
    public ClassRarity getRarity() { return rarity; }
    public int getMaxPermittedTier() { return maxPermittedTier; }
    public double getInitialMastery() { return initialMastery; }
    public double getMaxMasteryReached() { return maxMasteryReached; }

    public int getTotalTrainingEvents() { return totalTrainingEvents; }
    public int getTotalAbilityUseEvents() { return totalAbilityUseEvents; }
    public int getTotalProblemSolvingEvents() { return totalProblemSolvingEvents; }
    public int getTotalDemandingEvents() { return totalDemandingEvents; }
    public int getTotalEvents() {
        return totalTrainingEvents + totalAbilityUseEvents + totalProblemSolvingEvents + totalDemandingEvents;
    }

    public List<String> getMilestones() { return Collections.unmodifiableList(milestones); }
    public int getProcessedEventCount() { return processedEventIds.size(); }

    /** Returns the mastery bracket label for reporting */
    public String getMasteryBracketLabel() {
        if (mastery >= 100.0) return "Ceiling";
        if (mastery >= 90.0) return "Near-Perfect";
        if (mastery >= 75.0) return "Exceptional";
        if (mastery >= 60.0) return "Advanced";
        if (mastery >= 40.0) return "Reliable";
        if (mastery >= 25.0) return "Developing";
        if (mastery >= 10.0) return "Beginner";
        return "Unstable";
    }
}
