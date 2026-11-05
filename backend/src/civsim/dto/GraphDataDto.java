package civsim.dto;

import java.util.List;

/**
 * Composite graph dataset for @xyflow/react (nodes + edges).
 */
public record GraphDataDto(
        List<GraphNodeDto> nodes,
        List<GraphEdgeDto> edges
) {
    public GraphDataDto {
        nodes = (nodes != null) ? List.copyOf(nodes) : List.of();
        edges = (edges != null) ? List.copyOf(edges) : List.of();
    }
}

