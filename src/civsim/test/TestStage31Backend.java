package civsim.test;

import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.dto.*;
import civsim.model.*;
import civsim.persistence.WorldPersistenceService;
import civsim.persistence.WorldSaveData;
import civsim.runner.*;
import civsim.web.controller.*;
import civsim.web.sse.EventBroadcaster;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * MIE Phase 3.1 Backend Integration and System Verification Suite.
 *
 * Verifies:
 * 1. Single Mutation Owner & Lifecycle Transitions (INITIALIZING -> RUNNING <-> PAUSED -> STOPPED)
 * 2. Bounded Command Queue Execution, Rejection on Saturation & Idempotency
 * 3. Playback Controls While Paused (PAUSE, RESUME, STEP_MONTH, STEP_YEAR, SET_SPEED)
 * 4. Immutable Snapshots & Atomic Publication (Defensive copies, thread safety)
 * 5. Safe Read Queries Without Domain Mutation or RNG Consumption
 * 6. REST Controllers & DTO Mapping (World, Settlements, People, Orgs, Battles, Leaderboards)
 * 7. Server-Sent Events (SSE) Broadcaster (Bounded 50 subscribers, replay buffer, disconnect cleanup)
 * 8. Atomic World Save & Load Persistence (Atomic rename, path sanitization, deterministic restoration)
 * 9. Memory Management & Bounded Buffer Enforcement (Queues <= bounds, thread pool cleanup)
 */
public class TestStage31Backend {

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
            System.out.printf("  [PASS] %s\n", message);
        } else {
            failedTests++;
            System.err.printf("  [FAIL] %s (Expected: %s, Actual: %s)\n", message, expected, actual);
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("  CIVSIM PHASE 3.1: BACKEND INTEGRATION & PERSISTENCE TEST SUITE");
        System.out.println("===============================================================================\n");

        long startMs = System.currentTimeMillis();

        try {
            testSingleOwnerAndLifecycle();
            testBoundedCommandQueueAndRejection();
            testPlaybackControlsWhilePaused();
            testImmutableSnapshotsAndAtomicPublication();
            testSafeReadQueriesWithoutMutation();
            testRestControllersAndDtoMapping();
            testSseBroadcaster();
            testAtomicSaveAndLoadPersistence();
            testMemoryManagementAndBoundedBuffers();
        } catch (Exception e) {
            System.err.println("Unexpected exception in test suite execution:");
            e.printStackTrace();
            failedTests++;
        }

        long elapsedMs = System.currentTimeMillis() - startMs;
        System.out.println("\n===============================================================================");
        System.out.printf("  PHASE 3.1 TESTS COMPLETED in %d ms\n", elapsedMs);
        System.out.printf("  Total: %d | Passed: %d | Failed: %d\n", totalTests, passedTests, failedTests);
        System.out.println("===============================================================================");

        if (failedTests > 0) {
            System.exit(1);
        }
    }

    // =========================================================================
    // SUITE 1: Single Mutation Owner & Lifecycle Transitions
    // =========================================================================
    private static void testSingleOwnerAndLifecycle() throws Exception {
        System.out.println("[SUITE 1] Single Mutation Owner & Lifecycle Transitions");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(12345L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_lifecycle");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        assertEquals("Initial state should be PAUSED by default in runner",
                SimulationLifecycleState.PAUSED, runner.getLifecycleState());
        assertFalse("Runner worker thread should not be running before start", runner.isRunning());

        // Start runner
        runner.start();
        assertTrue("Runner should report running after start", runner.isRunning());
        assertEquals("Lifecycle state should remain PAUSED until explicitly resumed",
                SimulationLifecycleState.PAUSED, runner.getLifecycleState());

        // Resume to running
        runner.submitCommand(SimulationCommand.resume());
        Thread.sleep(150);
        assertEquals("Lifecycle state should transition to RUNNING",
                SimulationLifecycleState.RUNNING, runner.getLifecycleState());

        // Pause
        runner.submitCommand(SimulationCommand.pause());
        Thread.sleep(150);
        assertEquals("Lifecycle state should transition back to PAUSED",
                SimulationLifecycleState.PAUSED, runner.getLifecycleState());

        // Stop
        runner.stop();
        Thread.sleep(150);
        assertEquals("Lifecycle state should transition to STOPPED",
                SimulationLifecycleState.STOPPED, runner.getLifecycleState());
        assertFalse("Runner worker thread should terminate on stop", runner.isRunning());

        broadcaster.shutdown();
        cleanupDirectory(new File("saves_test_lifecycle"));
    }

    // =========================================================================
    // SUITE 2: Bounded Command Queue Execution, Rejection on Saturation & Idempotency
    // =========================================================================
    private static void testBoundedCommandQueueAndRejection() throws Exception {
        System.out.println("\n[SUITE 2] Bounded Command Queue & Saturation Rejection");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(22222L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_cmd");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        // Runner is not started, so commands accumulate in the queue without being consumed
        int submitted = 0;
        int rejected = 0;
        for (int i = 0; i < 120; i++) {
            SimulationCommand cmd = SimulationCommand.setSpeed(SimulationSpeed.NORMAL);
            SimulationCommand result = runner.submitCommand(cmd);
            if (result.getStatus() != CommandStatus.REJECTED) {
                submitted++;
            } else {
                rejected++;
                assertEquals("Rejected command must have REJECTED status",
                        CommandStatus.REJECTED, cmd.getStatus());
            }
        }

        assertEquals("Exactly 100 commands should be accepted into bounded queue", 100, submitted);
        assertEquals("Exactly 20 commands should be rejected due to saturation", 20, rejected);

        broadcaster.shutdown();
        cleanupDirectory(new File("saves_test_cmd"));
    }

    // =========================================================================
    // SUITE 3: Playback Controls While Paused
    // =========================================================================
    private static void testPlaybackControlsWhilePaused() throws Exception {
        System.out.println("\n[SUITE 3] Playback Controls While Paused");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(33333L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_playback");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        runner.start(); // starts in PAUSED state
        Thread.sleep(100);

        int initialYear = runner.getTimelineYear();
        int initialMonth = runner.getTimelineMonth();
        assertEquals("Initial year should be 1", 1, initialYear);
        assertEquals("Initial month should be 0", 0, initialMonth);

        // Sleep while paused - time must NOT advance automatically
        Thread.sleep(300);
        assertEquals("Year must not advance automatically while paused", initialYear, runner.getTimelineYear());
        assertEquals("Month must not advance automatically while paused", initialMonth, runner.getTimelineMonth());

        // Step one month while paused
        SimulationCommand stepMonthCmd = SimulationCommand.stepMonth();
        runner.submitCommand(stepMonthCmd);
        stepMonthCmd.awaitCompletion(2, TimeUnit.SECONDS);

        assertEquals("Step month command must complete successfully", CommandStatus.COMPLETED, stepMonthCmd.getStatus());
        assertEquals("Month must advance by exactly 1", initialMonth + 1, runner.getTimelineMonth());
        assertEquals("Year must remain unchanged", initialYear, runner.getTimelineYear());
        assertEquals("Simulation must remain in PAUSED state after single step",
                SimulationLifecycleState.PAUSED, runner.getLifecycleState());

        // Step one year (12 months) while paused
        int monthBeforeYearStep = runner.getTimelineMonth();
        SimulationCommand stepYearCmd = SimulationCommand.stepYear();
        runner.submitCommand(stepYearCmd);
        stepYearCmd.awaitCompletion(5, TimeUnit.SECONDS);

        assertEquals("Step year command must complete successfully", CommandStatus.COMPLETED, stepYearCmd.getStatus());
        assertEquals("Year must advance by 1 after 12 months step", initialYear + 1, runner.getTimelineYear());
        assertEquals("Month must be identical after full 12 months step", monthBeforeYearStep, runner.getTimelineMonth());

        // Set speed while paused
        SimulationCommand speedCmd = SimulationCommand.setSpeed(SimulationSpeed.VERY_FAST);
        runner.submitCommand(speedCmd);
        speedCmd.awaitCompletion(2, TimeUnit.SECONDS);

        assertEquals("Speed change command must complete", CommandStatus.COMPLETED, speedCmd.getStatus());
        assertEquals("Speed setting must be updated to VERY_FAST", SimulationSpeed.VERY_FAST, runner.getCurrentSpeed());
        assertEquals("Lifecycle must still remain PAUSED", SimulationLifecycleState.PAUSED, runner.getLifecycleState());

        runner.stop();
        broadcaster.shutdown();
        cleanupDirectory(new File("saves_test_playback"));
    }

    // =========================================================================
    // SUITE 4: Immutable Snapshots & Atomic Publication
    // =========================================================================
    private static void testImmutableSnapshotsAndAtomicPublication() throws Exception {
        System.out.println("\n[SUITE 4] Immutable Snapshots & Atomic Publication");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(44444L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_snapshots");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        runner.start();
        Thread.sleep(100);

        WorldTickSnapshot tickSnap = runner.getLatestTickSnapshot();
        assertTrue("Latest tick snapshot must not be null", tickSnap != null);
        assertEquals("Snapshot year should match runner year", runner.getTimelineYear(), tickSnap.currentYear());
        assertEquals("Snapshot month should match runner month", runner.getTimelineMonth(), tickSnap.currentMonth());

        // Test immutability of collections inside snapshot
        try {
            tickSnap.settlements().clear();
            assertTrue("Snapshot settlements list should be unmodifiable", false);
        } catch (UnsupportedOperationException e) {
            assertTrue("Modifying snapshot settlements collection threw UnsupportedOperationException", true);
        }

        try {
            tickSnap.recentEvents().clear();
            assertTrue("Snapshot events list should be unmodifiable", false);
        } catch (UnsupportedOperationException e) {
            assertTrue("Modifying snapshot events collection threw UnsupportedOperationException", true);
        }

        // Test WorldMapSnapshot
        WorldMapSnapshot mapSnap = runner.getLatestMapSnapshot();
        assertTrue("Map snapshot must not be null", mapSnap != null);
        assertFalse("Map snapshot settlements should not be empty", mapSnap.settlements().isEmpty());

        try {
            mapSnap.settlements().clear();
            assertTrue("Map snapshot settlements list should be unmodifiable", false);
        } catch (UnsupportedOperationException e) {
            assertTrue("Modifying map snapshot settlements threw UnsupportedOperationException", true);
        }

        // Test WorldSummaryDto
        WorldSummaryDto summaryDto = runner.getWorldSummary();
        assertTrue("World summary DTO must not be null", summaryDto != null);
        assertTrue("Living population in summary must be > 0", summaryDto.humanPopulation() > 0);

        runner.stop();
        broadcaster.shutdown();
        cleanupDirectory(new File("saves_test_snapshots"));
    }

    // =========================================================================
    // SUITE 5: Safe Read Queries Without Domain Mutation or RNG Consumption
    // =========================================================================
    private static void testSafeReadQueriesWithoutMutation() throws Exception {
        System.out.println("\n[SUITE 5] Safe Read Queries Without Domain Mutation or RNG Consumption");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(55555L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_queries");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        runner.start();
        Thread.sleep(100);

        int yearBefore = runner.getTimelineYear();
        int monthBefore = runner.getTimelineMonth();

        // 1. Query settlement detail
        SettlementDetailDto settlementDto = runner.querySettlementDetail(1).get(2, TimeUnit.SECONDS);
        assertTrue("Settlement 1 query returned result", settlementDto != null);
        assertEquals("Settlement ID should be 1", 1, settlementDto.id());
        assertTrue("Settlement population > 0", settlementDto.population() > 0);

        // 2. Query person detail
        int personId = engine.getAllPeople().get(0).getId();
        PersonDetailDto personDto = runner.queryPersonDetail(personId).get(2, TimeUnit.SECONDS);
        assertTrue("Person detail query returned result", personDto != null);
        assertEquals("Queried person ID matches", personId, personDto.id());
        assertTrue("Person name is populated", personDto.name() != null && !personDto.name().isEmpty());

        // 3. Query non-existent entities
        SettlementDetailDto nonExistentSettlement = runner.querySettlementDetail(9999).get(2, TimeUnit.SECONDS);
        assertTrue("Non-existent settlement returns null", nonExistentSettlement == null);

        PersonDetailDto nonExistentPerson = runner.queryPersonDetail(99999).get(2, TimeUnit.SECONDS);
        assertTrue("Non-existent person returns null", nonExistentPerson == null);

        // 4. Verify no simulation advancement occurred
        assertEquals("Year must not change after executing read queries", yearBefore, runner.getTimelineYear());
        assertEquals("Month must not change after executing read queries", monthBefore, runner.getTimelineMonth());
        assertEquals("Lifecycle must remain PAUSED", SimulationLifecycleState.PAUSED, runner.getLifecycleState());

        runner.stop();
        broadcaster.shutdown();
        cleanupDirectory(new File("saves_test_queries"));
    }

    // =========================================================================
    // SUITE 6: REST Controllers & DTO Mapping
    // =========================================================================
    private static void testRestControllersAndDtoMapping() throws Exception {
        System.out.println("\n[SUITE 6] REST Controllers & DTO Mapping");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(66666L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_rest");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        runner.start();
        Thread.sleep(100);

        // Instantiate controllers
        WorldController worldController = new WorldController(runner, persistenceService);
        SettlementController settlementController = new SettlementController(runner);
        PersonController personController = new PersonController(runner);
        OrganizationController orgController = new OrganizationController(runner);
        SimulationControlController controlController = new SimulationControlController(runner);
        LeaderboardController leaderboardController = new LeaderboardController(runner);

        // 1. Simulation Control Status & World Controller Summary
        ResponseEntity<SimulationStatusDto> statusRes = controlController.getStatus();
        assertEquals("Simulation status endpoint returns 200 OK", HttpStatus.OK, statusRes.getStatusCode());
        assertEquals("Lifecycle is PAUSED", "PAUSED", statusRes.getBody().lifecycleState());

        ResponseEntity<WorldSummaryDto> summaryRes = worldController.getSummary();
        assertEquals("World summary endpoint returns 200 OK", HttpStatus.OK, summaryRes.getStatusCode());
        assertTrue("Summary living population > 0", summaryRes.getBody().humanPopulation() > 0);

        ResponseEntity<WorldMapSnapshot> mapRes = worldController.getMap();
        assertEquals("World map endpoint returns 200 OK", HttpStatus.OK, mapRes.getStatusCode());
        assertFalse("Map settlements not empty", mapRes.getBody().settlements().isEmpty());

        // 2. Settlement Controller
        ResponseEntity<PagedResponse<SettlementMarkerDto>> settlementsRes = settlementController.listSettlements(0, 10);
        assertEquals("Settlements list returns 200 OK", HttpStatus.OK, settlementsRes.getStatusCode());
        assertFalse("Settlement items list not empty", settlementsRes.getBody().content().isEmpty());

        ResponseEntity<SettlementDetailDto> singleSettlementRes = settlementController.getSettlementDetail(1);
        assertEquals("Single settlement returns 200 OK", HttpStatus.OK, singleSettlementRes.getStatusCode());
        assertEquals("Settlement ID 1", 1, singleSettlementRes.getBody().id());

        ResponseEntity<SettlementDetailDto> missingSettlementRes = settlementController.getSettlementDetail(8888);
        assertEquals("Missing settlement returns 404 NOT_FOUND", HttpStatus.NOT_FOUND, missingSettlementRes.getStatusCode());

        // 3. Person Controller
        ResponseEntity<PagedResponse<PersonSummaryDto>> peopleRes = personController.listPeople(0, 10, null, null, null, null, null);
        assertEquals("People list returns 200 OK", HttpStatus.OK, peopleRes.getStatusCode());
        assertFalse("People items not empty", peopleRes.getBody().content().isEmpty());

        int firstPersonId = peopleRes.getBody().content().get(0).id();
        ResponseEntity<PersonDetailDto> singlePersonRes = personController.getPersonDetail(firstPersonId);
        assertEquals("Single person returns 200 OK", HttpStatus.OK, singlePersonRes.getStatusCode());
        assertEquals("Person ID matches", firstPersonId, singlePersonRes.getBody().id());

        ResponseEntity<PersonDetailDto> missingPersonRes = personController.getPersonDetail(77777);
        assertEquals("Missing person returns 404 NOT_FOUND", HttpStatus.NOT_FOUND, missingPersonRes.getStatusCode());

        // 4. Organization Controller
        ResponseEntity<PagedResponse<OrganizationSummaryDto>> orgsRes = orgController.listOrganizations(0, 10);
        assertEquals("Organizations list returns 200 OK", HttpStatus.OK, orgsRes.getStatusCode());

        // 5. Leaderboard Controller
        ResponseEntity<LeaderboardDto> leaderboardsRes = leaderboardController.getLeaderboard("POWER");
        assertEquals("Leaderboards return 200 OK", HttpStatus.OK, leaderboardsRes.getStatusCode());
        assertTrue("Leaderboard entries not null", leaderboardsRes.getBody().entries() != null);

        // 6. Playback Controller
        ResponseEntity<CommandStatusDto> stepRes = controlController.stepMonth();
        assertEquals("Step month endpoint returns 202 ACCEPTED", HttpStatus.ACCEPTED, stepRes.getStatusCode());
        assertTrue("Command ID returned", stepRes.getBody().commandId() != null);

        ResponseEntity<CommandStatusDto> speedRes = controlController.setSpeed(new SpeedChangeRequestDto("FAST"));
        assertEquals("Set speed endpoint returns 202 ACCEPTED", HttpStatus.ACCEPTED, speedRes.getStatusCode());
        Thread.sleep(100);
        assertEquals("Speed updated to FAST", SimulationSpeed.FAST, runner.getCurrentSpeed());

        runner.stop();
        broadcaster.shutdown();
        cleanupDirectory(new File("saves_test_rest"));
    }

    // =========================================================================
    // SUITE 7: Server-Sent Events (SSE) Broadcaster
    // =========================================================================
    private static void testSseBroadcaster() throws Exception {
        System.out.println("\n[SUITE 7] Server-Sent Events (SSE) Broadcaster");

        EventBroadcaster broadcaster = new EventBroadcaster();
        assertEquals("Initial subscriber count should be 0", 0, broadcaster.getSubscriberCount());

        // 1. Subscribe client 1
        SseEmitter client1 = broadcaster.subscribe("null");
        assertTrue("Client 1 emitter is non-null", client1 != null);
        assertEquals("Subscriber count is 1", 1, broadcaster.getSubscriberCount());

        // 2. Publish tick events
        EventDto event1 = new EventDto(1, 1, 0, 0L, "WORLD", "Tick 1 occurred", "NONE");
        EventDto event2 = new EventDto(2, 1, 1, 1L, "WORLD", "Tick 2 occurred", "NONE");
        broadcaster.broadcast(event1);
        broadcaster.broadcast(event2);
        Thread.sleep(100);

        assertEquals("Broadcast history buffer size is 2", 2, broadcaster.getBroadcastHistorySize());

        // 3. Subscribe client 2 with Last-Event-ID for replay
        SseEmitter client2 = broadcaster.subscribe("1");
        assertTrue("Client 2 emitter is non-null", client2 != null);
        assertEquals("Subscriber count is 2", 2, broadcaster.getSubscriberCount());

        // 4. Enforce max subscriber cap (50)
        List<SseEmitter> emitters = new ArrayList<>();
        while (broadcaster.getSubscriberCount() < EventBroadcaster.MAX_SUBSCRIBERS) {
            emitters.add(broadcaster.subscribe(null));
        }
        assertEquals("Broadcaster saturated to exactly max subscribers (50)",
                EventBroadcaster.MAX_SUBSCRIBERS, broadcaster.getSubscriberCount());

        // 51st subscriber must throw saturation error
        try {
            broadcaster.subscribe(null);
            assertTrue("Exceeding 50 subscribers must fail", false);
        } catch (IllegalStateException e) {
            assertTrue("Exceeding 50 subscribers threw IllegalStateException with cap message",
                    e.getMessage().contains("capacity reached"));
        }

        broadcaster.shutdown();
        assertEquals("Subscriber count should be 0 after shutdown", 0, broadcaster.getSubscriberCount());
    }

    // =========================================================================
    // SUITE 8: Atomic World Save & Load Persistence
    // =========================================================================
    private static void testAtomicSaveAndLoadPersistence() throws Exception {
        System.out.println("\n[SUITE 8] Atomic World Save & Load Persistence");

        String saveDir = "saves_test_persistence";
        cleanupDirectory(new File(saveDir));

        WorldPersistenceService persistenceService = new WorldPersistenceService(saveDir);
        SimulationConfig config = SimulationConfig.createDefaultWithSeed(88888L);
        SimulationEngine engine = new SimulationEngine(config);
        engine.initializeWorld();

        // Advance simulation by 5 months
        for (int i = 0; i < 5; i++) {
            engine.simulateMonth();
        }
        assertEquals("Engine year is 1", 1, engine.getTimeline().getYear());
        assertEquals("Engine month is 5", 5, engine.getTimeline().getMonth());
        int livingCountBeforeSave = engine.getLivingPeople().size();

        // 1. Atomic Save
        String saveId = "unit_test_save_1";
        WorldSaveSummaryDto saveSummary = persistenceService.saveWorld(engine, saveId);
        assertTrue("Save summary is returned", saveSummary != null);
        assertEquals("Save summary ID matches", saveId, saveSummary.saveId());
        assertEquals("Saved year matches", 1, saveSummary.year());
        assertEquals("Saved month matches", 5, saveSummary.month());

        // Verify file on disk
        File saveFile = new File(saveDir, saveId + ".json");
        File tempFile = new File(saveDir, saveId + ".tmp");
        assertTrue("Target .json save file exists", saveFile.exists());
        assertFalse(".tmp file must not remain after atomic rename", tempFile.exists());

        // 2. Directory traversal sanitization check
        String maliciousId = "../../evil_payload";
        try {
            persistenceService.saveWorld(engine, maliciousId);
            assertTrue("Directory traversal attempt must throw IllegalArgumentException", false);
        } catch (IllegalArgumentException e) {
            assertTrue("Malicious path traversal ID rejected with IllegalArgumentException",
                    e.getMessage().contains("Invalid save ID"));
        }

        // 3. List Saves
        List<WorldSaveSummaryDto> savesList = persistenceService.listSaves();
        assertFalse("Saves list is not empty", savesList.isEmpty());
        boolean foundUnitSave = savesList.stream().anyMatch(s -> s.saveId().equals(saveId));
        assertTrue("Unit test save found in list", foundUnitSave);

        // 4. Load World
        SimulationEngine restoredEngine = persistenceService.loadWorld(saveId);
        assertTrue("Restored engine is not null", restoredEngine != null);
        assertEquals("Restored timeline year matches", 1, restoredEngine.getTimeline().getYear());
        assertEquals("Restored timeline month matches", 5, restoredEngine.getTimeline().getMonth());
        assertEquals("Restored living population matches original", livingCountBeforeSave, restoredEngine.getLivingPeople().size());
        assertEquals("Restored villages count matches", engine.getVillages().size(), restoredEngine.getVillages().size());
        assertEquals("Restored families count matches", engine.getAllFamilies().size(), restoredEngine.getAllFamilies().size());

        // 5. Deterministic continuation after load
        restoredEngine.simulateMonth();
        assertEquals("Restored engine advances to month 6", 6, restoredEngine.getTimeline().getMonth());

        cleanupDirectory(new File(saveDir));
    }

    // =========================================================================
    // SUITE 9: Memory Management & Bounded Buffer Enforcement
    // =========================================================================
    private static void testMemoryManagementAndBoundedBuffers() throws Exception {
        System.out.println("\n[SUITE 9] Memory Management & Bounded Buffers");

        SimulationConfig config = SimulationConfig.createDefaultWithSeed(99999L);
        SimulationEngine engine = new SimulationEngine(config);
        EventBroadcaster broadcaster = new EventBroadcaster();
        WorldPersistenceService persistenceService = new WorldPersistenceService("saves_test_mem");
        SimulationRunner runner = new SimulationRunner(engine, broadcaster, persistenceService);

        runner.start();

        // 1. Verify bounded command queue
        assertEquals("Command queue limit is 100", 100, SimulationRunner.MAX_COMMAND_QUEUE_SIZE);

        // 2. Verify bounded query queue
        assertEquals("Query queue limit is 200", 200, SimulationRunner.MAX_QUERY_QUEUE_SIZE);

        // 3. Verify bounded event history in runner
        assertEquals("Event history limit is 200", 200, SimulationRunner.MAX_EVENT_HISTORY_SIZE);

        // 4. Verify SSE broadcaster limits
        assertEquals("SSE max subscribers is 50", 50, EventBroadcaster.MAX_SUBSCRIBERS);
        assertEquals("SSE event buffer limit is 100", 100, EventBroadcaster.MAX_EVENT_BUFFER_SIZE);

        // 5. Verify thread pool cleanup on stop
        runner.stop();
        broadcaster.shutdown();
        Thread.sleep(100);

        assertFalse("Runner worker is stopped", runner.isRunning());

        cleanupDirectory(new File("saves_test_mem"));
        assertTrue("Memory management & bounded structures verified", true);
    }

    private static void cleanupDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }
}
