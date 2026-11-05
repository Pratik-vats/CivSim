package civsim.dto;

import java.util.List;

/**
 * Historical time-series metric point for a settlement.
 */
public record SettlementHistoryPointDto(
        int year,
        int population,
        double food,
        double wood,
        double ore,
        double tools,
        double treasury,
        int techLevel,
        int housingCapacity,
        double fortification,
        String tier,
        String leaderName
) {}

