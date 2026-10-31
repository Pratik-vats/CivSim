package civsim.model;

/**
 * Authoritative lifecycle state of the central simulation instance:
 * - INITIALIZING: World generation and initial demographic founding in progress.
 * - RUNNING: Automatic monthly simulation loop actively executing.
 * - PAUSED: Simulation time paused; runner continues processing commands and queries.
 * - HUMAN_EXTINCT: All living human population has reached zero.
 * - STOPPED: Simulation execution explicitly halted or terminated.
 * - ERROR: Unrecoverable simulation tick failure; safe diagnostic state.
 */
public enum SimulationLifecycleState {
    INITIALIZING,
    RUNNING,
    PAUSED,
    HUMAN_EXTINCT,
    STOPPED,
    ERROR;

    public boolean isRunning() {
        return this == RUNNING;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }

    public boolean isExtinct() {
        return this == HUMAN_EXTINCT;
    }

    public boolean isStopped() {
        return this == STOPPED;
    }

    public boolean isError() {
        return this == ERROR;
    }
}

