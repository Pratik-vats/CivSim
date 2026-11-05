package civsim.dto;

/**
 * Detailed 5-layer score for a single attribute.
 */
public record AttributeScoresDto(
        String name,
        double baseline,
        double awakening,
        double development,
        double temporary,
        double effective
) {}

