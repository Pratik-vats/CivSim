package civsim.dto;

/**
 * Side-by-side comparison of two organizations at a specific historical point.
 */
public record OrganizationComparisonDto(
        OrganizationDetailDto organization1,
        OrganizationDetailDto organization2,
        String comparisonDate,
        String influenceSummary,
        String militarySummary
) {}

