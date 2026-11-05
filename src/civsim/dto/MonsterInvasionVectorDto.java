package civsim.dto;

/**
 * Phase 3.6 — Spatial vector for MonsterTide invasions approaching settlements.
 */
public record MonsterInvasionVectorDto(
        String tideId,
        double sourceX,
        double sourceY,
        double targetX,
        double targetY,
        double currentX,
        double currentY,
        String state,
        int monsterCount,
        String targetSettlementName,
        int targetSettlementId
) {}

