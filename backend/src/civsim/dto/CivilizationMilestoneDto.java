package civsim.dto;

/**
 * Phase 3.4 — Epochal milestone in civilization history.
 */
public record CivilizationMilestoneDto(
        int year,
        int month,
        String title,
        String description,
        String category,
        String era,
        String importance
) {}

