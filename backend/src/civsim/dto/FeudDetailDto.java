package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 — Detailed historical blood feud docket.
 */
public record FeudDetailDto(
        String feudId,
        String originGrievanceId,
        String grievanceIncident,
        String grievanceSeverity,
        String initiatorPartyId,
        String targetPartyId,
        String escalationLevel,
        String status,
        List<FeudActionRecordDto> actionHistory,
        List<String> involvedClanIds,
        List<String> involvedFamilyIds
) {
    public FeudDetailDto {
        actionHistory = (actionHistory != null) ? List.copyOf(actionHistory) : List.of();
        involvedClanIds = (involvedClanIds != null) ? List.copyOf(involvedClanIds) : List.of();
        involvedFamilyIds = (involvedFamilyIds != null) ? List.copyOf(involvedFamilyIds) : List.of();
    }
}

