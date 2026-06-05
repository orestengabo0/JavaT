package com.spring.JavaT.billing;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.UUID;

/**
 * Bill list filter — always excludes soft-deleted rows; optional customer scoping.
 */
public class BillSpecification extends BaseSpecification<Bill> {

    private final UUID customerId;

    public BillSpecification(List<SearchCriteria> criteria) {
        this(criteria, null);
    }

    public BillSpecification(List<SearchCriteria> criteria, UUID customerId) {
        super(criteria);
        this.customerId = customerId;
    }

    @Override
    public Predicate toPredicate(Root<Bill> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate base = super.toPredicate(root, query, cb);
        Predicate notDeleted = cb.isFalse(root.get("deleted"));

        if (customerId == null) {
            return cb.and(notDeleted, base);
        }

        Predicate forCustomer = cb.equal(root.get("customer").get("id"), customerId);
        return cb.and(notDeleted, forCustomer, base);
    }
}
