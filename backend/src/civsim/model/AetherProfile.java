package civsim.model;

import civsim.ProgressionConfig;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Contains all supernatural/Aether-related data for a Person.
 * Attached to every Person but only "active" after Year 50 (The Great Mutation).
 *
 * Pre-Year 50: alleles seeded on existing population; all other fields dormant.
 * Post-Year 50: awakening checks, class assignment, attribute bonuses become active.
 *
 * Stage 2A.6 — 4-Layer Attribute System (10 Attributes):
 *   Effective = clamp(Base + AwakeningBonus + DevelopmentBonus + Temporary, 0.0, 100.0)
 *   1. Baseline: mortals 10–30 (rolled at birth), immutable, aether 0 pre-awakening
 *   2. AwakeningBonus: permanent bonus from T1 class table upon awakening
 *   3. DevelopmentBonus: permanent double-precision bonus from focused training events
 *   4. Temporary: buffs/debuffs from items, injuries, status effects
 */
public class AetherProfile {

    // --- Hereditary Aether Genotype (2-allele Mendelian) ---
    private AetherAllele allele1;
    private AetherAllele allele2;

    // --- Personality/Soul Modifiers ---
    private double luck;            // -1.0 to +1.0 (0.0 = neutral, modifies awakening probability)
    private double greed;           // 0.0 to 1.0 (accumulates from wealth obsession, failure, envy)
    private double darkResonance;   // 0.0 to 1.0 (corruption potential, feeds Dark Awakening pathway)

    // --- Awakening State ---
    private boolean awakened;
    private AwakeningPathway awakeningPathway;
    private SupernaturalClass supernaturalClass;
    private ClassRarity rarity;
    private int powerTier;          // T1 to T10 (capped by rarity)
    private int awakeningYear;      // year awakened (-1 if not yet)
    private boolean darkAwakened;   // true if awakened through Dark Awakening pathway

    // --- Stage 2A.4: Progression ---
    private PrimaryAbility primaryAbility;
    private SupernaturalProgressionRecord progressionRecord;

    // --- Stage 2A.6: 4-Layer Attribute System (10 Core Attributes) ---
    // Layer 1: Immutable Baseline Attributes (10-30 rolled at creation; aether 0 pre-awakening)
    private int baseStrength;
    private int baseAgility;
    private int baseVitality;
    private int baseEndurance;
    private int baseIntelligence;
    private int basePerception;
    private int baseWillpower;
    private double baseLuck;
    private int baseAetherCapacity;
    private int baseAetherControl;
    private int baseWisdom; // legacy alias to baseWillpower
    private int baseAether; // legacy alias to baseAetherControl

    // Layer 2: Permanent Awakening Bonuses (from T1 class table)
    private int awakeningBonusStrength;
    private int awakeningBonusAgility;
    private int awakeningBonusVitality;
    private int awakeningBonusEndurance;
    private int awakeningBonusIntelligence;
    private int awakeningBonusPerception;
    private int awakeningBonusWillpower;
    private int awakeningBonusLuck;
    private int awakeningBonusAetherCapacity;
    private int awakeningBonusAetherControl;
    private int awakeningBonusWisdom;
    private int awakeningBonusAether;

    // Layer 3: Permanent Development Bonuses (double precision for fractional gains)
    private double devBonusStrength;
    private double devBonusAgility;
    private double devBonusVitality;
    private double devBonusEndurance;
    private double devBonusIntelligence;
    private double devBonusPerception;
    private double devBonusWillpower;
    private double devBonusLuck;
    private double devBonusAetherCapacity;
    private double devBonusAetherControl;

    // Layer 4: Temporary Condition Modifiers
    private int tempBonusStrength;
    private int tempBonusAgility;
    private int tempBonusVitality;
    private int tempBonusEndurance;
    private int tempBonusIntelligence;
    private int tempBonusPerception;
    private int tempBonusWillpower;
    private int tempBonusLuck;
    private int tempBonusAetherCapacity;
    private int tempBonusAetherControl;
    private int tempBonusWisdom;
    private int tempBonusAether;

    // Idempotency: Processed attribute training events
    private final Set<String> processedAttributeEventIds = new HashSet<>();

    /**
     * Creates a new AetherProfile for a person.
     * Base attributes are rolled at creation; Aether starts at 0.
     *
     * @param allele1 first Aether allele
     * @param allele2 second Aether allele
     * @param random  seeded random for deterministic attribute generation
     */
    public AetherProfile(AetherAllele allele1, AetherAllele allele2, Random random) {
        this.allele1 = allele1;
        this.allele2 = allele2;

        // Personality/soul
        this.luck = (random.nextDouble() * 2.0) - 1.0;   // uniform -1.0 to +1.0
        this.greed = random.nextDouble() * 0.15;           // low initial greed (0.0–0.15)
        this.darkResonance = 0.0;                          // starts clean

        // Awakening state — dormant
        this.awakened = false;
        this.awakeningPathway = null;
        this.supernaturalClass = null;
        this.rarity = null;
        this.powerTier = 0;
        this.awakeningYear = -1;
        this.darkAwakened = false;
        this.primaryAbility = null;
        this.progressionRecord = null;

        // Base attributes: mortal range 10–30
        this.baseStrength = 10 + random.nextInt(21);
        this.baseAgility = 10 + random.nextInt(21);
        this.baseVitality = 10 + random.nextInt(21);
        this.baseEndurance = 10 + random.nextInt(21);
        this.baseIntelligence = 10 + random.nextInt(21);
        this.basePerception = 10 + random.nextInt(21);
        this.baseWillpower = 10 + random.nextInt(21);
        this.baseWisdom = this.baseWillpower;
        this.baseLuck = 20.0 + (this.luck * 10.0);
        this.baseAetherCapacity = 0;
        this.baseAetherControl = 0;
        this.baseAether = 0;

        // Bonuses start at zero
        this.awakeningBonusStrength = 0;
        this.awakeningBonusAgility = 0;
        this.awakeningBonusVitality = 0;
        this.awakeningBonusEndurance = 0;
        this.awakeningBonusIntelligence = 0;
        this.awakeningBonusPerception = 0;
        this.awakeningBonusWillpower = 0;
        this.awakeningBonusLuck = 0;
        this.awakeningBonusAetherCapacity = 0;
        this.awakeningBonusAetherControl = 0;
        this.awakeningBonusWisdom = 0;
        this.awakeningBonusAether = 0;

        this.devBonusStrength = 0.0;
        this.devBonusAgility = 0.0;
        this.devBonusVitality = 0.0;
        this.devBonusEndurance = 0.0;
        this.devBonusIntelligence = 0.0;
        this.devBonusPerception = 0.0;
        this.devBonusWillpower = 0.0;
        this.devBonusLuck = 0.0;
        this.devBonusAetherCapacity = 0.0;
        this.devBonusAetherControl = 0.0;

        this.tempBonusStrength = 0;
        this.tempBonusAgility = 0;
        this.tempBonusVitality = 0;
        this.tempBonusEndurance = 0;
        this.tempBonusIntelligence = 0;
        this.tempBonusPerception = 0;
        this.tempBonusWillpower = 0;
        this.tempBonusLuck = 0;
        this.tempBonusAetherCapacity = 0;
        this.tempBonusAetherControl = 0;
        this.tempBonusWisdom = 0;
        this.tempBonusAether = 0;
    }

    // --- Genotype Queries ---

    /** Returns true if this person has at least one Dominant allele (DD or Dr) */
    public boolean hasAwakeningPotential() {
        return allele1.isDominant() || allele2.isDominant();
    }

    /** Returns the genotype string (e.g., "DD", "Dr", "rr") */
    public String getGenotypeString() {
        // Convention: D before r, so Dr not rD
        if (allele1.isDominant() && allele2.isDominant()) return "DD";
        if (allele1.isDominant() || allele2.isDominant()) return "Dr";
        return "rr";
    }

    /** Returns true if homozygous dominant (DD) — strongest Aether potential */
    public boolean isHomozygousDominant() {
        return allele1.isDominant() && allele2.isDominant();
    }

    // --- Mendelian Inheritance ---

    /**
     * Creates a child's AetherProfile by inheriting one allele from each parent.
     *
     * @param motherProfile mother's AetherProfile
     * @param fatherProfile father's AetherProfile
     * @param random seeded random
     * @return new AetherProfile for the child
     */
    public static AetherProfile inherit(AetherProfile motherProfile, AetherProfile fatherProfile, Random random) {
        AetherAllele fromMother = random.nextBoolean() ? motherProfile.allele1 : motherProfile.allele2;
        AetherAllele fromFather = random.nextBoolean() ? fatherProfile.allele1 : fatherProfile.allele2;

        AetherProfile child = new AetherProfile(fromMother, fromFather, random);

        // Inherit slight Luck tendency from parents (mean of parents + small perturbation)
        double parentLuckAvg = (motherProfile.luck + fatherProfile.luck) / 2.0;
        child.luck = Math.max(-1.0, Math.min(1.0, parentLuckAvg + (random.nextGaussian() * 0.3)));

        return child;
    }

    // --- Awakening ---

    /**
     * Performs the full awakening process: assigns pathway, class (with parental inheritance),
     * rarity, power tier, base Aether attribute, and T1 class bonuses.
     *
     * @param pathway  the pathway through which awakening occurs
     * @param year     simulation year of awakening
     * @param random   seeded random
     * @param motherClass  mother's supernatural class (nullable)
     * @param fatherClass  father's supernatural class (nullable)
     * @param livingMythicCount current count of living Mythic beings globally
     * @param maxMythicCap global cap on living Mythic beings
     */
    public void awaken(AwakeningPathway pathway, int year, int age, Random random,
                       SupernaturalClass motherClass, SupernaturalClass fatherClass,
                       int livingMythicCount, int maxMythicCap) {
        this.awakened = true;
        this.awakeningPathway = pathway;
        this.awakeningYear = year;
        this.darkAwakened = (pathway == AwakeningPathway.DARK_AWAKENING);

        // Roll rarity
        double rarityRoll = random.nextDouble();
        this.rarity = ClassRarity.fromRoll(rarityRoll);

        // Enforce global Mythic cap
        if (this.rarity == ClassRarity.MYTHIC && livingMythicCount >= maxMythicCap) {
            this.rarity = ClassRarity.LEGENDARY; // downgrade to Legendary
        }

        // Select class with hereditary affinity
        double classRoll = random.nextDouble();
        this.supernaturalClass = SupernaturalClass.selectWeighted(classRoll, motherClass, fatherClass);

        // Starting power tier and ceiling
        this.powerTier = this.rarity.getStartingTier();

        // Base Aether: DD gets 3–5, Dr gets 1–3
        int aetherRoll;
        if (isHomozygousDominant()) {
            aetherRoll = 3 + random.nextInt(3);  // 3–5
        } else {
            aetherRoll = 1 + random.nextInt(3);  // 1–3
        }
        this.baseAetherControl = aetherRoll;
        this.baseAetherCapacity = aetherRoll;
        this.baseAether = aetherRoll;

        // Apply T1 class-specific awakening attribute bonuses for all 10 attributes
        this.awakeningBonusStrength = this.supernaturalClass.getT1BonusStrength();
        this.awakeningBonusAgility = this.supernaturalClass.getT1BonusAgility();
        this.awakeningBonusVitality = this.supernaturalClass.getT1BonusVitality();
        this.awakeningBonusEndurance = this.supernaturalClass.getT1BonusEndurance();
        this.awakeningBonusIntelligence = this.supernaturalClass.getT1BonusIntelligence();
        this.awakeningBonusPerception = this.supernaturalClass.getT1BonusPerception();
        this.awakeningBonusWillpower = this.supernaturalClass.getT1BonusWillpower();
        this.awakeningBonusWisdom = this.awakeningBonusWillpower;
        this.awakeningBonusLuck = 0;
        this.awakeningBonusAetherCapacity = this.supernaturalClass.getT1BonusAetherCapacity();
        this.awakeningBonusAetherControl = this.supernaturalClass.getT1BonusAetherControl();
        this.awakeningBonusAether = this.awakeningBonusAetherControl;

        // Stage 2A.4: Primary ability and progression record
        this.primaryAbility = PrimaryAbility.forClass(this.supernaturalClass);

        // Sample initial mastery deterministically within [5.0, 15.0]
        double initialMastery = 5.0 + random.nextDouble() * 10.0;
        this.progressionRecord = new SupernaturalProgressionRecord(
                year, age, pathway, this.supernaturalClass,
                this.rarity, initialMastery, this.primaryAbility);
    }

    public void awaken(AwakeningPathway pathway, int year, Random random,
                       SupernaturalClass motherClass, SupernaturalClass fatherClass,
                       int livingMythicCount, int maxMythicCap) {
        awaken(pathway, year, 0, random, motherClass, fatherClass, livingMythicCount, maxMythicCap);
    }

    // --- Greed & Dark Resonance Drift ---

    /**
     * Accumulates Greed based on wealth obsession, failure, or envy.
     * Called yearly for all living persons in the Awakened Era (post Year 50).
     */
    public void driftGreed(double amount) {
        this.greed = Math.min(1.0, Math.max(0.0, this.greed + amount));
    }

    /**
     * Accumulates Dark Resonance from Greed overflow, failed awakenings, or trauma.
     */
    public void driftDarkResonance(double amount) {
        this.darkResonance = Math.min(1.0, Math.max(0.0, this.darkResonance + amount));
    }

    /**
     * Returns true if this person qualifies for Dark Awakening pathway.
     * Requires: not yet awakened, has potential, high greed+darkResonance, age >= 12.
     */
    public boolean qualifiesForDarkAwakening(int age) {
        if (awakened) return false;
        if (!hasAwakeningPotential()) return false;
        if (age < 12) return false;
        return (greed + darkResonance) >= 1.2; // combined threshold
    }

    // --- Stage 2A.6: Centralized 4-Layer Attribute Calculations ---

    public double getBaselineAttribute(AttributeType attr) {
        if (attr == null) return 0.0;
        switch (attr) {
            case STRENGTH: return baseStrength;
            case AGILITY: return baseAgility;
            case VITALITY: return baseVitality;
            case ENDURANCE: return baseEndurance;
            case INTELLIGENCE: return baseIntelligence;
            case PERCEPTION: return basePerception;
            case WILLPOWER: return baseWillpower;
            case LUCK: return baseLuck;
            case AETHER_CAPACITY: return baseAetherCapacity;
            case AETHER_CONTROL: return baseAetherControl;
            default: return 0.0;
        }
    }

    public double getAwakeningBonus(AttributeType attr) {
        if (attr == null) return 0.0;
        switch (attr) {
            case STRENGTH: return awakeningBonusStrength;
            case AGILITY: return awakeningBonusAgility;
            case VITALITY: return awakeningBonusVitality;
            case ENDURANCE: return awakeningBonusEndurance;
            case INTELLIGENCE: return awakeningBonusIntelligence;
            case PERCEPTION: return awakeningBonusPerception;
            case WILLPOWER: return awakeningBonusWillpower;
            case LUCK: return awakeningBonusLuck;
            case AETHER_CAPACITY: return awakeningBonusAetherCapacity;
            case AETHER_CONTROL: return awakeningBonusAetherControl;
            default: return 0.0;
        }
    }

    public double getDevelopmentBonus(AttributeType attr) {
        if (attr == null) return 0.0;
        switch (attr) {
            case STRENGTH: return devBonusStrength;
            case AGILITY: return devBonusAgility;
            case VITALITY: return devBonusVitality;
            case ENDURANCE: return devBonusEndurance;
            case INTELLIGENCE: return devBonusIntelligence;
            case PERCEPTION: return devBonusPerception;
            case WILLPOWER: return devBonusWillpower;
            case LUCK: return devBonusLuck;
            case AETHER_CAPACITY: return devBonusAetherCapacity;
            case AETHER_CONTROL: return devBonusAetherControl;
            default: return 0.0;
        }
    }

    public void setDevelopmentBonus(AttributeType attr, double value) {
        if (attr == null) return;
        double clamped = Math.max(0.0, value);
        switch (attr) {
            case STRENGTH: devBonusStrength = clamped; break;
            case AGILITY: devBonusAgility = clamped; break;
            case VITALITY: devBonusVitality = clamped; break;
            case ENDURANCE: devBonusEndurance = clamped; break;
            case INTELLIGENCE: devBonusIntelligence = clamped; break;
            case PERCEPTION: devBonusPerception = clamped; break;
            case WILLPOWER: devBonusWillpower = clamped; break;
            case LUCK: devBonusLuck = clamped; break;
            case AETHER_CAPACITY: devBonusAetherCapacity = clamped; break;
            case AETHER_CONTROL: devBonusAetherControl = clamped; break;
        }
    }

    public double getTemporaryBonus(AttributeType attr) {
        if (attr == null) return 0.0;
        switch (attr) {
            case STRENGTH: return tempBonusStrength;
            case AGILITY: return tempBonusAgility;
            case VITALITY: return tempBonusVitality;
            case ENDURANCE: return tempBonusEndurance;
            case INTELLIGENCE: return tempBonusIntelligence;
            case PERCEPTION: return tempBonusPerception;
            case WILLPOWER: return tempBonusWillpower;
            case LUCK: return tempBonusLuck;
            case AETHER_CAPACITY: return tempBonusAetherCapacity;
            case AETHER_CONTROL: return tempBonusAetherControl;
            default: return 0.0;
        }
    }

    /**
     * Authoritative calculation for effective attributes:
     * EffectiveAttribute = clamp(Baseline + AwakeningBonus + DevelopmentBonus + ApplicableConditions, 0.0, 100.0)
     */
    public double getEffectiveAttributeDouble(AttributeType attr) {
        if (attr == null) return 0.0;
        double base = getBaselineAttribute(attr);
        double awk = getAwakeningBonus(attr);
        double dev = getDevelopmentBonus(attr);
        double temp = getTemporaryBonus(attr);
        return Math.max(0.0, Math.min(100.0, base + awk + dev + temp));
    }

    public int getEffectiveAttribute(AttributeType attr) {
        return (int) Math.round(getEffectiveAttributeDouble(attr));
    }

    // Concrete getters for all 10 attributes
    public int getEffectiveStrength() { return getEffectiveAttribute(AttributeType.STRENGTH); }
    public int getEffectiveAgility() { return getEffectiveAttribute(AttributeType.AGILITY); }
    public int getEffectiveVitality() { return getEffectiveAttribute(AttributeType.VITALITY); }
    public int getEffectiveEndurance() { return getEffectiveAttribute(AttributeType.ENDURANCE); }
    public int getEffectiveIntelligence() { return getEffectiveAttribute(AttributeType.INTELLIGENCE); }
    public int getEffectivePerception() { return getEffectiveAttribute(AttributeType.PERCEPTION); }
    public int getEffectiveWillpower() { return getEffectiveAttribute(AttributeType.WILLPOWER); }
    public int getEffectiveWisdom() { return getEffectiveWillpower(); }
    public double getEffectiveLuckDouble() { return getEffectiveAttributeDouble(AttributeType.LUCK); }
    public int getEffectiveLuck() { return (int) Math.round(getEffectiveLuckDouble()); }
    public int getEffectiveAetherCapacity() { return getEffectiveAttribute(AttributeType.AETHER_CAPACITY); }
    public int getEffectiveAetherControl() { return getEffectiveAttribute(AttributeType.AETHER_CONTROL); }
    public int getEffectiveAether() { return getEffectiveAetherControl(); }

    /** Total combat power heuristic for quick comparison */
    public int getTotalEffectivePower() {
        return getEffectiveStrength() + getEffectiveAgility() + getEffectiveVitality()
                + getEffectiveEndurance() + getEffectiveIntelligence() + getEffectivePerception()
                + getEffectiveWillpower() + getEffectiveAetherControl();
    }

    // --- Focused Attribute-Training Processing (Stage 2A.6) ---

    /**
     * Processes an AttributeTrainingEvent using the approved diminishing returns and aptitude formulas.
     * Rejects duplicate events, dead individuals, and unawakened aether training.
     *
     * @return true if the event was processed and development bonus was updated
     */
    public boolean processAttributeTrainingEvent(AttributeTrainingEvent event, ProgressionConfig config, boolean isAlive) {
        if (event == null || !isAlive) return false;

        AttributeType target = event.getTargetAttribute();
        if (target == null || !target.isTrainable()) return false;

        // Reject events for unawakened characters when training supernatural Aether attributes
        if (!this.awakened && (target == AttributeType.AETHER_CAPACITY || target == AttributeType.AETHER_CONTROL)) {
            return false;
        }

        String eventId = event.getEventId();
        if (this.processedAttributeEventIds.contains(eventId)) {
            return false;
        }
        if (this.progressionRecord != null && this.progressionRecord.hasProcessedEvent(eventId)) {
            return false;
        }

        // Mark processed
        this.processedAttributeEventIds.add(eventId);
        if (this.progressionRecord != null) {
            this.progressionRecord.markEventProcessed(eventId);
        }

        // 1. Diminishing Returns: max(0.05, (1.0 - currentEffective / 100.0)^1.5)
        double currentEffective = getEffectiveAttributeDouble(target);
        double progressFraction = Math.max(0.0, Math.min(1.0, currentEffective / 100.0));
        double diminishingReturns = Math.max(0.05, Math.pow(1.0 - progressFraction, 1.5));

        // 2. Class Alignment Factor: +0.25 primary, +0.12 secondary 1 or 2, +0.00 otherwise
        double classAlignmentFactor = 0.0;
        if (this.supernaturalClass != null) {
            if (target == this.supernaturalClass.getPrimaryAttributeType()) {
                classAlignmentFactor = 0.25;
            } else if (target == this.supernaturalClass.getSecondaryAttribute1Type()
                    || target == this.supernaturalClass.getSecondaryAttribute2Type()) {
                classAlignmentFactor = 0.12;
            }
        }

        // Discipline Factor: (EffectiveWillpower + EffectiveIntelligence - 40.0) / 200.0
        double effWil = getEffectiveAttributeDouble(AttributeType.WILLPOWER);
        double effInt = getEffectiveAttributeDouble(AttributeType.INTELLIGENCE);
        double disciplineFactor = (effWil + effInt - 40.0) / 200.0;

        // AttributeAptitudeModifier = clamp(0.95 + ClassAlignmentFactor + DisciplineFactor, 0.75, 1.50)
        double aptitudeMod = Math.max(0.75, Math.min(1.50, 0.95 + classAlignmentFactor + disciplineFactor));

        // 3. Compute Gain: BaseGain * TrainingQuality * AttributeAptitudeModifier * DiminishingReturns
        double gain = event.getBaseTrainingValue() * event.getTrainingQuality() * aptitudeMod * diminishingReturns;

        // Apply gain, capping so Baseline + AwakeningBonus + DevelopmentBonus <= 100.0
        double base = getBaselineAttribute(target);
        double awk = getAwakeningBonus(target);
        double currentDev = getDevelopmentBonus(target);
        double maxAllowedDev = Math.max(0.0, 100.0 - base - awk);
        double newDev = Math.min(maxAllowedDev, currentDev + gain);
        setDevelopmentBonus(target, newDev);

        return true;
    }

    public void clearAttributeEventIdsForHeapPruning() {
        this.processedAttributeEventIds.clear();
    }

    // --- Getters & Setters ---

    public AetherAllele getAllele1() { return allele1; }
    public AetherAllele getAllele2() { return allele2; }

    public double getLuck() { return luck; }
    public void setLuck(double luck) { this.luck = Math.max(-1.0, Math.min(1.0, luck)); }

    public double getGreed() { return greed; }
    public void setGreed(double greed) { this.greed = Math.max(0.0, Math.min(1.0, greed)); }

    public double getDarkResonance() { return darkResonance; }
    public void setDarkResonance(double darkResonance) { this.darkResonance = Math.max(0.0, Math.min(1.0, darkResonance)); }

    public boolean isAwakened() { return awakened; }
    public AwakeningPathway getAwakeningPathway() { return awakeningPathway; }
    public SupernaturalClass getSupernaturalClass() { return supernaturalClass; }
    public ClassRarity getRarity() { return rarity; }

    public int getPowerTier() { return powerTier; }
    public int getMaxPowerTier() { return (rarity != null) ? rarity.getMaxPowerTier() : 0; }

    private int lastTierAdvancementYear = 0;

    /**
     * Guarded tier advancement — only through TierAdvancementEvaluator's validated path.
     * Direct mutation is prevented; callers must go through the evaluator.
     */
    public void advanceTierValidated(int newTier, int currentYear) {
        if (newTier != this.powerTier + 1) {
            throw new IllegalArgumentException("Tier advancement must be exactly +1 (current: T" + this.powerTier + ", requested: T" + newTier + ")");
        }
        if (newTier > getMaxPowerTier()) {
            throw new IllegalStateException("Cannot advance beyond max tier T" + getMaxPowerTier());
        }
        this.powerTier = newTier;
        this.lastTierAdvancementYear = currentYear;
    }

    public void advanceTierValidated(int newTier) {
        advanceTierValidated(newTier, 0);
    }

    public int getLastTierAdvancementYear() { return lastTierAdvancementYear; }

    // --- Stage 2A.4: Progression ---
    public PrimaryAbility getPrimaryAbility() { return primaryAbility; }
    public SupernaturalProgressionRecord getProgressionRecord() { return progressionRecord; }
    public double getMastery() { return progressionRecord != null ? progressionRecord.getMastery() : 0.0; }
    public AetherAspect getAspect() { return darkAwakened ? AetherAspect.DARK : AetherAspect.AETHERIC; }
    public double getDerivedPower() { return powerTier * 100.0 + getMastery(); }

    public int getAwakeningYear() { return awakeningYear; }
    public boolean isDarkAwakened() { return darkAwakened; }

    public int getBaseStrength() { return baseStrength; }
    public int getBaseIntelligence() { return baseIntelligence; }
    public int getBaseEndurance() { return baseEndurance; }
    public int getBaseWisdom() { return baseWisdom; }
    public int getBasePerception() { return basePerception; }
    public int getBaseAether() { return baseAether; }

    public void setTempBonusStrength(int v) { this.tempBonusStrength = v; }
    public void setTempBonusAgility(int v) { this.tempBonusAgility = v; }
    public void setTempBonusVitality(int v) { this.tempBonusVitality = v; }
    public void setTempBonusEndurance(int v) { this.tempBonusEndurance = v; }
    public void setTempBonusIntelligence(int v) { this.tempBonusIntelligence = v; }
    public void setTempBonusPerception(int v) { this.tempBonusPerception = v; }
    public void setTempBonusWillpower(int v) { this.tempBonusWillpower = v; this.tempBonusWisdom = v; }
    public void setTempBonusWisdom(int v) { this.tempBonusWisdom = v; this.tempBonusWillpower = v; }
    public void setTempBonusLuck(int v) { this.tempBonusLuck = v; }
    public void setTempBonusAetherCapacity(int v) { this.tempBonusAetherCapacity = v; }
    public void setTempBonusAetherControl(int v) { this.tempBonusAetherControl = v; this.tempBonusAether = v; }
    public void setTempBonusAether(int v) { this.tempBonusAether = v; this.tempBonusAetherControl = v; }

    public void setTemporaryBonus(AttributeType attr, int value) {
        if (attr == null) return;
        switch (attr) {
            case STRENGTH: setTempBonusStrength(value); break;
            case AGILITY: setTempBonusAgility(value); break;
            case VITALITY: setTempBonusVitality(value); break;
            case ENDURANCE: setTempBonusEndurance(value); break;
            case INTELLIGENCE: setTempBonusIntelligence(value); break;
            case PERCEPTION: setTempBonusPerception(value); break;
            case WILLPOWER: setTempBonusWillpower(value); break;
            case LUCK: setTempBonusLuck(value); break;
            case AETHER_CAPACITY: setTempBonusAetherCapacity(value); break;
            case AETHER_CONTROL: setTempBonusAetherControl(value); break;
        }
    }

    public void addTemporaryBonus(AttributeType attr, int delta) {
        if (attr == null) return;
        setTemporaryBonus(attr, (int) Math.round(getTemporaryBonus(attr) + delta));
    }

    public void applyCombatInjuryPenalty(CombatInjury injury) {
        if (injury == null) return;
        for (AttributeType attr : AttributeType.values()) {
            double penalty = injury.getAttributePenalty(attr);
            if (penalty > 0.0) {
                addTemporaryBonus(attr, (int) -Math.round(penalty));
            }
        }
    }

    public void removeCombatInjuryPenalty(CombatInjury injury) {
        if (injury == null) return;
        for (AttributeType attr : AttributeType.values()) {
            double penalty = injury.getAttributePenalty(attr);
            if (penalty > 0.0) {
                addTemporaryBonus(attr, (int) Math.round(penalty));
            }
        }
    }

    /** Compact string for chronicle/event logging */
    public String getAwakeningDescription() {
        if (!awakened) return "Mortal";
        String dark = darkAwakened ? " [DARK]" : "";
        return String.format("%s %s (T%d/%s)%s",
                rarity.getDisplayName(),
                supernaturalClass.getDisplayName(),
                powerTier,
                rarity.getDisplayName(),
                dark);
    }

    /** Compact attribute string: STR/INT/END/WIS/PER/AET */
    public String getEffectiveAttributeString() {
        return String.format("STR:%d INT:%d END:%d WIS:%d PER:%d AET:%d",
                getEffectiveStrength(), getEffectiveIntelligence(), getEffectiveEndurance(),
                getEffectiveWisdom(), getEffectivePerception(), getEffectiveAether());
    }

    /**
     * Testing factory method to create an awakened profile with deterministic fields.
     */
    public static AetherProfile createForTesting(SupernaturalClass cls, ClassRarity rarity, int tier, boolean dark) {
        AetherProfile ap = new AetherProfile(AetherAllele.DOMINANT, AetherAllele.DOMINANT, new Random(42));
        ap.awakened = true;
        ap.awakeningPathway = dark ? AwakeningPathway.DARK_AWAKENING : AwakeningPathway.NORMAL;
        ap.supernaturalClass = (cls != null) ? cls : SupernaturalClass.VANGUARD;
        ap.rarity = (rarity != null) ? rarity : ClassRarity.COMMON;
        ap.powerTier = tier;
        ap.darkAwakened = dark;
        ap.awakeningYear = 50;

        // Initialize T1 awakening bonuses from class
        ap.awakeningBonusStrength = ap.supernaturalClass.getT1BonusStrength();
        ap.awakeningBonusAgility = ap.supernaturalClass.getT1BonusAgility();
        ap.awakeningBonusVitality = ap.supernaturalClass.getT1BonusVitality();
        ap.awakeningBonusEndurance = ap.supernaturalClass.getT1BonusEndurance();
        ap.awakeningBonusIntelligence = ap.supernaturalClass.getT1BonusIntelligence();
        ap.awakeningBonusPerception = ap.supernaturalClass.getT1BonusPerception();
        ap.awakeningBonusWillpower = ap.supernaturalClass.getT1BonusWillpower();
        ap.awakeningBonusWisdom = ap.awakeningBonusWillpower;
        ap.awakeningBonusLuck = 0;
        ap.awakeningBonusAetherCapacity = ap.supernaturalClass.getT1BonusAetherCapacity();
        ap.awakeningBonusAetherControl = ap.supernaturalClass.getT1BonusAetherControl();
        ap.awakeningBonusAether = ap.awakeningBonusAetherControl;
        ap.baseAetherCapacity = 4;
        ap.baseAetherControl = 4;
        ap.baseAether = 4;

        ap.primaryAbility = PrimaryAbility.forClass(ap.supernaturalClass);
        ap.progressionRecord = new SupernaturalProgressionRecord(50, 20, ap.awakeningPathway, ap.supernaturalClass, ap.rarity, 10.0, ap.primaryAbility);
        return ap;
    }

    public static AetherProfile createAwakened(SupernaturalClass cls, AetherAspect aspect, ClassRarity rarity, int tier, double mastery) {
        AetherProfile ap = createForTesting(cls, rarity, tier, aspect == AetherAspect.DARK);
        if (ap.progressionRecord != null) {
            ap.progressionRecord.setMastery(mastery);
        }
        return ap;
    }
}
