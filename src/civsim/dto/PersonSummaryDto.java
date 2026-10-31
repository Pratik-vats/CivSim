package civsim.dto;

/**
 * Compact person summary for list views and pagination.
 */
public record PersonSummaryDto(
        int id,
        String name,
        String gender,
        int age,
        boolean alive,
        int birthYear,
        Integer deathYear,
        String villageName,
        String occupation,
        double wealth,
        boolean isAwakened,
        String supernaturalClass,
        String aspect,
        String rarity,
        int tier,
        double powerLevel
) {}

