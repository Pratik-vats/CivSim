package civsim.runner;

/**
 * Playback execution speeds for the simulation runner.
 * Controls the real-time delay between simulation ticks without altering simulation outcomes or RNG.
 */
public enum SimulationSpeed {
    PAUSED(-1L, "PAUSED"),
    SLOW(800L, "SLOW"),
    NORMAL(200L, "NORMAL"),
    FAST(50L, "FAST"),
    VERY_FAST(15L, "VERY_FAST"),
    MAXIMUM(0L, "MAXIMUM");

    private final long delayMillis;
    private final String label;

    SimulationSpeed(long delayMillis, String label) {
        this.delayMillis = delayMillis;
        this.label = label;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public String getLabel() {
        return label;
    }

    public static SimulationSpeed fromString(String text) {
        if (text == null || text.isBlank()) {
            return NORMAL;
        }
        for (SimulationSpeed s : values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.label.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown simulation speed: " + text + ". Allowed: PAUSED, SLOW, NORMAL, FAST, VERY_FAST, MAXIMUM");
    }
}

