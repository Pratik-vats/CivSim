package civsim.runner;

import civsim.dto.CommandStatusDto;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Encapsulates a control request queued for execution by the simulation runner thread.
 */
public class SimulationCommand {
    private final String commandId;
    private final CommandType type;
    private final Object payload;
    private final long submittedAt;
    private volatile CommandStatus status;
    private volatile Long completedAt;
    private volatile String message;
    private volatile Object result;
    private final CompletableFuture<SimulationCommand> completionFuture;

    public SimulationCommand(CommandType type, Object payload) {
        this(UUID.randomUUID().toString(), type, payload);
    }

    public SimulationCommand(String commandId, CommandType type, Object payload) {
        this.commandId = (commandId != null && !commandId.isBlank()) ? commandId : UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.submittedAt = System.currentTimeMillis();
        this.status = CommandStatus.QUEUED;
        this.completionFuture = new CompletableFuture<>();
    }

    public String getCommandId() {
        return commandId;
    }

    public CommandType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public CommandStatus getStatus() {
        return status;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public String getMessage() {
        return message;
    }

    public Object getResult() {
        return result;
    }

    public CompletableFuture<SimulationCommand> getCompletionFuture() {
        return completionFuture;
    }

    public void awaitCompletion(long timeout, java.util.concurrent.TimeUnit unit) throws Exception {
        completionFuture.get(timeout, unit);
    }

    public static SimulationCommand pause() {
        return new SimulationCommand(CommandType.PAUSE, null);
    }

    public static SimulationCommand resume() {
        return new SimulationCommand(CommandType.RESUME, null);
    }

    public static SimulationCommand stepMonth() {
        return new SimulationCommand(CommandType.STEP_MONTH, null);
    }

    public static SimulationCommand stepYear() {
        return new SimulationCommand(CommandType.STEP_YEAR, null);
    }

    public static SimulationCommand setSpeed(SimulationSpeed speed) {
        return new SimulationCommand(CommandType.SET_SPEED, speed != null ? speed.name() : "NORMAL");
    }

    public synchronized void markExecuting() {
        this.status = CommandStatus.EXECUTING;
    }

    public synchronized void markCompleted(String message, Object result) {
        this.status = CommandStatus.COMPLETED;
        this.completedAt = System.currentTimeMillis();
        this.message = message;
        this.result = result;
        this.completionFuture.complete(this);
    }

    public synchronized void markRejected(String reason) {
        this.status = CommandStatus.REJECTED;
        this.completedAt = System.currentTimeMillis();
        this.message = reason;
        this.completionFuture.complete(this);
    }

    public synchronized void markFailed(String error) {
        this.status = CommandStatus.FAILED;
        this.completedAt = System.currentTimeMillis();
        this.message = error;
        this.completionFuture.complete(this);
    }

    public CommandStatusDto toDto() {
        return new CommandStatusDto(commandId, type.name(), status.name(), submittedAt, completedAt, message, result);
    }
}
