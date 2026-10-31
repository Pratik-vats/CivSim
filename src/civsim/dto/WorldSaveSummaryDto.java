package civsim.dto;

/**
 * Metadata record for a saved simulation world file.
 */
public record WorldSaveSummaryDto(
        String saveId,
        String worldId,
        int year,
        int month,
        long totalMonths,
        long createdAt,
        int saveSchemaVersion,
        int humanPopulation,
        String worldStatus
) {}

