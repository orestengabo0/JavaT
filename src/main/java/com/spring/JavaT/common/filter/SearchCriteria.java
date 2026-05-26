package com.spring.JavaT.common.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a single filter condition applied to a JPA query.
 *
 * <p>Used as input to {@link BaseSpecification} to build a {@code WHERE} clause
 * dynamically without writing custom repository methods for every combination.
 *
 * <p>Example — find active users whose email contains "gmail":
 * <pre>
 * List&lt;SearchCriteria&gt; filters = List.of(
 *     new SearchCriteria("status", SearchCriteria.Op.EQ,  "ACTIVE"),
 *     new SearchCriteria("email",  SearchCriteria.Op.LIKE, "gmail")
 * );
 * </pre>
 */
@Getter
@AllArgsConstructor
public class SearchCriteria {

    /** The entity field name to filter on (must match the Java field name, not the column name). */
    private final String field;

    /** The comparison operator. */
    private final Op operator;

    /** The value to compare against. */
    private final Object value;

    /**
     * Supported filter operators.
     *
     * <p>Keep this list small and add operators only when there is a concrete use case.
     */
    public enum Op {
        /** Exact match: {@code field = value} */
        EQ,

        /** Negation: {@code field != value} */
        NEQ,

        /** Case-insensitive substring match: {@code LOWER(field) LIKE '%value%'} */
        LIKE,

        /** Greater than: {@code field > value} */
        GT,

        /** Greater than or equal: {@code field >= value} */
        GTE,

        /** Less than: {@code field < value} */
        LT,

        /** Less than or equal: {@code field <= value} */
        LTE
    }
}
