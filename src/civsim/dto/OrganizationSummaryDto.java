package civsim.dto;

/**
 * Compact organization summary for list views.
 */
public record OrganizationSummaryDto(
        String id,
        String name,
        String type,
        String alignment,
        String status,
        int foundingYear,
        String leaderId,
        String leaderName,
        double reputation,
        int memberCount,
        int heirloomCount,
        int battlesWon,
        int battlesTotal
) {}

