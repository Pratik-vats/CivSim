package civsim.dto;

import java.util.Map;

/**
 * Side-by-side comparison between two historical points in time in the same world.
 */
public record HistoricalDateComparisonDto(
        String worldId,
        int year1,
        int year2,
        int pop1,
        int pop2,
        int popDelta,
        double popGrowthPercent,
        int settlements1,
        int settlements2,
        int clans1,
        int clans2,
        int battles1,
        int battles2,
        int tides1,
        int tides2,
        int anomalies1,
        int anomalies2,
        Map<String, Integer> villagePopulations1,
        Map<String, Integer> villagePopulations2
) {
    public HistoricalDateComparisonDto {
        villagePopulations1 = (villagePopulations1 != null) ? Map.copyOf(villagePopulations1) : Map.of();
        villagePopulations2 = (villagePopulations2 != null) ? Map.copyOf(villagePopulations2) : Map.of();
    }
}

