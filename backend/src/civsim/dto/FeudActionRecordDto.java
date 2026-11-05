package civsim.dto;

/**
 * Phase 3.4 — Historical action/escalation event within a blood feud.
 */
public record FeudActionRecordDto(
        String actionId,
        String actionType,
        String actorId,
        String targetId,
        int year,
        int month,
        String description,
        String battleId
) {}

