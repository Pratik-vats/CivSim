package civsim.runner;

import civsim.ProgressionConfig;
import civsim.SimulationConfig;
import civsim.SimulationEngine;
import civsim.dto.*;
import civsim.model.ExtinctionPolicy;
import civsim.model.SimulationLifecycleState;
import civsim.persistence.WorldPersistenceService;
import civsim.web.sse.EventBroadcaster;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Authoritative single-owner execution coordinator for the central simulation.
 * Guarantees One World = One Active Simulation Mutation Owner invariant.
 * Exposes thread-safe immutable snapshots, bounded command queues, and read-query execution.
 */
@Service
public class SimulationRunner implements InitializingBean, DisposableBean {

    public static final int MAX_SIMULATION_YEARS = 300;
    private static final long MAX_SIMULATION_MONTHS = MAX_SIMULATION_YEARS * 12L;

    public static final int COMMAND_QUEUE_CAPACITY = 100;
    public static final int QUERY_QUEUE_CAPACITY = 200;
    public static final int MAX_COMMAND_HISTORY = 200;
    public static final int MAX_COMMAND_QUEUE_SIZE = COMMAND_QUEUE_CAPACITY;
    public static final int MAX_QUERY_QUEUE_SIZE = QUERY_QUEUE_CAPACITY;
    public static final int MAX_EVENT_HISTORY_SIZE = MAX_COMMAND_HISTORY;

    private volatile String worldId = "world-001";
    private final AtomicLong snapshotVersion = new AtomicLong(1);

    private SimulationEngine engine;
    private final EventBroadcaster eventBroadcaster;
    private final WorldPersistenceService persistenceService;

    private volatile SimulationLifecycleState lifecycleState = SimulationLifecycleState.INITIALIZING;
    private volatile SimulationSpeed speed = SimulationSpeed.NORMAL;

    private final BlockingQueue<SimulationCommand> commandQueue = new ArrayBlockingQueue<>(COMMAND_QUEUE_CAPACITY);
    private final BlockingQueue<ReadQueryCommand<?>> queryQueue = new ArrayBlockingQueue<>(QUERY_QUEUE_CAPACITY);

    private final Map<String, SimulationCommand> commandHistory = Collections.synchronizedMap(
            new LinkedHashMap<String, SimulationCommand>(MAX_COMMAND_HISTORY + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SimulationCommand> eldest) {
                    return size() > MAX_COMMAND_HISTORY;
                }
            }
    );

    private final AtomicReference<WorldTickSnapshot> latestSnapshot = new AtomicReference<>();
    private final AtomicReference<WorldMapSnapshot> latestMapSnapshot = new AtomicReference<>();
    private final AtomicReference<WorldSummaryDto> latestSummarySnapshot = new AtomicReference<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    @org.springframework.beans.factory.annotation.Autowired
    public SimulationRunner(EventBroadcaster eventBroadcaster, WorldPersistenceService persistenceService) {
        this.eventBroadcaster = eventBroadcaster;
        this.persistenceService = persistenceService;
    }

    public SimulationRunner(SimulationEngine engine, EventBroadcaster eventBroadcaster, WorldPersistenceService persistenceService) {
        this.engine = engine;
        this.eventBroadcaster = eventBroadcaster;
        this.persistenceService = persistenceService;
        if (!this.engine.isInitialized()) {
            this.engine.initializeWorld();
        }
        this.lifecycleState = SimulationLifecycleState.PAUSED;
        publishFreshSnapshots();
    }

    @Override
    public void afterPropertiesSet() {
        initialize(SimulationConfig.createDefault());
        startWorker();
    }

    public synchronized void initialize(SimulationConfig config) {
        this.engine = new SimulationEngine(config, ProgressionConfig.createDefault());
        this.engine.initializeWorld();
        this.lifecycleState = SimulationLifecycleState.PAUSED; // Default to PAUSED on start to allow initial inspection
        publishFreshSnapshots();
    }

    public synchronized void startWorker() {
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this::runExecutionLoop, "CivSim-Worker-Thread");
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    /**
     * Authoritative central execution loop running on the dedicated simulation thread.
     */
    private void runExecutionLoop() {
        while (running.get()) {
            try {
                // 1. Process all pending control commands
                processPendingCommands();

                // 2. Process all pending read queries
                processPendingQueries();

                // 3. Evaluate ticking based on lifecycle state
                if (lifecycleState == SimulationLifecycleState.RUNNING) {
                    executeMonthlyTick();
                    applySpeedDelay();
                } else if (lifecycleState == SimulationLifecycleState.PAUSED
                        || lifecycleState == SimulationLifecycleState.COMPLETED
                        || (lifecycleState == SimulationLifecycleState.HUMAN_EXTINCT && engine.getConfig().getExtinctionPolicy().isStopOnExtinction())
                        || lifecycleState == SimulationLifecycleState.ERROR) {
                    // When paused or stopped, wait briefly for incoming commands
                    try {
                        SimulationCommand nextCmd = commandQueue.poll(50, TimeUnit.MILLISECONDS);
                        if (nextCmd != null) {
                            executeCommand(nextCmd);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else if (lifecycleState == SimulationLifecycleState.HUMAN_EXTINCT
                        && engine.getConfig().getExtinctionPolicy() == ExtinctionPolicy.CONTINUE_NONHUMAN_WORLD) {
                    executeMonthlyTick();
                    applySpeedDelay();
                } else if (lifecycleState == SimulationLifecycleState.STOPPED) {
                    break;
                }
            } catch (Throwable t) {
                System.err.println("Fatal exception in CivSim-Worker-Thread: " + t.getMessage());
                t.printStackTrace();
                transitionState(SimulationLifecycleState.ERROR);
            }
        }
    }

    private void executeMonthlyTick() {
        try {
            if (markCompletedAtYearLimit()) {
                publishFreshSnapshots();
                announceSimulationComplete();
                return;
            }
            engine.processMonthlyTick();
            boolean completed = markCompletedAtYearLimit();
            long v = snapshotVersion.incrementAndGet();
            publishFreshSnapshots();

            int year = engine.getTimeline().getYear();
            int month = engine.getTimeline().getMonth();
            long totalMonths = engine.getTimeline().getTotalElapsedMonths();

            if (eventBroadcaster != null) {
                eventBroadcaster.broadcastTickCompleted(worldId, totalMonths, v, year, month, lifecycleState.name());
            }

            if (completed) {
                announceSimulationComplete();
                return;
            }

            if (engine.isHumanExtinct() && lifecycleState == SimulationLifecycleState.RUNNING) {
                transitionState(SimulationLifecycleState.HUMAN_EXTINCT);
                if (eventBroadcaster != null) {
                    eventBroadcaster.broadcastMajorEvent(worldId, "HUMAN_EXTINCTION", "Human civilization has completely perished.");
                }
            }
        } catch (Exception ex) {
            System.err.println("Error executing monthly tick: " + ex.getMessage());
            ex.printStackTrace();
            transitionState(SimulationLifecycleState.ERROR);
        }
    }

    private void applySpeedDelay() {
        long delay = speed.getDelayMillis();
        if (delay <= 0) {
            Thread.yield();
            return;
        }

        // Sleep in small 10ms intervals so incoming control commands are processed promptly
        long endTime = System.currentTimeMillis() + delay;
        while (System.currentTimeMillis() < endTime && running.get()) {
            if (!commandQueue.isEmpty() || !queryQueue.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(Math.min(10, endTime - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processPendingCommands() {
        SimulationCommand cmd;
        while ((cmd = commandQueue.poll()) != null) {
            executeCommand(cmd);
        }
    }

    private void processPendingQueries() {
        ReadQueryCommand<?> query;
        while ((query = queryQueue.poll()) != null) {
            query.execute(engine);
        }
    }

    private void executeCommand(SimulationCommand cmd) {
        cmd.markExecuting();
        try {
            switch (cmd.getType()) {
                case PAUSE -> {
                    if (lifecycleState == SimulationLifecycleState.RUNNING) {
                        transitionState(SimulationLifecycleState.PAUSED);
                        cmd.markCompleted("Simulation paused successfully", Map.of("state", lifecycleState.name()));
                    } else {
                        cmd.markCompleted("Simulation already " + lifecycleState.name(), Map.of("state", lifecycleState.name()));
                    }
                }
                case RESUME -> {
                    if (lifecycleState == SimulationLifecycleState.PAUSED) {
                        if (markCompletedAtYearLimit()) {
                            publishFreshSnapshots();
                            announceSimulationComplete();
                            cmd.markRejected("Simulation is complete; the 300-year limit has been reached.");
                        } else {
                            transitionState(SimulationLifecycleState.RUNNING);
                            cmd.markCompleted("Simulation resumed successfully", Map.of("state", lifecycleState.name()));
                        }
                    } else if (lifecycleState == SimulationLifecycleState.HUMAN_EXTINCT && engine.getConfig().getExtinctionPolicy() == ExtinctionPolicy.CONTINUE_NONHUMAN_WORLD) {
                        transitionState(SimulationLifecycleState.HUMAN_EXTINCT);
                        cmd.markCompleted("Non-human simulation resumed", Map.of("state", lifecycleState.name()));
                    } else {
                        cmd.markRejected("Cannot resume world in " + lifecycleState.name() + " state");
                    }
                }
                case STEP_MONTH -> {
                    if (lifecycleState != SimulationLifecycleState.PAUSED) {
                        cmd.markRejected("Manual step permitted only when simulation is PAUSED (Current: " + lifecycleState + ")");
                        return;
                    }
                    if (markCompletedAtYearLimit()) {
                        publishFreshSnapshots();
                        announceSimulationComplete();
                        cmd.markRejected("Simulation is complete; the 300-year limit has been reached.");
                        return;
                    }
                    engine.processMonthlyTick();
                    boolean completed = markCompletedAtYearLimit();
                    long v = snapshotVersion.incrementAndGet();
                    publishFreshSnapshots();
                    if (eventBroadcaster != null) {
                        eventBroadcaster.broadcastTickCompleted(
                                worldId, engine.getTimeline().getTotalElapsedMonths(), v,
                                engine.getTimeline().getYear(), engine.getTimeline().getMonth(),
                                lifecycleState.name()
                        );
                    }
                    if (completed) announceSimulationComplete();
                    cmd.markCompleted(completed ? "Simulation complete after 300 years" : "Advanced exactly 1 month to Y" + engine.getTimeline().getYear() + " M" + engine.getTimeline().getMonth(),
                            Map.of("year", engine.getTimeline().getYear(), "month", engine.getTimeline().getMonth()));
                }
                case STEP_YEAR -> {
                    if (lifecycleState != SimulationLifecycleState.PAUSED) {
                        cmd.markRejected("Manual step permitted only when simulation is PAUSED (Current: " + lifecycleState + ")");
                        return;
                    }
                    int monthsAdvanced = 0;
                    boolean completed = false;
                    for (int m = 0; m < 12; m++) {
                        if (markCompletedAtYearLimit()) {
                            completed = true;
                            break;
                        }
                        engine.processMonthlyTick();
                        monthsAdvanced++;
                        if (markCompletedAtYearLimit()) {
                            completed = true;
                            break;
                        }
                        if (engine.isHumanExtinct() && engine.getConfig().getExtinctionPolicy().isStopOnExtinction()) {
                            transitionState(SimulationLifecycleState.HUMAN_EXTINCT);
                            break;
                        }
                    }
                    long v = snapshotVersion.incrementAndGet();
                    publishFreshSnapshots();
                    if (eventBroadcaster != null) {
                        eventBroadcaster.broadcastTickCompleted(
                                worldId, engine.getTimeline().getTotalElapsedMonths(), v,
                                engine.getTimeline().getYear(), engine.getTimeline().getMonth(),
                                lifecycleState.name()
                        );
                    }
                    if (completed) announceSimulationComplete();
                    cmd.markCompleted(completed ? "Simulation complete after 300 years" : "Advanced " + monthsAdvanced + " months to Y" + engine.getTimeline().getYear() + " M" + engine.getTimeline().getMonth(),
                            Map.of("monthsAdvanced", monthsAdvanced, "year", engine.getTimeline().getYear(), "month", engine.getTimeline().getMonth()));
                }
                case SET_SPEED -> {
                    String speedStr = (cmd.getPayload() != null) ? cmd.getPayload().toString() : "NORMAL";
                    this.speed = SimulationSpeed.fromString(speedStr);
                    cmd.markCompleted("Speed updated to " + this.speed.getLabel(), Map.of("speed", this.speed.getLabel(), "delayMs", this.speed.getDelayMillis()));
                }
                case STOP_SIMULATION -> {
                    transitionState(SimulationLifecycleState.STOPPED);
                    engine.stop();
                    cmd.markCompleted("Simulation stopped", Map.of("state", lifecycleState.name()));
                }
                case SAVE_WORLD -> {
                    String customId = (cmd.getPayload() != null) ? cmd.getPayload().toString() : null;
                    if (persistenceService == null) {
                        cmd.markFailed("Persistence service unavailable");
                        return;
                    }
                    WorldSaveSummaryDto summary = persistenceService.saveWorld(engine, customId, worldId);
                    cmd.markCompleted("World successfully saved as " + summary.saveId(), summary);
                }
                case LOAD_WORLD -> {
                    String saveId = (cmd.getPayload() != null) ? cmd.getPayload().toString() : null;
                    if (persistenceService == null) {
                        cmd.markFailed("Persistence service unavailable");
                        return;
                    }
                    SimulationEngine loaded = persistenceService.loadWorld(saveId);
                    try {
                        civsim.persistence.WorldSaveData sd = persistenceService.loadSaveData(saveId);
                        if (sd.worldId != null && !sd.worldId.isBlank()) {
                            this.worldId = sd.worldId;
                        }
                    } catch (Exception ignored) {}
                    this.engine.stop();
                    this.engine = loaded;
                    this.lifecycleState = SimulationLifecycleState.PAUSED;
                    boolean completed = markCompletedAtYearLimit();
                    long v = snapshotVersion.incrementAndGet();
                    publishFreshSnapshots();
                    if (eventBroadcaster != null) {
                        eventBroadcaster.broadcastSnapshotUpdated(worldId, v);
                        eventBroadcaster.broadcastStatusChanged(worldId, "LOADING", lifecycleState.name());
                    }
                    if (completed) announceSimulationComplete();
                    cmd.markCompleted("World successfully loaded from " + saveId, Map.of("year", engine.getTimeline().getYear(), "month", engine.getTimeline().getMonth(), "worldId", worldId));
                }
            }
        } catch (Exception e) {
            cmd.markFailed("Command execution error: " + e.getMessage());
        }
    }

    private void transitionState(SimulationLifecycleState newState) {
        SimulationLifecycleState old = this.lifecycleState;
        this.lifecycleState = newState;
        if (engine != null) {
            engine.setLifecycleState(newState);
        }
        if (eventBroadcaster != null && old != newState) {
            eventBroadcaster.broadcastStatusChanged(worldId, old.name(), newState.name());
        }
    }

    private boolean markCompletedAtYearLimit() {
        if (engine == null || engine.getTimeline().getTotalElapsedMonths() < MAX_SIMULATION_MONTHS) return false;
        if (lifecycleState != SimulationLifecycleState.COMPLETED) {
            transitionState(SimulationLifecycleState.COMPLETED);
        }
        return true;
    }

    private void announceSimulationComplete() {
        if (eventBroadcaster != null) {
            eventBroadcaster.broadcastMajorEvent(worldId, "SIMULATION_COMPLETE", "Simulation complete after 300 years.");
        }
    }

    public void publishFreshSnapshots() {
        if (engine == null) return;
        long v = snapshotVersion.get();
        latestSnapshot.set(SnapshotBuilder.buildTickSnapshot(engine, v, lifecycleState.name(), worldId));
        latestMapSnapshot.set(SnapshotBuilder.buildMapSnapshot(engine, v, worldId));
        latestSummarySnapshot.set(SnapshotBuilder.buildSummary(engine, lifecycleState.name(), worldId));
    }

    // --- Controller API Methods ---

    public SimulationCommand submitCommand(SimulationCommand command) {
        commandHistory.put(command.getCommandId(), command);
        boolean offered = commandQueue.offer(command);
        if (!offered) {
            command.markRejected("Command queue full (Capacity " + COMMAND_QUEUE_CAPACITY + " exhausted)");
        }
        return command;
    }

    public <T> CompletableFuture<T> submitQuery(Function<SimulationEngine, T> queryFunction) {
        ReadQueryCommand<T> cmd = new ReadQueryCommand<>(queryFunction);
        boolean offered = queryQueue.offer(cmd);
        if (!offered) {
            cmd.getFuture().completeExceptionally(new IllegalStateException("Read query queue full (Capacity " + QUERY_QUEUE_CAPACITY + " exhausted)"));
        }
        return cmd.getFuture();
    }

    public String getWorldId() {
        return worldId;
    }

    public synchronized void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public synchronized void createNewWorld(String worldName, long seed, SimulationConfig config) {
        if (this.engine != null) {
            this.engine.stop();
        }
        this.worldId = "world-" + UUID.randomUUID().toString().substring(0, 8);
        this.engine = new SimulationEngine(config, ProgressionConfig.createDefault());
        this.engine.initializeWorld();
        this.lifecycleState = SimulationLifecycleState.PAUSED;
        long v = snapshotVersion.incrementAndGet();
        publishFreshSnapshots();
        if (eventBroadcaster != null) {
            eventBroadcaster.broadcastSnapshotUpdated(worldId, v);
            eventBroadcaster.broadcastStatusChanged(worldId, "INITIALIZING", lifecycleState.name());
        }
    }

    public WorldTickSnapshot getLatestSnapshot() {
        return latestSnapshot.get();
    }

    public WorldMapSnapshot getLatestMapSnapshot() {
        return latestMapSnapshot.get();
    }

    public WorldSummaryDto getLatestSummarySnapshot() {
        return latestSummarySnapshot.get();
    }

    public SimulationStatusDto getStatus() {
        long usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double usedMb = Math.round(usedBytes / (1024.0 * 1024.0) * 10.0) / 10.0;
        int activeSubscribers = (eventBroadcaster != null) ? eventBroadcaster.getActiveSubscriberCount() : 0;

        return new SimulationStatusDto(
                worldId,
                lifecycleState.name(),
                speed.getLabel(),
                speed.getDelayMillis(),
                engine != null ? engine.getTimeline().getYear() : 0,
                engine != null ? engine.getTimeline().getMonth() : 0,
                engine != null ? engine.getTimeline().getTotalElapsedMonths() : 0,
                engine != null ? engine.getTotalLivingPopulation() : 0,
                engine != null ? engine.getMonsterEcologySystem().getTotalLivingMonsterCount() : 0,
                engine != null ? engine.getMonsterEcologySystem().getActiveLairs().size() : 0,
                commandQueue.size(),
                queryQueue.size(),
                activeSubscribers,
                usedMb
        );
    }

    public SimulationCommand getCommand(String commandId) {
        return commandHistory.get(commandId);
    }

    public SimulationLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public SimulationSpeed getSpeed() {
        return speed;
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        startWorker();
    }

    public void stop() {
        destroy();
    }

    public int getTimelineYear() {
        return engine != null ? engine.getTimeline().getYear() : 0;
    }

    public int getTimelineMonth() {
        return engine != null ? engine.getTimeline().getMonth() : 0;
    }

    public SimulationSpeed getCurrentSpeed() {
        return speed;
    }

    public WorldSummaryDto getWorldSummary() {
        return latestSummarySnapshot.get();
    }

    public WorldTickSnapshot getLatestTickSnapshot() {
        return getLatestSnapshot();
    }

    public CompletableFuture<SettlementDetailDto> querySettlementDetail(int settlementId) {
        return submitQuery(eng -> SnapshotBuilder.buildSettlementDetail(eng.getVillage(settlementId), eng.getVillages()));
    }

    public CompletableFuture<PersonDetailDto> queryPersonDetail(int personId) {
        return submitQuery(eng -> SnapshotBuilder.buildPersonDetail(
                eng.getPerson(personId),
                eng.getOrganizationSystem(),
                eng.getEquipmentCraftingSystem(),
                eng.getClanHeirloomSystem()
        ));
    }

    @Override
    public void destroy() {
        transitionState(SimulationLifecycleState.STOPPED);
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        if (engine != null) {
            engine.stop();
        }
        if (eventBroadcaster != null) {
            eventBroadcaster.closeAll();
        }
        commandQueue.clear();
        queryQueue.clear();
        commandHistory.clear();
    }
}
