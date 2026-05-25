package com.spring.JavaT.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Reusable query-parameter object for paginated endpoints.
 *
 * <p>Controllers declare it as a {@code @ModelAttribute} or {@code @ParameterObject}
 * (springdoc) parameter so Swagger renders each field as an individual query param:
 *
 * <pre>
 * GET /api/v1/users?page=0&amp;size=10&amp;sortBy=createdAt&amp;sortDir=desc
 * </pre>
 *
 * <p>Usage in a controller:
 * <pre>
 * {@literal @}GetMapping
 * public ResponseEntity&lt;...&gt; list(
 *         {@literal @}ParameterObject {@literal @}Valid PageableRequest pageableRequest,
 *         HttpServletRequest request) {
 *
 *     Page&lt;UserDto&gt; page = userService.findAll(pageableRequest.toPageable());
 *     return ResponseBuilder.ok(page, "Users retrieved successfully", request);
 * }
 * </pre>
 */
@Getter
@Setter
@Schema(description = "Pagination and sorting parameters")
public class PageableRequest {

    /** Default page size applied when none is provided. */
    public static final int DEFAULT_SIZE = 10;

    /** Hard cap on page size to prevent clients from requesting huge result sets. */
    public static final int MAX_SIZE = 100;

    @Min(value = 0, message = "Page number must be 0 or greater")
    @Schema(description = "Zero-based page number", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = MAX_SIZE, message = "Page size must not exceed " + MAX_SIZE)
    @Schema(description = "Number of items per page (max " + MAX_SIZE + ")", example = "10", defaultValue = "10")
    private int size = DEFAULT_SIZE;

    @Schema(description = "Field name to sort by", example = "createdAt", defaultValue = "id")
    private String sortBy = "id";

    @Schema(description = "Sort direction: asc or desc", example = "desc", defaultValue = "asc",
            allowableValues = {"asc", "desc"})
    private String sortDir = "asc";

    /**
     * Converts this request into a Spring {@link Pageable} ready to pass to a repository.
     *
     * @return a {@link PageRequest} with the configured page, size, and sort
     */
    public Pageable toPageable() {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}
