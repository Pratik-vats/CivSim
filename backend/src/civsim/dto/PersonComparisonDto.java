package civsim.dto;

/**
 * Side-by-side comparison of two individuals at a specific historical point.
 */
public record PersonComparisonDto(
        PersonDetailDto person1,
        PersonDetailDto person2,
        String comparisonDate,
        String powerSummary,
        String wealthSummary
) {}

