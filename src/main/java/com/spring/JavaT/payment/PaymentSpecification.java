package com.spring.JavaT.payment;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.UUID;

/**
 * Payment list filter — optional bill and customer scoping.
 */
public class PaymentSpecification extends BaseSpecification<Payment> {

    private final UUID billId;
    private final UUID customerId;

    public PaymentSpecification(List<SearchCriteria> criteria) {
        this(criteria, null, null);
    }

    public PaymentSpecification(List<SearchCriteria> criteria, UUID billId) {
        this(criteria, billId, null);
    }

    public PaymentSpecification(List<SearchCriteria> criteria, UUID billId, UUID customerId) {
        super(criteria);
        this.billId = billId;
        this.customerId = customerId;
    }

    @Override
    public Predicate toPredicate(Root<Payment> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate base = super.toPredicate(root, query, cb);
        Predicate combined = base;

        if (billId != null) {
            combined = cb.and(combined, cb.equal(root.get("bill").get("id"), billId));
        }

        if (customerId != null) {
            combined = cb.and(combined, cb.equal(root.get("bill").get("customer").get("id"), customerId));
            combined = cb.and(combined, cb.isFalse(root.get("bill").get("deleted")));
        }

        return combined;
    }
}
