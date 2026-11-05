package civsim.dto;

/**
 * Command submission and tracking response.
 */
public record CommandStatusDto(
        String commandId,
        String type,
        String status,
        long submittedAt,
        Long completedAt,
        String message,
        Object result
) {}

