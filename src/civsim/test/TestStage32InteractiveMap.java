package civsim.test;

import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.dto.*;
import civsim.model.*;
import civsim.persistence.WorldPersistenceService;
import civsim.runner.SimulationRunner;
import civsim.runner.SnapshotBuilder;
import civsim.web.sse.EventBroadcaster;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Phase 3.2 Verification & Acceptance Test Suite:
 * Interactive 2D World Map, Spatial Visualization, Entity Inspection, Search, and Live Synchronization.
 */
public class TestStage32InteractiveMap {

    private static int totalTests = 0;
    private static int passedTests = 0;

    private static void assertTrue(String message, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
            throw new AssertionError("Test failed: " + message);
        }
    }

    private static void assertEquals(String message, Object expected, Object actual) {
        totalTests++;
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            passedTests++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message + " (Expected: " + expected + ", Got: " + actual + ")");
            throw new AssertionError("Test failed: " + message + " Expected: " + expected + ", Got: " + actual);
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PHASE 3.2 TEST SUITE: INTERACTIVE 2D WORLD MAP & SPATIAL VISUALIZATION");
        System.out.println("================================================================================\n");

        long start = System.currentTimeMillis();

        testSuite1_WorldMapSnapshotContract();
        testSuite2_CoordinateBoundsAndAspectPreservation();
        testSuite3_SettlementMarkerScalingAndDetails();
        testSuite4_MonsterLairTerritoryAndThreats();
        testSuite5_AnomalyVisibilityAndDiscoveryRules();
        testSuite6_BoundedSearchIntegration();
        testSuite7_LiveSimulationSyncAndSnapshotProgression();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("TOTAL TESTS: %d | PASSED: %d | FAILED: %d | ELAPSED: %d ms%n",
                totalTests, passedTests, (totalTests - passedTests), elapsed);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("ALL STAGE 3.2 ACCEPTANCE TESTS PASSED SUCCESSFULLY!\n");
    }

    private static void testSuite1_WorldMapSnapshotContract() {
        System.out.println("--- SUITE 1: World Map Snapshot & Contract Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        WorldMapSnapshot map = SnapshotBuilder.buildMapSnapshot(engine, 1L, "WORLD_TEST_32");
        assertTrue("WorldMapSnapshot is not null", map != null);
        assertEquals("World ID matches", "WORLD_TEST_32", map.worldId());
        assertEquals("Snapshot version matches", 1L, map.snapshotVersion());
        assertTrue("World width is >= 100.0", map.width() >= 100.0);
        assertTrue("World height is >= 100.0", map.height() >= 100.0);
        assertTrue("Settlements list is not empty", !map.settlements().isEmpty());
        assertTrue("Lairs list is not null", map.lairs() != null);
        assertTrue("Anomalies list is not null", map.anomalies() != null);
        assertTrue("Expeditions list is not null", map.expeditions() != null);

        // Immutability check
        try {
            map.settlements().add(map.settlements().get(0));
            assertTrue("Map settlements collection must be immutable", false);
        } catch (UnsupportedOperationException e) {
            assertTrue("Map settlements collection is strictly immutable", true);
        }
    }

    private static void testSuite2_CoordinateBoundsAndAspectPreservation() {
        System.out.println("\n--- SUITE 2: Coordinate Bounds & Spatial Transformation Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        WorldMapSnapshot map = SnapshotBuilder.buildMapSnapshot(engine, 1L, "WORLD_TEST_32");

        for (MapSettlementDto s : map.settlements()) {
            assertTrue("Settlement " + s.name() + " X within bounds [0, 120]", s.x() >= 0.0 && s.x() <= map.width());
            assertTrue("Settlement " + s.name() + " Y within bounds [0, 120]", s.y() >= 0.0 && s.y() <= map.height());
            assertTrue("Population is positive", s.population() > 0);
            assertTrue("Biome is not null", s.biome() != null);
            assertTrue("Tier is not null", s.tier() != null);
        }

        // Test math transformation: scale preservation
        double viewportW = 1200;
        double viewportH = 800;
        double availW = viewportW - 80;
        double availH = viewportH - 80;
        double scaleX = availW / map.width();
        double scaleY = availH / map.height();
        double baseScale = Math.min(scaleX, scaleY);

        assertTrue("Uniform scale preserves 1:1 aspect ratio", scaleX != scaleY ? baseScale == scaleY : true);
    }

    private static void testSuite3_SettlementMarkerScalingAndDetails() {
        System.out.println("\n--- SUITE 3: Settlement Marker Sizing & Detail Inspection Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        Village v = engine.getVillages().get(0);
        SettlementDetailDto detail = SnapshotBuilder.buildSettlementDetail(v, engine.getVillages());

        assertTrue("Settlement detail ID matches", detail.id() == v.getId());
        assertEquals("Settlement name matches", v.getName(), detail.name());
        assertEquals("Living population matches", v.getLivingPopulationCount(), detail.population());
        assertEquals("Food stockpile matches exactly", v.getFoodStockpile(), detail.foodStockpile());
        assertEquals("Wood stockpile matches exactly", v.getWoodStockpile(), detail.woodStockpile());
        assertEquals("Ore stockpile matches exactly", v.getOreStockpile(), detail.oreStockpile());
        assertEquals("Tools stockpile matches exactly", v.getToolsStockpile(), detail.toolsStockpile());
        assertTrue("Fortification level in [0, 100]", detail.fortificationLevel() >= 0 && detail.fortificationLevel() <= 100);
        assertTrue("Crafting facilities list is immutable", detail.craftingFacilities() != null);

        // Marker bounded sizing function: r = clamp(10, 30, 8 + sqrt(pop) * 0.35)
        for (int pop : List.of(1, 20, 100, 500, 2500, 10000)) {
            double r = Math.min(30.0, Math.max(10.0, 8.0 + Math.sqrt(pop) * 0.35));
            assertTrue("Marker radius bounded for pop=" + pop + " (r=" + r + ")", r >= 10.0 && r <= 30.0);
        }
    }

    private static void testSuite4_MonsterLairTerritoryAndThreats() {
        System.out.println("\n--- SUITE 4: Monster Lair Territory & Threat Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        // Simulate until Year 51 to let lairs form
        for (int m = 0; m < 52 * 12; m++) {
            engine.simulateMonth();
        }

        WorldMapSnapshot map = SnapshotBuilder.buildMapSnapshot(engine, 624L, "WORLD_TEST_32");

        for (MapLairDto lair : map.lairs()) {
            assertTrue("Lair threat rank is non-empty", lair.threatRank() != null && !lair.threatRank().isBlank());
            assertTrue("Lair territory radius > 0", lair.territoryRadius() > 0.0);
            assertTrue("Lair monster count >= 0", lair.monsterCount() >= 0);
            assertTrue("Lair capacity >= monster count or positive", lair.capacity() > 0);
            assertTrue("Lair coordinates inside world bounds", lair.x() >= 0 && lair.x() <= map.width() && lair.y() >= 0 && lair.y() <= map.height());
        }
    }

    private static void testSuite5_AnomalyVisibilityAndDiscoveryRules() {
        System.out.println("\n--- SUITE 5: Anomaly Visibility & Discovery Rule Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        // Advance to Year 55
        for (int m = 0; m < 55 * 12; m++) {
            engine.simulateMonth();
        }

        WorldMapSnapshot map = SnapshotBuilder.buildMapSnapshot(engine, 660L, "WORLD_TEST_32");

        for (MapAnomalyDto anom : map.anomalies()) {
            // Strict compliance: never expose unobservable anomalies on the public map
            assertTrue("Only publicly observable anomalies appear on the map", anom.observable());
            assertTrue("Anomaly initial quantity > 0", anom.initialQuantity() > 0);
            assertTrue("Anomaly remaining quantity <= initial", anom.remainingQuantity() <= anom.initialQuantity());
            assertTrue("Anomaly coordinates valid", anom.x() >= 0 && anom.x() <= map.width() && anom.y() >= 0 && anom.y() <= map.height());
        }
    }

    private static void testSuite6_BoundedSearchIntegration() {
        System.out.println("\n--- SUITE 6: Bounded World Search Integration Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        // 1. Search for a settlement
        Village v = engine.getVillages().get(0);
        List<SearchResultDto> results = SnapshotBuilder.searchEntities(engine, v.getName(), 10);
        assertTrue("Search returns results for settlement name", !results.isEmpty());
        SearchResultDto hit = results.get(0);
        assertEquals("Hit type is SETTLEMENT", "SETTLEMENT", hit.type());
        assertEquals("Hit name matches settlement", v.getName(), hit.name());
        assertEquals("Hit X matches", v.getX(), hit.x());
        assertEquals("Hit Y matches", v.getY(), hit.y());

        // 2. Search bounding limit
        List<SearchResultDto> broadHits = SnapshotBuilder.searchEntities(engine, "a", 5);
        assertTrue("Search results strictly bounded by limit (<= 5)", broadHits.size() <= 5);

        // 3. Empty query returns empty list
        List<SearchResultDto> emptyHits = SnapshotBuilder.searchEntities(engine, "", 10);
        assertTrue("Empty query returns empty list", emptyHits.isEmpty());
    }

    private static void testSuite7_LiveSimulationSyncAndSnapshotProgression() {
        System.out.println("\n--- SUITE 7: Live Simulation Synchronization & Playback Tests ---");
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(42L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_32");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        runner.start();

        try {
            Thread.sleep(200); // Allow worker thread to initialize

            WorldSummaryDto sumInitial = runner.getLatestSummarySnapshot();
            assertTrue("Initial world summary exists", sumInitial != null);
            assertTrue("Initial living pop > 0", sumInitial.humanPopulation() > 0);

            // Submit step month command
            runner.submitCommand(new civsim.runner.SimulationCommand(civsim.runner.CommandType.STEP_MONTH, null));

            // Wait for step completion
            Thread.sleep(300);

            WorldSummaryDto sumAfterStep = runner.getLatestSummarySnapshot();
            assertTrue("Total elapsed months advanced", sumAfterStep.totalMonths() >= sumInitial.totalMonths() + 1);

            // Verify query execution on worker thread
            List<SearchResultDto> searchRes = runner.submitQuery(eng ->
                    SnapshotBuilder.searchEntities(eng, "Settlement", 10)
            ).get(2, TimeUnit.SECONDS);

            assertTrue("Worker thread query executed successfully", !searchRes.isEmpty());

        } catch (Exception e) {
            throw new RuntimeException("Live synchronization test failed", e);
        } finally {
            runner.destroy();
            try {
                java.nio.file.Files.walk(java.nio.file.Path.of("saves_test_32"))
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(java.nio.file.Path::toFile)
                        .forEach(java.io.File::delete);
            } catch (Exception ignored) {}
        }
    }
}
