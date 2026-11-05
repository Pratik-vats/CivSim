package civsim.persistence;

import civsim.dto.WorldSaveDetailDto;
import civsim.dto.WorldSaveSummaryDto;

import java.util.*;

/**
 * Portable, structured serializable representation of a saved world state.
 * Supports full deterministic continuation, branch lineage, and checksum validation.
 */
public class WorldSaveData {
    public int schemaVersion = 3;
    public String worldId = "world-001";
    public String worldName = "CivSim World";
    public long createdAt;
    public int year;
    public int month;
    public long totalMonths;
    public long randomSeed;
    public long randomInvocations;
    public String lifecycleState;
    public String engineVersion = "3.6";

    // Branching and alternate timeline lineage
    public String parentWorldId;
    public String parentCheckpointId;
    public Integer branchForkYear;
    public Integer branchForkMonth;
    public String branchRngPolicy = "CONTINUE_CHECKPOINT_RNG";

    // Exact serialized random state
    public String rngStateBase64;
    public String checksum;

    public int nextPersonId;
    public int nextFamilyId;
    public int nextCoupleId;
    public int nextVillageId;
    public int totalRecordedPeople;

    // Engine yearly event accumulators
    public int crossVillageMarriagesThisYear;
    public int settlementUpgradesThisYear;

    public List<VillageSaveData> villages = new ArrayList<>();
    public List<PersonSaveData> people = new ArrayList<>();
    public List<FamilySaveData> families = new ArrayList<>();
    public List<CoupleSaveData> couples = new ArrayList<>();
    public List<OrgSaveData> organizations = new ArrayList<>();
    public List<AnomalySaveData> anomalies = new ArrayList<>();
    public List<LairSaveData> lairs = new ArrayList<>();
    public List<AnnualReportSaveData> annualReports = new ArrayList<>();

    // Phase 2C & Phase 3 full simulation persistence additions
    public CraftingSystemSaveData craftingSystem;
    public List<HeirloomSaveData> heirlooms = new ArrayList<>();
    public List<BloodFeudSaveData> bloodFeuds = new ArrayList<>();

    public static class VillageSaveData {
        public int id;
        public String name;
        public double x;
        public double y;
        public String biome;
        public String tier;
        public double food;
        public double wood;
        public double ore;
        public double tools;
        public double treasury;
        public int techLevel;
        public double fortification;
        public int housingCount;
        public Integer leaderId;

        // Diplomatic and Workshop State
        public Map<String, String> diplomaticRelations = new HashMap<>();
        public List<CraftingFacilitySaveData> craftingFacilities = new ArrayList<>();
        public Map<String, Double> supernaturalInventory = new HashMap<>();
        public Map<String, Double> monsterMaterials = new HashMap<>();
        public int monsterCores = 0;

        // Residents and Banking State
        public List<Integer> residentIds = new ArrayList<>();
        public double bankTotalDeposits = 0.0;
        public double bankTotalLoans = 0.0;

        // Market and Trade
        public double foodPrice = 2.0;
        public double annualTradeVolume = 0.0;
        public double totalTradeVolume = 0.0;
        public long lastMonsterTideMonth = -100L;
        public int peakPopulation = 0;

        // Yearly and Disaster Accumulators
        public int birthsThisYear = 0;
        public int deathsThisYear = 0;
        public int marriagesThisYear = 0;
        public int inMigrationsThisYear = 0;
        public int outMigrationsThisYear = 0;
        public int consecutiveFamineMonths = 0;
        public int famineDeathsThisYear = 0;
        public int freezingDeathsThisYear = 0;
        public String lastDisasterThisYear;
        public String lastRaidEventSummary;
    }

    public static class CraftingFacilitySaveData {
        public String facilityId;
        public String settlementId;
        public String ownerOrganizationId;
        public String grade;
        public int concurrentCapacity;
        public int activeJobsCount;
    }

    public static class PersonSaveData {
        public int id;
        public String name;
        public String gender;
        public int age;
        public int birthYear;
        public Integer deathYear;
        public Integer ageAtDeath;
        public String causeOfDeath;
        public boolean alive;
        public Integer villageId;
        public Integer familyId;
        public String occupation;
        public double wealth;
        public double bankBalance;
        public double loanDebt;

        // Supernatural profile
        public boolean isAwakened;
        public String supernaturalClass;
        public String aspect;
        public String rarity;
        public int tier;
        public double mastery;
        public double luck;
        public double greed;
        public double darkResonance;
        public double derivedPower;

        // Kinship
        public Integer fatherId;
        public Integer motherId;
        public Integer spouseId;
        public List<Integer> childrenIds = new ArrayList<>();
        public Map<String, String> equippedItems = new HashMap<>();

        // Crafter profile
        public CrafterProfileSaveData crafterProfile;
    }

    public static class CrafterProfileSaveData {
        public Map<String, Double> proficiencies = new HashMap<>();
        public int totalOrdersCompleted;
        public int totalCrafts;
        public int totalEnchants;
        public int totalRepairs;
        public int totalUpgrades;
    }

    public static class FamilySaveData {
        public int id;
        public String surname;
        public int foundedYear;
        public Integer founderPersonId;
        public Integer originVillageId;
    }

    public static class CoupleSaveData {
        public int id;
        public int partner1Id;
        public int partner2Id;
        public int marriageYear;
        public int plannedChildren;
        public int childrenBorn;
        public Long lastBirthTotalMonths;
        public long nextScheduledBirthMonth;
        public boolean active = true;
    }

    public static class OrgSaveData {
        public String id;
        public String name;
        public String type;
        public String alignment;
        public String status;
        public int foundingYear;
        public int foundingMonth;
        public String founderId;
        public String leaderId;
        public double reputation;
        public List<String> memberPersonIds = new ArrayList<>();
        public List<String> heirloomNames = new ArrayList<>();
        public int battlesWon;
        public int battlesTotal;
        public List<String> vaultEquipmentIds = new ArrayList<>();
    }

    public static class AnomalySaveData {
        public String id;
        public double x;
        public double y;
        public String state;
        public String category;
        public String grade;
        public double initialQty;
        public double remainingQty;
        public boolean observable;
        public String ownerId;
        public int revealedYear;
        public int discoveredYear;
    }

    public static class LairSaveData {
        public String id;
        public String sourceAnomalyId;
        public double x;
        public double y;
        public String biome;
        public String threatRank;
        public String state;
        public int capacity;
        public double territoryRadius;
        public String primarySpecies;
        public String secondarySpecies;
        public int emergenceYear;
    }

    public static class AnnualReportSaveData {
        public int year;
        public int totalLivingPopulation;
        public int livingMalePopulation;
        public int livingFemalePopulation;
        public Map<String, Integer> villageLivingPopulations = new HashMap<>();
        public int birthsDuringYear;
        public int deathsDuringYear;
        public int marriagesDuringYear;
        public int migrationsDuringYear;
        public int settlementUpgradesDuringYear;
        public int activeFamilies;
        public int activeClansCount;
        public int battlesCount;
        public int tidesCount;
        public int anomaliesCount;
        public String combatSummary;
        public String awakeningSummary;
        public String organizationSummary;
        public String monsterEcologySummary;
        public String equipmentSummary;
        public String bloodFeudSummary;
    }

    public static class EquipmentItemSaveData {
        public String equipmentId;
        public String displayName;
        public String artifactTitle;
        public String category;
        public String slot;
        public String type;
        public boolean twoHanded;
        public String rarity;
        public int tier;
        public double craftQuality;
        public double durability;
        public double maximumDurability;
        public String condition;
        public String lifecycleStatus;
        public int creationTotalMonths;
        public String creatorPersonId;
        public String creationFacilityId;
        public String currentOwnerId;
        public String currentCustodianId;
        public double posX;
        public double posY;
        public boolean registeredHeirloom;
        public String heirloomId;
        public EquipmentStatProfileSaveData baseStats;
        public List<EnchantmentRecordSaveData> enchantments = new ArrayList<>();
        public Map<String, Double> materialComposition = new HashMap<>();
    }

    public static class EquipmentStatProfileSaveData {
        public double attackBonus;
        public double defenseBonus;
        public double accuracyBonus;
        public double evasionBonus;
        public double aetherPotencyBonus;
        public double aetherCostEfficiency;
        public double barrierOrResistanceBonus;
    }

    public static class EnchantmentRecordSaveData {
        public String enchantmentId;
        public String displayName;
        public String family;
        public double primaryBonus;
        public double secondaryBonus;
        public int potencyTier;
        public String catalystType;
    }

    public static class CraftingWorkOrderSaveData {
        public String workOrderId;
        public String orderType;
        public String facilityId;
        public String settlementId;
        public String organizationId;
        public String crafterPersonId;
        public String recipeId;
        public String targetEquipmentId;
        public EnchantmentRecordSaveData pendingEnchantment;
        public int targetUpgradeTier;
        public String status;
        public int startYear;
        public int startMonth;
        public int totalMonthsRequired;
        public int monthsProgressed;
        public double reservedIron;
        public double reservedOre;
        public double reservedWood;
        public double reservedTools;
        public Map<String, Double> reservedSupernatural = new HashMap<>();
        public Map<String, Double> reservedMonster = new HashMap<>();
        public String reservedCoreGrade;
    }

    public static class CraftingSystemSaveData {
        public int nextEquipmentSeq = 1;
        public int nextWorkOrderSeq = 1;
        public int nextEnchantmentSeq = 1;
        public int ordersStarted;
        public int ordersCompletedCount;
        public int ordersFailedCount;
        public int ordersCancelledCount;
        public int enchantmentsAppliedCount;
        public int upgradesCompletedCount;
        public int repairsCompletedCount;
        public double totalCoresGenerated;
        public double totalCoresDelivered;
        public double totalCoresReserved;
        public double totalCoresConsumed;
        public double totalCoresLost;
        public double totalAnomalyGenerated;
        public double totalAnomalyDelivered;
        public double totalAnomalyReserved;
        public double totalAnomalyConsumed;
        public double totalAnomalyLost;
        public List<EquipmentItemSaveData> allEquipment = new ArrayList<>();
        public List<CraftingWorkOrderSaveData> activeOrders = new ArrayList<>();
        public List<CraftingWorkOrderSaveData> completedOrders = new ArrayList<>();
    }

    public static class HeirloomSaveData {
        public String heirloomId;
        public String equipmentId;
        public String owningClanId;
        public String artifactTitle;
        public int registrationYear;
        public int registrationMonth;
        public String successionPolicy;
        public String currentBearerPersonId;
        public String custodyStatus;
    }

    public static class BloodFeudSaveData {
        public String feudId;
        public String instigatorOrgId;
        public String targetOrgId;
        public double hatredLevel;
        public String state;
        public String feudType;
        public int activeEscalationTier;
    }

    public WorldSaveSummaryDto toSummary(String saveId) {
        int living = (int) people.stream().filter(p -> p.alive).count();
        return new WorldSaveSummaryDto(
                saveId,
                worldId != null ? worldId : "world-001",
                year,
                month,
                totalMonths,
                createdAt,
                schemaVersion,
                living,
                lifecycleState != null ? lifecycleState : "PAUSED"
        );
    }

    public WorldSaveDetailDto toDetail(String saveId, long fileSizeBytes, boolean isRestorable, String calculatedChecksum) {
        int living = (int) people.stream().filter(p -> p.alive).count();
        return new WorldSaveDetailDto(
                saveId,
                worldId != null ? worldId : "world-001",
                worldName != null ? worldName : "World",
                year,
                month,
                totalMonths,
                createdAt,
                schemaVersion,
                living,
                villages.size(),
                lairs.size(),
                lifecycleState != null ? lifecycleState : "PAUSED",
                isRestorable,
                parentWorldId,
                parentCheckpointId,
                branchRngPolicy != null ? branchRngPolicy : "CONTINUE_CHECKPOINT_RNG",
                randomSeed,
                calculatedChecksum != null ? calculatedChecksum : (this.checksum != null ? this.checksum : "NONE"),
                fileSizeBytes
        );
    }
}
