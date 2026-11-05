package civsim.dto;

/**
 * Parameter payload for deterministic new world generation.
 */
public record NewWorldRequest(
        String worldName,
        Long seed,
        Boolean fantasyMode,
        Integer awakeningThreshold,
        String extinctionPolicy,
        Integer targetYear
) {}

