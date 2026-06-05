package com.spring.JavaT.meter;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** JPA specification that always excludes soft-deleted meters. */
public class MeterSpecification extends BaseSpecification<Meter> {

    public MeterSpecification(List<SearchCriteria> criteria) {
        super(criteria);
    }

    @Override
    public Predicate toPredicate(Root<Meter> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate filters = super.toPredicate(root, query, cb);
        Predicate notDeleted = cb.isFalse(root.get("deleted"));
        return cb.and(filters, notDeleted);
    }
}
