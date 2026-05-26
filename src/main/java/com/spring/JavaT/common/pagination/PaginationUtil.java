package com.spring.JavaT.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Utility methods for building and validating {@link Pageable} instances.
 *
 * <p>Centralises defaults and guards so every paginated endpoint behaves
 * consistently without repeating the same logic.
 */
public final class PaginationUtil {

    // -------------------------------------------------------------------------
    // Defaults — change here to affect every paginated endpoint
    // -------------------------------------------------------------------------

    public static final int DEFAULT_PAGE      = 0;
    public static final int DEFAULT_SIZE      = 10;
    public static final int MAX_SIZE          = 100;
    public static final String DEFAULT_SORT   = "id";
    public static final String DEFAULT_DIR    = "asc";

    private PaginationUtil() {}

    /**
     * Builds a {@link Pageable} from raw query parameters, applying defaults
     * and capping the size at {@link #MAX_SIZE}.
     *
     * @param page    zero-based page number (null → {@link #DEFAULT_PAGE})
     * @param size    items per page (null → {@link #DEFAULT_SIZE}, capped at {@link #MAX_SIZE})
     * @param sortBy  field to sort by (null → {@link #DEFAULT_SORT})
     * @param sortDir {@code "asc"} or {@code "desc"} (null → {@link #DEFAULT_DIR})
     * @return a ready-to-use {@link Pageable}
     */
    public static Pageable toPageable(Integer page, Integer size, String sortBy, String sortDir) {
        int resolvedPage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int resolvedSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        String resolvedSort = (sortBy != null && !sortBy.isBlank()) ? sortBy : DEFAULT_SORT;
        String resolvedDir  = (sortDir != null && !sortDir.isBlank()) ? sortDir : DEFAULT_DIR;

        Sort sort = "desc".equalsIgnoreCase(resolvedDir)
                ? Sort.by(resolvedSort).descending()
                : Sort.by(resolvedSort).ascending();

        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }

    /**
     * Builds a {@link Pageable} from raw query parameters, additionally validating
     * that the requested sort field is in the allowed set.
     *
     * <p>If the requested sort field is not allowed, falls back to {@link #DEFAULT_SORT}
     * silently — this prevents clients from probing internal field names.
     *
     * @param page          zero-based page number
     * @param size          items per page
     * @param sortBy        field to sort by
     * @param sortDir       {@code "asc"} or {@code "desc"}
     * @param allowedFields set of field names the client is permitted to sort by
     * @return a ready-to-use {@link Pageable}
     */
    public static Pageable toPageable(Integer page, Integer size, String sortBy, String sortDir,
                                      Set<String> allowedFields) {
        String safeSort = (sortBy != null && allowedFields.contains(sortBy)) ? sortBy : DEFAULT_SORT;
        return toPageable(page, size, safeSort, sortDir);
    }
}
