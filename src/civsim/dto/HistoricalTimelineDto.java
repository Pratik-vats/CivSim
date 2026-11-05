package civsim.dto;

import java.util.List;

/**
 * Historical navigation timeline index across the entire recorded lifetime of a world.
 */
public record HistoricalTimelineDto(
        String worldId,
        int minYear,
        int maxYear,
        int currentLiveYear,
        int currentLiveMonth,
        List<HistoricalSnapshotMetadataDto> availableSnapshots,
        List<Integer> milestoneYears,
        List<Integer> checkpointYears
) {
    public HistoricalTimelineDto {
        availableSnapshots = (availableSnapshots != null) ? List.copyOf(availableSnapshots) : List.of();
        milestoneYears = (milestoneYears != null) ? List.copyOf(milestoneYears) : List.of();
        checkpointYears = (checkpointYears != null) ? List.copyOf(checkpointYears) : List.of();
    }
}

