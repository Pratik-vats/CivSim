package civsim.dto;

/**
 * Summary DTO for the Heirloom Explorer directory.
 */
public record HeirloomSummaryDto(
        String heirloomId,
        String equipmentId,
        String artifactTitle,
        String equipmentType,
        String rarity,
        int tier,
        String condition,
        double durability,
        double maxDurability,
        String owningClanId,
        String owningClanName,
        String currentBearerId,
        String currentBearerName,
        String custodyStatus,
        String successionPolicy,
        int registrationYear
) {}

