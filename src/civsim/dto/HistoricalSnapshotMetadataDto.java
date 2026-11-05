package civsim.dto;

/**
 * Immutable metadata describing a recorded historical snapshot point.
 */
public record HistoricalSnapshotMetadataDto(
        String snapshotId,
        String worldId,
        int year,
        int month,
        long totalMonths,
        long timestamp,
        String snapshotType,
        HistoricalDataResolution historicalResolution,
        String engineVersion,
        int schemaVersion,
        boolean isForkCapable,
        String checksum,
        long worldSeed,
        String worldName
) {}

