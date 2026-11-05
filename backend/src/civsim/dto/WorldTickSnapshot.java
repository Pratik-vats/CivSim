package civsim.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable published world read model emitted after each completed simulation tick.
 * Contains world-level indicators, summaries, and compact entity markers.
 */
public record WorldTickSnapshot(
        String worldId,
        long snapshotVersion,
        long tickId,
        long totalMonths,
        int currentYear,
        int currentMonth,
        String simulationStatus,
        boolean fantasyModeEnabled,
        int awakeningStartYear,
        int humanPopulation,
        int livingMonsterCount,
        int settlementCount,
        int activeOrganizationCount,
        int activeMonsterLairCount,
        WorldEconomySummaryDto economy,
        List<SettlementMarkerDto> settlements,
        List<LairMarkerDto> lairs,
        List<AnomalyMarkerDto> anomalies,
        List<String> recentEvents
) {
    public WorldTickSnapshot {
        settlements = (settlements != null) ? List.copyOf(settlements) : List.of();
        lairs = (lairs != null) ? List.copyOf(lairs) : List.of();
        anomalies = (anomalies != null) ? List.copyOf(anomalies) : List.of();
        recentEvents = (recentEvents != null) ? List.copyOf(recentEvents) : List.of();
    }
}

