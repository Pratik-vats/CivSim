package civsim;

import civsim.model.ExtinctionPolicy;
import java.util.List;

public class SimulationConfig {
    private final List<String> villageNames;
    private final int initialPopulationPerVillage;
    private final long tickDurationMillis;
    private final int maxInitialAge;
    private final int deathAge;
    private final int minMarriageAgeExclusive;
    private final int maxMarriageAgeDifference;
    private final int maxFemaleFertilityAge;
    private final int minChildrenPerCouple;
    private final int maxChildrenPerCouple;
    private final int minBirthIntervalMonths;
    private final long randomSeed;
    private final int maxSimulationYears;
    private final boolean printIndividualEvents;
    private final int maxKinshipGenerations;
    private final boolean preventSameFamilyMarriage;
    private final boolean allowCrossVillageMarriage;
    private final boolean allowMigration;
    private final int migrationPopulationThreshold;
    private final int maxMigrantsPerYear;
    private final int villagePopulationThreshold;
    private final int townPopulationThreshold;
    private final int cityPopulationThreshold;
    private final int megacityPopulationThreshold;
    private final boolean allowColonization;
    private final int minPopulationForColonization;
    private final int pioneerPartySize;
    private final double foodPerPersonPerMonth;
    private final double foodProducedPerFarmer;
    private final int housingCapacityPerBuilding;
    private final double initialPersonalWealth;
    private final double bankInterestRate;
    private final double initialFoodPrice;
    private final double taxRate;
    private final double baseTechUpgradeCost;
    private final double disasterProbabilityPerYear;
    private final boolean allowDiplomacyAndRaids;
    private final double raidProbability;
    private final int awakeningStartYear;

    // Phase 2B.4 Anomaly Skirmish & Conflict Configuration
    private final double personalWealthLootFraction;
    private final int contestMusterWindowMonths;
    private final int contestReengagementCooldownMonths;
    private final int securingDurationMonths;
    private final int minSecuringPersonnel;
    private final int unclaimedAnomalyMutationMonths;

    // Extinction Policy Configuration
    private final ExtinctionPolicy extinctionPolicy;

    public SimulationConfig(
            List<String> villageNames,
            int initialPopulationPerVillage,
            long tickDurationMillis,
            int maxInitialAge,
            int deathAge,
            int minMarriageAgeExclusive,
            int maxMarriageAgeDifference,
            int maxFemaleFertilityAge,
            int minChildrenPerCouple,
            int maxChildrenPerCouple,
            int minBirthIntervalMonths,
            long randomSeed,
            int maxSimulationYears,
            boolean printIndividualEvents,
            int maxKinshipGenerations,
            boolean preventSameFamilyMarriage,
            boolean allowCrossVillageMarriage,
            boolean allowMigration,
            int migrationPopulationThreshold,
            int maxMigrantsPerYear,
            int villagePopulationThreshold,
            int townPopulationThreshold,
            int cityPopulationThreshold,
            int megacityPopulationThreshold,
            boolean allowColonization,
            int minPopulationForColonization,
            int pioneerPartySize,
            double foodPerPersonPerMonth,
            double foodProducedPerFarmer,
            int housingCapacityPerBuilding,
            double initialPersonalWealth,
            double bankInterestRate,
            double initialFoodPrice,
            double taxRate,
            double baseTechUpgradeCost,
            double disasterProbabilityPerYear,
            boolean allowDiplomacyAndRaids,
            double raidProbability,
            int awakeningStartYear) {
        this.villageNames = List.copyOf(villageNames);
        this.initialPopulationPerVillage = initialPopulationPerVillage;
        this.tickDurationMillis = tickDurationMillis;
        this.maxInitialAge = maxInitialAge;
        this.deathAge = deathAge;
        this.minMarriageAgeExclusive = minMarriageAgeExclusive;
        this.maxMarriageAgeDifference = maxMarriageAgeDifference;
        this.maxFemaleFertilityAge = maxFemaleFertilityAge;
        this.minChildrenPerCouple = minChildrenPerCouple;
        this.maxChildrenPerCouple = maxChildrenPerCouple;
        this.minBirthIntervalMonths = minBirthIntervalMonths;
        this.randomSeed = randomSeed;
        this.maxSimulationYears = maxSimulationYears;
        this.printIndividualEvents = printIndividualEvents;
        this.maxKinshipGenerations = maxKinshipGenerations;
        this.preventSameFamilyMarriage = preventSameFamilyMarriage;
        this.allowCrossVillageMarriage = allowCrossVillageMarriage;
        this.allowMigration = allowMigration;
        this.migrationPopulationThreshold = migrationPopulationThreshold;
        this.maxMigrantsPerYear = maxMigrantsPerYear;
        this.villagePopulationThreshold = villagePopulationThreshold;
        this.townPopulationThreshold = townPopulationThreshold;
        this.cityPopulationThreshold = cityPopulationThreshold;
        this.megacityPopulationThreshold = megacityPopulationThreshold;
        this.allowColonization = allowColonization;
        this.minPopulationForColonization = minPopulationForColonization;
        this.pioneerPartySize = pioneerPartySize;
        this.foodPerPersonPerMonth = foodPerPersonPerMonth;
        this.foodProducedPerFarmer = foodProducedPerFarmer;
        this.housingCapacityPerBuilding = housingCapacityPerBuilding;
        this.initialPersonalWealth = initialPersonalWealth;
        this.bankInterestRate = bankInterestRate;
        this.initialFoodPrice = initialFoodPrice;
        this.taxRate = taxRate;
        this.baseTechUpgradeCost = baseTechUpgradeCost;
        this.disasterProbabilityPerYear = disasterProbabilityPerYear;
        this.allowDiplomacyAndRaids = allowDiplomacyAndRaids;
        this.raidProbability = raidProbability;
        this.awakeningStartYear = awakeningStartYear;

        this.personalWealthLootFraction = 0.50;
        this.contestMusterWindowMonths = 1;
        this.contestReengagementCooldownMonths = 2;
        this.securingDurationMonths = 1;
        this.minSecuringPersonnel = 1;
        this.unclaimedAnomalyMutationMonths = 6;
        this.extinctionPolicy = ExtinctionPolicy.STOP_ON_HUMAN_EXTINCTION;
    }

    public SimulationConfig(
            List<String> villageNames,
            int initialPopulationPerVillage,
            long tickDurationMillis,
            int maxInitialAge,
            int deathAge,
            int minMarriageAgeExclusive,
            int maxMarriageAgeDifference,
            int maxFemaleFertilityAge,
            int minChildrenPerCouple,
            int maxChildrenPerCouple,
            int minBirthIntervalMonths,
            long randomSeed,
            int maxSimulationYears,
            boolean printIndividualEvents,
            int maxKinshipGenerations,
            boolean preventSameFamilyMarriage,
            boolean allowCrossVillageMarriage,
            boolean allowMigration,
            int migrationPopulationThreshold,
            int maxMigrantsPerYear,
            int villagePopulationThreshold,
            int townPopulationThreshold,
            int cityPopulationThreshold,
            int megacityPopulationThreshold,
            boolean allowColonization,
            int minPopulationForColonization,
            int pioneerPartySize,
            double foodPerPersonPerMonth,
            double foodProducedPerFarmer,
            int housingCapacityPerBuilding,
            double initialPersonalWealth,
            double bankInterestRate,
            double initialFoodPrice,
            double taxRate,
            double baseTechUpgradeCost,
            double disasterProbabilityPerYear,
            boolean allowDiplomacyAndRaids,
            double raidProbability,
            int awakeningStartYear,
            ExtinctionPolicy extinctionPolicy) {
        this.villageNames = List.copyOf(villageNames);
        this.initialPopulationPerVillage = initialPopulationPerVillage;
        this.tickDurationMillis = tickDurationMillis;
        this.maxInitialAge = maxInitialAge;
        this.deathAge = deathAge;
        this.minMarriageAgeExclusive = minMarriageAgeExclusive;
        this.maxMarriageAgeDifference = maxMarriageAgeDifference;
        this.maxFemaleFertilityAge = maxFemaleFertilityAge;
        this.minChildrenPerCouple = minChildrenPerCouple;
        this.maxChildrenPerCouple = maxChildrenPerCouple;
        this.minBirthIntervalMonths = minBirthIntervalMonths;
        this.randomSeed = randomSeed;
        this.maxSimulationYears = maxSimulationYears;
        this.printIndividualEvents = printIndividualEvents;
        this.maxKinshipGenerations = maxKinshipGenerations;
        this.preventSameFamilyMarriage = preventSameFamilyMarriage;
        this.allowCrossVillageMarriage = allowCrossVillageMarriage;
        this.allowMigration = allowMigration;
        this.migrationPopulationThreshold = migrationPopulationThreshold;
        this.maxMigrantsPerYear = maxMigrantsPerYear;
        this.villagePopulationThreshold = villagePopulationThreshold;
        this.townPopulationThreshold = townPopulationThreshold;
        this.cityPopulationThreshold = cityPopulationThreshold;
        this.megacityPopulationThreshold = megacityPopulationThreshold;
        this.allowColonization = allowColonization;
        this.minPopulationForColonization = minPopulationForColonization;
        this.pioneerPartySize = pioneerPartySize;
        this.foodPerPersonPerMonth = foodPerPersonPerMonth;
        this.foodProducedPerFarmer = foodProducedPerFarmer;
        this.housingCapacityPerBuilding = housingCapacityPerBuilding;
        this.initialPersonalWealth = initialPersonalWealth;
        this.bankInterestRate = bankInterestRate;
        this.initialFoodPrice = initialFoodPrice;
        this.taxRate = taxRate;
        this.baseTechUpgradeCost = baseTechUpgradeCost;
        this.disasterProbabilityPerYear = disasterProbabilityPerYear;
        this.allowDiplomacyAndRaids = allowDiplomacyAndRaids;
        this.raidProbability = raidProbability;
        this.awakeningStartYear = awakeningStartYear;

        this.personalWealthLootFraction = 0.50;
        this.contestMusterWindowMonths = 1;
        this.contestReengagementCooldownMonths = 2;
        this.securingDurationMonths = 1;
        this.minSecuringPersonnel = 1;
        this.unclaimedAnomalyMutationMonths = 6;
        this.extinctionPolicy = (extinctionPolicy != null) ? extinctionPolicy : ExtinctionPolicy.STOP_ON_HUMAN_EXTINCTION;
    }

    public SimulationConfig(
            List<String> villageNames,
            int initialPopulationPerVillage,
            long tickDurationMillis,
            int maxInitialAge,
            int deathAge,
            int minMarriageAgeExclusive,
            int maxMarriageAgeDifference,
            int maxFemaleFertilityAge,
            int minChildrenPerCouple,
            int maxChildrenPerCouple,
            int minBirthIntervalMonths,
            long randomSeed,
            int maxSimulationYears,
            boolean printIndividualEvents,
            int maxKinshipGenerations,
            boolean preventSameFamilyMarriage,
            boolean allowCrossVillageMarriage,
            boolean allowMigration,
            int migrationPopulationThreshold,
            int maxMigrantsPerYear,
            int villagePopulationThreshold,
            int townPopulationThreshold,
            int cityPopulationThreshold,
            int megacityPopulationThreshold,
            boolean allowColonization,
            int minPopulationForColonization,
            int pioneerPartySize,
            double foodPerPersonPerMonth,
            double foodProducedPerFarmer,
            int housingCapacityPerBuilding,
            double initialPersonalWealth,
            double bankInterestRate,
            double initialFoodPrice,
            double taxRate,
            double baseTechUpgradeCost,
            double disasterProbabilityPerYear,
            boolean allowDiplomacyAndRaids,
            double raidProbability) {
        this(villageNames, initialPopulationPerVillage, tickDurationMillis, maxInitialAge, deathAge,
                minMarriageAgeExclusive, maxMarriageAgeDifference, maxFemaleFertilityAge, minChildrenPerCouple,
                maxChildrenPerCouple, minBirthIntervalMonths, randomSeed, maxSimulationYears, printIndividualEvents,
                maxKinshipGenerations, preventSameFamilyMarriage, allowCrossVillageMarriage, allowMigration,
                migrationPopulationThreshold, maxMigrantsPerYear, villagePopulationThreshold, townPopulationThreshold,
                cityPopulationThreshold, megacityPopulationThreshold, allowColonization, minPopulationForColonization,
                pioneerPartySize, foodPerPersonPerMonth, foodProducedPerFarmer, housingCapacityPerBuilding,
                initialPersonalWealth, bankInterestRate, initialFoodPrice, taxRate, baseTechUpgradeCost,
                disasterProbabilityPerYear, allowDiplomacyAndRaids, raidProbability, 50);
    }

    public static SimulationConfig createDefault() {
        return new SimulationConfig(
                List.of("Settlement A", "Settlement B"),
                25,     // 25 people per settlement (50 total founder pop)
                80L,   // 200ms per month = 2.4 real seconds per simulated year
                50,     // Initial age 0 to 50 (realistic founding demographic, not elderly)
                75,     // Base median death age
                18,     // Eligible when age > 18 (19+)
                15,     // Max marriage age gap
                45,     // Max female fertility age
                2,      // Min planned children
                5,      // Max planned children
                12,     // Min birth interval months
                42L,    // Default seed
                0,      // Run until stopped
                false,  // Print only annual reports
                2,      // Max kinship generations
                true,   // Prevent same family marriage
                true,   // Allow cross-village marriage
                true,   // Allow migration
                120,     // Migration attractiveness difference threshold
                20,      // Max migrants per year per distressed settlement
                50,     // Hamlet -> Village threshold (was 15)
                150,    // Village -> Town threshold (was 45)
                400,    // Town -> City threshold (was 100)
                1500,   // City -> Megacity threshold (uncapped housing)
                true,   // Allow colonization
                120,    // Min population for colonization (was 50)
                10,     // Pioneer party size (5 males, 5 females)
                1.0,    // Food per person per month
                3.5,    // Base food produced per farmer per month
                8,      // Housing capacity per building
                50.0,   // Initial personal gold/wealth for adults
                0.04,   // Annual bank interest rate (4%)
                2.0,    // Initial market price of food per unit
                0.08,   // 8% annual wealth tax into settlement treasury
                180.0,  // Base gold cost in treasury to advance Tech Level
                0.12,   // 12% annual chance of a natural disaster per settlement
                true,   // Stage 6 Diplomacy: Enable inter-settlement alliances and raids
                0.15,   // 15% annual chance of military conflict between non-allied settlements
                50      // Year 50: The Great Mutation — supernatural awakening begins
        );
    }

    public static SimulationConfig createDefaultWithSeed(long seed) {
        return new SimulationConfig(
                List.of("Settlement A", "Settlement B"),
                25,     // 25 people per settlement (50 total founder pop)
                80L,   // 200ms per month = 2.4 real seconds per simulated year
                50,     // Initial age 0 to 50 (realistic founding demographic, not elderly)
                75,     // Base median death age
                18,     // Eligible when age > 18 (19+)
                15,     // Max marriage age gap
                45,     // Max female fertility age
                2,      // Min planned children
                5,      // Max planned children
                12,     // Min birth interval months
                seed,   // Custom seed
                0,      // Run until stopped
                false,  // Print only annual reports
                2,      // Max kinship generations
                true,   // Prevent same family marriage
                true,   // Allow cross-village marriage
                true,   // Allow migration
                120,     // Migration attractiveness difference threshold
                20,      // Max migrants per year per distressed settlement
                50,     // Hamlet -> Village threshold (was 15)
                150,    // Village -> Town threshold (was 45)
                400,    // Town -> City threshold (was 100)
                1500,   // City -> Megacity threshold (uncapped housing)
                true,   // Allow colonization
                120,    // Min population for colonization (was 50)
                10,     // Pioneer party size (5 males, 5 females)
                1.0,    // Food per person per month
                3.5,    // Base food produced per farmer per month
                8,      // Housing capacity per building
                50.0,   // Initial personal gold/wealth for adults
                0.04,   // Annual bank interest rate (4%)
                2.0,    // Initial market price of food per unit
                0.08,   // 8% annual wealth tax into settlement treasury
                180.0,  // Base gold cost in treasury to advance Tech Level
                0.12,   // 12% annual chance of a natural disaster per settlement
                true,   // Stage 6 Diplomacy: Enable inter-settlement alliances and raids
                0.15,   // 15% annual chance of military conflict between non-allied settlements
                50      // Year 50: The Great Mutation — supernatural awakening begins
        );
    }

    public SimulationConfig withExtinctionPolicy(ExtinctionPolicy policy) {
        return new SimulationConfig(
                this.villageNames, this.initialPopulationPerVillage, this.tickDurationMillis, this.maxInitialAge, this.deathAge,
                this.minMarriageAgeExclusive, this.maxMarriageAgeDifference, this.maxFemaleFertilityAge, this.minChildrenPerCouple,
                this.maxChildrenPerCouple, this.minBirthIntervalMonths, this.randomSeed, this.maxSimulationYears, this.printIndividualEvents,
                this.maxKinshipGenerations, this.preventSameFamilyMarriage, this.allowCrossVillageMarriage, this.allowMigration,
                this.migrationPopulationThreshold, this.maxMigrantsPerYear, this.villagePopulationThreshold, this.townPopulationThreshold,
                this.cityPopulationThreshold, this.megacityPopulationThreshold, this.allowColonization, this.minPopulationForColonization,
                this.pioneerPartySize, this.foodPerPersonPerMonth, this.foodProducedPerFarmer, this.housingCapacityPerBuilding,
                this.initialPersonalWealth, this.bankInterestRate, this.initialFoodPrice, this.taxRate, this.baseTechUpgradeCost,
                this.disasterProbabilityPerYear, this.allowDiplomacyAndRaids, this.raidProbability, this.awakeningStartYear,
                policy);
    }

    public SimulationConfig withAwakeningStartYear(int awakeningStartYear) {
        return new SimulationConfig(
                this.villageNames, this.initialPopulationPerVillage, this.tickDurationMillis, this.maxInitialAge, this.deathAge,
                this.minMarriageAgeExclusive, this.maxMarriageAgeDifference, this.maxFemaleFertilityAge, this.minChildrenPerCouple,
                this.maxChildrenPerCouple, this.minBirthIntervalMonths, this.randomSeed, this.maxSimulationYears, this.printIndividualEvents,
                this.maxKinshipGenerations, this.preventSameFamilyMarriage, this.allowCrossVillageMarriage, this.allowMigration,
                this.migrationPopulationThreshold, this.maxMigrantsPerYear, this.villagePopulationThreshold, this.townPopulationThreshold,
                this.cityPopulationThreshold, this.megacityPopulationThreshold, this.allowColonization, this.minPopulationForColonization,
                this.pioneerPartySize, this.foodPerPersonPerMonth, this.foodProducedPerFarmer, this.housingCapacityPerBuilding,
                this.initialPersonalWealth, this.bankInterestRate, this.initialFoodPrice, this.taxRate, this.baseTechUpgradeCost,
                this.disasterProbabilityPerYear, this.allowDiplomacyAndRaids, this.raidProbability, awakeningStartYear,
                this.extinctionPolicy);
    }

    public List<String> getVillageNames() { return villageNames; }
    public int getInitialPopulationPerVillage() { return initialPopulationPerVillage; }
    public long getTickDurationMillis() { return tickDurationMillis; }
    public int getMaxInitialAge() { return maxInitialAge; }
    public int getDeathAge() { return deathAge; }
    public int getMinMarriageAgeExclusive() { return minMarriageAgeExclusive; }
    public int getMaxMarriageAgeDifference() { return maxMarriageAgeDifference; }
    public int getMaxFemaleFertilityAge() { return maxFemaleFertilityAge; }
    public int getMinChildrenPerCouple() { return minChildrenPerCouple; }
    public int getMaxChildrenPerCouple() { return maxChildrenPerCouple; }
    public int getMinBirthIntervalMonths() { return minBirthIntervalMonths; }
    public long getRandomSeed() { return randomSeed; }
    public int getMaxSimulationYears() { return maxSimulationYears; }
    public boolean isPrintIndividualEvents() { return printIndividualEvents; }
    public int getMaxKinshipGenerations() { return maxKinshipGenerations; }
    public boolean isPreventSameFamilyMarriage() { return preventSameFamilyMarriage; }
    public boolean isAllowCrossVillageMarriage() { return allowCrossVillageMarriage; }
    public boolean isAllowMigration() { return allowMigration; }
    public int getMigrationPopulationThreshold() { return migrationPopulationThreshold; }
    public int getMaxMigrantsPerYear() { return maxMigrantsPerYear; }
    public int getVillagePopulationThreshold() { return villagePopulationThreshold; }
    public int getTownPopulationThreshold() { return townPopulationThreshold; }
    public int getCityPopulationThreshold() { return cityPopulationThreshold; }
    public int getMegacityPopulationThreshold() { return megacityPopulationThreshold; }
    public boolean isAllowColonization() { return allowColonization; }
    public int getMinPopulationForColonization() { return minPopulationForColonization; }
    public int getPioneerPartySize() { return pioneerPartySize; }
    public double getFoodPerPersonPerMonth() { return foodPerPersonPerMonth; }
    public double getFoodProducedPerFarmer() { return foodProducedPerFarmer; }
    public int getHousingCapacityPerBuilding() { return housingCapacityPerBuilding; }
    public double getInitialPersonalWealth() { return initialPersonalWealth; }
    public double getBankInterestRate() { return bankInterestRate; }
    public double getInitialFoodPrice() { return initialFoodPrice; }
    public double getTaxRate() { return taxRate; }
    public double getBaseTechUpgradeCost() { return baseTechUpgradeCost; }
    public double getDisasterProbabilityPerYear() { return disasterProbabilityPerYear; }
    public boolean isAllowDiplomacyAndRaids() { return allowDiplomacyAndRaids; }
    public double getRaidProbability() { return raidProbability; }
    public int getAwakeningStartYear() { return awakeningStartYear; }

    public double getPersonalWealthLootFraction() { return personalWealthLootFraction; }
    public int getContestMusterWindowMonths() { return contestMusterWindowMonths; }
    public int getContestReengagementCooldownMonths() { return contestReengagementCooldownMonths; }
    public int getSecuringDurationMonths() { return securingDurationMonths; }
    public int getMinSecuringPersonnel() { return minSecuringPersonnel; }
    public int getUnclaimedAnomalyMutationMonths() { return unclaimedAnomalyMutationMonths; }
    public ExtinctionPolicy getExtinctionPolicy() { return extinctionPolicy; }
}