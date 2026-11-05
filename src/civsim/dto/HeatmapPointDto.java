package civsim.dto;

/**
 * Phase 3.6 — Data point for continuous intensity field heatmaps (threat, aether, density, prosperity).
 */
public record HeatmapPointDto(
        double x,
        double y,
        double intensity,
        double radius,
        String sourceId,
        String label
) {}

