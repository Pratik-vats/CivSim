package civsim.dto;

import java.util.List;

/**
 * Historical/current leaderboard ranking for a given category.
 */
public record LeaderboardDto(
        String category,
        int year,
        int month,
        boolean historicalAvailable,
        String unavailableMessage,
        List<LeaderboardEntryDto> entries
) {
    public LeaderboardDto {
        entries = (entries != null) ? List.copyOf(entries) : List.of();
    }

    public LeaderboardDto(String category, int year, int month, List<LeaderboardEntryDto> entries) {
        this(category, year, month, true, null, entries);
    }
}
