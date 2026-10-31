package civsim;

import civsim.economy.Currency;
import civsim.model.VillageAnnualStats;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnnualReport {
    private final int year;
    private final int totalLivingPopulation;
    private final int livingMalePopulation;
    private final int livingFemalePopulation;
    private final Map<String, Integer> villageLivingPopulations;
    private final int birthsDuringYear;
    private final int deathsDuringYear;
    private final int marriagesDuringYear;
    private final int crossVillageMarriagesDuringYear;
    private final int migrationsDuringYear;
    private final int settlementUpgradesDuringYear;
    private final int effectiveMaxLifespan;
    private final int eligibleUnmarriedPopulation;
    private final int activeCouples;
    private final int fertileCouples;
    private final int totalRecordedPeople;
    private final int activeFamilies;
    private final int extinctFamilies;
    private final String largestFamilySummary;
    private final List<VillageAnnualStats> villageStats;
    private final String awakeningSummary;
    private final String progressionSummary;
    private final String longevitySummary;
    private final String breakthroughSummary;
    private final String combatSummary;
    private final String organizationSummary;
    private final String anomalySkirmishSummary;
    private final String anomalyExplorationSummary;
    private final String monsterEcologySummary;
    private final String equipmentSummary;
    private final String bloodFeudSummary;

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary) {
        this(
                year,
                totalLivingPopulation,
                livingMalePopulation,
                livingFemalePopulation,
                villageLivingPopulations,
                birthsDuringYear,
                deathsDuringYear,
                marriagesDuringYear,
                crossVillageMarriagesDuringYear,
                migrationsDuringYear,
                settlementUpgradesDuringYear,
                effectiveMaxLifespan,
                eligibleUnmarriedPopulation,
                activeCouples,
                fertileCouples,
                totalRecordedPeople,
                activeFamilies,
                extinctFamilies,
                largestFamilySummary,
                villageStats,
                awakeningSummary,
                progressionSummary,
                null,
                null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary) {
        this(
                year,
                totalLivingPopulation,
                livingMalePopulation,
                livingFemalePopulation,
                villageLivingPopulations,
                birthsDuringYear,
                deathsDuringYear,
                marriagesDuringYear,
                crossVillageMarriagesDuringYear,
                migrationsDuringYear,
                settlementUpgradesDuringYear,
                effectiveMaxLifespan,
                eligibleUnmarriedPopulation,
                activeCouples,
                fertileCouples,
                totalRecordedPeople,
                activeFamilies,
                extinctFamilies,
                largestFamilySummary,
                villageStats,
                awakeningSummary,
                progressionSummary,
                longevitySummary,
                null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary) {
        this(
                year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary) {
        this(
                year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, combatSummary, null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary,
            String organizationSummary) {
        this(
                year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, combatSummary, organizationSummary, null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary,
            String organizationSummary,
            String anomalySkirmishSummary) {
        this(
                year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, combatSummary, organizationSummary, anomalySkirmishSummary, null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary,
            String organizationSummary,
            String anomalySkirmishSummary,
            String anomalyExplorationSummary) {
        this(
                year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, combatSummary, organizationSummary, anomalySkirmishSummary,
                anomalyExplorationSummary, null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary,
            String organizationSummary,
            String anomalySkirmishSummary,
            String anomalyExplorationSummary,
            String monsterEcologySummary) {
        this(year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, combatSummary, organizationSummary, anomalySkirmishSummary,
                anomalyExplorationSummary, monsterEcologySummary, null);
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary,
            String organizationSummary,
            String anomalySkirmishSummary,
            String anomalyExplorationSummary,
            String monsterEcologySummary,
            String equipmentSummary) {
        this(
                year, totalLivingPopulation, livingMalePopulation, livingFemalePopulation,
                villageLivingPopulations, birthsDuringYear, deathsDuringYear, marriagesDuringYear,
                crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear,
                effectiveMaxLifespan, eligibleUnmarriedPopulation, activeCouples, fertileCouples,
                totalRecordedPeople, activeFamilies, extinctFamilies, largestFamilySummary,
                villageStats, awakeningSummary, progressionSummary, longevitySummary,
                breakthroughSummary, combatSummary, organizationSummary, anomalySkirmishSummary,
                anomalyExplorationSummary, monsterEcologySummary, equipmentSummary, null
        );
    }

    public AnnualReport(
            int year,
            int totalLivingPopulation,
            int livingMalePopulation,
            int livingFemalePopulation,
            Map<String, Integer> villageLivingPopulations,
            int birthsDuringYear,
            int deathsDuringYear,
            int marriagesDuringYear,
            int crossVillageMarriagesDuringYear,
            int migrationsDuringYear,
            int settlementUpgradesDuringYear,
            int effectiveMaxLifespan,
            int eligibleUnmarriedPopulation,
            int activeCouples,
            int fertileCouples,
            int totalRecordedPeople,
            int activeFamilies,
            int extinctFamilies,
            String largestFamilySummary,
            List<VillageAnnualStats> villageStats,
            String awakeningSummary,
            String progressionSummary,
            String longevitySummary,
            String breakthroughSummary,
            String combatSummary,
            String organizationSummary,
            String anomalySkirmishSummary,
            String anomalyExplorationSummary,
            String monsterEcologySummary,
            String equipmentSummary,
            String bloodFeudSummary) {
        this.year = year;
        this.totalLivingPopulation = totalLivingPopulation;
        this.livingMalePopulation = livingMalePopulation;
        this.livingFemalePopulation = livingFemalePopulation;
        this.villageLivingPopulations = Collections.unmodifiableMap(new LinkedHashMap<>(villageLivingPopulations));
        this.birthsDuringYear = birthsDuringYear;
        this.deathsDuringYear = deathsDuringYear;
        this.marriagesDuringYear = marriagesDuringYear;
        this.crossVillageMarriagesDuringYear = crossVillageMarriagesDuringYear;
        this.migrationsDuringYear = migrationsDuringYear;
        this.settlementUpgradesDuringYear = settlementUpgradesDuringYear;
        this.effectiveMaxLifespan = effectiveMaxLifespan;
        this.eligibleUnmarriedPopulation = eligibleUnmarriedPopulation;
        this.activeCouples = activeCouples;
        this.fertileCouples = fertileCouples;
        this.totalRecordedPeople = totalRecordedPeople;
        this.activeFamilies = activeFamilies;
        this.extinctFamilies = extinctFamilies;
        this.largestFamilySummary = largestFamilySummary;
        this.villageStats = List.copyOf(villageStats);
        this.awakeningSummary = awakeningSummary;
        this.progressionSummary = progressionSummary;
        this.longevitySummary = longevitySummary;
        this.breakthroughSummary = breakthroughSummary;
        this.combatSummary = combatSummary;
        this.organizationSummary = organizationSummary;
        this.anomalySkirmishSummary = anomalySkirmishSummary;
        this.anomalyExplorationSummary = anomalyExplorationSummary;
        this.monsterEcologySummary = monsterEcologySummary;
        this.equipmentSummary = equipmentSummary;
        this.bloodFeudSummary = bloodFeudSummary;
    }

    private static String formatResource(double val) {
        if (val >= 1_000_000) return String.format("%.1fM", val / 1_000_000.0);
        if (val >= 10_000) return String.format("%.1fk", val / 1000.0);
        if (val >= 1000) return String.format("%.0f", val);
        return String.format("%.1f", Math.max(0.0, val));
    }

    public String formatSummaryLine() {
        double worldWealth = 0.0;
        double worldDeposits = 0.0;
        double worldTreasury = 0.0;
        double worldTrade = 0.0;
        double worldFood = 0.0;
        double worldWood = 0.0;
        double worldOre = 0.0;
        double worldTools = 0.0;
        int worldFamineDeaths = 0;
        int worldFrostDeaths = 0;
        for (VillageAnnualStats vs : villageStats) {
            worldWealth += vs.getTotalWealth();
            worldDeposits += vs.getBankDeposits();
            worldTreasury += vs.getTreasury();
            worldTrade += vs.getTradeVolume();
            worldFood += vs.getFoodStockpile();
            worldWood += vs.getWoodStockpile();
            worldOre += vs.getIronStoneStockpile();
            worldTools += vs.getToolsStockpile();
            worldFamineDeaths += vs.getFamineDeaths();
            worldFrostDeaths += vs.getFreezingDeaths();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n+-------------------------------------------------------------------------------------------------------------------------------------------------+\n");
        sb.append(String.format("| ANNUAL REPORT - YEAR %-122d |\n", year));
        sb.append("+-------------------------------------------------------------------------------------------------------------------------------------------------+\n");
        sb.append(String.format("| World Population : %-6d (Males: %-4d | Females: %-4d | Unmarried 19+: %-4d | Max Lifespan: %-3d | Recorded: %-6d)                    |\n",
                totalLivingPopulation, livingMalePopulation, livingFemalePopulation, eligibleUnmarriedPopulation, effectiveMaxLifespan, totalRecordedPeople));
        sb.append(String.format("| Yearly Events    : Births: %-4d | Deaths: %-4d | Marriages: %-3d (Cross: %-2d) | Migrations: %-3d | Tier Upgrades: %-2d                       |\n",
                birthsDuringYear, deathsDuringYear, marriagesDuringYear, crossVillageMarriagesDuringYear, migrationsDuringYear, settlementUpgradesDuringYear));
        sb.append(String.format("| World Economy    : Citizen Wealth: %-11s | Bank Deposits: %-11s | Civic Treasury: %-11s | Trade Vol: %-8s                 |\n",
                Currency.formatHeader(worldWealth),
                Currency.formatHeader(worldDeposits),
                Currency.formatHeader(worldTreasury),
                Currency.formatHeader(worldTrade)));
        sb.append(String.format("| World Stockpiles : Food: %-7s | Wood: %-7s | Ore: %-7s | Tools: %-7s | Famine Dth: %-4d | Frost Dth: %-4d                             |\n",
                formatResource(worldFood),
                formatResource(worldWood),
                formatResource(worldOre),
                formatResource(worldTools),
                worldFamineDeaths,
                worldFrostDeaths));
        sb.append(String.format("| Dynasties        : Active Couples: %-4d (Fertile: %-4d) | Active Houses: %-4d (Extinct: %-4d) | Largest: %-22s       |\n",
                activeCouples, fertileCouples, activeFamilies, extinctFamilies, largestFamilySummary));
        if (awakeningSummary != null && !awakeningSummary.isEmpty()) {
            sb.append(String.format("| Aether / Awakened: %-126s|\n", awakeningSummary));
        }
        if (progressionSummary != null && !progressionSummary.isEmpty()) {
            sb.append(String.format("| Progression      : %-126s|\n", progressionSummary));
        }
        if (longevitySummary != null && !longevitySummary.isEmpty()) {
            sb.append(String.format("| Longevity / Aging: %-126s|\n", longevitySummary));
        }
        if (breakthroughSummary != null && !breakthroughSummary.isEmpty()) {
            sb.append(String.format("| Breakthroughs    : %-126s|\n", breakthroughSummary));
        }
        if (combatSummary != null && !combatSummary.isEmpty()) {
            sb.append(String.format("| Combat / Battles  : %-126s|\n", combatSummary));
        }
        if (organizationSummary != null && !organizationSummary.isEmpty()) {
            sb.append(String.format("| Organizations    : %-126s|\n", organizationSummary));
        }
        if (anomalySkirmishSummary != null && !anomalySkirmishSummary.isEmpty()) {
            sb.append(String.format("| Anomaly Skirmish : %-126s|\n", anomalySkirmishSummary));
        }
        if (anomalyExplorationSummary != null && !anomalyExplorationSummary.isEmpty()) {
            sb.append(String.format("| Anomalies & Exped: %-126s|\n", anomalyExplorationSummary));
        }
        if (monsterEcologySummary != null && !monsterEcologySummary.isEmpty()) {
            sb.append(String.format("| Monster Ecology  : %-126s|\n", monsterEcologySummary));
        }
        if (equipmentSummary != null && !equipmentSummary.isEmpty()) {
            sb.append(String.format("| Equipment & Relics: %-126s|\n", equipmentSummary));
        }
        if (bloodFeudSummary != null && !bloodFeudSummary.isEmpty()) {
            sb.append(String.format("| Blood Feuds & Griev: %-126s|\n", bloodFeudSummary));
        }
        sb.append("+--------------+-------+-------+-------+-----+-------+-------+-------+-------+--------+--------+----+--------------+------------------+-----+-----+\n");
        sb.append("| Settlement   | Tier  | Biome | Pos   | Pop | Food  | Wood  | Ore   | Tool  | Wealth | Treas  |Tch | Leader       | Diplo / Raid     | Hse | Dth |\n");
        sb.append("+--------------+-------+-------+-------+-----+-------+-------+-------+-------+--------+--------+----+--------------+------------------+-----+-----+\n");

        for (VillageAnnualStats vs : villageStats) {
            String leaderShort = vs.getLeaderName().length() > 12
                    ? vs.getLeaderName().substring(0, 12)
                    : vs.getLeaderName();
            String raidShort = vs.getRaidSummary().length() > 16
                    ? vs.getRaidSummary().substring(0, 16)
                    : vs.getRaidSummary();
            String posStr = String.format("%.0f,%.0f", vs.getX(), vs.getY());
            sb.append(String.format("| %-12s | %-5s | %-5s | %-5s |%-4d | %-5s | %-5s | %-5s | %-5s | %-6s | %-6s | %-2d | %-12s | %-16s | %-3d | %-3d |\n",
                    vs.getVillageName(),
                    vs.getTier().getDisplayName().substring(0, Math.min(5, vs.getTier().getDisplayName().length())),
                    vs.getBiome().getShortCode(),
                    posStr,
                    vs.getLivingPopulation(),
                    formatResource(vs.getFoodStockpile()),
                    formatResource(vs.getWoodStockpile()),
                    formatResource(vs.getIronStoneStockpile()),
                    formatResource(vs.getToolsStockpile()),
                    Currency.formatCompact(vs.getTotalWealth()),
                    Currency.formatCompact(vs.getTreasury()),
                    vs.getTechLevel(),
                    leaderShort,
                    raidShort,
                    vs.getHousingCount(),
                    vs.getDeaths()));
        }
        sb.append("+--------------+-------+-------+-------+-----+-------+-------+-------+-------+--------+--------+----+--------------+------------------+-----+-----+");
        return sb.toString();
    }

    public int getYear() { return year; }
    public int getTotalLivingPopulation() { return totalLivingPopulation; }
    public int getLivingMalePopulation() { return livingMalePopulation; }
    public int getLivingFemalePopulation() { return livingFemalePopulation; }
    public Map<String, Integer> getVillageLivingPopulations() { return villageLivingPopulations; }
    public int getBirthsDuringYear() { return birthsDuringYear; }
    public int getDeathsDuringYear() { return deathsDuringYear; }
    public int getMarriagesDuringYear() { return marriagesDuringYear; }
    public int getCrossVillageMarriagesDuringYear() { return crossVillageMarriagesDuringYear; }
    public int getMigrationsDuringYear() { return migrationsDuringYear; }
    public int getSettlementUpgradesDuringYear() { return settlementUpgradesDuringYear; }
    public int getEffectiveMaxLifespan() { return effectiveMaxLifespan; }
    public int getEligibleUnmarriedPopulation() { return eligibleUnmarriedPopulation; }
    public int getActiveCouples() { return activeCouples; }
    public int getFertileCouples() { return fertileCouples; }
    public int getTotalRecordedPeople() { return totalRecordedPeople; }
    public int getActiveFamilies() { return activeFamilies; }
    public int getExtinctFamilies() { return extinctFamilies; }
    public String getLargestFamilySummary() { return largestFamilySummary; }
    public List<VillageAnnualStats> getVillageStats() { return villageStats; }
    public String getAwakeningSummary() { return awakeningSummary; }
    public String getProgressionSummary() { return progressionSummary; }
    public String getLongevitySummary() { return longevitySummary; }
    public String getBreakthroughSummary() { return breakthroughSummary; }
    public String getCombatSummary() { return combatSummary; }
    public String getOrganizationSummary() { return organizationSummary; }
    public String getAnomalySkirmishSummary() { return anomalySkirmishSummary; }
    public String getAnomalyExplorationSummary() { return anomalyExplorationSummary; }
    public String getMonsterEcologySummary() { return monsterEcologySummary; }
    public String getEquipmentSummary() { return equipmentSummary; }
    public String getBloodFeudSummary() { return bloodFeudSummary; }
}