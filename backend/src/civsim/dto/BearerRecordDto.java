package civsim.dto;

/**
 * Historical bearer chronicle record for an heirloom.
 */
public record BearerRecordDto(
        String bearerId,
        String bearerName,
        String tenureInfo,
        String successionReason
) {}

