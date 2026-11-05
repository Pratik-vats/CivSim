package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 — Record of a territorial/extraction contest over an anomaly.
 */
public record AnomalyContestDto(
        String contestId,
        String anomalyId,
        int year,
        int month,
        List<String> contestants,
        String winnerId,
        boolean resolved,
        boolean skirmishOccurred,
        String battleId
) {
    public AnomalyContestDto {
        contestants = (contestants != null) ? List.copyOf(contestants) : List.of();
    }
}

