package civsim.dto;

/**
 * Compact anomaly marker for world snapshots.
 */
public record AnomalyMarkerDto(
        String anomalyId,
        double x,
        double y,
        String state,
        String resourceCategory,
        String resourceGrade,
        double remainingQuantity,
        boolean observable
) {}

