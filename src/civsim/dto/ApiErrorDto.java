package civsim.dto;

/**
 * Standard structured API error response.
 */
public record ApiErrorDto(
        long timestamp,
        int status,
        String error,
        String message,
        String path
) {}

