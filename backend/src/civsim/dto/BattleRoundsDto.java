package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 & 3.6 — Round-by-round replay package for a battle with participant rosters.
 */
public record BattleRoundsDto(
        String battleId,
        boolean hasRoundReplay,
        String unavailableMessage,
        List<BattleRoundRecordDto> rounds,
        List<BattleParticipantDto> participants
) {
    public BattleRoundsDto {
        rounds = (rounds != null) ? List.copyOf(rounds) : List.of();
        participants = (participants != null) ? List.copyOf(participants) : List.of();
    }

    /**
     * Backwards-compatible 4-argument constructor.
     */
    public BattleRoundsDto(
            String battleId,
            boolean hasRoundReplay,
            String unavailableMessage,
            List<BattleRoundRecordDto> rounds
    ) {
        this(battleId, hasRoundReplay, unavailableMessage, rounds, List.of());
    }
}

