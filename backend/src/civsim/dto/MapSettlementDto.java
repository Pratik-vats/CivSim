package civsim.dto;

import java.util.List;

/**
 * Detailed map settlement representation for SVG/Canvas rendering.
 * Enriched in Phase 3.6 with territory bounds, military, prosperity, threat, and diplomatic links.
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
        boolean inFamine,
        double territoryRadius,
        double militaryStrength,
        double prosperityScore,
        double threatLevel,
        String dominantClanName,
        List<Integer> alliedSettlementIds,
        List<Integer> hostileSettlementIds
) {
    public MapSettlementDto {
        alliedSettlementIds = (alliedSettlementIds != null) ? List.copyOf(alliedSettlementIds) : List.of();
        hostileSettlementIds = (hostileSettlementIds != null) ? List.copyOf(hostileSettlementIds) : List.of();
    }

    /**
     * Backwards-compatible 11-argument constructor for Stages 3.2-3.5.
     */
    public MapSettlementDto(
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
    ) {
        this(
                id,
                name,
                x,
                y,
                biome,
                tier,
                population,
                fortificationLevel,
                techLevel,
                leaderName,
                inFamine,
                Math.max(6.0, 4.0 + Math.sqrt(Math.max(1, population)) * 0.75),
                fortificationLevel * 10.0 + population * 0.5,
                population * 2.0,
                0.0,
                null,
                List.of(),
                List.of()
        );
    }
}

