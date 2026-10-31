package civsim.dto;

/**
 * Detailed map settlement representation for SVG/Canvas rendering.
 */
public record MapSettlementDto(
        int id,
        String name,
        double x,
        double y,
        String biome,
        String tier,
        int population,
        double fortificationLevel,
        int techLevel,
        String leaderName,
        boolean inFamine
) {}

