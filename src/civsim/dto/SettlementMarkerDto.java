package civsim.dto;

/**
 * Compact settlement marker for world snapshots.
 */
public record SettlementMarkerDto(
        int id,
        String name,
        double x,
        double y,
        String biome,
        String tier,
        int population,
        String status
) {}

