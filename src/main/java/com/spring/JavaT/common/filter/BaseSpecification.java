package com.spring.JavaT.common.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic JPA {@link Specification} that builds a {@code WHERE} clause from a
 * list of {@link SearchCriteria}.
 *
 * <p>All criteria are combined with {@code AND}. Extend this class to add
 * domain-specific logic (e.g. always exclude soft-deleted rows).
 *
 * <p>Usage — filter users by role and status:
 * <pre>
 * Specification&lt;User&gt; spec = new BaseSpecification&lt;&gt;(List.of(
 *     new SearchCriteria("role",   SearchCriteria.Op.EQ, "ADMIN"),
 *     new SearchCriteria("status", SearchCriteria.Op.EQ, "ACTIVE")
 * ));
 * Page&lt;User&gt; page = userRepository.findAll(spec, pageable);
 * </pre>
 *
 * <p>For domain-specific filtering, subclass and add fixed predicates:
 * <pre>
 * public class UserSpecification extends BaseSpecification&lt;User&gt; {
 *     public UserSpecification(List&lt;SearchCriteria&gt; criteria) {
 *         super(criteria);
 *     }
 *
 *     {@literal @}Override
 *     public Predicate toPredicate(Root&lt;User&gt; root, CriteriaQuery&lt;?&gt; query, CriteriaBuilder cb) {
 *         // Always exclude soft-deleted users
 *         Predicate base    = super.toPredicate(root, query, cb);
 *         Predicate notDel  = cb.isFalse(root.get("deleted"));
 *         return cb.and(base, notDel);
 *     }
 * }
 * </pre>
 *
 * @param <T> the entity type
 */
public class BaseSpecification<T> implements Specification<T> {

    private final List<SearchCriteria> criteria;

    public BaseSpecification(List<SearchCriteria> criteria) {
        this.criteria = criteria != null ? criteria : List.of();
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        for (SearchCriteria criterion : criteria) {
            Predicate predicate = buildPredicate(criterion, root, cb);
            if (predicate != null) {
                predicates.add(predicate);
            }
        }

        return predicates.isEmpty()
                ? cb.conjunction()                          // no filters → match everything
                : cb.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate buildPredicate(SearchCriteria criterion, Root<T> root, CriteriaBuilder cb) {
        String field  = criterion.getField();
        Object value  = criterion.getValue();

        return switch (criterion.getOperator()) {
            case EQ   -> cb.equal(root.get(field), value);
            case NEQ  -> cb.notEqual(root.get(field), value);
            case LIKE -> cb.like(
                            cb.lower(root.get(field)),
                            "%" + value.toString().toLowerCase() + "%"
                         );
            case GT   -> cb.greaterThan(root.get(field), value.toString());
            case GTE  -> cb.greaterThanOrEqualTo(root.get(field), value.toString());
            case LT   -> cb.lessThan(root.get(field), value.toString());
            case LTE  -> cb.lessThanOrEqualTo(root.get(field), value.toString());
        };
    }
}
