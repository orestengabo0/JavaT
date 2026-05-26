package com.spring.JavaT.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata extracted from a Spring {@link Page}.
 *
 * <p>Kept separate from the content list so consumers can read metadata
 * without iterating the items.
 */
@Getter
@Builder
@Schema(description = "Pagination metadata")
public class PaginationMeta {

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private final int page;

    @Schema(description = "Number of items per page", example = "10")
    private final int size;

    @Schema(description = "Total number of items across all pages", example = "47")
    private final long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    private final int totalPages;

    @Schema(description = "Whether this is the first page", example = "true")
    private final boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    private final boolean last;

    @Schema(description = "Whether the current page has no content", example = "false")
    private final boolean empty;

    /** Builds a {@link PaginationMeta} from any Spring {@link Page}. */
    public static PaginationMeta of(Page<?> page) {
        return PaginationMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
