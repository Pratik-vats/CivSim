package civsim.dto;

/**
 * Phase 3.4 — High-level summary of a blood feud case.
 */
public record FeudSummaryDto(
        String feudId,
        String originGrievanceId,
        String initiatorPartyId,
        String targetPartyId,
        String escalationLevel,
        String status,
        int actionsCount,
        int startingYear,
        int lastActionYear
) {}

