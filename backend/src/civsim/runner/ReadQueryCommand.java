package civsim.runner;

import civsim.SimulationEngine;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Encapsulates a read-only query submitted to the simulation execution thread.
 * Guarantees zero mutation of world state and complete thread safety.
 */
public class ReadQueryCommand<T> {
    private final Function<SimulationEngine, T> queryFunction;
    private final CompletableFuture<T> future;
    private final long submittedAt;

    public ReadQueryCommand(Function<SimulationEngine, T> queryFunction) {
        this.queryFunction = queryFunction;
        this.future = new CompletableFuture<>();
        this.submittedAt = System.currentTimeMillis();
    }

    public Function<SimulationEngine, T> getQueryFunction() {
        return queryFunction;
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void execute(SimulationEngine engine) {
        try {
            T result = queryFunction.apply(engine);
            future.complete(result);
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
    }
}

