package civsim.runner;

/**
 * Lifecycle execution status of a simulation command.
 */
public enum CommandStatus {
    RECEIVED,
    QUEUED,
    EXECUTING,
    COMPLETED,
    REJECTED,
    FAILED,
    CANCELLED
}

