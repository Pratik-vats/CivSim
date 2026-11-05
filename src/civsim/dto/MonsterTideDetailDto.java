package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 — Detailed MonsterTide invasion dossier with spatial coordinates and wave composition.
 */
public record MonsterTideDetailDto(
        String tideId,
        String sourceLairId,
        int targetVillageId,
        String targetVillageName,
        double sourceX,
        double sourceY,
        double targetX,
        double targetY,
        double currentX,
        double currentY,
        String state,
        int mobilizedYear,
        int mobilizedMonth,
        int arrivalYear,
        int arrivalMonth,
        int travelDurationMonths,
        boolean seerDetected,
        int warningLeadMonths,
        String outcome,
        List<String> wave1MonsterIds,
        List<String> wave2MonsterIds,
        List<String> wave3MonsterIds,
        int totalMobilized,
        int slainCount,
        int retreatCount,
        int defenderFatalities,
        int civilianFatalities,
        double structuralDamagePercent,
        double foodDestroyed,
        String associatedBattleId
) {
    public MonsterTideDetailDto {
        wave1MonsterIds = (wave1MonsterIds != null) ? List.copyOf(wave1MonsterIds) : List.of();
        wave2MonsterIds = (wave2MonsterIds != null) ? List.copyOf(wave2MonsterIds) : List.of();
        wave3MonsterIds = (wave3MonsterIds != null) ? List.copyOf(wave3MonsterIds) : List.of();
    }
}

