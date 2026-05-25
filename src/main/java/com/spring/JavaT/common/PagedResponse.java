package com.spring.JavaT.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagination metadata wrapper used as the {@code data} payload inside {@link ApiResponse}
 * whenever a paginated list is returned.
 *
 * <p>Construct it from a Spring {@link Page} using the static factory:
 * <pre>
 *     Page&lt;UserDto&gt; page = userService.findAll(pageable);
 *     return ResponseBuilder.ok(PagedResponse.of(page), request);
 * </pre>
 *
 * <p>The resulting JSON looks like:
 * <pre>
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 47,
 *   "totalPages": 5,
 *   "first": true,
 *   "last": false,
 *   "empty": false
 * }
 * </pre>
 *
 * @param <T> the type of elements in the page
 */
@Getter
@Schema(description = "Paginated response payload containing content and pagination metadata")
public class PagedResponse<T> {

    @Schema(description = "List of items on the current page")
    private final List<T> content;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private final int page;

    @Schema(description = "Number of items requested per page", example = "10")
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

    private PagedResponse(List<T> content, int page, int size,
                          long totalElements, int totalPages,
                          boolean first, boolean last, boolean empty) {
        this.content       = content;
        this.page          = page;
        this.size          = size;
        this.totalElements = totalElements;
        this.totalPages    = totalPages;
        this.first         = first;
        this.last          = last;
        this.empty         = empty;
    }

    /**
     * Creates a {@code PagedResponse} directly from a Spring {@link Page}.
     *
     * @param springPage the Spring Data page result
     * @param <T>        the element type
     * @return a populated {@code PagedResponse}
     */
    public static <T> PagedResponse<T> of(Page<T> springPage) {
        return new PagedResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast(),
                springPage.isEmpty()
        );
    }

    /**
     * Creates a {@code PagedResponse} when you have already mapped the content
     * to a different type (e.g. entity → DTO) but still hold the original {@link Page}
     * for its metadata.
     *
     * <pre>
     *     Page&lt;User&gt; page = userRepository.findAll(pageable);
     *     List&lt;UserDto&gt; dtos = page.getContent().stream()
     *             .map(mapper::toDto)
     *             .toList();
     *     return PagedResponse.of(dtos, page);
     * </pre>
     *
     * @param mappedContent the already-mapped list of items
     * @param sourcePage    the original page used only for metadata
     * @param <T>           the mapped element type
     * @param <S>           the source element type
     * @return a populated {@code PagedResponse}
     */
    public static <T, S> PagedResponse<T> of(List<T> mappedContent, Page<S> sourcePage) {
        return new PagedResponse<>(
                mappedContent,
                sourcePage.getNumber(),
                sourcePage.getSize(),
                sourcePage.getTotalElements(),
                sourcePage.getTotalPages(),
                sourcePage.isFirst(),
                sourcePage.isLast(),
                mappedContent.isEmpty()
        );
    }
}
