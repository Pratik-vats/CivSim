package civsim.dto;

/**
 * Individual entry in a leaderboard ranking.
 */
public record LeaderboardEntryDto(
        int rank,
        String entityId,
        String entityName,
        double score,
        String subtitle,
        String details
) {}

