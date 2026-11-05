package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 — Detailed expedition dossier with roster, cargo, and spatial coordinates.
 */
public record ExpeditionDetailDto(
        String expeditionId,
        String sponsorId,
        String sponsorType,
        String targetAnomalyId,
        String anomalyName,
        double originX,
        double originY,
        double targetX,
        double targetY,
        double currentX,
        double currentY,
        String status,
        String claimObjective,
        List<String> rosterPersonIds,
        List<String> rosterPersonNames,
        double carriedCargo,
        double maxCapacity,
        double supplies,
        long departureMonth,
        long arrivalMonth,
        long returnArrivalMonth
) {
    public ExpeditionDetailDto {
        rosterPersonIds = (rosterPersonIds != null) ? List.copyOf(rosterPersonIds) : List.of();
        rosterPersonNames = (rosterPersonNames != null) ? List.copyOf(rosterPersonNames) : List.of();
    }
}

