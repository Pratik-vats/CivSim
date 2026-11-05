package civsim.dto;

import java.util.List;

/**
 * Phase 3.6 — Collection of all simulation-derived heatmap layers.
 */
public record HeatmapsDto(
        List<HeatmapPointDto> monsterThreat,
        List<HeatmapPointDto> aethericActivity,
        List<HeatmapPointDto> populationDensity,
        List<HeatmapPointDto> economicProsperity
) {
    public HeatmapsDto {
        monsterThreat = (monsterThreat != null) ? List.copyOf(monsterThreat) : List.of();
        aethericActivity = (aethericActivity != null) ? List.copyOf(aethericActivity) : List.of();
        populationDensity = (populationDensity != null) ? List.copyOf(populationDensity) : List.of();
        economicProsperity = (economicProsperity != null) ? List.copyOf(economicProsperity) : List.of();
    }
}

