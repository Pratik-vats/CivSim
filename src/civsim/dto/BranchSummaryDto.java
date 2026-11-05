package civsim.dto;

/**
 * Summary record of an alternate timeline world branch.
 */
public record BranchSummaryDto(
        String branchWorldId,
        String branchName,
        String parentWorldId,
        String parentCheckpointId,
        int forkYear,
        int forkMonth,
        long createdAt,
        String rngPolicy,
        long worldSeed,
        int currentYear,
        int currentMonth,
        int currentPopulation,
        String lifecycleState
) {}

