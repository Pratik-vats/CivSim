package civsim.dto;

import java.util.List;
import java.util.Map;

/**
 * Full details for a historical battle record.
 */
public record BattleDetailDto(
        String battleId,
        int year,
        int month,
        String battleType,
        String outcome,
        String victoriousSideId,
        int totalRounds,
        int totalInjuriesInflicted,
        Map<String, Integer> initialCountsPerSide,
        Map<String, Integer> finalLivingCountsPerSide,
        Map<String, Double> damageDealtPerSide,
        List<String> fatalities,
        List<String> incapacitated,
        List<String> escaped,
        String summaryText
) {
    public BattleDetailDto {
        initialCountsPerSide = (initialCountsPerSide != null) ? Map.copyOf(initialCountsPerSide) : Map.of();
        finalLivingCountsPerSide = (finalLivingCountsPerSide != null) ? Map.copyOf(finalLivingCountsPerSide) : Map.of();
        damageDealtPerSide = (damageDealtPerSide != null) ? Map.copyOf(damageDealtPerSide) : Map.of();
        fatalities = (fatalities != null) ? List.copyOf(fatalities) : List.of();
        incapacitated = (incapacitated != null) ? List.copyOf(incapacitated) : List.of();
        escaped = (escaped != null) ? List.copyOf(escaped) : List.of();
    }
}

