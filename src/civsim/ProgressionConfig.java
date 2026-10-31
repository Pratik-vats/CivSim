package civsim;

import civsim.model.SupernaturalClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage 2A.4 — Centralized configuration for all supernatural progression parameters.
 * Covers initial mastery range, class aptitude weights, diminishing-returns exponent,
 * class gain modifiers, tier potency multipliers, and tier transition rule registry.
 */
public class ProgressionConfig {

    // --- Mastery Initialization ---
    private final double initialMasteryMin;       // default 5.0
    private final double initialMasteryMax;       // default 15.0

    // --- Class Aptitude Weights ---
    private final double primaryAptitudeWeight;   // default 0.60
    private final double secondaryAptitudeWeight; // default 0.20 (each of 2 secondaries)

    // --- Diminishing Returns ---
    private final double diminishingReturnsAlpha;  // default 0.75

    // --- Aptitude Modifier Bounds ---
    private final double aptitudeModifierMin;     // default 0.50
    private final double aptitudeModifierMax;     // default 2.00

    // --- Class-specific gain modifiers (optional per-class tuning) ---
    private final Map<SupernaturalClass, Double> classGainModifiers;

    // --- Tier potency multipliers for DerivedPowerProfile ---
    private final double[] tierPotencyMultipliers;  // index = tier (0 unused, 1-10)

    // --- Tier Transition Rules ---
    private final Map<Integer, TierTransitionRule> transitionRules; // key = fromTier

    // --- Stage 2A.5: Superhuman Lifespan & Longevity Offsets ---
    private final boolean supernaturalLifespanEnabled;
    private final Map<Integer, Integer> tierLongevityOffsets; // key = currentTier (1–10) -> years offset

    public ProgressionConfig(
            double initialMasteryMin, double initialMasteryMax,
            double primaryAptitudeWeight, double secondaryAptitudeWeight,
            double diminishingReturnsAlpha,
            double aptitudeModifierMin, double aptitudeModifierMax,
            Map<SupernaturalClass, Double> classGainModifiers,
            double[] tierPotencyMultipliers,
            Map<Integer, TierTransitionRule> transitionRules,
            boolean supernaturalLifespanEnabled,
            Map<Integer, Integer> tierLongevityOffsets) {
        this.initialMasteryMin = initialMasteryMin;
        this.initialMasteryMax = initialMasteryMax;
        this.primaryAptitudeWeight = primaryAptitudeWeight;
        this.secondaryAptitudeWeight = secondaryAptitudeWeight;
        this.diminishingReturnsAlpha = diminishingReturnsAlpha;
        this.aptitudeModifierMin = aptitudeModifierMin;
        this.aptitudeModifierMax = aptitudeModifierMax;
        this.classGainModifiers = Collections.unmodifiableMap(new HashMap<>(classGainModifiers));
        this.tierPotencyMultipliers = tierPotencyMultipliers.clone();
        this.transitionRules = Collections.unmodifiableMap(new HashMap<>(transitionRules));
        this.supernaturalLifespanEnabled = supernaturalLifespanEnabled;

        Map<Integer, Integer> validatedOffsets = new HashMap<>();
        if (tierLongevityOffsets != null) {
            for (Map.Entry<Integer, Integer> entry : tierLongevityOffsets.entrySet()) {
                if (entry.getValue() < 0) {
                    throw new IllegalArgumentException("Tier longevity offset cannot be negative: T" + entry.getKey() + " -> " + entry.getValue());
                }
                if (entry.getValue() > 300) {
                    throw new IllegalArgumentException("Unapproved unbounded tier longevity offset: T" + entry.getKey() + " -> " + entry.getValue());
                }
                validatedOffsets.put(entry.getKey(), entry.getValue());
            }
        }
        this.tierLongevityOffsets = Collections.unmodifiableMap(validatedOffsets);
    }

    public ProgressionConfig(
            double initialMasteryMin, double initialMasteryMax,
            double primaryAptitudeWeight, double secondaryAptitudeWeight,
            double diminishingReturnsAlpha,
            double aptitudeModifierMin, double aptitudeModifierMax,
            Map<SupernaturalClass, Double> classGainModifiers,
            double[] tierPotencyMultipliers,
            Map<Integer, TierTransitionRule> transitionRules) {
        this(initialMasteryMin, initialMasteryMax, primaryAptitudeWeight, secondaryAptitudeWeight,
                diminishingReturnsAlpha, aptitudeModifierMin, aptitudeModifierMax,
                classGainModifiers, tierPotencyMultipliers, transitionRules,
                true, createDefaultTierLongevityOffsets());
    }

    /**
     * Creates the default production configuration with Stage 2A.6 approved transition rules
     * and nonlinear tier ability-potency multipliers.
     */
    public static ProgressionConfig createDefault() {
        Map<SupernaturalClass, Double> classModifiers = new HashMap<>();
        for (SupernaturalClass cls : SupernaturalClass.values()) {
            classModifiers.put(cls, 1.0); // no class-specific tuning by default
        }

        // Stage 2A.6: Authoritative nonlinear ability-potency multipliers (1.00x at T1 up to 6.70x at T10)
        double[] potency = {0.0, 1.00, 1.20, 1.45, 1.75, 2.15, 2.65, 3.30, 4.15, 5.25, 6.70};

        // Stage 2A.6: Register default transition rules (T1->T2 through T9->T10)
        Map<Integer, TierTransitionRule> rules = createDefaultTransitionRules();

        return new ProgressionConfig(
                5.0, 15.0,      // initial mastery range
                0.60, 0.20,     // aptitude weights
                0.75,           // diminishing returns alpha
                0.50, 2.00,     // aptitude modifier bounds
                classModifiers,
                potency,
                rules
        );
    }

    // --- Getters ---
    public double getInitialMasteryMin() { return initialMasteryMin; }
    public double getInitialMasteryMax() { return initialMasteryMax; }
    public double getPrimaryAptitudeWeight() { return primaryAptitudeWeight; }
    public double getSecondaryAptitudeWeight() { return secondaryAptitudeWeight; }
    public double getDiminishingReturnsAlpha() { return diminishingReturnsAlpha; }
    public double getAptitudeModifierMin() { return aptitudeModifierMin; }
    public double getAptitudeModifierMax() { return aptitudeModifierMax; }

    public double getClassGainModifier(SupernaturalClass cls) {
        return classGainModifiers.getOrDefault(cls, 1.0);
    }

    public double getTierPotencyMultiplier(int tier) {
        if (tier < 0 || tier >= tierPotencyMultipliers.length) return 1.0;
        return tierPotencyMultipliers[tier];
    }

    public TierTransitionRule getTransitionRule(int fromTier) {
        return transitionRules.get(fromTier);
    }

    public boolean hasTransitionRule(int fromTier) {
        return transitionRules.containsKey(fromTier);
    }

    public Map<Integer, TierTransitionRule> getAllTransitionRules() {
        return transitionRules;
    }

    public ProgressionConfig withSupernaturalLifespanEnabled(boolean enabled) {
        return new ProgressionConfig(
                this.initialMasteryMin, this.initialMasteryMax,
                this.primaryAptitudeWeight, this.secondaryAptitudeWeight,
                this.diminishingReturnsAlpha,
                this.aptitudeModifierMin, this.aptitudeModifierMax,
                this.classGainModifiers,
                this.tierPotencyMultipliers,
                this.transitionRules,
                enabled,
                this.tierLongevityOffsets
        );
    }

    // --- Stage 2A.5: Longevity Configuration & Authoritative Calculations ---

    public static Map<Integer, Integer> createDefaultTierLongevityOffsets() {
        Map<Integer, Integer> offsets = new HashMap<>();
        offsets.put(1, 2);
        offsets.put(2, 4);
        offsets.put(3, 7);
        offsets.put(4, 10);
        offsets.put(5, 14);
        offsets.put(6, 19);
        offsets.put(7, 25);
        offsets.put(8, 32);
        offsets.put(9, 40);
        offsets.put(10, 50);
        return offsets;
    }

    public boolean isSupernaturalLifespanEnabled() {
        return supernaturalLifespanEnabled;
    }

    public Map<Integer, Integer> getTierLongevityOffsets() {
        return tierLongevityOffsets;
    }

    /**
     * Looks up the configured natural longevity offset for a given tier.
     * Non-cumulative: each tier maps directly to its configured offset.
     * Safe fallback: returns 0 if disabled, tier <= 0, or missing key.
     */
    public int getTierLongevityOffset(int tier) {
        if (!supernaturalLifespanEnabled || tier <= 0) return 0;
        Integer offset = tierLongevityOffsets.get(tier);
        if (offset == null) {
            return 0;
        }
        return Math.max(0, offset);
    }

    /**
     * Authoritative calculation of a person's natural longevity offset.
     * Returns 0 if unawakened, if supernatural lifespan is disabled,
     * or if simulation is in the Mundane Era (currentYear < awakeningStartYear).
     */
    public int getTierLongevityOffset(civsim.model.Person person, int currentYear, int awakeningStartYear) {
        if (person == null) return 0;
        if (!supernaturalLifespanEnabled) return 0;
        if (awakeningStartYear > 0 && currentYear < awakeningStartYear) return 0;
        civsim.model.AetherProfile ap = person.getAetherProfile();
        if (ap == null || !ap.isAwakened()) return 0;
        return getTierLongevityOffset(ap.getPowerTier());
    }

    /**
     * Authoritative calculation of a person's natural lifespan ceiling:
     * PersonalNaturalLifespanCeiling = Phase1BaselineCeiling + TierLongevityOffset(person)
     * Where Phase1BaselineCeiling = effectiveDeathAge (baseDeathAge + techLevel).
     */
    public int getPersonalNaturalLifespanCeiling(civsim.model.Person person, int effectiveDeathAge, int currentYear, int awakeningStartYear) {
        return effectiveDeathAge + getTierLongevityOffset(person, currentYear, awakeningStartYear);
    }

    public int getPersonalNaturalLifespanCeiling(civsim.model.Person person, int baseDeathAge, int maxWorldTechLevel, int currentYear, int awakeningStartYear) {
        return (baseDeathAge + maxWorldTechLevel) + getTierLongevityOffset(person, currentYear, awakeningStartYear);
    }

    /**
     * Stage 2A.6: Creates the approved default transition rules for all 9 tier boundaries (T1->T2 to T9->T10).
     */
    public static Map<Integer, TierTransitionRule> createDefaultTransitionRules() {
        Map<Integer, TierTransitionRule> rules = new HashMap<>();
        rules.put(1, new TierTransitionRule(1, 2, true, 25.0, 25.0, 22.0, 25.0, 10.0, 0.90, 3, 0.10));
        rules.put(2, new TierTransitionRule(2, 3, true, 40.0, 32.0, 28.0, 60.0, 35.0, 0.80, 3, 0.10));
        rules.put(3, new TierTransitionRule(3, 4, true, 55.0, 40.0, 34.0, 120.0, 80.0, 0.68, 3, 0.10));
        rules.put(4, new TierTransitionRule(4, 5, true, 70.0, 48.0, 40.0, 220.0, 150.0, 0.55, 6, 0.20));
        rules.put(5, new TierTransitionRule(5, 6, true, 78.0, 56.0, 46.0, 360.0, 260.0, 0.40, 6, 0.20));
        rules.put(6, new TierTransitionRule(6, 7, true, 84.0, 64.0, 52.0, 550.0, 410.0, 0.28, 6, 0.20));
        rules.put(7, new TierTransitionRule(7, 8, true, 90.0, 72.0, 58.0, 800.0, 620.0, 0.18, 12, 0.30));
        rules.put(8, new TierTransitionRule(8, 9, true, 95.0, 82.0, 65.0, 1100.0, 900.0, 0.10, 12, 0.30));
        rules.put(9, new TierTransitionRule(9, 10, true, 98.0, 90.0, 72.0, 1500.0, 1300.0, 0.05, 12, 0.30));
        return rules;
    }

    /**
     * Configurable tier transition rule for gated advancement.
     * If no rule exists for a given tier, advancement is blocked with MISSING_APPROVED_TRANSITION_RULE.
     */
    public static class TierTransitionRule {
        private final int fromTier;
        private final int toTier;
        private final boolean advancementEnabled;
        private final double minMastery;
        private final double minPrimaryAttribute;
        private final double minWeightedAptitude;
        private final double minTrainingProgress;
        private final double minExperience;
        private final double baseSuccessChance;
        private final int cooldownMonths;
        private final double retryPreparationFraction;
        private final Map<String, Integer> minAttributes; // attribute name -> minimum value (backward compat)

        public TierTransitionRule(int fromTier, int toTier, boolean advancementEnabled,
                                  double minMastery, double minPrimaryAttribute, double minWeightedAptitude,
                                  double minTrainingProgress, double minExperience,
                                  double baseSuccessChance, int cooldownMonths, double retryPreparationFraction,
                                  Map<String, Integer> minAttributes) {
            this.fromTier = fromTier;
            this.toTier = toTier;
            this.advancementEnabled = advancementEnabled;
            this.minMastery = minMastery;
            this.minPrimaryAttribute = minPrimaryAttribute;
            this.minWeightedAptitude = minWeightedAptitude;
            this.minTrainingProgress = minTrainingProgress;
            this.minExperience = minExperience;
            this.baseSuccessChance = baseSuccessChance;
            this.cooldownMonths = cooldownMonths;
            this.retryPreparationFraction = retryPreparationFraction;
            this.minAttributes = (minAttributes != null)
                    ? Collections.unmodifiableMap(new HashMap<>(minAttributes))
                    : Collections.emptyMap();
        }

        public TierTransitionRule(int fromTier, int toTier, boolean advancementEnabled,
                                  double minMastery, double minPrimaryAttribute, double minWeightedAptitude,
                                  double minTrainingProgress, double minExperience,
                                  double baseSuccessChance, int cooldownMonths, double retryPreparationFraction) {
            this(fromTier, toTier, advancementEnabled, minMastery, minPrimaryAttribute, minWeightedAptitude,
                    minTrainingProgress, minExperience, baseSuccessChance, cooldownMonths, retryPreparationFraction, null);
        }

        public TierTransitionRule(int fromTier, int toTier, boolean advancementEnabled,
                                  double minMastery, double minTrainingProgress,
                                  double minExperience, Map<String, Integer> minAttributes) {
            this(fromTier, toTier, advancementEnabled, minMastery, 0.0, 0.0,
                    minTrainingProgress, minExperience, 1.00, 3, 0.10, minAttributes);
        }

        public int getFromTier() { return fromTier; }
        public int getToTier() { return toTier; }
        public boolean isAdvancementEnabled() { return advancementEnabled; }
        public double getMinMastery() { return minMastery; }
        public double getMinPrimaryAttribute() { return minPrimaryAttribute; }
        public double getMinWeightedAptitude() { return minWeightedAptitude; }
        public double getMinTrainingProgress() { return minTrainingProgress; }
        public double getMinExperience() { return minExperience; }
        public double getBaseSuccessChance() { return baseSuccessChance; }
        public double getBaseChancePercent() { return baseSuccessChance * 100.0; }
        public int getCooldownMonths() { return cooldownMonths; }
        public double getRetryPreparationFraction() { return retryPreparationFraction; }
        public Map<String, Integer> getMinAttributes() { return minAttributes; }
    }
}
