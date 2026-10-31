package civsim.dto;

/**
 * Detailed map expedition representation.
 */
public record MapExpeditionDto(
        String expeditionId,
        String originVillageId,
        String targetAnomalyId,
        double currentX,
        double currentY,
        String status,
        int personnelCount,
        double carriedCargo
) {}

