package civsim.model;

import civsim.spatial.Biome;

/**
 * Immutable annual snapshot of a settlement's vital demographic, economic, and infrastructure metrics.
 */
public class VillageAnnualStats {
    private final int year;
    private final String villageName;
    private final SettlementTier tier;
    private final Biome biome;
    private final double x;
    private final double y;
    private final String leaderName;
    private final double treasury;
    private final int techLevel;
    private final DisasterType disaster;
    private final String raidSummary;
    private final int infrastructureSlots;
    private final int housingCount;
    private final int housingCapacity;
    private final double foodStockpile;
    private final double woodStockpile;
    private final double ironStoneStockpile;
    private final double toolsStockpile;
    private final double foodPrice;
    private final double totalWealth;
    private final double bankDeposits;
    private final double bankLoans;
    private final double tradeVolume;
    private final double cumulativeTradeVolume;
    private final int peakPopulation;
    private final int livingPopulation;
    private final int livingMales;
    private final int livingFemales;
    private final int farmers;
    private final int lumberjacks;
    private final int miners;
    private final int blacksmiths;
    private final int builders;
    private final int merchants;
    private final int soldiers;
    private final int births;
    private final int deaths;
    private final int marriages;
    private final int inMigrations;
    private final int outMigrations;
    private final int eligibleUnmarried;
    private final int totalRecordedResidents;
    private final int famineDeaths;
    private final int freezingDeaths;

    public VillageAnnualStats(
            int year,
            String villageName,
            SettlementTier tier,
            Biome biome,
            double x,
            double y,
            String leaderName,
            double treasury,
            int techLevel,
            DisasterType disaster,
            String raidSummary,
            int infrastructureSlots,
            int housingCount,
            int housingCapacity,
            double foodStockpile,
            double woodStockpile,
            double ironStoneStockpile,
            double toolsStockpile,
            double foodPrice,
            double totalWealth,
            double bankDeposits,
            double bankLoans,
            double tradeVolume,
            int peakPopulation,
            int livingPopulation,
            int livingMales,
            int livingFemales,
            int farmers,
            int lumberjacks,
            int miners,
            int blacksmiths,
            int builders,
            int merchants,
            int soldiers,
            int births,
            int deaths,
            int marriages,
            int inMigrations,
            int outMigrations,
            int eligibleUnmarried,
            int totalRecordedResidents,
            int famineDeaths,
            int freezingDeaths) {
        this(year, villageName, tier, biome, x, y, leaderName, treasury, techLevel, disaster, raidSummary,
                infrastructureSlots, housingCount, housingCapacity, foodStockpile, woodStockpile, ironStoneStockpile,
                toolsStockpile, foodPrice, totalWealth, bankDeposits, bankLoans, tradeVolume, tradeVolume,
                peakPopulation, livingPopulation, livingMales, livingFemales, farmers, lumberjacks, miners,
                blacksmiths, builders, merchants, soldiers, births, deaths, marriages, inMigrations, outMigrations,
                eligibleUnmarried, totalRecordedResidents, famineDeaths, freezingDeaths);
    }

    public VillageAnnualStats(
            int year,
            String villageName,
            SettlementTier tier,
            Biome biome,
            double x,
            double y,
            String leaderName,
            double treasury,
            int techLevel,
            DisasterType disaster,
            String raidSummary,
            int infrastructureSlots,
            int housingCount,
            int housingCapacity,
            double foodStockpile,
            double woodStockpile,
            double ironStoneStockpile,
            double toolsStockpile,
            double foodPrice,
            double totalWealth,
            double bankDeposits,
            double bankLoans,
            double tradeVolume,
            double cumulativeTradeVolume,
            int peakPopulation,
            int livingPopulation,
            int livingMales,
            int livingFemales,
            int farmers,
            int lumberjacks,
            int miners,
            int blacksmiths,
            int builders,
            int merchants,
            int soldiers,
            int births,
            int deaths,
            int marriages,
            int inMigrations,
            int outMigrations,
            int eligibleUnmarried,
            int totalRecordedResidents,
            int famineDeaths,
            int freezingDeaths) {
        this.year = year;
        this.villageName = villageName;
        this.tier = tier;
        this.biome = biome;
        this.x = x;
        this.y = y;
        this.leaderName = leaderName;
        this.treasury = treasury;
        this.techLevel = techLevel;
        this.disaster = disaster;
        this.raidSummary = raidSummary;
        this.infrastructureSlots = infrastructureSlots;
        this.housingCount = housingCount;
        this.housingCapacity = housingCapacity;
        this.foodStockpile = foodStockpile;
        this.woodStockpile = woodStockpile;
        this.ironStoneStockpile = ironStoneStockpile;
        this.toolsStockpile = toolsStockpile;
        this.foodPrice = foodPrice;
        this.totalWealth = totalWealth;
        this.bankDeposits = bankDeposits;
        this.bankLoans = bankLoans;
        this.tradeVolume = tradeVolume;
        this.cumulativeTradeVolume = cumulativeTradeVolume;
        this.peakPopulation = peakPopulation;
        this.livingPopulation = livingPopulation;
        this.livingMales = livingMales;
        this.livingFemales = livingFemales;
        this.farmers = farmers;
        this.lumberjacks = lumberjacks;
        this.miners = miners;
        this.blacksmiths = blacksmiths;
        this.builders = builders;
        this.merchants = merchants;
        this.soldiers = soldiers;
        this.births = births;
        this.deaths = deaths;
        this.marriages = marriages;
        this.inMigrations = inMigrations;
        this.outMigrations = outMigrations;
        this.eligibleUnmarried = eligibleUnmarried;
        this.totalRecordedResidents = totalRecordedResidents;
        this.famineDeaths = famineDeaths;
        this.freezingDeaths = freezingDeaths;
    }

    public int getYear() { return year; }
    public String getVillageName() { return villageName; }
    public SettlementTier getTier() { return tier; }
    public Biome getBiome() { return biome; }
    public double getX() { return x; }
    public double getY() { return y; }
    public String getLeaderName() { return leaderName; }
    public double getTreasury() { return treasury; }
    public int getTechLevel() { return techLevel; }
    public DisasterType getDisaster() { return disaster; }
    public String getRaidSummary() { return raidSummary; }
    public int getInfrastructureSlots() { return infrastructureSlots; }
    public int getHousingCount() { return housingCount; }
    public int getHousingCapacity() { return housingCapacity; }
    public double getFoodStockpile() { return foodStockpile; }
    public double getWoodStockpile() { return woodStockpile; }
    public double getIronStoneStockpile() { return ironStoneStockpile; }
    public double getToolsStockpile() { return toolsStockpile; }
    public double getFoodPrice() { return foodPrice; }
    public double getTotalWealth() { return totalWealth; }
    public double getBankDeposits() { return bankDeposits; }
    public double getBankLoans() { return bankLoans; }
    public double getTradeVolume() { return tradeVolume; }
    public double getCumulativeTradeVolume() { return cumulativeTradeVolume; }
    public int getPeakPopulation() { return peakPopulation; }
    public int getLivingPopulation() { return livingPopulation; }
    public int getLivingMales() { return livingMales; }
    public int getLivingFemales() { return livingFemales; }
    public int getFarmers() { return farmers; }
    public int getLumberjacks() { return lumberjacks; }
    public int getMiners() { return miners; }
    public int getBlacksmiths() { return blacksmiths; }
    public int getBuilders() { return builders; }
    public int getMerchants() { return merchants; }
    public int getSoldiers() { return soldiers; }
    public int getBirths() { return births; }
    public int getDeaths() { return deaths; }
    public int getMarriages() { return marriages; }
    public int getInMigrations() { return inMigrations; }
    public int getOutMigrations() { return outMigrations; }
    public int getEligibleUnmarried() { return eligibleUnmarried; }
    public int getTotalRecordedResidents() { return totalRecordedResidents; }
    public int getFamineDeaths() { return famineDeaths; }
    public int getFreezingDeaths() { return freezingDeaths; }
}
