package civsim.model;

/**
 * Defines simulation behavior when authoritative global living human population reaches zero:
 * - STOP_ON_HUMAN_EXTINCTION (Default): Immediately halt civilization processing, generate final report, and stop engine.
 * - CONTINUE_NONHUMAN_WORLD: Allow non-human ecological systems (e.g. monster biomass & evolution) to continue running,
 *   while strictly disabling all human civilization, birth, and settlement operations.
 */
public enum ExtinctionPolicy {
    STOP_ON_HUMAN_EXTINCTION,
    CONTINUE_NONHUMAN_WORLD;

    public boolean isStopOnExtinction() {
        return this == STOP_ON_HUMAN_EXTINCTION;
    }
}

