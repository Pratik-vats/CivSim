package civsim.dto;

/**
 * Historical event record summary.
 */
public record EventDto(
        int eventId,
        int year,
        int month,
        long totalMonths,
        String category,
        String summary,
        String severity
) {}

