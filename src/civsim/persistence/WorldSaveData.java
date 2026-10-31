package civsim.persistence;

import civsim.dto.WorldSaveSummaryDto;

import java.util.*;

/**
 * Portable, structured serializable representation of a saved world state.
 */
public class WorldSaveData {
    public int schemaVersion = 1;
    public String worldId;
    public long createdAt;
    public int year;
    public int month;
    public long totalMonths;
    public long randomSeed;
    public long randomInvocations;
    public String lifecycleState;

    public int nextPersonId;
    public int nextFamilyId;
    public int nextCoupleId;
    public int nextVillageId;
    public int totalRecordedPeople;

    public List<VillageSaveData> villages = new ArrayList<>();
    public List<PersonSaveData> people = new ArrayList<>();
    public List<FamilySaveData> families = new ArrayList<>();
    public List<CoupleSaveData> couples = new ArrayList<>();
    public List<OrgSaveData> organizations = new ArrayList<>();
    public List<AnomalySaveData> anomalies = new ArrayList<>();
    public List<LairSaveData> lairs = new ArrayList<>();

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
        public Integer leaderId;
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
    }

    public WorldSaveSummaryDto toSummary(String saveId) {
        int living = (int) people.stream().filter(p -> p.alive).count();
        return new WorldSaveSummaryDto(
                saveId,
                worldId,
                year,
                month,
                totalMonths,
                createdAt,
                schemaVersion,
                living,
                lifecycleState
        );
    }
}
