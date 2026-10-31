package civsim.test;

import civsim.AnnualReport;
import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.model.*;
import civsim.spatial.Biome;
import civsim.systems.CombatEngine;
import civsim.systems.EquipmentCraftingSystem;
import civsim.systems.ClanHeirloomSystem;

import java.util.*;

/**
 * Critical Simulation Integrity Audit Test Suite.
 *
 * Verifies:
 * 1. Multi-Seed Famine & Population Accounting (Investigating and eliminating the 26-death bug)
 * 2. Human Extinction Lifecycle & Policy Enforcement (STOP_ON_HUMAN_EXTINCTION vs CONTINUE_NONHUMAN_WORLD)
 * 3. Probability & Seed Determinism (Strict determinism for identical seeds, divergence for different seeds, no unseeded RNG)
 * 4. Annual Reporting & Timeline Synchronization (All 12 months execute before annual report, mid-year casualties reflected)
 * 5. JVM Memory Management & Leak Elimination (Bounded LRU caches, bounded history, static cleanup)
 */
public class TestSimulationIntegrityAudit {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    private static void assertTrue(String message, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.printf("  [PASS] %s\n", message);
        } else {
            failedTests++;
            System.err.printf("  [FAIL] %s\n", message);
        }
    }

    private static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        totalTests++;
        boolean eq = Objects.equals(expected, actual);
        if (eq) {
            passedTests++;
            System.out.printf("  [PASS] %s (Expected: %s, Got: %s)\n", message, expected, actual);
        } else {
            failedTests++;
            System.err.printf("  [FAIL] %s (Expected: %s, Got: %s)\n", message, expected, actual);
        }
    }

    private static void assertNotNull(String message, Object actual) {
        assertTrue(message + " (not null)", actual != null);
    }

    private static void assertNull(String message, Object actual) {
        assertTrue(message + " (null)", actual == null);
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   CRITICAL SIMULATION INTEGRITY AUDIT TEST SUITE");
        System.out.println("================================================================================");

        try {
            testSuite1FamineRegression();
            testSuite2HumanExtinctionLifecycle();
            testSuite3ProbabilityAndDeterminism();
            testSuite4AnnualReportingAndTimelineSync();
            testSuite5HeapAndMemoryManagement();
        } catch (Exception e) {
            System.err.println("Fatal exception during audit test execution: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("TOTAL TESTS: %d | PASSED: %d | FAILED: %d\n", totalTests, passedTests, failedTests);
        System.out.println("--------------------------------------------------------------------------------");

        if (failedTests == 0) {
            System.out.println("ALL SIMULATION INTEGRITY AUDIT TESTS PASSED SUCCESSFULLY!\n");
        } else {
            System.err.printf("%d AUDIT TESTS FAILED!\n\n", failedTests);
            System.exit(1);
        }
    }

    // =========================================================================
    // SUITE 1: Multi-Seed Famine & Population Accounting
    // =========================================================================
    private static void testSuite1FamineRegression() {
        System.out.println("\n--- SUITE 1: Multi-Seed Famine & Population Accounting Tests ---");

        long[] testSeeds = {42L, 12345L, 9999L, 777L};
        List<Integer> year2DeathsInSettlementB = new ArrayList<>();

        for (long seed : testSeeds) {
            SimulationConfig config = SimulationConfig.createDefaultWithSeed(seed);
            SimulationEngine engine = new SimulationEngine(config);
            engine.initializeWorld();

            // Run for 3 full years (36 months)
            for (int m = 0; m < 36; m++) {
                engine.processMonthlyTick();
            }

            Village settlementB = null;
            for (Village v : engine.getVillages()) {
                if ("Settlement B".equals(v.getName())) {
                    settlementB = v;
                    break;
                }
            }
            assertNotNull("Settlement B exists for seed " + seed, settlementB);

            // Fetch Year 2 annual report
            AnnualReport repYear2 = null;
            for (AnnualReport rep : engine.getAnnualReports()) {
                if (rep.getYear() == 2) {
                    repYear2 = rep;
                    break;
                }
            }
            assertNotNull("Year 2 report exists for seed " + seed, repYear2);

            int villageBDeathsYear2 = 0;
            for (VillageAnnualStats vs : repYear2.getVillageStats()) {
                if ("Settlement B".equals(vs.getVillageName())) {
                    villageBDeathsYear2 = vs.getDeaths();
                    break;
                }
            }
            year2DeathsInSettlementB.add(villageBDeathsYear2);

            // CRITICAL AUDIT ASSERTION: Settlement B must NOT experience 26 deaths
            assertTrue("Seed " + seed + " Settlement B Year 2 deaths != 26 (Got: " + villageBDeathsYear2 + ")",
                    villageBDeathsYear2 != 26);

            // Settlement B must retain a healthy population (not wiped out)
            assertTrue("Seed " + seed + " Settlement B pop > 15 (Got: " + settlementB.getLivingPopulationCount() + ")",
                    settlementB.getLivingPopulationCount() > 15);

            // Food stockpile must not be negative
            assertTrue("Seed " + seed + " Settlement B food stockpile >= 0 (Got: " + settlementB.getFoodStockpile() + ")",
                    settlementB.getFoodStockpile() >= 0.0);

            // Total world population at Year 3 must be healthy
            assertTrue("Seed " + seed + " World population >= 50 at Year 3 (Got: " + engine.getTotalLivingPopulation() + ")",
                    engine.getTotalLivingPopulation() >= 50);

            engine.stop();
        }

        // Verify that deaths are not a static 26 or catastrophic constant
        System.out.printf("  Settlement B Year 2 deaths across seeds: %s\n", year2DeathsInSettlementB);
        assertTrue("Year 2 deaths remain realistic across seeds (All <= 10)",
                year2DeathsInSettlementB.stream().allMatch(d -> d <= 10));
    }

    // =========================================================================
    // SUITE 2: Human Extinction Lifecycle & Policy Tests
    // =========================================================================
    private static void testSuite2HumanExtinctionLifecycle() {
        System.out.println("\n--- SUITE 2: Human Extinction Lifecycle & Policy Tests ---");

        // Sub-test A: Policy STOP_ON_HUMAN_EXTINCTION (Default)
        {
            SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L)
                    .withExtinctionPolicy(ExtinctionPolicy.STOP_ON_HUMAN_EXTINCTION);

            SimulationEngine engine = new SimulationEngine(config);
            engine.initializeWorld();

            assertEquals("Initial lifecycle state is RUNNING", SimulationLifecycleState.RUNNING, engine.getLifecycleState());
            assertFalse("Initially not human extinct", engine.isHumanExtinct());

            // Manually kill all living humans to simulate complete extinction
            for (Person p : engine.getAllPeople()) {
                p.markDeceased(1, "EXTINCTION_TEST");
            }

            assertEquals("Living human count is 0", 0, engine.getAuthoritativeLivingHumanCount());

            // Process one tick
            engine.processMonthlyTick();

            assertEquals("Lifecycle transitioned to HUMAN_EXTINCT", SimulationLifecycleState.HUMAN_EXTINCT, engine.getLifecycleState());
            assertTrue("isHumanExtinct() is true", engine.isHumanExtinct());
            assertFalse("Simulation engine stopped automatically under STOP_ON_HUMAN_EXTINCTION", engine.isRunning());

            // Process subsequent tick: must be a no-op, no NPE, no infinite loop
            engine.processMonthlyTick();
            assertEquals("Lifecycle state remains HUMAN_EXTINCT on subsequent tick", SimulationLifecycleState.HUMAN_EXTINCT, engine.getLifecycleState());
        }

        // Sub-test B: Policy CONTINUE_NONHUMAN_WORLD
        {
            SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L)
                    .withExtinctionPolicy(ExtinctionPolicy.CONTINUE_NONHUMAN_WORLD);

            SimulationEngine engine = new SimulationEngine(config);
            engine.initializeWorld();

            // Kill all humans
            for (Person p : engine.getAllPeople()) {
                p.markDeceased(1, "EXTINCTION_TEST");
            }

            // Process tick: transitions to HUMAN_EXTINCT but does NOT call stop()
            engine.processMonthlyTick();

            assertEquals("Lifecycle transitioned to HUMAN_EXTINCT", SimulationLifecycleState.HUMAN_EXTINCT, engine.getLifecycleState());
            assertTrue("isHumanExtinct() is true", engine.isHumanExtinct());

            // Under CONTINUE_NONHUMAN_WORLD, timeline and non-human systems continue advancing
            int startYear = engine.getTimeline().getYear();
            long startMonths = engine.getTimeline().getTotalElapsedMonths();

            for (int m = 0; m < 24; m++) {
                engine.processMonthlyTick();
            }

            long elapsedMonths = engine.getTimeline().getTotalElapsedMonths() - startMonths;
            assertEquals("Timeline successfully advanced 24 months without humans", 24L, elapsedMonths);
            assertTrue("Timeline advanced 2 years", engine.getTimeline().getYear() >= startYear + 2);
            assertEquals("State remains HUMAN_EXTINCT", SimulationLifecycleState.HUMAN_EXTINCT, engine.getLifecycleState());

            engine.stop();
        }
    }

    // =========================================================================
    // SUITE 3: Probability & Seed Determinism Tests
    // =========================================================================
    private static void testSuite3ProbabilityAndDeterminism() {
        System.out.println("\n--- SUITE 3: Probability & Seed Determinism Tests ---");

        // Determinism Check: Two independent runs with identical seed 42L
        SimulationConfig configA = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engineA = new SimulationEngine(configA);
        engineA.initializeWorld();

        SimulationConfig configB = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engineB = new SimulationEngine(configB);
        engineB.initializeWorld();

        for (int m = 0; m < 60; m++) { // 5 years
            engineA.processMonthlyTick();
            engineB.processMonthlyTick();
        }

        assertEquals("Strict Determinism: Identical Year 5 living population",
                engineA.getTotalLivingPopulation(), engineB.getTotalLivingPopulation());
        assertEquals("Strict Determinism: Identical total recorded people count",
                engineA.getAllPeople().size(), engineB.getAllPeople().size());
        assertEquals("Strict Determinism: Identical annual reports count",
                engineA.getAnnualReports().size(), engineB.getAnnualReports().size());

        // Compare annual reports year by year
        for (int i = 0; i < engineA.getAnnualReports().size(); i++) {
            AnnualReport repA = engineA.getAnnualReports().get(i);
            AnnualReport repB = engineB.getAnnualReports().get(i);
            assertEquals("Year " + repA.getYear() + " births match exactly", repA.getBirthsDuringYear(), repB.getBirthsDuringYear());
            assertEquals("Year " + repA.getYear() + " deaths match exactly", repA.getDeathsDuringYear(), repB.getDeathsDuringYear());
        }

        // Divergence Check: Independent run with seed 9999L
        SimulationConfig configC = SimulationConfig.createDefaultWithSeed(9999L);
        SimulationEngine engineC = new SimulationEngine(configC);
        engineC.initializeWorld();

        for (int m = 0; m < 60; m++) {
            engineC.processMonthlyTick();
        }

        boolean populationsDiverged = engineA.getTotalLivingPopulation() != engineC.getTotalLivingPopulation();
        boolean peopleCountsDiverged = engineA.getAllPeople().size() != engineC.getAllPeople().size();
        assertTrue("Seed 42L and Seed 9999L diverge in population or people count",
                populationsDiverged || peopleCountsDiverged);

        engineA.stop();
        engineB.stop();
        engineC.stop();
    }

    // =========================================================================
    // SUITE 4: Annual Reporting & Timeline Synchronization Tests
    // =========================================================================
    private static void testSuite4AnnualReportingAndTimelineSync() {
        System.out.println("\n--- SUITE 4: Annual Reporting & Timeline Synchronization Tests ---");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        // Engine starts at Year 1, Month 0
        assertEquals("Timeline begins at Year 1", 1, engine.getTimeline().getYear());
        assertEquals("Timeline begins at Month 0", 0, engine.getTimeline().getMonth());
        assertEquals("Initial annual reports count is 1 (Year 0 founding report)", 1, engine.getAnnualReports().size());

        // Process months 0 through 10 (11 months)
        for (int m = 0; m < 11; m++) {
            engine.processMonthlyTick();
            assertEquals("Year remains 1 during months 0..10", 1, engine.getTimeline().getYear());
            assertEquals("No new annual report before year completion", 1, engine.getAnnualReports().size());
        }

        // Month is now 11 (December) of Year 1
        assertEquals("Timeline is at Month 11", 11, engine.getTimeline().getMonth());
        assertEquals("Timeline is still in Year 1", 1, engine.getTimeline().getYear());

        // Process Month 11 tick: this will complete Year 1, generate report for Year 1, then advance to Year 2 Month 0
        engine.processMonthlyTick();

        assertEquals("Exactly 2 annual reports generated (Year 0 and Year 1)", 2, engine.getAnnualReports().size());
        AnnualReport y1Report = engine.getAnnualReports().get(1);
        assertEquals("Annual report is for Year 1", 1, y1Report.getYear());
        assertEquals("Timeline has advanced to Year 2", 2, engine.getTimeline().getYear());
        assertEquals("Timeline has rolled to Month 0", 0, engine.getTimeline().getMonth());

        // Verify that mid-year deaths in Year 2 are accurately captured in Year 2 report
        // In Month 5 of Year 2, introduce a casualty in Settlement A
        for (int m = 0; m < 5; m++) {
            engine.processMonthlyTick();
        }
        assertEquals("Timeline is at Year 2 Month 5", 5, engine.getTimeline().getMonth());

        Village villageA = engine.getVillages().get(0);
        int preDeaths = villageA.getDeathsThisYear();
        Person victim = null;
        for (Person p : villageA.getResidents()) {
            if (p.isAlive()) {
                victim = p;
                break;
            }
        }
        assertNotNull("Victim exists", victim);
        engine.getDemographicsSystem().killPerson(victim, engine.getAllCouples(), 2, "ACCIDENTAL_HAZARD", config, (msg, flag) -> {});

        assertEquals("Village yearly deaths incremented immediately", preDeaths + 1, villageA.getDeathsThisYear());

        // Complete remaining months of Year 2 (months 5..11 -> 7 ticks)
        for (int m = 5; m < 11; m++) {
            engine.processMonthlyTick();
        }
        engine.processMonthlyTick(); // Month 11 tick generates Year 2 report

        assertEquals("Exactly 3 annual reports generated", 3, engine.getAnnualReports().size());
        AnnualReport y2Report = engine.getAnnualReports().get(2);
        assertEquals("Annual report is for Year 2", 2, y2Report.getYear());
        assertTrue("Year 2 report deaths reflects the mid-year casualty (Deaths >= 1)", y2Report.getDeathsDuringYear() >= 1);

        engine.stop();
    }

    // =========================================================================
    // SUITE 5: JVM Heap & Memory Management Tests
    // =========================================================================
    private static void testSuite5HeapAndMemoryManagement() {
        System.out.println("\n--- SUITE 5: JVM Heap & Memory Management Tests ---");

        // 1. Verify Bounded Collections
        assertTrue("CombatEngine.battleCache <= 100", CombatEngine.getBattleCacheSize() <= 100);

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        // Verify initial archives
        assertTrue("Monster dead archive initially <= 200",
                engine.getMonsterEcologySystem().getDeadMonstersArchiveSize() <= 200);

        // Run 25 simulated years to test memory retention
        System.out.println("  Simulating 25 years to verify memory bounded behavior...");
        for (int y = 0; y < 25; y++) {
            for (int m = 0; m < 12; m++) {
                engine.processMonthlyTick();
            }
        }

        // Verify collection bounds remain strictly enforced
        int battleCacheSize = CombatEngine.getBattleCacheSize();
        int deadMonstersArchiveSize = engine.getMonsterEcologySystem().getDeadMonstersArchiveSize();
        int eventHistorySize = engine.getEventHistory().size();

        System.out.printf("  Year 25 Telemetry: BattleCache=%d, DeadMonstersArchive=%d, EventHistory=%d\n",
                battleCacheSize, deadMonstersArchiveSize, eventHistorySize);

        assertTrue("CombatEngine.battleCache bounded <= 100 (Got: " + battleCacheSize + ")", battleCacheSize <= 100);
        assertTrue("MonsterEcologySystem.deadMonstersArchive bounded <= 200 (Got: " + deadMonstersArchiveSize + ")",
                deadMonstersArchiveSize <= 200);
        assertTrue("SimulationEngine.eventHistory bounded <= 10000 (Got: " + eventHistorySize + ")",
                eventHistorySize <= 10000);

        // 2. Static Leak Cleansing
        engine.stop();
        assertNull("EquipmentCraftingSystem active instance cleared on stop()", EquipmentCraftingSystem.getActiveInstance());
        assertNull("ClanHeirloomSystem active instance cleared on stop()", ClanHeirloomSystem.getActiveInstance());
        assertEquals("CombatEngine battle cache cleared on stop()", 0, CombatEngine.getBattleCacheSize());

        // 3. Heap Footprint Audit
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        long usedMemoryBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double usedMemoryMB = usedMemoryBytes / (1024.0 * 1024.0);
        System.out.printf("  Post-GC Used Heap: %.2f MB\n", usedMemoryMB);

        // Under 256MB is typical and excellent
        assertTrue("Heap consumption is bounded and healthy (< 256 MB)", usedMemoryMB < 256.0);
    }
}
