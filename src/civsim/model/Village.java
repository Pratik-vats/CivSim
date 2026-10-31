package civsim.model;

import civsim.Season;
import civsim.economy.Bank;
import civsim.economy.Market;
import civsim.spatial.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Core domain entity representing a human settlement.
 * Delegates resource storage to VillageStockpile and housing capacity to VillageHousing,
 * while managing demographic residents, civic governance, and inter-settlement relations.
 */
public class Village {
    private final int id;
    private final String name;
    private final List<Person> residents;
    private final List<Person> historicalResidents;
    private final Market market;
    private final Bank bank;
    private final Map<Integer, DiplomaticStatus> relations;
    private final List<VillageAnnualStats> annualHistory;

    private SettlementTier tier;
    private int infrastructureSlots;
    private final VillageHousing housing;
    private final VillageStockpile stockpile;
    private final SupernaturalResourceInventory supernaturalInventory;
    private double treasury;
    private int techLevel;
    private Person leader;
    private DisasterType lastDisasterThisYear;
    private String lastRaidEventSummary;
    private int peakPopulation;

    // 2D Spatial Geography & Biome
    private double x;
    private double y;
    private Biome biome;

    // Annual tracking counters
    private int birthsThisYear;
    private int deathsThisYear;
    private int marriagesThisYear;
    private int inMigrationsThisYear;
    private int outMigrationsThisYear;
    private int consecutiveFamineMonths;
    private int famineDeathsThisYear;
    private int freezingDeathsThisYear;

    // Phase 2B.5 Monster & Invasion Tracking
    private long lastMonsterTideMonth = -100L;
    private double fortificationLevel = 1.0;
    private final Map<MonsterMaterialType, Double> monsterMaterials = new EnumMap<>(MonsterMaterialType.class);
    private int monsterCoreCount = 0;

    // Phase 2C.1 Equipment Storage & Crafting Facilities
    private final Map<String, EquipmentItem> storedEquipment = new HashMap<>();
    private final Map<String, CraftingFacility> craftingFacilities = new HashMap<>();

    public Village(int id, String name, double initialFoodPrice, double bankInterestRate) {
        this(id, name, initialFoodPrice, bankInterestRate, 50.0, 50.0, Biome.RIVER_VALLEY);
    }

    public Village(int id, String name, double initialFoodPrice, double bankInterestRate, double x, double y, Biome biome) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.biome = (biome != null) ? biome : Biome.RIVER_VALLEY;
        this.residents = new ArrayList<>();
        this.historicalResidents = new ArrayList<>();
        this.market = new Market(initialFoodPrice);
        this.bank = new Bank(bankInterestRate);
        this.relations = new HashMap<>();
        this.annualHistory = new ArrayList<>();

        this.tier = SettlementTier.HAMLET;
        this.infrastructureSlots = this.tier.getBaseInfrastructureSlots();
        this.housing = new VillageHousing(3);
        this.stockpile = new VillageStockpile(this.biome);
        this.supernaturalInventory = new SupernaturalResourceInventory();

        this.treasury = 40.0;
        this.techLevel = 0;
        this.leader = null;
        this.lastDisasterThisYear = DisasterType.NONE;
        this.lastRaidEventSummary = "Peaceful";
        this.peakPopulation = 0;

        this.birthsThisYear = 0;
        this.deathsThisYear = 0;
        this.marriagesThisYear = 0;
        this.inMigrationsThisYear = 0;
        this.outMigrationsThisYear = 0;
        this.consecutiveFamineMonths = 0;
        this.famineDeathsThisYear = 0;
        this.freezingDeathsThisYear = 0;
    }

    // --- Spatial Geometry ---
    public double getX() { return x; }
    public double getY() { return y; }
    public Biome getBiome() { return biome; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public void setBiome(Biome biome) { this.biome = biome; }

    public double distanceTo(Village other) {
        if (other == null) return 0.0;
        return distanceTo(other.x, other.y);
    }

    public double distanceTo(double ox, double oy) {
        double dx = this.x - ox;
        double dy = this.y - oy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public int getArableLandSlots() {
        int baseSlots = this.tier.getBaseArableLandSlots();
        return Math.max(5, (int) Math.round(baseSlots * this.biome.getArableLandMultiplier()));
    }

    public double getForestReserves() {
        double baseReserves = this.tier.getBaseForestReserves();
        return Math.max(100.0, baseReserves * this.biome.getForestReserveMultiplier());
    }

    // --- Stockpile Delegation ---
    public VillageStockpile getStockpile() { return stockpile; }
    public double getFoodStockpile() { return stockpile.getFoodStockpile(); }
    public void addFood(double amount) { stockpile.addFood(amount); }
    public void deductFood(double amount) { stockpile.deductFood(amount); }

    public double getWoodStockpile() { return stockpile.getWoodStockpile(); }
    public void addWood(double amount) { stockpile.addWood(amount); }
    public void deductWood(double amount) { stockpile.deductWood(amount); }

    public double getIronStoneStockpile() { return stockpile.getIronStoneStockpile(); }
    public void addIronStone(double amount) { stockpile.addIronStone(amount); }
    public void deductIronStone(double amount) { stockpile.deductIronStone(amount); }
    public double getOreStockpile() { return getIronStoneStockpile(); }
    public void addOre(double amount) { addIronStone(amount); }
    public void deductOre(double amount) { deductIronStone(amount); }

    public double getToolsStockpile() { return stockpile.getToolsStockpile(); }
    public void addTools(double amount) { stockpile.addTools(amount); }
    public void deductTools(double amount) { stockpile.deductTools(amount); }

    public boolean hasWeaponsForMilitary() {
        return stockpile.hasToolsForSoldiers(getOccupationCount(Occupation.SOLDIER));
    }

    public boolean hasToolsForFarming() {
        return stockpile.hasToolsForFarmers(getOccupationCount(Occupation.FARMER));
    }

    // --- Housing Delegation ---
    public VillageHousing getHousing() { return housing; }
    public int getHousingCount() { return housing.getHousingCount(); }
    public int getTotalHousingCapacity(int capacityPerBuilding) {
        return housing.getTotalCapacity(capacityPerBuilding);
    }

    // --- Treasury & Civic ---
    public double getTreasury() { return treasury; }
    public void addTreasury(double amount) {
        if (amount > 0) this.treasury += amount;
    }
    public void deductTreasury(double amount) {
        if (amount > 0) this.treasury = Math.max(0.0, this.treasury - amount);
    }

    public int getTechLevel() { return techLevel; }
    public void incrementTechLevel() { this.techLevel++; }

    // --- Diplomacy ---
    public DiplomaticStatus getRelationWith(int targetVillageId) {
        return relations.getOrDefault(targetVillageId, DiplomaticStatus.NEUTRAL);
    }

    public void setRelationWith(int targetVillageId, DiplomaticStatus status) {
        relations.put(targetVillageId, status);
    }

    public void removeRelationWith(int targetVillageId) {
        relations.remove(targetVillageId);
    }

    public String buildDiplomacyAndRaidStatus(List<Village> allVillages) {
        StringBuilder sb = new StringBuilder();
        int allies = 0;
        int wars = 0;
        for (Village other : allVillages) {
            if (other.getId() == this.id || other.getLivingPopulationCount() == 0) continue;
            DiplomaticStatus st = getRelationWith(other.getId());
            if (st == DiplomaticStatus.ALLIED) {
                if (allies > 0) sb.append(" ");
                sb.append("Ally:").append(other.getName().replace("Settlement ", ""));
                allies++;
            } else if (st == DiplomaticStatus.AT_WAR) {
                if (sb.length() > 0) sb.append(" ");
                sb.append("WAR:").append(other.getName().replace("Settlement ", ""));
                wars++;
            }
        }
        if (sb.length() == 0) {
            return "Peaceful";
        }
        return sb.toString();
    }

    // --- Demographic Residents ---
    public void addResident(Person person) {
        if (person != null && !residents.contains(person)) {
            residents.add(person);
            if (!historicalResidents.contains(person)) {
                historicalResidents.add(person);
            }
            int living = getLivingPopulationCount();
            if (living > peakPopulation) {
                peakPopulation = living;
            }
        }
    }

    public void migrateOut(Person person) {
        if (person != null) {
            residents.remove(person);
            outMigrationsThisYear++;
            if (leader == person) {
                leader = null;
                electLeaderIfNeeded();
            }
        }
    }

    public void migrateIn(Person person) {
        if (person != null) {
            addResident(person);
            inMigrationsThisYear++;
        }
    }

    public void pruneDeceasedResidents() {
        this.residents.removeIf(p -> !p.isAlive());
        this.historicalResidents.removeIf(p -> !p.isAlive());
    }

    public int getLivingPopulationCount() {
        int count = 0;
        for (Person p : residents) {
            if (p.isAlive()) count++;
        }
        return count;
    }

    public int getLivingMaleCount() {
        int count = 0;
        for (Person p : residents) {
            if (p.isAlive() && p.getGender() == Gender.MALE) count++;
        }
        return count;
    }

    public int getLivingFemaleCount() {
        int count = 0;
        for (Person p : residents) {
            if (p.isAlive() && p.getGender() == Gender.FEMALE) count++;
        }
        return count;
    }

    public int getOccupationCount(Occupation occ) {
        int count = 0;
        for (Person p : residents) {
            if (p.isAlive() && p.getOccupation() == occ) count++;
        }
        return count;
    }

    public int getEligibleUnmarriedCount(int minMarriageAgeExclusive) {
        int count = 0;
        for (Person p : residents) {
            if (p.isEligibleForMarriage(minMarriageAgeExclusive)) count++;
        }
        return count;
    }

    public List<Person> getEligibleUnmarriedAdults(int minMarriageAgeExclusive) {
        List<Person> list = new ArrayList<>();
        for (Person p : residents) {
            if (p.isEligibleForMarriage(minMarriageAgeExclusive)) list.add(p);
        }
        return list;
    }

    public List<Person> getEligibleUnmarriedMales(int minMarriageAgeExclusive) {
        List<Person> list = new ArrayList<>();
        for (Person p : residents) {
            if (p.isEligibleForMarriage(minMarriageAgeExclusive) && p.getGender() == Gender.MALE) list.add(p);
        }
        return list;
    }

    public List<Person> getEligibleUnmarriedFemales(int minMarriageAgeExclusive) {
        List<Person> list = new ArrayList<>();
        for (Person p : residents) {
            if (p.isEligibleForMarriage(minMarriageAgeExclusive) && p.getGender() == Gender.FEMALE) list.add(p);
        }
        return list;
    }

    public List<Person> getEligiblePioneers(int minAge, int maxAge) {
        List<Person> list = new ArrayList<>();
        for (Person p : residents) {
            if (p.isAlive() && !p.isMarried() && p.getAge() >= minAge && p.getAge() <= maxAge) {
                list.add(p);
            }
        }
        return list;
    }

    // --- Leader & Governance ---
    public Person getLeader() { return leader; }

    public boolean electLeaderIfNeeded() {
        if (leader != null && leader.isAlive() && leader.getVillage() == this) {
            return false;
        }
        Person bestCandidate = null;
        double bestScore = -1.0;
        for (Person p : residents) {
            if (!p.isAlive() || p.getAge() < 19) continue;
            double score = p.getWealth() + p.getBankBalance() + (p.getAge() * 2.0);
            if (p.getAge() >= 30 && p.getAge() <= 60) score += 20.0;
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = p;
            }
        }
        if (bestCandidate != null) {
            this.leader = bestCandidate;
            return true;
        }
        this.leader = null;
        return false;
    }

    public double[] plunderTreasuryAndFood(double plunderFraction) {
        double stolenTreasury = this.treasury * plunderFraction;
        double stolenFood = this.stockpile.getFoodStockpile() * plunderFraction;
        this.treasury -= stolenTreasury;
        this.stockpile.deductFood(stolenFood);
        return new double[]{stolenTreasury, stolenFood};
    }

    public void receivePlunder(double gold, double food) {
        this.treasury += gold;
        this.stockpile.addFood(food);
        int soldiers = getOccupationCount(Occupation.SOLDIER);
        if (soldiers > 0 && gold > 0) {
            double bonusPool = gold * 0.25;
            this.treasury -= bonusPool;
            double perSoldier = bonusPool / soldiers;
            for (Person p : residents) {
                if (p.isAlive() && p.getOccupation() == Occupation.SOLDIER) {
                    p.addWealth(perSoldier);
                }
            }
        }
    }

    public double payReparations(double fraction) {
        double indemnity = this.treasury * fraction;
        this.treasury -= indemnity;
        return indemnity;
    }

    public void applyDrought() {
        double mitigation = Math.min(0.20, this.techLevel * 0.04);
        double lossFraction = 0.40 - mitigation;
        this.stockpile.deductFood(this.stockpile.getFoodStockpile() * lossFraction);
        this.lastDisasterThisYear = DisasterType.DROUGHT;
    }

    public void applyEarthquake() {
        this.housing.destroyHousing(1);
        double relief = Math.min(30.0, this.treasury * 0.5);
        this.treasury -= relief;
        this.lastDisasterThisYear = DisasterType.EARTHQUAKE;
    }

    public void markPlagueOccurred() {
        this.lastDisasterThisYear = DisasterType.PLAGUE;
    }

    // --- Settlement Progression Ratchet ---
    public boolean evaluateSettlementProgression(int villageThreshold, int townThreshold, int cityThreshold, int megacityThreshold) {
        int living = getLivingPopulationCount();
        SettlementTier targetTier = SettlementTier.forPopulation(living, villageThreshold, townThreshold, cityThreshold, megacityThreshold);

        if (targetTier.ordinal() > this.tier.ordinal()) {
            double stoneNeeded = targetTier.getStoneRequiredForUpgrade();
            if (this.stockpile.getIronStoneStockpile() >= stoneNeeded) {
                this.stockpile.deductIronStone(stoneNeeded);
            }
            this.tier = targetTier;
            this.infrastructureSlots = targetTier.getBaseInfrastructureSlots();
            return true;
        }
        return false;
    }

    public boolean evaluateSettlementProgression(int villageThreshold, int townThreshold, int cityThreshold) {
        return evaluateSettlementProgression(villageThreshold, townThreshold, cityThreshold, cityThreshold * 3);
    }

    // --- Economy, Trade & Tariffs ---
    public double getTotalLiquidWealth() {
        double sum = 0.0;
        for (Person p : residents) {
            if (p.isAlive()) sum += p.getWealth();
        }
        return sum;
    }

    public double getTotalCitizenWealth() {
        double sum = 0.0;
        for (Person p : residents) {
            if (p.isAlive()) sum += (p.getWealth() + p.getBankBalance() - p.getLoanDebt());
        }
        return sum;
    }

    public void collectWaypointTariff(double tariff) {
        addTreasury(tariff);
        this.market.addTradeVolume(tariff);
    }

    public double exportFood(double amount, double totalRevenue) {
        deductFood(amount);
        double tariff = totalRevenue * 0.10;
        addTreasury(tariff);
        this.market.addTradeVolume(totalRevenue);
        int farmers = getOccupationCount(Occupation.FARMER);
        int merchants = getOccupationCount(Occupation.MERCHANT);
        double farmerShare = totalRevenue * 0.70;
        double merchantShare = totalRevenue * 0.20;
        if (farmers > 0) {
            double each = farmerShare / farmers;
            for (Person p : residents) {
                if (p.isAlive() && p.getOccupation() == Occupation.FARMER) p.addWealth(each);
            }
        }
        if (merchants > 0) {
            double each = merchantShare / merchants;
            for (Person p : residents) {
                if (p.isAlive() && p.getOccupation() == Occupation.MERCHANT) p.addWealth(each);
            }
        }
        return totalRevenue;
    }

    public boolean importFood(double amount, double totalCost) {
        double subsidy = Math.min(this.treasury, totalCost * 0.40);
        deductTreasury(subsidy);
        double remaining = totalCost - subsidy;
        int count = getLivingPopulationCount();
        if (count > 0 && remaining > 0) {
            double perPerson = remaining / count;
            for (Person p : residents) {
                if (p.isAlive()) {
                    if (p.getWealth() >= perPerson) p.deductWealth(perPerson);
                    else {
                        double deficit = perPerson - p.getWealth();
                        p.deductWealth(p.getWealth());
                        if (bank.issueLoan(deficit)) p.setLoanDebt(p.getLoanDebt() + deficit);
                    }
                }
            }
        }
        addFood(amount);
        this.market.addTradeVolume(totalCost);
        return true;
    }

    public double exportOre(double amount, double totalRevenue) {
        deductIronStone(amount);
        double tariff = totalRevenue * 0.10;
        addTreasury(tariff);
        this.market.addTradeVolume(totalRevenue);
        int miners = getOccupationCount(Occupation.MINER);
        if (miners > 0) {
            double each = (totalRevenue * 0.85) / miners;
            for (Person p : residents) {
                if (p.isAlive() && p.getOccupation() == Occupation.MINER) p.addWealth(each);
            }
        }
        return totalRevenue;
    }

    public boolean importOre(double amount, double totalCost) {
        double subsidy = Math.min(this.treasury, totalCost * 0.50);
        deductTreasury(subsidy);
        addIronStone(amount);
        this.market.addTradeVolume(totalCost);
        return true;
    }

    public double exportTools(double amount, double totalRevenue) {
        deductTools(amount);
        double tariff = totalRevenue * 0.12;
        addTreasury(tariff);
        this.market.addTradeVolume(totalRevenue);
        int smiths = getOccupationCount(Occupation.BLACKSMITH);
        if (smiths > 0) {
            double each = (totalRevenue * 0.85) / smiths;
            for (Person p : residents) {
                if (p.isAlive() && p.getOccupation() == Occupation.BLACKSMITH) p.addWealth(each);
            }
        }
        return totalRevenue;
    }

    public boolean importTools(double amount, double totalCost) {
        double subsidy = Math.min(this.treasury, totalCost * 0.60);
        deductTreasury(subsidy);
        addTools(amount);
        this.market.addTradeVolume(totalCost);
        return true;
    }

    public double exportWood(double amount, double totalRevenue) {
        deductWood(amount);
        double tariff = totalRevenue * 0.10;
        addTreasury(tariff);
        this.market.addTradeVolume(totalRevenue);
        int loggers = getOccupationCount(Occupation.LUMBERJACK);
        if (loggers > 0) {
            double each = (totalRevenue * 0.85) / loggers;
            for (Person p : residents) {
                if (p.isAlive() && p.getOccupation() == Occupation.LUMBERJACK) p.addWealth(each);
            }
        }
        return totalRevenue;
    }

    public boolean importWood(double amount, double totalCost) {
        double subsidy = Math.min(this.treasury, totalCost * 0.40);
        deductTreasury(subsidy);
        addWood(amount);
        this.market.addTradeVolume(totalCost);
        return true;
    }

    // --- Famine and Frost Status ---
    public boolean isInFamine() { return consecutiveFamineMonths > 0; }
    public int getConsecutiveFamineMonths() { return consecutiveFamineMonths; }
    public void setConsecutiveFamineMonths(int months) { this.consecutiveFamineMonths = months; }
    public int getFamineDeathsThisYear() { return famineDeathsThisYear; }
    public void addFamineDeath() { this.famineDeathsThisYear++; }
    public int getFreezingDeathsThisYear() { return freezingDeathsThisYear; }
    public void addFreezingDeath() { this.freezingDeathsThisYear++; }

    public double computeAttractivenessScore(double foodPerPerson, int capacityPerBuilding) {
        double score = 50.0;
        int pop = getLivingPopulationCount();
        if (pop == 0) return 0.0;

        double monthsReserve = stockpile.getMonthsOfFoodReserve(pop);
        score += Math.min(30.0, monthsReserve * 3.5);

        int capacity = getTotalHousingCapacity(capacityPerBuilding);
        if (capacity > pop) {
            score += Math.min(20.0, (capacity - pop) * 3.0);
        } else {
            score -= (pop - capacity) * 8.0;
        }

        score += Math.min(15.0, this.treasury * 0.10);
        score += this.techLevel * 4.0;

        if (isInFamine()) {
            score -= 45.0;
        }
        return Math.max(0.0, score);
    }

    // --- Annual Snapshots ---
    public int getDeathsThisYear() { return this.deathsThisYear; }
    public void recordBirthThisYear() { this.birthsThisYear++; }
    public void recordDeathThisYear() { this.deathsThisYear++; }
    public void recordMarriageThisYear() { this.marriagesThisYear++; }
    public void setLastDisasterThisYear(DisasterType disaster) { this.lastDisasterThisYear = disaster; }
    public void setLastRaidEventSummary(String summary) { this.lastRaidEventSummary = summary; }

    public VillageAnnualStats snapshotAndResetYearlyCounters(
            int currentYear,
            int minMarriageAgeExclusive,
            int housingCapacityPerBuilding,
            List<Village> allVillages) {
        int livingPop = getLivingPopulationCount();
        int livingMales = getLivingMaleCount();
        int livingFemales = getLivingFemaleCount();
        int farmers = getOccupationCount(Occupation.FARMER);
        int lumberjacks = getOccupationCount(Occupation.LUMBERJACK);
        int miners = getOccupationCount(Occupation.MINER);
        int blacksmiths = getOccupationCount(Occupation.BLACKSMITH);
        int builders = getOccupationCount(Occupation.BUILDER);
        int merchants = getOccupationCount(Occupation.MERCHANT);
        int soldiers = getOccupationCount(Occupation.SOLDIER);
        int eligibleUnmarried = getEligibleUnmarriedCount(minMarriageAgeExclusive);
        int housingCap = getTotalHousingCapacity(housingCapacityPerBuilding);
        double totalWealth = getTotalLiquidWealth();
        String leaderName = (leader != null) ? leader.getFullName() : "None";
        String raidSummary = (allVillages != null) ? buildDiplomacyAndRaidStatus(allVillages) : lastRaidEventSummary;

        VillageAnnualStats stats = new VillageAnnualStats(
                currentYear,
                this.name,
                this.tier,
                this.biome,
                this.x,
                this.y,
                leaderName,
                this.treasury,
                this.techLevel,
                this.lastDisasterThisYear,
                raidSummary,
                this.infrastructureSlots,
                this.housing.getHousingCount(),
                housingCap,
                this.stockpile.getFoodStockpile(),
                this.stockpile.getWoodStockpile(),
                this.stockpile.getIronStoneStockpile(),
                this.stockpile.getToolsStockpile(),
                this.market.getFoodPrice(),
                totalWealth,
                this.bank.getTotalDeposits(),
                this.bank.getTotalLoansOutstanding(),
                this.market.getAnnualTradeVolume(),
                this.market.getTotalTradeVolume(),
                this.peakPopulation,
                livingPop,
                livingMales,
                livingFemales,
                farmers,
                lumberjacks,
                miners,
                blacksmiths,
                builders,
                merchants,
                soldiers,
                this.birthsThisYear,
                this.deathsThisYear,
                this.marriagesThisYear,
                this.inMigrationsThisYear,
                this.outMigrationsThisYear,
                eligibleUnmarried,
                this.historicalResidents.size(),
                this.famineDeathsThisYear,
                this.freezingDeathsThisYear
        );

        this.annualHistory.add(stats);

        this.market.resetAnnualTradeVolume();
        this.birthsThisYear = 0;
        this.deathsThisYear = 0;
        this.marriagesThisYear = 0;
        this.inMigrationsThisYear = 0;
        this.outMigrationsThisYear = 0;
        this.famineDeathsThisYear = 0;
        this.freezingDeathsThisYear = 0;
        this.lastDisasterThisYear = DisasterType.NONE;

        return stats;
    }

    public double getAnnualTradeVolume() { return market.getAnnualTradeVolume(); }
    public double getTotalTradeVolume() { return market.getTotalTradeVolume(); }

    // --- Standard Getters ---
    public int getId() { return id; }
    public String getName() { return name; }
    public SettlementTier getTier() { return tier; }
    public void setTier(SettlementTier tier) {
        this.tier = tier;
        if (tier != null) {
            this.infrastructureSlots = tier.getBaseInfrastructureSlots();
        }
    }
    public void setTreasury(double treasury) { this.treasury = Math.max(0.0, treasury); }
    public void setTechLevel(int techLevel) { this.techLevel = techLevel; }
    public void setLeader(Person leader) { this.leader = leader; }
    public DisasterType getLastDisasterThisYear() { return lastDisasterThisYear; }
    public int getInfrastructureSlots() { return infrastructureSlots; }
    public Bank getBank() { return bank; }
    public Market getMarket() { return market; }
    public int getPeakPopulation() { return peakPopulation; }
    public List<Person> getResidents() { return Collections.unmodifiableList(residents); }
    public List<Person> getHistoricalResidents() { return Collections.unmodifiableList(historicalResidents); }
    public List<VillageAnnualStats> getAnnualHistory() { return Collections.unmodifiableList(annualHistory); }
    public SupernaturalResourceInventory getSupernaturalInventory() { return supernaturalInventory; }

    // --- Phase 2B.5 Monster & Invasion Getters & Helpers ---
    public long getLastMonsterTideMonth() { return lastMonsterTideMonth; }
    public void setLastMonsterTideMonth(long month) { this.lastMonsterTideMonth = month; }

    public double getFortificationLevel() { return fortificationLevel; }
    public void setFortificationLevel(double level) { this.fortificationLevel = Math.max(0.1, level); }
    public void damageFortification(double percentDamage) {
        double factor = Math.max(0.0, 1.0 - (percentDamage / 100.0));
        this.fortificationLevel = Math.max(0.1, this.fortificationLevel * factor);
    }

    public int getSoldierCount() {
        return getOccupationCount(Occupation.SOLDIER);
    }

    public void addMonsterMaterial(MonsterMaterialType type, double quantity) {
        if (type != null && quantity > 0.0) {
            monsterMaterials.merge(type, quantity, Double::sum);
        }
    }

    public double getMonsterMaterial(MonsterMaterialType type) {
        return (type != null) ? monsterMaterials.getOrDefault(type, 0.0) : 0.0;
    }

    public Map<MonsterMaterialType, Double> getMonsterMaterials() {
        return Collections.unmodifiableMap(monsterMaterials);
    }

    public int getMonsterCoreCount() { return monsterCoreCount; }
    public void addMonsterCore(int count) { this.monsterCoreCount += Math.max(0, count); }
    public boolean deductMonsterCore(int count) {
        if (count <= 0) return true;
        if (this.monsterCoreCount >= count) {
            this.monsterCoreCount -= count;
            return true;
        }
        return false;
    }

    public double deductMonsterMaterial(MonsterMaterialType type, double quantity) {
        if (type == null || quantity <= 0.0) return 0.0;
        double current = monsterMaterials.getOrDefault(type, 0.0);
        double actualDeducted = Math.min(current, quantity);
        monsterMaterials.put(type, Math.max(0.0, current - actualDeducted));
        return actualDeducted;
    }

    // --- Phase 2C.1 Equipment Storage & Crafting Facilities ---
    public Map<String, EquipmentItem> getStoredEquipment() {
        return Collections.unmodifiableMap(storedEquipment);
    }

    public void addEquipment(EquipmentItem item) {
        if (item != null) {
            storedEquipment.put(item.getEquipmentId(), item);
            item.setLifecycleStatus(EquipmentLifecycleStatus.IN_INVENTORY);
            item.setCurrentCustodianId("VILLAGE_" + this.id);
            item.setPosition(this.x, this.y);
        }
    }

    public EquipmentItem removeEquipment(String equipmentId) {
        if (equipmentId == null) return null;
        return storedEquipment.remove(equipmentId);
    }

    public EquipmentItem getEquipment(String equipmentId) {
        if (equipmentId == null) return null;
        return storedEquipment.get(equipmentId);
    }

    public Map<String, CraftingFacility> getCraftingFacilities() {
        return Collections.unmodifiableMap(craftingFacilities);
    }

    public void addCraftingFacility(CraftingFacility facility) {
        if (facility != null) {
            craftingFacilities.put(facility.getFacilityId(), facility);
        }
    }

    public CraftingFacility getCraftingFacility(String facilityId) {
        if (facilityId == null) return null;
        return craftingFacilities.get(facilityId);
    }

    public CraftingFacility getBestCraftingFacility() {
        CraftingFacility best = null;
        for (CraftingFacility f : craftingFacilities.values()) {
            if (best == null || f.getGrade().ordinal() > best.getGrade().ordinal()) {
                best = f;
            }
        }
        return best;
    }
}
