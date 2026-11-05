package civsim.dto;

/**
 * Phase 3.4 — High-level summary of an exploration/extraction expedition.
 */
public record ExpeditionSummaryDto(
        String expeditionId,
        String sponsorId,
        String sponsorType,
        String targetAnomalyId,
        String status,
        int personnelCount,
        double currentX,
        double currentY,
        double carriedCargo,
        double maxCapacity,
        double supplies,
        long departureMonth,
        long arrivalMonth
) {}

