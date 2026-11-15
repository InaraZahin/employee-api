package io.github.inarazahin.employees.employee;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A stable JSON shape for paged results, so the API contract doesn't change with framework
 * defaults: {@code { "content": [...], "page": { "number", "size", "totalElements", "totalPages" } }}.
 */
public record PageResponse<T>(List<T> content, PageMetadata page) {

    public record PageMetadata(int number, int size, long totalElements, int totalPages) {
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(),
                new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
