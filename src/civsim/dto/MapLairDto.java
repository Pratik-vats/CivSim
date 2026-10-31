package civsim.dto;

/**
 * Detailed map lair representation.
 */
public record MapLairDto(
        String lairId,
        String sourceAnomalyId,
        double x,
        double y,
        String biome,
        String threatRank,
        String state,
        int monsterCount,
        int capacity,
        double territoryRadius,
        String primarySpecies,
        String secondarySpecies
) {}

