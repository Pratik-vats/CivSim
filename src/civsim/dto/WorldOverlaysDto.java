package civsim.dto;

import java.util.List;

/**
 * Phase 3.6 — Complete dynamic overlay package for the interactive world map.
 */
public record WorldOverlaysDto(
        String worldId,
        int year,
        int month,
        boolean isHistorical,
        List<SettlementTerritoryDto> territories,
        List<DiplomaticLinkDto> diplomaticLinks,
        List<ActiveConflictDto> activeConflicts,
        List<MonsterInvasionVectorDto> monsterInvasions,
        HeatmapsDto heatmaps
) {
    public WorldOverlaysDto {
        territories = (territories != null) ? List.copyOf(territories) : List.of();
        diplomaticLinks = (diplomaticLinks != null) ? List.copyOf(diplomaticLinks) : List.of();
        activeConflicts = (activeConflicts != null) ? List.copyOf(activeConflicts) : List.of();
        monsterInvasions = (monsterInvasions != null) ? List.copyOf(monsterInvasions) : List.of();
    }
}

