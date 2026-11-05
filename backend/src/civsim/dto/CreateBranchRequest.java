package civsim.dto;

/**
 * Parameter payload for creating an independent alternate timeline branch.
 */
public record CreateBranchRequest(
        String sourceSaveId,
        String branchName,
        String rngPolicy
) {}

