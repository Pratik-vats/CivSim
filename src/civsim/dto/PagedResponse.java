package civsim.dto;

import java.util.List;

/**
 * Standard paginated container for list queries.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PagedResponse {
        content = (content != null) ? List.copyOf(content) : List.of();
    }

    public static <T> PagedResponse<T> of(List<T> allItems, int page, int size) {
        if (allItems == null || allItems.isEmpty()) {
            return new PagedResponse<>(List.of(), page, size, 0, 0);
        }
        int total = allItems.size();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int totalPages = (int) Math.ceil((double) total / safeSize);

        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        List<T> slice = allItems.subList(fromIndex, toIndex);
        return new PagedResponse<>(slice, safePage, safeSize, total, totalPages);
    }
}

