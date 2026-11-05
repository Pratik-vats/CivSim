package civsim.dto;

/**
 * Compact combat encounter summary.
 */
public record BattleSummaryDto(
        String battleId,
        int year,
        int month,
        String battleType,
        String outcome,
        String victoriousSideId,
        int totalRounds,
        int fatalitiesCount,
        int incapacitatedCount,
        int escapedCount,
        String summaryText
) {}

