package com.spring.JavaT.notification;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.UUID;

public class NotificationSpecification extends BaseSpecification<Notification> {

    private final UUID customerId;

    public NotificationSpecification(List<SearchCriteria> criteria) {
        this(criteria, null);
    }

    public NotificationSpecification(List<SearchCriteria> criteria, UUID customerId) {
        super(criteria);
        this.customerId = customerId;
    }

    @Override
    public Predicate toPredicate(Root<Notification> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate base = super.toPredicate(root, query, cb);

        if (customerId == null) {
            return base;
        }

        return cb.and(base, cb.equal(root.get("customer").get("id"), customerId));
    }
}
