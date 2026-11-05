package civsim.dto;

import java.util.List;

/**
 * Immutable spatial map snapshot containing entities with 2D world coordinates.
 */
public record WorldMapSnapshot(
        String worldId,
        long snapshotVersion,
        long tickId,
        int year,
        int month,
        double width,
        double height,
        List<MapSettlementDto> settlements,
        List<MapLairDto> lairs,
        List<MapAnomalyDto> anomalies,
        List<MapExpeditionDto> expeditions
) {
    public WorldMapSnapshot {
        settlements = (settlements != null) ? List.copyOf(settlements) : List.of();
        lairs = (lairs != null) ? List.copyOf(lairs) : List.of();
        anomalies = (anomalies != null) ? List.copyOf(anomalies) : List.of();
        expeditions = (expeditions != null) ? List.copyOf(expeditions) : List.of();
    }
}

