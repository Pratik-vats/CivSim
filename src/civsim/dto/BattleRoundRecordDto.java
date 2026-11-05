package civsim.dto;

/**
 * Phase 3.4 — Granular combat action event within a battle round.
 */
public record BattleRoundRecordDto(
        int roundNumber,
        int actionSequence,
        String actorId,
        String actorName,
        String targetId,
        String targetName,
        String actionType,
        double damage,
        double healing,
        double staminaCost,
        double aetherCost,
        String injuryInflicted,
        boolean targetDefeated,
        boolean actorEscaped,
        String description
) {}

