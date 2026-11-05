package civsim.dto;

import java.util.List;

/**
 * Complete historical progression dossier for a settlement across simulated time.
 */
public record SettlementHistoryDto(
        int settlementId,
        String settlementName,
        String biome,
        double x,
        double y,
        int foundedYear,
        boolean isDestroyed,
        Integer destroyedYear,
        List<SettlementHistoryPointDto> historyPoints
) {
    public SettlementHistoryDto {
        historyPoints = (historyPoints != null) ? List.copyOf(historyPoints) : List.of();
    }
}

