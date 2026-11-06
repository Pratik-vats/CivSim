package civsim;

import civsim.model.*;
import civsim.spatial.Biome;
import civsim.spatial.WorldGeography;
import civsim.systems.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Master simulation engine and central orchestrator. Coordinates the central scheduler thread,
 * advances the timeline, and delegates demographic, economic, and diplomatic operations
 * to specialized subsystem engines.
 */
public class SimulationEngine {
    private final SimulationConfig config;
    private final Timeline timeline;
    private Random random;

    private final List<Village> villages;
    private final List<Person> allPeople;
    private final List<Family> allFamilies;
    private final List<Couple> allCouples;
    private final List<AnnualReport> annualReports;
    private final List<String> eventHistory;

    // Specialized Simulation Subsystems
    private final DemographicsSystem demographicsSystem;
    private final ProductionAndSurvivalSystem productionSystem;
    private final MigrationSystem migrationSystem;
    private final ColonizationSystem colonizationSystem;
    private final TradeSystem tradeSystem;
    private final WarfareAndDiplomacySystem warfareSystem;
    private final DisasterSystem disasterSystem;
    private final AwakeningSystem awakeningSystem;
    private final ProgressionSystem progressionSystem;
    private final OrganizationSystem organizationSystem;
    private final FactionConflictSystem factionConflictSystem;
    private final AnomalyExplorationSystem anomalySystem;
    private final MonsterEcologySystem monsterEcologySystem;
    private final EquipmentCraftingSystem equipmentCraftingSystem;
    private final ClanHeirloomSystem clanHeirloomSystem;
    private final BloodFeudSystem bloodFeudSystem;
    private final CombatEngine combatEngine;
    private final ProgressionConfig progressionConfig;

    private int nextPersonId = 1;
    private int nextFamilyId = 1;
    private int nextCoupleId = 1;
    private int nextVillageId = 1;
    private int crossVillageMarriagesThisYear = 0;
    private int settlementUpgradesThisYear = 0;
    private int totalRecordedPeopleCount = 0;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean initialized = false;
    private SimulationLifecycleState lifecycleState = SimulationLifecycleState.INITIALIZING;

    public synchronized SimulationLifecycleState getLifecycleState() { return lifecycleState; }
    public synchronized void setLifecycleState(SimulationLifecycleState state) { this.lifecycleState = state; }
    public synchronized boolean isHumanExtinct() { return lifecycleState == SimulationLifecycleState.HUMAN_EXTINCT; }
    public synchronized boolean isInitialized() { return initialized; }
    public synchronized void setInitialized(boolean initialized) { this.initialized = initialized; }
    public synchronized int getNextPersonId() { return nextPersonId; }
    public synchronized void setNextPersonId(int id) { this.nextPersonId = id; }
    public synchronized int getNextFamilyId() { return nextFamilyId; }
    public synchronized void setNextFamilyId(int id) { this.nextFamilyId = id; }
    public synchronized int getNextCoupleId() { return nextCoupleId; }
    public synchronized void setNextCoupleId(int id) { this.nextCoupleId = id; }
    public synchronized int getNextVillageId() { return nextVillageId; }
    public synchronized void setNextVillageId(int id) { this.nextVillageId = id; }
    public synchronized int getTotalRecordedPeopleCount() { return totalRecordedPeopleCount; }
    public synchronized void setTotalRecordedPeopleCount(int count) { this.totalRecordedPeopleCount = count; }
    public Random getRandom() { return random; }
    public synchronized void setRandom(Random random) { this.random = random; }

    public SimulationEngine(SimulationConfig config) {
        this(config, ProgressionConfig.createDefault());
    }

    public SimulationEngine(SimulationConfig config, ProgressionConfig progressionConfig) {
        this.config = config;
        this.timeline = new Timeline();
        this.random = (config.getRandomSeed() != 0) ? new Random(config.getRandomSeed()) : new Random();

        this.villages = new ArrayList<>();
        this.allPeople = new ArrayList<>();
        this.allFamilies = new ArrayList<>();
        this.allCouples = new ArrayList<>();
        this.annualReports = new ArrayList<>();
        this.eventHistory = new ArrayList<>();

        // Initialize Specialized Systems
        this.demographicsSystem = new DemographicsSystem();
        this.productionSystem = new ProductionAndSurvivalSystem();
        this.migrationSystem = new MigrationSystem(this.demographicsSystem);
        this.colonizationSystem = new ColonizationSystem(this.demographicsSystem);
        this.tradeSystem = new TradeSystem();
        this.warfareSystem = new WarfareAndDiplomacySystem(this.demographicsSystem);
        this.disasterSystem = new DisasterSystem(this.demographicsSystem);
        this.awakeningSystem = new AwakeningSystem();
        this.progressionSystem = new ProgressionSystem();
        this.organizationSystem = new OrganizationSystem();
        this.organizationSystem.setPeopleSupplier(() -> this.allPeople);
        this.factionConflictSystem = new FactionConflictSystem();
        this.anomalySystem = new AnomalyExplorationSystem();
        this.monsterEcologySystem = new MonsterEcologySystem();
        this.equipmentCraftingSystem = new EquipmentCraftingSystem();
        this.clanHeirloomSystem = new ClanHeirloomSystem();
        this.bloodFeudSystem = new BloodFeudSystem();
        this.combatEngine = new CombatEngine();
        EquipmentCraftingSystem.setActiveInstance(this.equipmentCraftingSystem);
        ClanHeirloomSystem.setActiveInstance(this.clanHeirloomSystem);
        this.progressionConfig = (progressionConfig != null) ? progressionConfig : ProgressionConfig.createDefault();
        this.demographicsSystem.setProgressionConfig(this.progressionConfig);
        this.demographicsSystem.setOrganizationSystem(this.organizationSystem);
        this.demographicsSystem.setCombatLootListener(this.factionConflictSystem);
    }

    public synchronized void initializeWorld() {
        if (initialized) return;

        double[][] initialCoords = {
                {25.0, 25.0}, // Settlement A: River Valley southwest
                {85.0, 85.0}  // Settlement B: Highland Mine northeast
        };

        Biome[] initialBiomes = {
                Biome.RIVER_VALLEY,
                Biome.HIGHLAND_MINE
        };

        int vIdx = 0;
        for (String villageName : config.getVillageNames()) {
            double originalX = (vIdx < initialCoords.length) ? initialCoords[vIdx][0] : 15.0 + ((vIdx * 23.0) % 70.0);
            double originalY = (vIdx < initialCoords.length) ? initialCoords[vIdx][1] : 15.0 + ((vIdx * 31.0) % 70.0);
            WorldGeography.Point2D mapPoint = WorldGeography.legacyToMap(originalX, originalY);
            double vx = mapPoint.x();
            double vy = mapPoint.y();
            Biome vBiome = (vIdx < initialBiomes.length) ? initialBiomes[vIdx] : Biome.determineBiomeAt(vx, vy);
            vIdx++;

            Village village = new Village(nextVillageId++, villageName, config.getInitialFoodPrice(), config.getBankInterestRate(), vx, vy, vBiome);
            villages.add(village);

            for (int i = 0; i < config.getInitialPopulationPerVillage(); i++) {
                int id = nextPersonId++;
                String name = "Person" + id;
                Gender gender = random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
                int age = random.nextInt(config.getMaxInitialAge() + 1);
                int birthYear = -age;

                int famId = nextFamilyId++;
                String baseSurname = ColonizationSystem.BASE_SURNAMES[(famId - 1) % ColonizationSystem.BASE_SURNAMES.length];
                String uniqueSurname = baseSurname + "-" + famId;

                Person person = new Person(id, name, gender, age, birthYear, village, null, null, null, config.getInitialPersonalWealth());
                Family foundingFamily = new Family(famId, uniqueSurname, 0, person);
                person.setFamily(foundingFamily);

                allFamilies.add(foundingFamily);
                allPeople.add(person);
                totalRecordedPeopleCount++;
                village.addResident(person);

                recordEvent(String.format("Year 0: %s (%s, age %d) founded Family %s in %s.",
                        person.getFullName(), person.getGender(), person.getAge(),
                        foundingFamily.getSurname(), village.getName()), false);
            }
        }

        for (Village village : villages) {
            productionSystem.assignOccupations(village, config.getFoodPerPersonPerMonth(), config.getFoodProducedPerFarmer(), config.getHousingCapacityPerBuilding());
            village.electLeaderIfNeeded();
            if (village.getCraftingFacilities().isEmpty()) {
                village.addCraftingFacility(new CraftingFacility("FORGE_" + village.getId() + "_1", "VILLAGE_" + village.getId(), null, FacilityGrade.BASIC_FORGE, 3));
            }
        }
        evaluateAllSettlementTiers(0);

        int[] coupleCounter = new int[]{nextCoupleId};
        crossVillageMarriagesThisYear = demographicsSystem.matchMarriages(
                villages, allCouples, 0, 0, random, config, coupleCounter, this::recordEvent);
        nextCoupleId = coupleCounter[0];

        AnnualReport initialReport = generateAnnualReport(0);
        annualReports.add(initialReport);
        System.out.println(initialReport.formatSummaryLine());

        initialized = true;
        if (getAuthoritativeLivingHumanCount() == 0) {
            this.lifecycleState = SimulationLifecycleState.HUMAN_EXTINCT;
            recordEvent("Year 0: World initialized with zero living human population. Extinction detected.", true);
            if (config.getExtinctionPolicy().isStopOnExtinction()) {
                stop();
            }
        } else if (this.lifecycleState == SimulationLifecycleState.INITIALIZING) {
            this.lifecycleState = SimulationLifecycleState.RUNNING;
        }
    }

    public synchronized void start() {
        if (!initialized) {
            initializeWorld();
        }
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "CivSim-Central-Scheduler");
                t.setDaemon(false);
                return t;
            });
            scheduler.scheduleAtFixedRate(
                    this::processMonthlyTickSafe,
                    config.getTickDurationMillis(),
                    config.getTickDurationMillis(),
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public synchronized void stop() {
        boolean wasRunning = running.getAndSet(false);
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        EquipmentCraftingSystem.setActiveInstance(null);
        ClanHeirloomSystem.setActiveInstance(null);
        CombatEngine.clearCache();
        if (lifecycleState != SimulationLifecycleState.HUMAN_EXTINCT) {
            lifecycleState = SimulationLifecycleState.STOPPED;
        }
        recordEvent(String.format("Year %d (Month %d): Simulation stopped.",
                timeline.getYear(), timeline.getMonth() + 1), config.isPrintIndividualEvents());
    }

    private void processMonthlyTickSafe() {
        try {
            processMonthlyTick();
        } catch (Exception e) {
            System.err.println("Unexpected error in simulation tick: " + e.getMessage());
            e.printStackTrace();
            stop();
        }
    }

    public synchronized void processMonthlyTick() {
        if (!running.get() && scheduler != null) {
            return;
        }
        if (lifecycleState == SimulationLifecycleState.HUMAN_EXTINCT && config.getExtinctionPolicy().isStopOnExtinction()) {
            return;
        }

        int currentYear = timeline.getYear();
        int currentMonth = timeline.getMonth();
        long currentTotalMonths = timeline.getTotalElapsedMonths();
        Season currentSeason = timeline.getCurrentSeason();
        boolean yearCompleted = (currentMonth == 11);
        for (Village village : villages) {
            List<MonthlyCasualty> casualties = productionSystem.simulateMonthlyEconomy(
                    village,
                    currentSeason,
                    config.getFoodPerPersonPerMonth(),
                    config.getFoodProducedPerFarmer(),
                    random
            );
            for (MonthlyCasualty casualty : casualties) {
                demographicsSystem.killPerson(casualty.getPerson(), allCouples, currentYear, casualty.getCause(), config, this::recordEvent);
            }
        }

        // Phase 2B.1: Monthly combat state recovery & injury expiration
        CombatEngine.processMonthlyRecovery(allPeople, currentYear, timeline.getMonth(), currentTotalMonths, config);

        // Phase 2B.3: Monthly anomaly scheduling, appearance, detection, expedition dispatch & travel
        anomalySystem.evaluateMonthlySpawning(
                currentYear, timeline.getMonth(), currentTotalMonths, villages, random, config, this::recordEvent);
        anomalySystem.processMonthlyAppearanceAndDetection(
                currentYear, timeline.getMonth(), currentTotalMonths, allPeople, villages,
                organizationSystem, progressionSystem, progressionConfig, random, this::recordEvent);
        anomalySystem.evaluateAndDispatchExpeditions(
                currentYear, timeline.getMonth(), currentTotalMonths, allPeople, villages,
                organizationSystem, this::recordEvent);
        anomalySystem.processMonthlyTravelAndContention(
                currentYear, timeline.getMonth(), currentTotalMonths, organizationSystem, villages, this::recordEvent);

        // Synchronize pending contested sites to FactionConflictSystem (Phase 2B.4)
        for (AnomalyContestRecord contest : anomalySystem.getPendingContests()) {
            if (factionConflictSystem.getContest(contest.getContestId()) == null) {
                factionConflictSystem.registerContest(contest);
                WorldAnomaly anom = anomalySystem.getAnomaly(contest.getAnomalyId());
                if (anom != null && factionConflictSystem.getAnomaly(anom.getAnomalyId()) == null) {
                    factionConflictSystem.registerAnomaly(anom);
                }
            }
        }

        // Phase 2B.4: Monthly anomaly contest evaluation, negotiations, and skirmishes
        factionConflictSystem.processMonthlyContests(
                currentYear, timeline.getMonth(), currentTotalMonths, allPeople,
                config, progressionConfig, demographicsSystem, awakeningSystem,
                progressionSystem, organizationSystem, allCouples, random, this::recordEvent);

        // Synchronize pending lairs to MonsterEcologySystem (Phase 2B.5)
        for (PendingMonsterLairRecord lairRec : anomalySystem.getPendingLairs()) {
            if (monsterEcologySystem.getLair("LAIR_" + lairRec.getAnomalyId()) == null) {
                monsterEcologySystem.createLairFromPendingRecord(lairRec, random, (int) currentTotalMonths);
            }
        }

        // Phase 2B.5: Monthly monster ecology, biomass consumption & generational evolution
        monsterEcologySystem.processMonthlyEcology(
                currentYear, timeline.getMonth(), currentTotalMonths, config, random, this::recordEvent);

        // Phase 2B.5: Periodic MonsterTide scheduling & invasion combat resolution
        monsterEcologySystem.evaluateMonsterTideScheduling(
                currentYear, timeline.getMonth(), currentTotalMonths, villages, allPeople, random, this::recordEvent);

        for (MonsterTide tide : new ArrayList<>(monsterEcologySystem.getTides().values())) {
            if (tide.getState() == MonsterTideState.INVADING) {
                Village targetV = null;
                for (Village v : villages) {
                    if (v.getId() == tide.getTargetVillageId()) {
                        targetV = v;
                        break;
                    }
                }
                if (targetV != null) {
                    monsterEcologySystem.resolveMonsterTideInvasion(
                            tide, targetV, allPeople, config, progressionConfig,
                            demographicsSystem, awakeningSystem, progressionSystem,
                            random, allCouples, this::recordEvent, currentYear, timeline.getMonth());
                }
            }
        }

        // Phase 2C.1: Monthly Equipment Crafting & Clan Heirloom Succession
        EquipmentCraftingSystem.setActiveInstance(this.equipmentCraftingSystem);
        ClanHeirloomSystem.setActiveInstance(this.clanHeirloomSystem);

        Map<Integer, Person> peopleMap = new HashMap<>();
        for (Person p : allPeople) {
            peopleMap.put(p.getId(), p);
        }

        equipmentCraftingSystem.processMonthlyCrafting(
                currentYear, timeline.getMonth(), villages, peopleMap, organizationSystem, random);

        // Dynamic crafting opportunities in settlements with blacksmiths
        int awakeningStart = config.getAwakeningStartYear();
        for (Village village : villages) {
            // Check facility progression
            if (village.getTechLevel() >= 2 && village.getCraftingFacility("FORGE_" + village.getId() + "_ADV") == null) {
                village.addCraftingFacility(new CraftingFacility("FORGE_" + village.getId() + "_ADV", "VILLAGE_" + village.getId(), null, FacilityGrade.ADVANCED_FORGE, 3));
            }
            if (currentYear >= awakeningStart && village.getTechLevel() >= 3 && village.getCraftingFacility("FORGE_" + village.getId() + "_SUP") == null
                    && village.getSupernaturalInventory().getAmount(SupernaturalResourceType.RAW_AETHER_CRYSTAL) >= 10.0) {
                village.addCraftingFacility(new CraftingFacility("FORGE_" + village.getId() + "_SUP", "VILLAGE_" + village.getId(), null, FacilityGrade.SUPERNATURAL_FORGE, 3));
            }

            CraftingFacility bestForge = village.getBestCraftingFacility();
            if (bestForge != null && bestForge.hasAvailableCapacity()) {
                Person blacksmith = null;
                for (Person r : village.getResidents()) {
                    if (r.isAlive() && r.getOccupation() == Occupation.BLACKSMITH) {
                        blacksmith = r;
                        break;
                    }
                }
                if (blacksmith != null) {
                    if (currentYear >= awakeningStart && village.getMonsterCoreCount() >= 1
                            && village.getSupernaturalInventory().getAmount(SupernaturalResourceType.RAW_AETHER_CRYSTAL) >= 10.0
                            && bestForge.getGrade().getMaxEquipmentTier() >= 3) {
                        equipmentCraftingSystem.createCraftOrder("RECIPE_AETHER_BLADE_T3", village, null, blacksmith, bestForge, currentYear, timeline.getMonth(), awakeningStart);
                    } else if (bestForge.getGrade().getMaxEquipmentTier() >= 2 && village.getOreStockpile() >= 35.0 && village.getTreasury() >= 30.0) {
                        equipmentCraftingSystem.createCraftOrder("RECIPE_STEEL_BLADE_T2", village, null, blacksmith, bestForge, currentYear, timeline.getMonth(), awakeningStart);
                    } else if (village.getOreStockpile() >= 15.0 && village.getTreasury() >= 10.0) {
                        equipmentCraftingSystem.createCraftOrder("RECIPE_IRON_SWORD_T1", village, null, blacksmith, bestForge, currentYear, timeline.getMonth(), awakeningStart);
                    }
                }
            }
        }

        // Dynamic Heirloom Registration & Succession for Clans
        if (organizationSystem != null) {
            for (Organization org : organizationSystem.getAllOrganizations()) {
                if (org.getStatus() == OrganizationStatus.ACTIVE && org.getOrganizationType() == OrganizationType.CLAN) {
                    for (EquipmentItem item : org.getClanVault().values()) {
                        if (!item.isRegisteredHeirloom() && (item.getEquipmentRarity() == ClassRarity.RARE
                                || item.getEquipmentRarity() == ClassRarity.LEGENDARY
                                || item.getEquipmentRarity() == ClassRarity.MYTHIC)) {
                            clanHeirloomSystem.registerHeirloom(item, org, org.getDisplayName() + "'s " + item.getDisplayName(),
                                    HeirloomSuccessionPolicy.MERIT_FIRST, currentYear, timeline.getMonth());
                        }
                    }

                    for (ClanHeirloom heirloom : org.getHeirlooms().values()) {
                        if (heirloom.getCustodyStatus() == HeirloomCustodyStatus.IN_CLAN_VAULT) {
                            EquipmentItem item = equipmentCraftingSystem.getEquipment(heirloom.getEquipmentId());
                            if (item != null) {
                                clanHeirloomSystem.executeSuccession(heirloom, item, org, peopleMap, random);
                            }
                        }
                    }
                }
            }
        }

        // Phase 2C.2: Forward grievances and process blood feuds
        if (clanHeirloomSystem != null) {
            for (HistoricalGrievanceRecord g : clanHeirloomSystem.getHeirloomGrievances()) {
                bloodFeudSystem.recordGrievance(g, currentYear, timeline.getMonth());
            }
        }
        if (factionConflictSystem != null) {
            for (HistoricalGrievanceRecord g : factionConflictSystem.getGrievanceRecords()) {
                bloodFeudSystem.recordGrievance(g, currentYear, timeline.getMonth());
            }
        }

        bloodFeudSystem.processMonthlyTick(
                (int) currentTotalMonths, currentYear, timeline.getMonth(),
                villages, organizationSystem, combatEngine,
                clanHeirloomSystem, equipmentCraftingSystem, random);

        if (yearCompleted) {
            // Phase 2C.2: Evaluate deceased claimants and targets BEFORE pruning allPeople
            for (Person p : allPeople) {
                if (!p.isAlive() && p.getDeathYear() != null && p.getDeathYear() == currentYear) {
                    bloodFeudSystem.handlePersonDeath(p, currentYear, 12, peopleMap, organizationSystem);
                }
            }

            demographicsSystem.processYearlyAgingAndDeaths(
                    allPeople, allCouples, villages, allFamilies, currentYear, getEffectiveDeathAge(), random, config, this::recordEvent);
            disasterSystem.processAnnualDisasters(villages, allCouples, currentYear, random, config, this::recordEvent);
        }

        if (yearCompleted && config.isAllowMigration()) {
            migrationSystem.processAnnualMigration(villages, currentYear, random, config, this::recordEvent);
        }

        if (yearCompleted) {
            int[] vIdCounter = new int[]{nextVillageId};
            int[] fIdCounter = new int[]{nextFamilyId};
            colonizationSystem.processAnnualColonization(villages, allFamilies, currentYear, random, config, vIdCounter, fIdCounter, this::recordEvent);
            nextVillageId = vIdCounter[0];
            nextFamilyId = fIdCounter[0];

            tradeSystem.processInterSettlementTrade(villages, currentYear, random, config, this::recordEvent);

            int[] coupleCounter = new int[]{nextCoupleId};
            crossVillageMarriagesThisYear = demographicsSystem.matchMarriages(
                    villages, allCouples, currentYear, currentTotalMonths, random, config, coupleCounter, this::recordEvent);
            nextCoupleId = coupleCounter[0];

            for (Village village : villages) {
                productionSystem.assignOccupations(village, config.getFoodPerPersonPerMonth(), config.getFoodProducedPerFarmer(), config.getHousingCapacityPerBuilding());
                productionSystem.collectAnnualTaxes(village, config.getTaxRate());
                if (productionSystem.investInTechnology(village, config.getBaseTechUpgradeCost())) {
                    recordEvent(String.format("Year %d: %s researched Tech Level %d!",
                                    currentYear, village.getName(), village.getTechLevel()),
                            config.isPrintIndividualEvents());
                }
                productionSystem.buildHousingIfNeeded(village, config.getHousingCapacityPerBuilding());
                productionSystem.processAnnualBankingAndWealth(village);
                if (village.electLeaderIfNeeded() && village.getLeader() != null) {
                    recordEvent(String.format("Year %d: %s elected %s as new settlement leader.",
                                    currentYear, village.getName(), village.getLeader().getFullName()),
                            config.isPrintIndividualEvents());
                }
            }

            if (config.isAllowDiplomacyAndRaids()) {
                warfareSystem.processAnnualDiplomacyAndRaids(villages, allCouples, currentYear, random, config, this::recordEvent);
            }

            // Phase 2A: Supernatural Awakening System
            if (currentYear == config.getAwakeningStartYear()) {
                awakeningSystem.triggerGreatMutation(allPeople, currentYear, random, config, this::recordEvent);
            }
            if (currentYear >= config.getAwakeningStartYear() && awakeningSystem.isGreatMutationTriggered()) {
                AwakeningSystem.AwakeningYearSummary awakeningSummary =
                        awakeningSystem.processAnnualAwakenings(allPeople, villages, currentYear, random, config, progressionConfig, this::recordEvent);
                if (awakeningSummary.getTotalAwakenings() > 0) {
                    recordEvent(String.format("Year %d: %s", currentYear, awakeningSummary.toSummaryString()),
                            config.isPrintIndividualEvents());
                }

                // Stage 2A.4: Process annual progression events
                progressionSystem.emitAndProcessAnnualProgressionEvents(
                        allPeople, villages, currentYear, random,
                        progressionConfig, config, this::recordEvent);
            }
        }

        int[] pCounter = new int[]{nextPersonId};
        int[] recCounter = new int[]{totalRecordedPeopleCount};
        demographicsSystem.processScheduledBirths(
                allCouples, allPeople, currentYear, currentTotalMonths, random, config, pCounter, recCounter, this::recordEvent);
        nextPersonId = pCounter[0];
        totalRecordedPeopleCount = recCounter[0];

        if (yearCompleted) {
            evaluateAllSettlementTiers(currentYear);
            colonizationSystem.cleanupAbandonedVillages(villages, currentYear, this::recordEvent, config);
            AnnualReport report = generateAnnualReport(currentYear);
            annualReports.add(report);
            System.out.println(report.formatSummaryLine());
            progressionSystem.resetYearlyCounters();
            CombatEngine.resetYearlyCounters();
            organizationSystem.resetYearlyCounters();
            factionConflictSystem.resetYearlyCounters();
            anomalySystem.resetYearlyCounters();
            monsterEcologySystem.resetYearlyCounters();
            bloodFeudSystem.resetYearlyCounters();

            if (config.getMaxSimulationYears() > 0 && currentYear >= config.getMaxSimulationYears()) {
                stop();
            }
        }

        // Advance timeline by 1 month
        timeline.advanceMonth();

        // Extinction evaluation: Authoritative living human check
        int livingHumans = getAuthoritativeLivingHumanCount();
        if (livingHumans == 0 && lifecycleState == SimulationLifecycleState.RUNNING) {
            this.lifecycleState = SimulationLifecycleState.HUMAN_EXTINCT;
            recordEvent(String.format("Year %d (Month %d): Human Extinction Detected. All human population has perished.",
                    currentYear, currentMonth + 1), true);
            if (config.getExtinctionPolicy().isStopOnExtinction()) {
                stop();
            }
        }
    }

    private void evaluateAllSettlementTiers(int currentYear) {
        for (Village village : villages) {
            SettlementTier previousTier = village.getTier();
            boolean upgraded = village.evaluateSettlementProgression(
                    config.getVillagePopulationThreshold(),
                    config.getTownPopulationThreshold(),
                    config.getCityPopulationThreshold(),
                    config.getMegacityPopulationThreshold()
            );
            if (upgraded) {
                settlementUpgradesThisYear++;
                String slotsDesc = village.getTier().hasHousingCap()
                        ? village.getInfrastructureSlots() + " building slots"
                        : "Unlimited housing cap";
                recordEvent(String.format("Year %d: %s has evolved from %s to %s (Population: %d, %s)!",
                                currentYear,
                                village.getName(),
                                previousTier.getDisplayName(),
                                village.getTier().getDisplayName(),
                                village.getLivingPopulationCount(),
                                slotsDesc),
                        config.isPrintIndividualEvents());
            }
        }
    }

    private int getEffectiveDeathAge() {
        int maxTech = 0;
        for (Village v : villages) {
            if (v.getTechLevel() > maxTech) {
                maxTech = v.getTechLevel();
            }
        }
        return config.getDeathAge() + maxTech;
    }

    private AnnualReport generateAnnualReport(int year) {
        int totalLiving = 0;
        int totalMales = 0;
        int totalFemales = 0;
        int totalBirths = 0;
        int totalDeaths = 0;
        int totalMarriages = 0;
        int totalMigrations = 0;
        int totalEligibleUnmarried = 0;

        Map<String, Integer> villagePopMap = new LinkedHashMap<>();
        List<VillageAnnualStats> villageStatsList = new ArrayList<>();

        for (Village village : villages) {
            VillageAnnualStats vStats = village.snapshotAndResetYearlyCounters(
                    year, config.getMinMarriageAgeExclusive(), config.getHousingCapacityPerBuilding(), villages);
            villageStatsList.add(vStats);

            totalLiving += vStats.getLivingPopulation();
            totalMales += vStats.getLivingMales();
            totalFemales += vStats.getLivingFemales();
            totalBirths += vStats.getBirths();
            totalDeaths += vStats.getDeaths();
            totalMarriages += vStats.getMarriages();
            totalMigrations += vStats.getInMigrations();
            totalEligibleUnmarried += vStats.getEligibleUnmarried();
            villagePopMap.put(village.getName(), vStats.getLivingPopulation());
        }

        int crossVillageCount = this.crossVillageMarriagesThisYear;
        this.crossVillageMarriagesThisYear = 0;

        int upgradesCount = this.settlementUpgradesThisYear;
        this.settlementUpgradesThisYear = 0;

        int activeCouplesCount = 0;
        int fertileCouplesCount = 0;
        for (Couple c : allCouples) {
            if (c.isActive() && c.getHusband().isAlive() && c.getWife().isAlive()) {
                activeCouplesCount++;
                if (c.isFertile(config.getMaxFemaleFertilityAge())) {
                    fertileCouplesCount++;
                }
            }
        }

        int activeFams = 0;
        int extinctFams = 0;
        Family largestFamily = null;
        int largestCount = 0;

        for (Family fam : allFamilies) {
            int livingCount = fam.getLivingMemberCount();
            if (livingCount > 0) {
                activeFams++;
                if (livingCount > largestCount) {
                    largestCount = livingCount;
                    largestFamily = fam;
                }
            } else {
                extinctFams++;
            }
        }

        String largestFamSummary = (largestFamily != null)
                ? String.format("%s (%d)", largestFamily.getSurname(), largestCount)
                : "None";

        // Phase 2A: Awakening summary for report
        String awakeningSummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            int totalAwakened = 0;
            int totalDD = 0, totalDr = 0, totalRr = 0;
            int livingMythicCount = 0;
            int darkAwakenedCount = 0;
            for (Person p : allPeople) {
                if (!p.isAlive()) continue;
                AetherProfile ap = p.getAetherProfile();
                if (ap == null) continue;
                String geno = ap.getGenotypeString();
                switch (geno) {
                    case "DD": totalDD++; break;
                    case "Dr": totalDr++; break;
                    case "rr": totalRr++; break;
                }
                if (ap.isAwakened()) {
                    totalAwakened++;
                    if (ap.getRarity() == civsim.model.ClassRarity.MYTHIC) livingMythicCount++;
                    if (ap.isDarkAwakened()) darkAwakenedCount++;
                }
            }
            awakeningSummary = String.format(
                    "Awakened: %d | DD:%d Dr:%d rr:%d | Mythic:%d Dark:%d",
                    totalAwakened, totalDD, totalDr, totalRr, livingMythicCount, darkAwakenedCount);
        }
        // Stage 2A.4: Progression summary
        String progressionSummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            progressionSummary = progressionSystem.generateAnnualProgressionSummary(allPeople);
        }
        // Stage 2A.5: Longevity / Aging summary
        String longevitySummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            longevitySummary = demographicsSystem.generateAnnualLongevitySummary(allPeople, year, config);
        }
        // Stage 2A.6: Breakthrough summary
        String breakthroughSummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            breakthroughSummary = progressionSystem.generateAnnualBreakthroughSummary(allPeople, progressionConfig, year);
        }
        // Phase 2B.1: Combat summary
        String combatSummary = CombatEngine.generateAnnualCombatSummary();
        // Phase 2B.2: Organization summary
        String organizationSummary = organizationSystem.generateAnnualOrganizationSummary(year, allPeople);
        // Phase 2B.4: Anomaly Skirmish summary
        String anomalySkirmishSummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            anomalySkirmishSummary = factionConflictSystem.generateAnnualSummary(year);
        }
        // Phase 2B.3: Anomaly Exploration summary
        String anomalyExplorationSummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            anomalyExplorationSummary = anomalySystem.generateAnnualSummary(year);
        }
        // Phase 2B.5: Monster Ecology summary
        String monsterEcologySummary = null;
        if (awakeningSystem.isGreatMutationTriggered()) {
            monsterEcologySummary = monsterEcologySystem.generateAnnualSummary(year);
        }
        // Phase 2C.1: Equipment & Clan Heirloom summary
        String equipmentSummary = equipmentCraftingSystem.generateAnnualSummary(clanHeirloomSystem);
        // Phase 2C.2: Blood Feud summary
        String bloodFeudSummary = bloodFeudSystem.generateAnnualSummary();

        return new AnnualReport(
                year,
                totalLiving,
                totalMales,
                totalFemales,
                villagePopMap,
                totalBirths,
                totalDeaths,
                totalMarriages,
                crossVillageCount,
                totalMigrations,
                upgradesCount,
                getEffectiveDeathAge(),
                totalEligibleUnmarried,
                activeCouplesCount,
                fertileCouplesCount,
                totalRecordedPeopleCount,
                activeFams,
                extinctFams,
                largestFamSummary,
                villageStatsList,
                awakeningSummary,
                progressionSummary,
                longevitySummary,
                breakthroughSummary,
                combatSummary,
                organizationSummary,
                anomalySkirmishSummary,
                anomalyExplorationSummary,
                monsterEcologySummary,
                equipmentSummary,
                bloodFeudSummary
        );
    }

    private void recordEvent(String eventDescription, boolean printToConsole) {
        if (eventHistory.size() >= 10000) {
            eventHistory.subList(0, 1000).clear();
        }
        eventHistory.add(eventDescription);
        if (printToConsole) {
            System.out.println(eventDescription);
        }
    }

    public synchronized int getAuthoritativeLivingHumanCount() {
        int count = 0;
        for (Person p : allPeople) {
            if (p.isAlive()) count++;
        }
        return count;
    }

    public synchronized List<Person> getLivingPeople() {
        List<Person> list = new ArrayList<>();
        for (Person p : allPeople) {
            if (p.isAlive()) list.add(p);
        }
        return list;
    }

    public synchronized Village getVillage(int id) {
        for (Village v : villages) {
            if (v.getId() == id) return v;
        }
        return null;
    }

    public synchronized Person getPerson(int id) {
        for (Person p : allPeople) {
            if (p.getId() == id) return p;
        }
        if (demographicsSystem != null) {
            return demographicsSystem.getDeceasedPerson(id);
        }
        return null;
    }

    public synchronized List<Person> getAllPeopleIncludingDeceased() {
        List<Person> list = new ArrayList<>(allPeople);
        if (demographicsSystem != null) {
            for (Person dp : demographicsSystem.getAllDeceasedPeople()) {
                if (!list.contains(dp)) {
                    list.add(dp);
                }
            }
        }
        return Collections.unmodifiableList(list);
    }

    public synchronized void simulateMonth() {
        processMonthlyTick();
    }

    public synchronized void addVillage(Village village) {
        if (village != null && !villages.contains(village)) {
            villages.add(village);
        }
    }

    public synchronized void addPerson(Person person) {
        if (person != null && !allPeople.contains(person)) {
            allPeople.add(person);
        }
    }

    public synchronized void addFamily(Family family) {
        if (family != null && !allFamilies.contains(family)) {
            allFamilies.add(family);
        }
    }

    public synchronized void addCouple(Couple couple) {
        if (couple != null && !allCouples.contains(couple)) {
            allCouples.add(couple);
        }
    }

    public synchronized void clearEntitiesForLoad() {
        allPeople.clear();
        allFamilies.clear();
        allCouples.clear();
        villages.clear();
    }

    public synchronized int getTotalLivingPopulation() {
        int sum = 0;
        for (Village v : villages) {
            sum += v.getLivingPopulationCount();
        }
        return sum;
    }

    public synchronized int getCrossVillageMarriagesThisYear() { return crossVillageMarriagesThisYear; }
    public synchronized void setCrossVillageMarriagesThisYear(int count) { this.crossVillageMarriagesThisYear = count; }
    public synchronized int getSettlementUpgradesThisYear() { return settlementUpgradesThisYear; }
    public synchronized void setSettlementUpgradesThisYear(int count) { this.settlementUpgradesThisYear = count; }

    public synchronized List<Village> getVillages() { return Collections.unmodifiableList(new ArrayList<>(villages)); }
    public synchronized List<Person> getAllPeople() { return Collections.unmodifiableList(new ArrayList<>(allPeople)); }
    public synchronized List<Family> getAllFamilies() { return Collections.unmodifiableList(new ArrayList<>(allFamilies)); }
    public synchronized List<Couple> getAllCouples() { return Collections.unmodifiableList(new ArrayList<>(allCouples)); }
    public synchronized List<AnnualReport> getAnnualReports() { return Collections.unmodifiableList(new ArrayList<>(annualReports)); }
    public synchronized void addAnnualReport(AnnualReport report) { if (report != null) this.annualReports.add(report); }
    public synchronized List<String> getEventHistory() { return Collections.unmodifiableList(new ArrayList<>(eventHistory)); }
    public Timeline getTimeline() { return timeline; }
    public SimulationConfig getConfig() { return config; }
    public AwakeningSystem getAwakeningSystem() { return awakeningSystem; }
    public ProgressionSystem getProgressionSystem() { return progressionSystem; }
    public ProgressionConfig getProgressionConfig() { return progressionConfig; }
    public DemographicsSystem getDemographicsSystem() { return demographicsSystem; }
    public OrganizationSystem getOrganizationSystem() { return organizationSystem; }
    public FactionConflictSystem getFactionConflictSystem() { return factionConflictSystem; }
    public AnomalyExplorationSystem getAnomalySystem() { return anomalySystem; }
    public MonsterEcologySystem getMonsterEcologySystem() { return monsterEcologySystem; }
    public EquipmentCraftingSystem getEquipmentCraftingSystem() { return equipmentCraftingSystem; }
    public ClanHeirloomSystem getClanHeirloomSystem() { return clanHeirloomSystem; }
    public BloodFeudSystem getBloodFeudSystem() { return bloodFeudSystem; }
    public boolean isRunning() { return running.get(); }
}
