package civsim.runner;

/**
 * Supported simulation commands processed by the authoritative simulation execution thread.
 */
public enum CommandType {
    PAUSE,
    RESUME,
    STEP_MONTH,
    STEP_YEAR,
    SET_SPEED,
    STOP_SIMULATION,
    SAVE_WORLD,
    LOAD_WORLD
}

