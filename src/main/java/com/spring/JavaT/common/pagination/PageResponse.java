package com.spring.JavaT.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic paginated response wrapping a content list and {@link PaginationMeta}.
 *
 * <p>This replaces the earlier {@code PagedResponse} class and follows the
 * structure you defined: content + separate metadata object.
 *
 * <p>Usage:
 * <pre>
 * Page&lt;User&gt; page = userRepository.findAll(spec, pageable);
 * return PageResponse.of(page, userMapper::toDto);
 * </pre>
 *
 * @param <T> the DTO type exposed to clients
 */
@Getter
@Schema(description = "Paginated response with content and metadata")
public class PageResponse<T> {

    @Schema(description = "List of items on the current page")
    private final List<T> content;

    @Schema(description = "Pagination metadata")
    private final PaginationMeta meta;

    private PageResponse(List<T> content, PaginationMeta meta) {
        this.content = content;
        this.meta    = meta;
    }

    /**
     * Builds a {@link PageResponse} from a Spring {@link Page} of the same type.
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), PaginationMeta.of(page));
    }

    /**
     * Builds a {@link PageResponse} by mapping a {@link Page} of one type to another.
     * The most common use case: entity page → DTO page.
     *
     * <pre>
     * Page&lt;User&gt; page = userRepository.findAll(pageable);
     * PageResponse&lt;UserDto&gt; response = PageResponse.of(page, userMapper::toDto);
     * </pre>
     *
     * @param page   the source page (entity type)
     * @param mapper a function that converts each element
     * @param <T>    the target (DTO) type
     * @param <S>    the source (entity) type
     */
    public static <T, S> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        List<T> content = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(content, PaginationMeta.of(page));
    }
}
