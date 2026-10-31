package civsim.dto;

/**
 * Compact search hit representing a navigable entity in the world.
 */
public record SearchResultDto(
        String id,
        String name,
        String type,
        double x,
        double y,
        String description,
        String subInfo
) {}

