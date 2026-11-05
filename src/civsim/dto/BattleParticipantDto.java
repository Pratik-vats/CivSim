package civsim.dto;

/**
 * Phase 3.6 — Participant metadata for tactical battle replay visualization.
 */
public record BattleParticipantDto(
        String combatantId,
        String name,
        String sideId,
        String entityType,
        String supernaturalClass,
        double initialHealth,
        double maxHealth,
        int tier,
        boolean isAliveAtEnd
) {}

