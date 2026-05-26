package com.spring.JavaT.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link User} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@link com.spring.JavaT.common.filter.BaseSpecification}.
 *
 * <p>Soft-deleted users are not automatically excluded here — add
 * {@code @SQLRestriction("deleted = false")} to the {@link User} entity
 * if you want that behaviour globally, or use explicit {@code findByDeletedFalse}
 * variants for specific queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
