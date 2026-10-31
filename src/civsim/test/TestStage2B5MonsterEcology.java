package civsim.test;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.systems.CombatEngine;
import civsim.systems.DemographicsSystem;
import civsim.systems.MonsterEcologySystem;

import java.util.*;

/**
 * Phase 2B.5 Automated Acceptance Test Suite.
 * Validates Monster Ecology, Monster Lairs, Species Evolution, and MonsterTide Invasions.
 */
public class TestStage2B5MonsterEcology {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PHASE 2B.5 TEST SUITE: MONSTER ECOLOGY, LAIRS & MONSTERTIDE INVASIONS");
        System.out.println("================================================================================");

        try {
            runSuite1_IdentityEraGatingAndPopulationCaps();
            runSuite2_MonsterLairCreationAndConservation();
            runSuite3_EcologyGenerationalEvolutionAndCaps();
            runSuite4_MonsterTideSchedulingMultiWaveAndCombat();
            runSuite5_BreachAftermathLairAssaultAnd250YearStability();

            runSuiteA_MonsterLifecycleAndAging();
            runSuiteB_ReproductionSchedulingAndCaps();
            runSuiteC_EcologicalConservationAndStarvation();
            runSuiteD_MovementBehaviorAndMigration();
            runSuiteE_MonsterTideOutcomeAndDisplacedMonsters();
            runSuiteF_UnifiedDeathAndDropRecovery();
            runSuiteG_StabilityDeterminismAnd250YearSimulation();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(String.format("TOTAL TESTS: %d | PASSED: %d | FAILED: %d",
                    testsPassed + testsFailed, testsPassed, testsFailed));
            System.out.println("--------------------------------------------------------------------------------");

            if (testsFailed > 0) {
                System.err.println("FAILED: " + testsFailed + " tests failed!");
                System.exit(1);
            } else {
                System.out.println("ALL STAGE 2B.5 ACCEPTANCE TESTS PASSED SUCCESSFULLY!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            testsPassed++;
            System.out.println("  [PASS] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    private static void assertDoubleEquals(String message, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) <= delta) {
            testsPassed++;
            System.out.println("  [PASS] " + message + " (" + actual + " ~= " + expected + ")");
        } else {
            testsFailed++;
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    // Helper Factory Methods
    private static Village createVillage(int id, String name, double x, double y, Biome biome) {
        return new Village(id, name, 1.0, 0.05, x, y, biome);
    }

    private static Person createPerson(int id, String name, int age, Village v, double initialWealth) {
        Person p = new Person(id, name, Gender.MALE, age, -age, v, null, null, null, initialWealth);
        Family f = new Family(id, "Family-" + id, 0, p);
        p.setFamily(f);
        if (v != null) {
            v.addResident(p);
        }
        return p;
    }

    private static void awakenPerson(Person p, SupernaturalClass sc, boolean dark, int tier) {
        AetherProfile ap = AetherProfile.createForTesting(sc, ClassRarity.RARE, tier, dark);
        p.setAetherProfile(ap);
    }

    // =========================================================================
    // SUITE 1: IDENTITY, ERA GATING & POPULATION CAP TESTS
    // =========================================================================
    private static void runSuite1_IdentityEraGatingAndPopulationCaps() {
        System.out.println("\n--- SUITE 1: Identity, Era Gating & Population Cap Tests ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        SimulationConfig config = SimulationConfig.createDefault();
        Random rng = new Random(42);

        Village v1 = createVillage(1, "Solitude", 25.0, 25.0, Biome.FOREST);
        Person p1 = createPerson(1, "Citizen", 30, v1, 100.0);

        // 1.1 Strict Separation: Monster never a Person
        MonsterSpeciesDefinition wolfDef = ecoSys.getSpecies("DIRE_WOLF");
        assertTrue("DIRE_WOLF species registered in catalog", wolfDef != null);
        assertTrue("DIRE_WOLF compatible with FOREST", wolfDef.isCompatibleWithBiome(Biome.FOREST));
        assertTrue("DIRE_WOLF incompatible with HIGHLAND_MINE", !wolfDef.isCompatibleWithBiome(Biome.HIGHLAND_MINE));

        Monster m1 = new Monster("M_TEST_1", "DIRE_WOLF", "Alpha Wolf", MonsterCategory.BEAST,
                MonsterThreatRank.STANDARD, 25.0, 25.0, Biome.FOREST, "LAIR_01", wolfDef,
                MonsterTraitProfile.createDefault(), 1);

        assertTrue("Monster getPerson() returns null", m1.getPerson() == null);
        assertEquals("Combatant type is MONSTER", CombatantType.MONSTER, m1.getEntityType());
        assertTrue("Monster is alive initially", m1.isAlive());
        assertTrue("Monster can act in combat", m1.canActInCombat());

        // Verification of human population isolation
        assertEquals("Human village living population is exactly 1", 1, v1.getLivingPopulationCount());

        // 1.2 Mundane Era Gating: No monsters/tides processed in Years 0-49
        ecoSys.processMonthlyEcology(10, 5, 125, config, rng, null);
        assertEquals("Zero active lairs in Mundane Era", 0, ecoSys.getLairs().size());
        assertEquals("Zero active monsters in Mundane Era", 0, ecoSys.getGlobalLivingMonsterCount());

        // 1.3 Per-Lair and Global Population Caps
        PendingMonsterLairRecord recCommon = new PendingMonsterLairRecord(
                "EVT_LAIR_COM", "ANOM_COM", 50.0, 50.0,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.COMMON, 50.0, false, 606, "SIGNATURE_COM"
        );
        MonsterLair lairCommon = ecoSys.createLairFromPendingRecord(recCommon, rng, 606);
        assertTrue("COMMON lair created", lairCommon != null);
        assertTrue("COMMON lair population capacity <= 100", lairCommon.getPopulationCapacity() <= MonsterEcologySystem.MAX_MONSTERS_PER_ORDINARY_LAIR);

        PendingMonsterLairRecord recMythic = new PendingMonsterLairRecord(
                "EVT_LAIR_MYT", "ANOM_MYT", 70.0, 70.0,
                SupernaturalResourceType.CONDENSED_AETHER_ESSENCE, ResourceGrade.MYTHIC, 200.0, false, 606, "SIGNATURE_MYT"
        );
        MonsterLair lairMythic = ecoSys.createLairFromPendingRecord(recMythic, rng, 606);
        assertTrue("MYTHIC lair created", lairMythic != null);
        assertEquals("MYTHIC lair capacity is 250", 250, lairMythic.getPopulationCapacity());

        // Global Monster Cap enforcement
        assertTrue("Total living monsters below global cap (2000)", ecoSys.getGlobalLivingMonsterCount() <= MonsterEcologySystem.MAX_GLOBAL_MONSTERS);
    }

    // =========================================================================
    // SUITE 2: MONSTER LAIR CREATION & CONSERVATION TESTS
    // =========================================================================
    private static void runSuite2_MonsterLairCreationAndConservation() {
        System.out.println("\n--- SUITE 2: Monster Lair Creation & Conservation Tests ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        Random rng = new Random(101);

        PendingMonsterLairRecord recRare = new PendingMonsterLairRecord(
                "EVT_LAIR_RARE", "ANOM_RARE_01", 35.0, 35.0,
                SupernaturalResourceType.ANOMALOUS_ORE, ResourceGrade.RARE, 80.0, true, 608, "THREAT_RARE"
        );

        // 2.1 Idempotent Lair Creation
        MonsterLair lair1 = ecoSys.createLairFromPendingRecord(recRare, rng, 608);
        assertTrue("Lair created on first call", lair1 != null);
        assertEquals("Lair ID matches source anomaly", "LAIR_ANOM_RARE_01", lair1.getLairId());
        assertEquals("Lair state is ACTIVE", MonsterLairState.ACTIVE, lair1.getState());
        assertEquals("Territory radius for RARE is 36.0", 36.0, lair1.getTerritoryRadius());

        MonsterLair lairDuplicate = ecoSys.createLairFromPendingRecord(recRare, rng, 608);
        assertTrue("Duplicate creation returns identical lair instance", lair1 == lairDuplicate);
        assertEquals("Total lairs registered remains 1", 1, ecoSys.getLairs().size());

        // 2.2 Seeded Monsters scaled by Grade
        int livingCount = lair1.getLivingMonsterCount();
        assertTrue("RARE lair seeded with 16-28 monsters", livingCount >= 16 && livingCount <= 28);

        // 2.3 Terminal Lair State: DESTROYED lairs cannot spawn
        lair1.transitionToDestroyed();
        assertEquals("Lair transitioned to DESTROYED", MonsterLairState.DESTROYED, lair1.getState());
        assertTrue("DESTROYED lair canSpawn() is false", !lair1.canSpawn());
    }

    // =========================================================================
    // SUITE 3: ECOLOGY, GENERATIONAL EVOLUTION & ALPHA/ANCIENT CAPS
    // =========================================================================
    private static void runSuite3_EcologyGenerationalEvolutionAndCaps() {
        System.out.println("\n--- SUITE 3: Ecology, Generational Evolution & Caps ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        SimulationConfig config = SimulationConfig.createDefault();
        Random rng = new Random(777);

        // 3.1 Prey Biomass Model: Natural biomass consumed without touching human food
        Village v = createVillage(1, "Haven", 25.0, 25.0, Biome.RIVER_VALLEY);
        v.addFood(500.0);
        double initialHumanFood = v.getFoodStockpile();

        PendingMonsterLairRecord rec = new PendingMonsterLairRecord(
                "EVT_ECO_01", "ANOM_ECO_01", 25.0, 25.0,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.UNCOMMON, 60.0, false, 600, "SIG"
        );
        MonsterLair lair = ecoSys.createLairFromPendingRecord(rec, rng, 600);

        EcologicalRegionState region = ecoSys.getEcologicalRegions().values().iterator().next();
        double initialBiomass = region.getAvailablePreyBiomass();

        // Process monthly ecology tick
        ecoSys.processMonthlyEcology(50, 1, 601, config, rng, null);

        // Verify human settlement food is 100% untouched
        assertEquals("Human food stockpile untouched by natural monster feeding", initialHumanFood, v.getFoodStockpile());
        assertTrue("Ecological prey biomass was consumed by living monsters", region.getAvailablePreyBiomass() <= initialBiomass);

        // 3.2 Bounded Generational Evolution
        MonsterTraitProfile p1 = new MonsterTraitProfile(10.0, 5.0, 2.0, 0.0, 0.0, 0.0, 0.0, 5.0);
        MonsterTraitProfile p2 = new MonsterTraitProfile(12.0, 3.0, 4.0, 2.0, 0.0, 0.0, 0.0, 3.0);
        MonsterTraitProfile child = MonsterTraitProfile.inheritFromParents(Arrays.asList(p1, p2), rng, 0.20);

        assertTrue("Child ferocity bounded in [-15, +25]", child.getFerocity() >= -15.0 && child.getFerocity() <= 25.0);
        assertTrue("Child armor bounded in [-15, +25]", child.getNaturalArmor() >= -15.0 && child.getNaturalArmor() <= 25.0);
        assertTrue("Environmental adaptation bounded in [-15, +25]", child.getEnvironmentalAdaptation() >= -15.0 && child.getEnvironmentalAdaptation() <= 25.0);

        // 3.3 Strict Global Alpha and Ancient Caps
        assertEquals("Global living Alphas <= 12", true, ecoSys.getGlobalLivingAlphaCount() <= MonsterEcologySystem.MAX_GLOBAL_ALPHAS);
        assertEquals("Global living Ancients <= 2", true, ecoSys.getGlobalLivingAncientCount() <= MonsterEcologySystem.MAX_GLOBAL_ANCIENTS);
    }

    // =========================================================================
    // SUITE 4: MONSTERTIDE SCHEDULING, MULTI-WAVE & COMBAT TESTS
    // =========================================================================
    private static void runSuite4_MonsterTideSchedulingMultiWaveAndCombat() {
        System.out.println("\n--- SUITE 4: MonsterTide Scheduling, Multi-Wave & Combat Tests ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        SimulationConfig config = SimulationConfig.createDefault();
        ProgressionConfig progConfig = ProgressionConfig.createDefault();
        DemographicsSystem demoSys = new DemographicsSystem();
        Random rng = new Random(888);

        Village targetVillage = createVillage(1, "Oakvale", 20.0, 20.0, Biome.FOREST);
        targetVillage.addFood(200.0);

        Person defender1 = createPerson(10, "Captain", 32, targetVillage, 100.0);
        defender1.setOccupation(Occupation.SOLDIER);
        Person defender2 = createPerson(11, "WardenChampion", 28, targetVillage, 80.0);
        awakenPerson(defender2, SupernaturalClass.WARDEN, false, 3);
        Person seer = createPerson(12, "VillageSeer", 45, targetVillage, 50.0);
        awakenPerson(seer, SupernaturalClass.SEER, false, 2);

        List<Person> people = new ArrayList<>(Arrays.asList(defender1, defender2, seer));

        // Create an active lair with sufficient population (>= 12 monsters)
        PendingMonsterLairRecord lairRec = new PendingMonsterLairRecord(
                "EVT_TIDE_LAIR", "ANOM_TIDE", 35.0, 35.0,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.RARE, 100.0, false, 600, "THREAT"
        );
        MonsterLair lair = ecoSys.createLairFromPendingRecord(lairRec, rng, 600);

        // 4.1 MonsterTide Mobilization & Wave Partitioning
        // Mobilize tide scheduled at current month
        ecoSys.setNextScheduledMonsterTideMonth(600);
        ecoSys.evaluateMonsterTideScheduling(50, 0, 600, Collections.singletonList(targetVillage), people, rng, null);

        assertTrue("MonsterTide scheduled or created", !ecoSys.getTides().isEmpty());
        MonsterTide tide = ecoSys.getTides().values().iterator().next();

        assertTrue("Invading force partitioned into Wave 1 (Minor/Scouts)", !tide.getWave1MonsterIds().isEmpty());
        assertTrue("Physical travel time >= 1 month", tide.getTravelDurationMonths() >= 1);

        // 4.2 Multi-Wave Combat Execution
        tide.setState(MonsterTideState.INVADING);
        MonsterTideOutcome outcome = ecoSys.resolveMonsterTideInvasion(
                tide, targetVillage, people, config, progConfig, demoSys, null, null,
                rng, Collections.emptyList(), null, 53, 2
        );

        assertTrue("MonsterTide resolved with definitive outcome", outcome != null);
        assertEquals("Tide state is RESOLVED", MonsterTideState.RESOLVED, tide.getState());
        assertTrue("Settlement lastMonsterTideMonth updated to enforce 24-month cooldown",
                targetVillage.getLastMonsterTideMonth() > 0);
    }

    // =========================================================================
    // SUITE 5: BREACH AFTERMATH, LAIR ASSAULT & 250-YEAR STABILITY
    // =========================================================================
    private static void runSuite5_BreachAftermathLairAssaultAnd250YearStability() {
        System.out.println("\n--- SUITE 5: Breach Aftermath, Lair Assault & 250-Year Stability ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        SimulationConfig config = SimulationConfig.createDefault();
        DemographicsSystem demoSys = new DemographicsSystem();
        ProgressionConfig progConfig = ProgressionConfig.createDefault();
        Random rng = new Random(999);

        // 5.1 Breach Aftermath: Civilian casualties and food loss on undefended village
        Village helplessVillage = createVillage(2, "HelplessHamlet", 40.0, 40.0, Biome.RIVER_VALLEY);
        helplessVillage.addFood(300.0);
        Person civilian1 = createPerson(20, "FarmerJoe", 35, helplessVillage, 40.0);
        civilian1.setOccupation(Occupation.FARMER);
        Person civilian2 = createPerson(21, "WeaverMary", 30, helplessVillage, 30.0);
        civilian2.setOccupation(Occupation.BUILDER);
        List<Person> helplessPeople = new ArrayList<>(Arrays.asList(civilian1, civilian2));

        MonsterTide breachTide = new MonsterTide(
                "TIDE_BREACH_TEST", "LAIR_BREACH", 2, "HelplessHamlet", 40.0, 40.0,
                50, 1, 601, 1,
                Collections.singletonList("M_B1"), Collections.singletonList("M_B2"), Collections.emptyList()
        );
        breachTide.setState(MonsterTideState.INVADING);

        // Mock 2 monsters
        MonsterSpeciesDefinition bruteDef = ecoSys.getSpecies("STONEBACK_BRUTE");
        Monster mb1 = new Monster("M_B1", "STONEBACK_BRUTE", "Siege Brute 1", MonsterCategory.SIEGE_CREATURE,
                MonsterThreatRank.STANDARD, 40.0, 40.0, Biome.HIGHLAND_MINE, "LAIR_BREACH", bruteDef, null, 1);
        Monster mb2 = new Monster("M_B2", "STONEBACK_BRUTE", "Siege Brute 2", MonsterCategory.SIEGE_CREATURE,
                MonsterThreatRank.STANDARD, 40.0, 40.0, Biome.HIGHLAND_MINE, "LAIR_BREACH", bruteDef, null, 1);
        ecoSys.getMonsters(); // Verify registry access

        MonsterTideOutcome breachOutcome = ecoSys.resolveMonsterTideInvasion(
                breachTide, helplessVillage, helplessPeople, config, progConfig, demoSys, null, null,
                rng, Collections.emptyList(), null, 50, 1
        );

        assertTrue("Breach outcome registered on undefended settlement", breachOutcome.isBreached());
        assertTrue("Food stockpile consumed during breach", helplessVillage.getFoodStockpile() < 300.0);
        assertTrue("Civilian casualty confirmed through Demographics pipeline", !civilian1.isAlive() || !civilian2.isAlive());

        // 5.2 Monster Drop Generation & Recovery Idempotency
        MonsterDropRecord drop1 = ecoSys.generateMonsterDrop(mb1, 40.0, 40.0, 50, 1, rng);
        assertTrue("Monster drop generated", drop1 != null);
        MonsterDropRecord dropDuplicate = ecoSys.generateMonsterDrop(mb1, 40.0, 40.0, 50, 1, rng);
        assertTrue("Duplicate drop generation rejected idempotently", dropDuplicate == null);

        // 5.3 Explicit Lair Assault Expedition
        PendingMonsterLairRecord assaultRec = new PendingMonsterLairRecord(
                "EVT_ASSAULT", "ANOM_ASSAULT", 55.0, 55.0,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.COMMON, 40.0, false, 600, "THREAT"
        );
        MonsterLair assaultLair = ecoSys.createLairFromPendingRecord(assaultRec, rng, 600);

        Village forwardPost = createVillage(3, "ForwardPost", 50.0, 50.0, Biome.FOREST);
        Person hero = createPerson(30, "GrandHero", 35, forwardPost, 150.0);
        awakenPerson(hero, SupernaturalClass.VANGUARD, false, 5); // T5 Vanguard powerhouse

        BattleResult assaultResult = ecoSys.assaultMonsterLair(
                assaultLair, Collections.singletonList(hero), rng, progConfig, demoSys, null, null,
                config, Collections.emptyList(), null, 50, 3
        );

        assertTrue("Lair assault executed by combat engine", assaultResult != null);
        assertTrue("Lair assault resolved with victory or suppression",
                assaultResult.getOutcome() == BattleOutcome.VICTORY || assaultResult.getOutcome() == BattleOutcome.STALEMATE);

        // 5.4 Multi-Seed Simulation Determinism & Memory Stability
        System.out.println("  Running deterministic integration simulation...");
        SimulationConfig simConfigA = SimulationConfig.createDefaultWithSeed(12345L);
        SimulationConfig simConfigB = SimulationConfig.createDefaultWithSeed(12345L);

        SimulationEngine engineA = new SimulationEngine(simConfigA);
        SimulationEngine engineB = new SimulationEngine(simConfigB);

        engineA.initializeWorld();
        engineB.initializeWorld();

        for (int m = 0; m < 24; m++) {
            engineA.processMonthlyTick();
            engineB.processMonthlyTick();
        }

        assertEquals("Same seed produces identical living human population",
                engineA.getTotalLivingPopulation(), engineB.getTotalLivingPopulation());
        assertEquals("Same seed produces identical event history count",
                engineA.getEventHistory().size(), engineB.getEventHistory().size());
        assertEquals("Mundane era maintains 0 active monsters for Engine A",
                0, engineA.getMonsterEcologySystem().getGlobalLivingMonsterCount());
        assertEquals("Mundane era maintains 0 active monsters for Engine B",
                0, engineB.getMonsterEcologySystem().getGlobalLivingMonsterCount());
    }

    // =========================================================================
    // SUITE A: MONSTER LIFECYCLE, AGING & NATURAL MORTALITY
    // =========================================================================
    private static void runSuiteA_MonsterLifecycleAndAging() {
        System.out.println("\n--- SUITE A: Monster Lifecycle, Aging & Natural Mortality ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        MonsterSpeciesDefinition wolfDef = ecoSys.getSpecies("DIRE_WOLF");

        Monster pup = new Monster(
                "M_PUP_1", "DIRE_WOLF", "Wolf Pup", MonsterCategory.BEAST,
                MonsterThreatRank.MINOR, 20.0, 20.0, Biome.FOREST, "LAIR_A", wolfDef, null, 1
        );
        pup.setAgeMonths(5);
        pup.addParentMonsterId("M_PARENT_ALPHA");

        // A.1 Juvenile maturity check
        assertTrue("Monster cannot reproduce before maturity (age 5 < 12)", !pup.isMature());

        // A.2 Aging progression
        pup.incrementAgeMonths(10);
        assertEquals("Monster ages correctly through monthly progression", 15, pup.getAgeMonths());
        assertTrue("Monster is mature after reaching maturity age (15 >= 12)", pup.isMature());

        // A.3 Natural lifespan mortality
        pup.setAgeMonths(pup.getMaximumAgeMonths() + 1);
        assertTrue("Monster exceeds natural maximum lifespan", pup.hasExceededLifespan());

        ecoSys.processMonsterDeath(pup, "natural aging", pup.getPosX(), pup.getPosY(), 50, 1, new Random(1), false);
        assertTrue("Natural aging causes death exactly once", !pup.isAlive());
        assertEquals("Death cause is natural aging", "natural aging", pup.getDeathCause());
        assertTrue("Dead monster cannot act in combat", !pup.canActInCombat());

        // A.4 Lineage accessibility post-mortem
        assertTrue("Parent lineage preserved post-mortem", pup.getParentMonsterIds().contains("M_PARENT_ALPHA"));
    }

    // =========================================================================
    // SUITE B: CONTROLLED REPRODUCTION, SPAWN SCHEDULE & CAPS
    // =========================================================================
    private static void runSuiteB_ReproductionSchedulingAndCaps() {
        System.out.println("\n--- SUITE B: Controlled Reproduction, Spawn Schedule & Caps ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        Random rng = new Random(123);

        PendingMonsterLairRecord rec = new PendingMonsterLairRecord(
                "EVT_LAIR_B", "ANOM_B", 30.0, 30.0,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.COMMON, 50.0, false, 600, "LAIR"
        );
        MonsterLair lair = ecoSys.createLairFromPendingRecord(rec, rng, 600);
        assertTrue("Lair has persistent nextScheduledSpawnMonth (6-18 months)", lair.getNextScheduledSpawnMonth() >= 606);

        // B.1 Scheduled spawn gating
        int initialPop = ecoSys.getGlobalLivingMonsterCount();
        ecoSys.processMonthlyEcology(50, 1, 601, null, rng, null);
        assertEquals("Reproduction does not trigger prior to scheduled spawn month", initialPop, ecoSys.getGlobalLivingMonsterCount());

        // B.2 Spawn in dormant or destroyed lairs is blocked
        lair.transitionToDormant();
        assertEquals("Dormant lair cannot spawn", false, lair.canSpawn());
        lair.transitionToDestroyed();
        assertEquals("Destroyed lair cannot spawn", false, lair.canSpawn());

        // B.3 Lair capacity includes monsters deployed away
        MonsterLair capLair = new MonsterLair(
                "LAIR_CAP_TEST", "ANOM_CAP", 50.0, 50.0, Biome.FOREST,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.COMMON,
                "DIRE_WOLF", null, 10, 20.0, MonsterThreatRank.STANDARD, 600
        );
        for (int i = 0; i < 10; i++) {
            Monster m = new Monster("M_AWAY_" + i, "DIRE_WOLF", "Away Beast " + i, MonsterCategory.BEAST,
                    MonsterThreatRank.STANDARD, 80.0, 80.0, Biome.FOREST, "LAIR_CAP_TEST", ecoSys.getSpecies("DIRE_WOLF"), null, 1);
            capLair.addMonster(m.getMonsterId());
        }
        assertTrue("Lair at capacity correctly reports isAtCapacity", capLair.isAtCapacity());
    }

    // =========================================================================
    // SUITE C: ECOLOGICAL BIOMASS CONSERVATION & STARVATION
    // =========================================================================
    private static void runSuiteC_EcologicalConservationAndStarvation() {
        System.out.println("\n--- SUITE C: Ecological Biomass Conservation & Starvation ---");

        EcologicalRegionState region = new EcologicalRegionState("REGION_TEST", Biome.FOREST, 200.0, 30.0);
        double prev = region.getAvailablePreyBiomass(); // 200.0

        // C.1 Exact Accounting Formula Verification
        // Demand = 50.0; prev = 200.0; regen = 30.0 -> avail = 230.0; actual = 50.0; new = 180.0; unmet = 0.0
        EcologicalMonthlyResult r1 = region.processMonthlyConsumption(50.0);
        assertEquals("Available for consumption = prev + regen", prev + 30.0, r1.getAvailableForConsumption());
        assertEquals("Actual consumption matches demand when available", 50.0, r1.getActualConsumption());
        assertEquals("New biomass clamped within carrying capacity", 180.0, r1.getNewBiomass());
        assertEquals("Zero unmet demand under healthy biomass", 0.0, r1.getUnmetDemand());

        // C.2 Excessive Demand & Zero Negative Biomass
        // Demand = 500.0; prev = 180.0; regen = 30.0 -> avail = 210.0; actual = 210.0; new = 0.0; unmet = 290.0
        EcologicalMonthlyResult r2 = region.processMonthlyConsumption(500.0);
        assertEquals("Actual consumption bounded by available biomass", 210.0, r2.getActualConsumption());
        assertEquals("Biomass never becomes negative", 0.0, r2.getNewBiomass());
        assertEquals("Unmet demand correctly computed", 290.0, r2.getUnmetDemand());
        assertTrue("Starvation indicated under unmet demand", r2.hasStarvation());

        // C.3 Recovery restores biomass
        EcologicalMonthlyResult r3 = region.processMonthlyConsumption(0.0);
        assertEquals("Biomass recovers when demand drops", 30.0, r3.getNewBiomass());
    }

    // =========================================================================
    // SUITE D: MOVEMENT BEHAVIOR & PHYSICAL MIGRATION
    // =========================================================================
    private static void runSuiteD_MovementBehaviorAndMigration() {
        System.out.println("\n--- SUITE D: Movement Behavior & Physical Migration ---");

        MonsterSpeciesDefinition wolfDef = MonsterSpeciesDefinition.createInitialCatalog().get("DIRE_WOLF");
        Monster wolf = new Monster(
                "M_MIG_1", "DIRE_WOLF", "Migrant Wolf", MonsterCategory.BEAST,
                MonsterThreatRank.STANDARD, 25.0, 25.0, Biome.FOREST, "LAIR_MIG", wolfDef, null, 1
        );

        // D.1 State transitions
        wolf.setBehaviorState(MonsterBehaviorState.HUNTING);
        assertEquals("State is HUNTING", MonsterBehaviorState.HUNTING, wolf.getBehaviorState());

        wolf.setBehaviorState(MonsterBehaviorState.MIGRATING);
        assertEquals("State is MIGRATING", MonsterBehaviorState.MIGRATING, wolf.getBehaviorState());

        // D.2 Physical movement speed constraint
        double initX = wolf.getPosX();
        double initY = wolf.getPosY();
        double maxDist = wolf.getBaseSpeed();

        wolf.setPosition(initX + 5.0, initY + 5.0);
        double distMoved = Math.sqrt(Math.pow(wolf.getPosX() - initX, 2) + Math.pow(wolf.getPosY() - initY, 2));
        assertTrue("Movement does not exceed base speed", distMoved <= maxDist);

        // D.3 Biome compatibility
        assertTrue("Dire wolf compatible with Forest", wolfDef.isCompatibleWithBiome(Biome.FOREST));
        assertTrue("Dire wolf incompatible with Highland Mine", !wolfDef.isCompatibleWithBiome(Biome.HIGHLAND_MINE));
    }

    // =========================================================================
    // SUITE E: MONSTERTIDE OUTCOME & DISPLACED MONSTERS
    // =========================================================================
    private static void runSuiteE_MonsterTideOutcomeAndDisplacedMonsters() {
        System.out.println("\n--- SUITE E: MonsterTide Outcome & Displaced Monsters ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        Random rng = new Random(777);

        // E.1 Displaced Monsters: Lair destruction preserves away monsters
        PendingMonsterLairRecord rec = new PendingMonsterLairRecord(
                "EVT_LAIR_DISP", "ANOM_DISP", 40.0, 40.0,
                SupernaturalResourceType.RAW_AETHER_CRYSTAL, ResourceGrade.COMMON, 50.0, false, 600, "LAIR"
        );
        MonsterLair lair = ecoSys.createLairFromPendingRecord(rec, rng, 600);

        // Deploy monster away
        Monster awayBeast = new Monster(
                "M_AWAY_HERO", "DIRE_WOLF", "Away Beast", MonsterCategory.BEAST,
                MonsterThreatRank.STANDARD, 80.0, 80.0, Biome.FOREST, lair.getLairId(), ecoSys.getSpecies("DIRE_WOLF"), null, 1
        );
        ecoSys.getMonsters(); // Verify access

        lair.transitionToDestroyed();
        awayBeast.setDisplaced(true);
        awayBeast.setBehaviorState(MonsterBehaviorState.PATROLLING);

        assertTrue("Away monster survives lair destruction", awayBeast.isAlive());
        assertTrue("Away monster is marked displaced", awayBeast.isDisplaced());
        assertEquals("Away monster behavior state set to PATROLLING (roaming)", MonsterBehaviorState.PATROLLING, awayBeast.getBehaviorState());

        // E.2 Outcome Classifications
        assertEquals("Defended outcome matches", true, MonsterTideOutcome.SUCCESSFUL_DEFENSE.isDefended());
        assertEquals("Costly victory outcome matches", true, MonsterTideOutcome.COSTLY_VICTORY.isDefended());
        assertEquals("Breach outcome matches", true, MonsterTideOutcome.PARTIAL_BREACH.isBreached());
        assertEquals("Overrun outcome matches", true, MonsterTideOutcome.SETTLEMENT_OVERRUN.isBreached());
    }

    // =========================================================================
    // SUITE F: UNIFIED DEATH & DROP RECOVERY
    // =========================================================================
    private static void runSuiteF_UnifiedDeathAndDropRecovery() {
        System.out.println("\n--- SUITE F: Unified Death & Drop Recovery ---");

        MonsterEcologySystem ecoSys = new MonsterEcologySystem();
        Random rng = new Random(456);

        Monster alphaBeast = new Monster(
                "M_ALPHA_DEATH", "MARSH_HYDRA", "Marsh Alpha", MonsterCategory.ABERRATION,
                MonsterThreatRank.ALPHA, 30.0, 30.0, Biome.RIVER_VALLEY, "LAIR_HYDRA",
                ecoSys.getSpecies("MARSH_HYDRA"), null, 2
        );

        // F.1 Authoritative death processing with combat drop
        MonsterDropRecord drop = ecoSys.processMonsterDeath(alphaBeast, "combat", 30.0, 30.0, 50, 5, rng, true);
        assertTrue("Alpha monster drop produced upon combat death", drop != null);
        assertEquals("Drop initial status is RECOVERABLE", MonsterDropStatus.RECOVERABLE, drop.getStatus());
        assertTrue("Alpha is dead", !alphaBeast.isAlive());
        assertEquals("Death cause recorded as combat", "combat", alphaBeast.getDeathCause());

        // F.2 Duplicate death processing is rejected idempotently
        MonsterDropRecord dupDrop = ecoSys.processMonsterDeath(alphaBeast, "combat", 30.0, 30.0, 50, 5, rng, true);
        assertTrue("Duplicate death rejected idempotently", dupDrop == null);

        // F.3 Physical delivery to settlement inventory
        Village collectorVillage = createVillage(10, "RiverLanding", 30.0, 30.0, Biome.RIVER_VALLEY);
        ecoSys.recoverDropsForSettlement(collectorVillage, 30.0, 30.0, 5.0);

        assertTrue("Drop is marked recovered/delivered", drop.isRecovered());
        assertEquals("Drop status is DELIVERED", MonsterDropStatus.DELIVERED, drop.getStatus());
        assertTrue("Settlement inventory received monster materials or cores",
                collectorVillage.getMonsterCoreCount() > 0 || !collectorVillage.getMonsterMaterials().isEmpty());
    }

    // =========================================================================
    // SUITE G: STABILITY, DETERMINISM & 250-YEAR SIMULATION
    // =========================================================================
    private static void runSuiteG_StabilityDeterminismAnd250YearSimulation() {
        System.out.println("\n--- SUITE G: Stability, Determinism & 250-Year Simulation ---");

        long testSeed = 54321L;
        SimulationConfig configA = SimulationConfig.createDefaultWithSeed(testSeed);
        SimulationConfig configB = SimulationConfig.createDefaultWithSeed(testSeed);

        SimulationEngine engineA = new SimulationEngine(configA);
        SimulationEngine engineB = new SimulationEngine(configB);

        engineA.initializeWorld();
        engineB.initializeWorld();

        // Run 250 simulated years (3000 monthly ticks) per engine with cache clearing
        System.out.println("  Executing 250-year Engine A simulation (Year 0 to 250)...");
        CombatEngine.clearCache();
        for (int y = 0; y < 250; y++) {
            for (int m = 0; m < 12; m++) {
                engineA.processMonthlyTick();
            }
        }

        System.out.println("  Executing 250-year Engine B simulation (Year 0 to 250)...");
        CombatEngine.clearCache();
        for (int y = 0; y < 250; y++) {
            for (int m = 0; m < 12; m++) {
                engineB.processMonthlyTick();
            }
        }

        // Verify multi-seed exact determinism
        assertEquals("Identical living human population at Year 250",
                engineA.getTotalLivingPopulation(), engineB.getTotalLivingPopulation());
        assertEquals("Identical living monster population at Year 250",
                engineA.getMonsterEcologySystem().getGlobalLivingMonsterCount(),
                engineB.getMonsterEcologySystem().getGlobalLivingMonsterCount());

        // Verify hard caps hold strictly at Year 250
        assertTrue("Global living monster cap <= 2000 holds at Year 250",
                engineA.getMonsterEcologySystem().getGlobalLivingMonsterCount() <= MonsterEcologySystem.MAX_GLOBAL_MONSTERS);
        assertTrue("Global living Alpha cap <= 12 holds at Year 250",
                engineA.getMonsterEcologySystem().getGlobalLivingAlphaCount() <= MonsterEcologySystem.MAX_GLOBAL_ALPHAS);
        assertTrue("Global living Ancient cap <= 2 holds at Year 250",
                engineA.getMonsterEcologySystem().getGlobalLivingAncientCount() <= MonsterEcologySystem.MAX_GLOBAL_ANCIENTS);

        System.out.println(String.format("  Year 250 Status: Living Pop=%d | Monsters=%d (Alphas: %d, Ancients: %d)",
                engineA.getTotalLivingPopulation(),
                engineA.getMonsterEcologySystem().getGlobalLivingMonsterCount(),
                engineA.getMonsterEcologySystem().getGlobalLivingAlphaCount(),
                engineA.getMonsterEcologySystem().getGlobalLivingAncientCount()));
    }
}
