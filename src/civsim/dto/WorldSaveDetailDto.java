package civsim.dto;

/**
 * Detailed metadata for an immutable simulation checkpoint / save record.
 */
public record WorldSaveDetailDto(
        String saveId,
        String worldId,
        String worldName,
        int year,
        int month,
        long totalMonths,
        long createdAt,
        int schemaVersion,
        int livingPopulation,
        int settlementCount,
        int monsterCount,
        String lifecycleState,
        boolean isRestorable,
        String parentWorldId,
        String parentCheckpointId,
        String branchRngPolicy,
        long worldSeed,
        String checksum,
        long fileSizeBytes
) {}

