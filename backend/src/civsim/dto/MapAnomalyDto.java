package civsim.dto;

/**
 * Detailed map anomaly representation.
 */
public record MapAnomalyDto(
        String anomalyId,
        double x,
        double y,
        String state,
        String resourceCategory,
        String resourceGrade,
        double initialQuantity,
        double remainingQuantity,
        boolean observable,
        String controllingOwnerId,
        int securingRemainingMonths
) {}

