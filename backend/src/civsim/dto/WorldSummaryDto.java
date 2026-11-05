package civsim.dto;

/**
 * Compact world-level demographic, economic, and era summary.
 */
public record WorldSummaryDto(
        String worldId,
        int year,
        int month,
        long totalMonths,
        String status,
        int humanPopulation,
        int livingMalePopulation,
        int livingFemalePopulation,
        int settlementCount,
        int activeOrganizationCount,
        int awakenedPopulation,
        int livingMonsterCount,
        int activeMonsterLairCount,
        double totalFoodStockpile,
        double totalWoodStockpile,
        double totalOreStockpile,
        double totalToolsStockpile,
        double totalCitizenWealth,
        double totalCivicTreasury,
        String currentEra
) {}

