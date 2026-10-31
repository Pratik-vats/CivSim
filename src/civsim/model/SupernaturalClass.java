package civsim.model;

/**
 * Seven supernatural classes an awakened person can manifest.
 * Each class has a base probability weight, a primary and secondary attribute focus,
 * and T1-level awakening attribute bonuses.
 *
 * Hereditary class affinity: Children inherit a weighted bias toward their parents' classes.
 * If both parents share the same class, child has 60% affinity for that class.
 * If parents have different classes, child has 35% for each parent's class, 30% random.
 */
public enum SupernaturalClass {
    /** Front-line warrior with enhanced physical prowess */
    VANGUARD("Vanguard", 0.20,
            "Strength", "Endurance",
            5, 0, 3, 2, 0, 0,
            "Melee specialist with overwhelming physical force"),

    /** Elemental magic wielder — fire, ice, lightning, earth */
    ELEMENTALIST("Elementalist", 0.18,
            "Intelligence", "Aether",
            0, 5, 1, 0, 2, 3,
            "Commands elemental forces with devastating ranged power"),

    /** Healer and life-force manipulator */
    VITALIST("Vitalist", 0.15,
            "Aether", "Endurance",
            0, 2, 0, 4, 1, 4,
            "Restores life, purifies corruption, and bolsters allies"),

    /** Fortress defender and barrier specialist */
    WARDEN("Warden", 0.17,
            "Endurance", "Strength",
            3, 0, 5, 1, 0, 1,
            "Immovable protector who shields settlements from harm"),

    /** Oracle with precognition and strategic foresight */
    SEER("Seer", 0.12,
            "Intelligence", "Luck",
            0, 4, 0, 1, 5, 2,
            "Peer into probability streams for prophecy and counsel"),

    /** Tames and bonds with mutated fauna */
    BEASTBINDER("Beastbinder", 0.10,
            "Aether", "Strength",
            2, 1, 2, 0, 1, 4,
            "Forms symbiotic bonds with Great Mutation beasts"),

    /** Spatial manipulator — teleportation and dimensional rifts */
    RIFTWALKER("Riftwalker", 0.08,
            "Intelligence", "Aether",
            1, 3, 0, 0, 3, 5,
            "Tears rifts in space for instant travel and surprise attacks");

    private final String displayName;
    private final double baseWeight;
    private final String primaryAttribute;
    private final String secondaryAttribute;

    // T1 Awakening Attribute Bonuses: [STR, INT, END, WIS, PER, AET]
    private final int t1BonusStrength;
    private final int t1BonusIntelligence;
    private final int t1BonusEndurance;
    private final int t1BonusWisdom;
    private final int t1BonusPerception;
    private final int t1BonusAether;

    private final String description;

    SupernaturalClass(String displayName, double baseWeight,
                      String primaryAttribute, String secondaryAttribute,
                      int t1Str, int t1Int, int t1End, int t1Wis, int t1Per, int t1Aet,
                      String description) {
        this.displayName = displayName;
        this.baseWeight = baseWeight;
        this.primaryAttribute = primaryAttribute;
        this.secondaryAttribute = secondaryAttribute;
        this.t1BonusStrength = t1Str;
        this.t1BonusIntelligence = t1Int;
        this.t1BonusEndurance = t1End;
        this.t1BonusWisdom = t1Wis;
        this.t1BonusPerception = t1Per;
        this.t1BonusAether = t1Aet;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public double getBaseWeight() { return baseWeight; }
    public String getPrimaryAttribute() { return primaryAttribute; }
    public String getSecondaryAttribute() { return secondaryAttribute; }
    public String getDescription() { return description; }

    public int getT1BonusStrength() { return t1BonusStrength; }
    public int getT1BonusIntelligence() { return t1BonusIntelligence; }
    public int getT1BonusEndurance() { return t1BonusEndurance; }
    public int getT1BonusWisdom() { return t1BonusWisdom; }
    public int getT1BonusPerception() { return t1BonusPerception; }
    public int getT1BonusAether() { return t1BonusAether; }

    public AttributeType getPrimaryAttributeType() {
        switch (this) {
            case VANGUARD: return AttributeType.STRENGTH;
            case ELEMENTALIST: return AttributeType.AETHER_CONTROL;
            case VITALIST: return AttributeType.AETHER_CONTROL;
            case WARDEN: return AttributeType.AETHER_CONTROL;
            case SEER: return AttributeType.PERCEPTION;
            case BEASTBINDER: return AttributeType.WILLPOWER;
            case RIFTWALKER: return AttributeType.AETHER_CONTROL;
            default: return AttributeType.STRENGTH;
        }
    }

    public AttributeType getSecondaryAttribute1Type() {
        switch (this) {
            case VANGUARD: return AttributeType.VITALITY;
            case ELEMENTALIST: return AttributeType.AETHER_CAPACITY;
            case VITALIST: return AttributeType.VITALITY;
            case WARDEN: return AttributeType.ENDURANCE;
            case SEER: return AttributeType.INTELLIGENCE;
            case BEASTBINDER: return AttributeType.PERCEPTION;
            case RIFTWALKER: return AttributeType.AGILITY;
            default: return AttributeType.ENDURANCE;
        }
    }

    public AttributeType getSecondaryAttribute2Type() {
        switch (this) {
            case VANGUARD: return AttributeType.ENDURANCE;
            case ELEMENTALIST: return AttributeType.INTELLIGENCE;
            case VITALIST: return AttributeType.WILLPOWER;
            case WARDEN: return AttributeType.WILLPOWER;
            case SEER: return AttributeType.AETHER_CONTROL;
            case BEASTBINDER: return AttributeType.AETHER_CONTROL;
            case RIFTWALKER: return AttributeType.PERCEPTION;
            default: return AttributeType.WILLPOWER;
        }
    }

    public int getT1Bonus(AttributeType attr) {
        if (attr == null) return 0;
        switch (attr) {
            case STRENGTH: return t1BonusStrength;
            case AGILITY: return (this == RIFTWALKER) ? 2 : 0;
            case VITALITY: return (this == VANGUARD) ? 2 : (this == VITALIST ? 3 : (this == WARDEN ? 2 : 0));
            case ENDURANCE: return t1BonusEndurance;
            case INTELLIGENCE: return t1BonusIntelligence;
            case PERCEPTION: return t1BonusPerception;
            case WILLPOWER: return t1BonusWisdom;
            case LUCK: return 0;
            case AETHER_CAPACITY: return (this == ELEMENTALIST) ? 3 : (this == VITALIST || this == WARDEN || this == RIFTWALKER ? 2 : (this == SEER || this == BEASTBINDER ? 1 : 0));
            case AETHER_CONTROL: return t1BonusAether;
            default: return 0;
        }
    }

    public int getT1BonusAgility() { return getT1Bonus(AttributeType.AGILITY); }
    public int getT1BonusVitality() { return getT1Bonus(AttributeType.VITALITY); }
    public int getT1BonusWillpower() { return getT1Bonus(AttributeType.WILLPOWER); }
    public int getT1BonusAetherCapacity() { return getT1Bonus(AttributeType.AETHER_CAPACITY); }
    public int getT1BonusAetherControl() { return getT1Bonus(AttributeType.AETHER_CONTROL); }

    /**
     * Select a class using weighted random selection, optionally biased
     * toward inherited parental affinities.
     *
     * @param roll random 0.0–1.0
     * @param parentClass1 first parent's class (nullable for no inheritance)
     * @param parentClass2 second parent's class (nullable)
     * @return selected supernatural class
     */
    public static SupernaturalClass selectWeighted(double roll, SupernaturalClass parentClass1, SupernaturalClass parentClass2) {
        // If both parents have the same class: 60% affinity for that class
        if (parentClass1 != null && parentClass1 == parentClass2) {
            if (roll < 0.60) return parentClass1;
            // Remaining 40% distributed among other classes
            return selectFromPool(roll - 0.60, 0.40, parentClass1);
        }

        // If parents have different classes: 35% each, 30% random
        if (parentClass1 != null && parentClass2 != null) {
            if (roll < 0.35) return parentClass1;
            if (roll < 0.70) return parentClass2;
            return selectFromPool(roll - 0.70, 0.30, null);
        }

        // If one parent has a class: 40% that class, 60% random
        SupernaturalClass inherited = (parentClass1 != null) ? parentClass1 : parentClass2;
        if (inherited != null) {
            if (roll < 0.40) return inherited;
            return selectFromPool(roll - 0.40, 0.60, null);
        }

        // No parental class — pure weight-based selection
        return selectFromPool(roll, 1.0, null);
    }

    private static SupernaturalClass selectFromPool(double normalizedRoll, double totalPoolWeight, SupernaturalClass excludeClass) {
        double adjustedRoll = normalizedRoll / totalPoolWeight; // normalize to 0.0–1.0
        double totalWeight = 0.0;
        for (SupernaturalClass sc : values()) {
            if (sc == excludeClass) continue;
            totalWeight += sc.baseWeight;
        }

        double cumulative = 0.0;
        for (SupernaturalClass sc : values()) {
            if (sc == excludeClass) continue;
            cumulative += sc.baseWeight / totalWeight;
            if (adjustedRoll < cumulative) return sc;
        }
        return VANGUARD; // fallback
    }
}
