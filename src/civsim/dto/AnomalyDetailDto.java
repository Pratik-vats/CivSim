package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 — Detailed supernatural anomaly record.
 */
public record AnomalyDetailDto(
        String anomalyId,
        String resourceCategory,
        String resourceGrade,
        double x,
        double y,
        String state,
        double initialQuantity,
        double remainingQuantity,
        String controllingOwnerId,
        boolean observable,
        int revealedYear,
        int discoveredYear,
        int contestsCount
) {}

