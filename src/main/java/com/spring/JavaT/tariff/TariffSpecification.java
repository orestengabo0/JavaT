package com.spring.JavaT.tariff;

import com.spring.JavaT.common.filter.BaseSpecification;
import com.spring.JavaT.common.filter.SearchCriteria;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/** Excludes soft-deleted tariff versions from list queries. */
public class TariffSpecification extends BaseSpecification<TariffVersion> {

    public TariffSpecification(List<SearchCriteria> criteria) {
        super(criteria);
    }

    @Override
    public Predicate toPredicate(Root<TariffVersion> root,
                                 jakarta.persistence.criteria.CriteriaQuery<?> query,
                                 jakarta.persistence.criteria.CriteriaBuilder cb) {
        Predicate filters = super.toPredicate(root, query, cb);
        Predicate notDeleted = cb.isFalse(root.get("deleted"));
        return cb.and(filters, notDeleted);
    }
}
