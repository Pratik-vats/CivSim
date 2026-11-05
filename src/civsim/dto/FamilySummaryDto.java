package civsim.dto;

/**
 * High-level summary of a family dynasty for explorer lists.
 */
public record FamilySummaryDto(
        int id,
        String surname,
        int foundedYear,
        Integer founderId,
        String founderName,
        int livingCount,
        int deceasedCount,
        int totalLineageCount,
        int generationsCount,
        Integer mostInfluentialPersonId,
        String mostInfluentialPersonName,
        Integer highestTierPersonId,
        String highestTierPersonName,
        int highestTier
) {}

