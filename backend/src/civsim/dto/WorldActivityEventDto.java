package civsim.dto;

import java.util.List;

/**
 * Phase 3.4 — Unified World Activity Event Record.
 */
public record WorldActivityEventDto(
        String id,
        String worldId,
        int year,
        int month,
        long totalMonths,
        int sequence,
        String category,
        String subtype,
        String title,
        String summary,
        String severity,
        String location,
        Double posX,
        Double posY,
        String primaryEntityId,
        String primaryEntityType,
        List<String> relatedEntityIds,
        String dataAvailability
) {
    public WorldActivityEventDto {
        relatedEntityIds = (relatedEntityIds != null) ? List.copyOf(relatedEntityIds) : List.of();
    }
}

