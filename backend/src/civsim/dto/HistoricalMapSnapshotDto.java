package civsim.dto;

import java.util.List;

/**
 * Compact, immutable spatial map state at a specific historical point in time.
 * Entities founded/revealed after this date are strictly excluded.
 */
public record HistoricalMapSnapshotDto(
        String worldId,
        int year,
        int month,
        long totalMonths,
        double width,
        double height,
        List<MapSettlementDto> settlements,
        List<MapLairDto> lairs,
        List<MapAnomalyDto> anomalies,
        List<MapExpeditionDto> expeditions,
        HistoricalDataResolution resolution,
        boolean isHistorical
) {
    public HistoricalMapSnapshotDto {
        settlements = (settlements != null) ? List.copyOf(settlements) : List.of();
        lairs = (lairs != null) ? List.copyOf(lairs) : List.of();
        anomalies = (anomalies != null) ? List.copyOf(anomalies) : List.of();
        expeditions = (expeditions != null) ? List.copyOf(expeditions) : List.of();
    }
}
