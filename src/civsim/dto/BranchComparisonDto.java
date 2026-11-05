package civsim.dto;

import java.util.List;

/**
 * Authoritative side-by-side comparison and divergence timeline between two world branches.
 */
public record BranchComparisonDto(
        String world1Id,
        String world1Name,
        String world2Id,
        String world2Name,
        boolean divergenceFound,
        Integer divergenceYear,
        Integer divergenceMonth,
        String divergenceDescription,
        List<BranchTimeSeriesPointDto> world1Timeline,
        List<BranchTimeSeriesPointDto> world2Timeline
) {
    public BranchComparisonDto {
        world1Timeline = (world1Timeline != null) ? List.copyOf(world1Timeline) : List.of();
        world2Timeline = (world2Timeline != null) ? List.copyOf(world2Timeline) : List.of();
    }
}

