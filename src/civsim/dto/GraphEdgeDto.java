package civsim.dto;

/**
 * Edge structure formatted for @xyflow/react (React Flow).
 */
public record GraphEdgeDto(
        String id,
        String source,
        String target,
        String type,
        String label
) {}

