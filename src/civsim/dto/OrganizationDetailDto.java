package civsim.dto;

import java.util.List;
import java.util.Map;

/**
 * Full details for an organization.
 */
public record OrganizationDetailDto(
        String id,
        String name,
        String type,
        String alignment,
        String status,
        int foundingYear,
        int foundingMonth,
        String founderPersonId,
        String leaderId,
        String leaderName,
        double reputation,
        int memberCount,
        int heirloomCount,
        List<String> heirloomNames,
        int battlesWon,
        int battlesTotal,
        int memberCasualties,
        List<String> activeFeudIds,
        List<String> recentEvents
) {
    public OrganizationDetailDto {
        heirloomNames = (heirloomNames != null) ? List.copyOf(heirloomNames) : List.of();
        activeFeudIds = (activeFeudIds != null) ? List.copyOf(activeFeudIds) : List.of();
        recentEvents = (recentEvents != null) ? List.copyOf(recentEvents) : List.of();
    }
}

