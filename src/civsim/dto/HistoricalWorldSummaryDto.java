package civsim.dto;

import java.util.List;

/**
 * Historical world summary at a specific date with explicit data availability indicators.
 */
public record HistoricalWorldSummaryDto(
        String worldId,
        int year,
        int month,
        long totalMonths,
        String currentEra,
        int totalLivingPopulation,
        int settlementsCount,
        int activeClansCount,
        int battlesCount,
        int monsterLairsCount,
        int revealedAnomaliesCount,
        HistoricalDataResolution resolution,
        List<String> availableFields,
        List<String> missingFields
) {
    public HistoricalWorldSummaryDto {
        availableFields = (availableFields != null) ? List.copyOf(availableFields) : List.of();
        missingFields = (missingFields != null) ? List.copyOf(missingFields) : List.of();
    }
}

