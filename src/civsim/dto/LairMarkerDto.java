package civsim.dto;

/**
 * Compact monster lair marker for world snapshots.
 */
public record LairMarkerDto(
        String lairId,
        double x,
        double y,
        String biome,
        String threatRank,
        String state,
        int monsterCount,
        double territoryRadius
) {}

