package civsim.dto;

import java.util.Map;

/**
 * Phase 3.4 — Annual historical chronicle record.
 */
public record AnnualChronicleDto(
        int year,
        long totalMonths,
        String era,
        int openingPopulation,
        int closingPopulation,
        int births,
        int deaths,
        int marriages,
        int migrations,
        int settlementsCount,
        int activeClansCount,
        int battlesCount,
        int tidesCount,
        int anomaliesCount,
        String awakeningSummary,
        String combatSummary,
        String organizationSummary,
        String monsterEcologySummary,
        String equipmentSummary,
        String bloodFeudSummary,
        Map<String, Integer> villagePopulations
) {
    public AnnualChronicleDto {
        villagePopulations = (villagePopulations != null) ? Map.copyOf(villagePopulations) : Map.of();
    }
}

