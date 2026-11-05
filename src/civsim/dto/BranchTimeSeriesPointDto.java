package civsim.dto;

import java.util.List;

/**
 * Metric point at a given simulation year for branch time-series comparison.
 */
public record BranchTimeSeriesPointDto(
        int year,
        int population,
        int settlementsCount,
        int activeClansCount,
        int battlesCount,
        int monsterLairsCount,
        int revealedAnomaliesCount,
        int awakenedCount
) {}

