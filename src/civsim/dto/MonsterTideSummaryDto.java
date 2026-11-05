package civsim.dto;

/**
 * Phase 3.4 — High-level summary of a MonsterTide invasion.
 */
public record MonsterTideSummaryDto(
        String tideId,
        String sourceLairId,
        int targetVillageId,
        String targetVillageName,
        String state,
        int mobilizedYear,
        int mobilizedMonth,
        int arrivalYear,
        int arrivalMonth,
        int totalMobilizedCount,
        String outcome,
        boolean seerDetected,
        int warningLeadMonths
) {}

