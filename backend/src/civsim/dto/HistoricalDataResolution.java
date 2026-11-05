package civsim.dto;

/**
 * Authoritative classification of historical data fidelity and availability.
 * Guarantees that historical gaps are never silently filled with fabricated data.
 */
public enum HistoricalDataResolution {
    /** Exact recorded monthly simulation state */
    FULL_HISTORICAL_STATE,

    /** Partial recorded metrics available; unrecorded fields omitted */
    PARTIAL_HISTORICAL_STATE,

    /** Aggregated statistics derived from annual simulation reports */
    AGGREGATED_HISTORICAL_DATA,

    /** Only occurrence event records exist for this historical point */
    EVENT_HISTORY_ONLY,

    /** Only current live state is known; historical state was not captured */
    CURRENT_STATE_ONLY,

    /** Data is unavailable and cannot be deterministically reconstructed */
    UNAVAILABLE
}

