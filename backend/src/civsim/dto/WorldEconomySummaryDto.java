package civsim.dto;

/**
 * Compact economy summary for world snapshots.
 */
public record WorldEconomySummaryDto(
        double foodStockpile,
        double woodStockpile,
        double oreStockpile,
        double toolsStockpile,
        double citizenWealth,
        double civicTreasuries,
        double bankDeposits
) {}

