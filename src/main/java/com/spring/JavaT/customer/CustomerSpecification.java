package com.spring.JavaT.customer;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** JPA specification that always excludes soft-deleted customers. */
public class CustomerSpecification extends BaseSpecification<Customer> {

    public CustomerSpecification(List<SearchCriteria> criteria) {
        super(criteria);
    }

    @Override
    public Predicate toPredicate(Root<Customer> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate filters = super.toPredicate(root, query, cb);
        Predicate notDeleted = cb.isFalse(root.get("deleted"));
        return cb.and(filters, notDeleted);
    }
}
