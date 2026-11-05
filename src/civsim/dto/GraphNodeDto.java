package civsim.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Node structure formatted for @xyflow/react (React Flow).
 */
public record GraphNodeDto(
        String id,
        String label,
        String type,
        Map<String, Object> data
) {
    public GraphNodeDto {
        data = (data != null) ? Collections.unmodifiableMap(new LinkedHashMap<>(data)) : Map.of();
    }
}
