package civsim.systems;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.model.AdvancementEvaluationResult;
import civsim.model.AetherProfile;
import civsim.model.AttributeTrainingEvent;
import civsim.model.AttributeType;
import civsim.model.BreakthroughAttemptRecord;
import civsim.model.Occupation;
import civsim.model.Person;
import civsim.model.PrimaryAbility;
import civsim.model.ProgressionEvent;
import civsim.model.ProgressionEventType;
import civsim.model.SupernaturalClass;
import civsim.model.SupernaturalProgressionRecord;
import civsim.model.Village;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Stage 2A.4 / 2A.6 — Main supernatural progression & breakthrough system.
 * Processes explicit ProgressionEvents for mastery/training/experience,
 * focused AttributeTrainingEvents for layered attribute development,
 * and gated BreakthroughAttemptRecords for tier advancement.
 */
public class ProgressionSystem {

    // --- Year tracking for reporting ---
    private int eventsProcessedThisYear = 0;
    private int attributeEventsProcessedThisYear = 0;
    private int highMasteryGatedCount = 0; // awakened with mastery >= 75 gated at T1
    private int breakthroughAttemptsThisYear = 0;
    private int breakthroughSuccessesThisYear = 0;
    private int breakthroughFailuresThisYear = 0;

    // Per-transition tracking (indices 1..10 where index = targetTier)
    private final int[] annualAttemptsByTransition = new int[11];
    private final int[] annualSuccessesByTransition = new int[11];
    private final int[] annualFailuresByTransition = new int[11];

    private final int[] cumulativeAttemptsByTransition = new int[11];
    private final int[] cumulativeSuccessesByTransition = new int[11];
    private final int[] cumulativeFailuresByTransition = new int[11];

    private int totalCumulativeAttempts = 0;
    private int totalCumulativeSuccesses = 0;
    private int totalCumulativeFailures = 0;

    /**
     * Processes a single ProgressionEvent for a person.
     * Returns the mastery delta applied (0.0 if rejected/duplicate).
     */
    public double processEvent(Person person, ProgressionEvent event, ProgressionConfig config) {
        if (person == null || !person.isAlive()) return 0.0;
        AetherProfile ap = person.getAetherProfile();
        if (ap == null || !ap.isAwakened()) return 0.0;

        SupernaturalProgressionRecord record = ap.getProgressionRecord();
        if (record == null) return 0.0;

        // Idempotency: reject duplicates
        if (!record.markEventProcessed(event.getEventId())) {
            return 0.0;
        }

        // Increment event counter
        record.incrementEventCounter(event.getType());
        eventsProcessedThisYear++;

        // Compute gains using the centralized formula
        ProgressionEventType type = event.getType();
        double baseValue = type.getBaseEventValue();
        double aptitudeMod = computeAptitudeModifier(ap, config);
        record.setAptitudeModifier(aptitudeMod);
        double trainingQuality = event.getTrainingQuality();
        double classGainMod = config.getClassGainModifier(ap.getSupernaturalClass());
        double currentMastery = record.getMastery();

        // Diminishing returns: learning at mastery 10 is much faster than at mastery 85
        double alpha = config.getDiminishingReturnsAlpha();
        double diminishing = Math.max(0.08, Math.pow(1.0 - currentMastery / 100.0, alpha));

        // Mastery delta
        double deltaMastery = baseValue * aptitudeMod * trainingQuality * classGainMod * diminishing;
        double actualDelta = record.applyMasteryGain(deltaMastery, event.getYear());

        // Training progress (only for DELIBERATE_TRAINING)
        if (type.contributesToTrainingProgress()) {
            double trainingGain = baseValue * aptitudeMod * trainingQuality;
            record.addTrainingProgress(trainingGain);
        }

        // Experience (for occupational use, problem solving, demanding events)
        if (type.contributesToExperience()) {
            double expGain = baseValue * trainingQuality;
            record.addExperience(expGain);
        }

        return actualDelta;
    }

    /**
     * Computes and caches the class-specific learning aptitude modifier for a person.
     * Uses the authoritative effective attributes and the class's primary/secondary aptitude weights.
     *
     * AptitudeScore = w1 * PrimaryAttr + w2 * SecondaryAttr1 + w2 * SecondaryAttr2
     * AptitudeModifier = clamp(0.50 + AptitudeScore / 50.0, 0.50, 2.00)
     */
    public static double computeAptitudeModifier(AetherProfile ap, ProgressionConfig config) {
        if (ap == null || !ap.isAwakened()) return 1.0;
        SupernaturalClass cls = ap.getSupernaturalClass();

        double w1 = config.getPrimaryAptitudeWeight();
        double w2 = config.getSecondaryAptitudeWeight();

        double primaryAttr = ap.getEffectiveAttributeDouble(cls.getPrimaryAttributeType());
        double secondaryAttr1 = ap.getEffectiveAttributeDouble(cls.getSecondaryAttribute1Type());
        double secondaryAttr2 = ap.getEffectiveAttributeDouble(cls.getSecondaryAttribute2Type());

        double aptitudeScore = w1 * primaryAttr + w2 * secondaryAttr1 + w2 * secondaryAttr2;
        double aptitudeMod = 0.50 + aptitudeScore / 50.0;
        return Math.max(config.getAptitudeModifierMin(),
                Math.min(config.getAptitudeModifierMax(), aptitudeMod));
    }

    /**
     * Emits deterministic progression events during the simulation's annual work cycle.
     * Called once per year for all living awakened citizens (when year >= awakeningStartYear).
     *
     * In Stage 2A.6:
     * - Submits focused AttributeTrainingEvents for class-relevant attributes.
     * - Evaluates breakthrough eligibility and initiates seeded attempts when eligible.
     */
    public void emitAndProcessAnnualProgressionEvents(
            List<Person> allPeople, List<Village> villages,
            int currentYear, Random random,
            ProgressionConfig progConfig, SimulationConfig simConfig,
            BiConsumer<String, Boolean> eventRecorder) {

        eventsProcessedThisYear = 0;
        attributeEventsProcessedThisYear = 0;
        highMasteryGatedCount = 0;

        for (Person person : allPeople) {
            if (!person.isAlive()) continue;
            AetherProfile ap = person.getAetherProfile();
            if (ap == null || !ap.isAwakened()) continue;

            SupernaturalProgressionRecord record = ap.getProgressionRecord();
            if (record == null) continue;

            Village village = person.getVillage();
            if (village == null) continue;

            boolean isInFamine = village.isInFamine();
            Occupation occ = person.getOccupation();
            int pid = person.getId();
            boolean trainedThisYear = false;

            // 1. OCCUPATIONAL_ABILITY_USE — if working a relevant occupation
            if (occ != null && occ.isWorker() && !isInFamine && isOccupationRelevantToClass(occ, ap.getSupernaturalClass())) {
                double quality = 0.8 + (village.getTechLevel() * 0.04); // tech improves quality
                ProgressionEvent occEvent = new ProgressionEvent(
                        String.format("OCC:Y%d:P%d", currentYear, pid),
                        pid, ProgressionEventType.OCCUPATIONAL_ABILITY_USE,
                        currentYear, 6, quality);
                processEvent(person, occEvent, progConfig);
                trainedThisYear = true;
            }

            // 2. DELIBERATE_TRAINING — if healthy and not in famine
            if (!isInFamine && person.getAge() >= 10) {
                double quality = 0.6 + random.nextDouble() * 0.8; // 0.6–1.4 variance
                ProgressionEvent trainEvent = new ProgressionEvent(
                        String.format("TRN:Y%d:P%d", currentYear, pid),
                        pid, ProgressionEventType.DELIBERATE_TRAINING,
                        currentYear, 3, quality);
                processEvent(person, trainEvent, progConfig);
                trainedThisYear = true;
            }

            // 3. DEMANDING_EVENT_SURVIVAL — famine or disaster survival
            if (village.getFamineDeathsThisYear() > 0 || village.getFreezingDeathsThisYear() > 0 ||
                    (village.getLastDisasterThisYear() != null &&
                            village.getLastDisasterThisYear() != civsim.model.DisasterType.NONE)) {
                ProgressionEvent survivalEvent = new ProgressionEvent(
                        String.format("SRV:Y%d:P%d", currentYear, pid),
                        pid, ProgressionEventType.DEMANDING_EVENT_SURVIVAL,
                        currentYear, 9, 1.5);
                processEvent(person, survivalEvent, progConfig);
            }

            // 4. PROBLEM_SOLVING — settlement leaders
            if (village.getLeader() == person) {
                ProgressionEvent leaderEvent = new ProgressionEvent(
                        String.format("LDR:Y%d:P%d", currentYear, pid),
                        pid, ProgressionEventType.PROBLEM_SOLVING,
                        currentYear, 12, 1.2);
                processEvent(person, leaderEvent, progConfig);
            }

            // 5. Stage 2A.6: Focused Attribute-Training Event
            // Active citizens train class-relevant attributes (Primary, Sec1, Sec2)
            if (trainedThisYear) {
                SupernaturalClass cls = ap.getSupernaturalClass();
                AttributeType[] classAttrs = {
                        cls.getPrimaryAttributeType(),
                        cls.getSecondaryAttribute1Type(),
                        cls.getSecondaryAttribute2Type()
                };
                int attrIndex = Math.abs((currentYear * 31 + pid * 17) % classAttrs.length);
                AttributeType targetAttr = classAttrs[attrIndex];
                if (targetAttr != null && targetAttr.isTrainable()) {
                    double quality = 0.80 + (random.nextDouble() * 0.40); // [0.80, 1.20]
                    AttributeTrainingEvent attrEvt = new AttributeTrainingEvent(
                            String.format("ATTR:Y%d:P%d:%s", currentYear, pid, targetAttr.name()),
                            pid, targetAttr, quality, 0.30, currentYear, 6);
                    if (ap.processAttributeTrainingEvent(attrEvt, progConfig, person.isAlive())) {
                        attributeEventsProcessedThisYear++;
                    }
                }
            }

            // 6. Stage 2A.6: Gated Breakthrough Attempt Check
            if (ap.getPowerTier() < ap.getMaxPowerTier()) {
                AdvancementEvaluationResult eval = TierAdvancementEvaluator.evaluateAdvancementEligibility(
                        person, progConfig, currentYear, 6);

                if (eval.isEligible()) {
                    int targetTier = eval.getTargetTier();
                    String attemptId = String.format("BRK:Y%d:P%d:T%d", currentYear, pid, targetTier);
                    BreakthroughAttemptRecord attempt = TierAdvancementEvaluator.attemptTierAdvancement(
                            person, progConfig, attemptId, currentYear, 6, random);

                    trackBreakthroughTelemetry(attempt);

                    if (attempt != null && attempt.isSuccess() && eventRecorder != null) {
                        eventRecorder.accept(String.format("Year %d: %s achieved breakthrough to Tier %d (%s, %s)!",
                                currentYear, person.getFullName(), ap.getPowerTier(),
                                ap.getRarity().name(), ap.getSupernaturalClass().getDisplayName()), false);
                    }
                }
            }

            // Track high mastery gated at T1
            if (record.getMastery() >= 75.0 && ap.getPowerTier() == 1) {
                highMasteryGatedCount++;
            }
        }
    }

    private void trackBreakthroughTelemetry(BreakthroughAttemptRecord attempt) {
        if (attempt == null) return;
        int toTier = attempt.getToTier();
        if (toTier >= 1 && toTier <= 10) {
            annualAttemptsByTransition[toTier]++;
            cumulativeAttemptsByTransition[toTier]++;
            totalCumulativeAttempts++;
            breakthroughAttemptsThisYear++;
            if (attempt.isSuccess()) {
                annualSuccessesByTransition[toTier]++;
                cumulativeSuccessesByTransition[toTier]++;
                totalCumulativeSuccesses++;
                breakthroughSuccessesThisYear++;
            } else {
                annualFailuresByTransition[toTier]++;
                cumulativeFailuresByTransition[toTier]++;
                totalCumulativeFailures++;
                breakthroughFailuresThisYear++;
            }
        }
    }

    /**
     * Checks if an occupation is relevant to a supernatural class's abilities.
     */
    private boolean isOccupationRelevantToClass(Occupation occ, SupernaturalClass cls) {
        switch (cls) {
            case VANGUARD: return occ == Occupation.SOLDIER || occ == Occupation.BUILDER;
            case ELEMENTALIST: return occ == Occupation.BLACKSMITH || occ == Occupation.MINER;
            case VITALIST: return occ == Occupation.FARMER;
            case WARDEN: return occ == Occupation.SOLDIER || occ == Occupation.BUILDER;
            case SEER: return occ == Occupation.MERCHANT;
            case BEASTBINDER: return occ == Occupation.FARMER || occ == Occupation.LUMBERJACK;
            case RIFTWALKER: return occ == Occupation.MERCHANT;
            default: return false;
        }
    }

    // --- Reporting ---

    public int getEventsProcessedThisYear() { return eventsProcessedThisYear; }
    public int getAttributeEventsProcessedThisYear() { return attributeEventsProcessedThisYear; }
    public int getHighMasteryGatedCount() { return highMasteryGatedCount; }

    public int getBreakthroughAttemptsThisYear() { return breakthroughAttemptsThisYear; }
    public int getBreakthroughSuccessesThisYear() { return breakthroughSuccessesThisYear; }
    public int getBreakthroughFailuresThisYear() { return breakthroughFailuresThisYear; }

    public int getTotalCumulativeAttempts() { return totalCumulativeAttempts; }
    public int getTotalCumulativeSuccesses() { return totalCumulativeSuccesses; }
    public int getTotalCumulativeFailures() { return totalCumulativeFailures; }

    public int[] getAnnualAttemptsByTransition() { return annualAttemptsByTransition.clone(); }
    public int[] getAnnualSuccessesByTransition() { return annualSuccessesByTransition.clone(); }
    public int[] getCumulativeAttemptsByTransition() { return cumulativeAttemptsByTransition.clone(); }
    public int[] getCumulativeSuccessesByTransition() { return cumulativeSuccessesByTransition.clone(); }

    /**
     * Generates a compact progression summary for the annual report.
     */
    public String generateAnnualProgressionSummary(List<Person> allPeople) {
        int totalAwakened = 0;
        double sumMastery = 0, minMastery = Double.MAX_VALUE, maxMastery = 0;
        double sumTraining = 0, sumExperience = 0;
        int[] bracketCounts = new int[8]; // Unstable, Beginner, Developing, Reliable, Advanced, Exceptional, Near-Perfect, Ceiling

        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            AetherProfile ap = p.getAetherProfile();
            if (ap == null || !ap.isAwakened()) continue;
            SupernaturalProgressionRecord rec = ap.getProgressionRecord();
            if (rec == null) continue;

            totalAwakened++;
            double m = rec.getMastery();
            sumMastery += m;
            if (m < minMastery) minMastery = m;
            if (m > maxMastery) maxMastery = m;
            sumTraining += rec.getTrainingProgress();
            sumExperience += rec.getSupernaturalExperience();

            // Bracket distribution
            if (m >= 100.0) bracketCounts[7]++;
            else if (m >= 90.0) bracketCounts[6]++;
            else if (m >= 75.0) bracketCounts[5]++;
            else if (m >= 60.0) bracketCounts[4]++;
            else if (m >= 40.0) bracketCounts[3]++;
            else if (m >= 25.0) bracketCounts[2]++;
            else if (m >= 10.0) bracketCounts[1]++;
            else bracketCounts[0]++;
        }

        if (totalAwakened == 0) return null;

        double avgMastery = sumMastery / totalAwakened;
        double avgTraining = sumTraining / totalAwakened;
        double avgExperience = sumExperience / totalAwakened;
        if (minMastery == Double.MAX_VALUE) minMastery = 0;

        return String.format(
                "Prog[%d] Mst avg:%.1f[%.1f-%.1f] Trn:%.0f Exp:%.0f | " +
                "Brk U:%d B:%d D:%d R:%d A:%d E:%d NP:%d C:%d | " +
                "Evt:%d (Attr:%d) Gated@T1:%d",
                totalAwakened, avgMastery, minMastery, maxMastery, avgTraining, avgExperience,
                bracketCounts[0], bracketCounts[1], bracketCounts[2], bracketCounts[3],
                bracketCounts[4], bracketCounts[5], bracketCounts[6], bracketCounts[7],
                eventsProcessedThisYear, attributeEventsProcessedThisYear, highMasteryGatedCount);
    }

    /**
     * Stage 2A.6: Generates the annual breakthrough and tier distribution summary.
     */
    public String generateAnnualBreakthroughSummary(List<Person> allPeople, ProgressionConfig config, int currentYear) {
        int[] tierCounts = new int[11];
        int cappedCount = 0;
        int blockedHighMasteryCount = 0;
        int t5Plus = 0;
        int t8Plus = 0;
        int totalAwakened = 0;

        for (Person p : allPeople) {
            if (!p.isAlive()) continue;
            AetherProfile ap = p.getAetherProfile();
            if (ap == null || !ap.isAwakened()) continue;
            SupernaturalProgressionRecord rec = ap.getProgressionRecord();
            if (rec == null) continue;

            totalAwakened++;
            int tier = ap.getPowerTier();
            if (tier >= 1 && tier <= 10) {
                tierCounts[tier]++;
            }
            if (tier >= ap.getMaxPowerTier()) {
                cappedCount++;
            }
            if (tier >= 5) {
                t5Plus++;
            }
            if (tier >= 8) {
                t8Plus++;
            }

            // High mastery (>= 70) blocked check
            if (rec.getMastery() >= 70.0 && tier < ap.getMaxPowerTier()) {
                AdvancementEvaluationResult eval = TierAdvancementEvaluator.evaluateAdvancementEligibility(
                        p, config, currentYear, 6);
                if (!eval.isEligible()) {
                    blockedHighMasteryCount++;
                }
            }
        }

        if (totalAwakened == 0) return null;

        return String.format(
                "Tiers[T1:%d T2:%d T3:%d T4:%d T5:%d T6:%d T7:%d T8:%d T9:%d T10:%d] | " +
                "Cap:%d BlkMst>=70:%d T5+:%d T8+:%d | " +
                "Breakthroughs Yr(att:%d suc:%d) Cum(att:%d suc:%d)",
                tierCounts[1], tierCounts[2], tierCounts[3], tierCounts[4], tierCounts[5],
                tierCounts[6], tierCounts[7], tierCounts[8], tierCounts[9], tierCounts[10],
                cappedCount, blockedHighMasteryCount, t5Plus, t8Plus,
                breakthroughAttemptsThisYear, breakthroughSuccessesThisYear,
                totalCumulativeAttempts, totalCumulativeSuccesses);
    }

    public void resetYearlyCounters() {
        eventsProcessedThisYear = 0;
        attributeEventsProcessedThisYear = 0;
        highMasteryGatedCount = 0;
        breakthroughAttemptsThisYear = 0;
        breakthroughSuccessesThisYear = 0;
        breakthroughFailuresThisYear = 0;
        Arrays.fill(annualAttemptsByTransition, 0);
        Arrays.fill(annualSuccessesByTransition, 0);
        Arrays.fill(annualFailuresByTransition, 0);
    }
}
