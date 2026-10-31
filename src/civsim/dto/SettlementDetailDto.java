package civsim.dto;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive read-only details for a settlement.
 */
public record SettlementDetailDto(
        int id,
        String name,
        double x,
        double y,
        String biome,
        String tier,
        int population,
        int livingMales,
        int livingFemales,
        Integer leaderId,
        String leaderName,
        double foodStockpile,
        double woodStockpile,
        double oreStockpile,
        double toolsStockpile,
        double treasury,
        int techLevel,
        double fortificationLevel,
        int housingCapacity,
        int peakPopulation,
        int monsterCores,
        boolean inFamine,
        String diplomaticSummary,
        List<String> craftingFacilities,
        int storedEquipmentCount
) {
    public SettlementDetailDto {
        craftingFacilities = (craftingFacilities != null) ? List.copyOf(craftingFacilities) : List.of();
    }
}

