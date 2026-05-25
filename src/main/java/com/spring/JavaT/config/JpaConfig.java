package com.spring.JavaT.config;

import com.spring.JavaT.audit.AuditAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration.
 *
 * <p>{@code @EnableJpaAuditing} activates Spring Data JPA's auditing support,
 * which drives the automatic population of:
 * <ul>
 *   <li>{@code @CreatedDate}     → {@code BaseEntity.createdAt}</li>
 *   <li>{@code @LastModifiedDate}→ {@code BaseEntity.updatedAt}</li>
 *   <li>{@code @CreatedBy}       → {@code BaseEntity.createdBy}</li>
 *   <li>{@code @LastModifiedBy}  → {@code BaseEntity.updatedBy}</li>
 * </ul>
 *
 * <p>The {@code auditorAwareRef} attribute points to the {@link AuditAwareImpl}
 * bean by name, which resolves the current principal from the Spring Security context.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
public class JpaConfig {

    /**
     * Explicitly declares {@link AuditAwareImpl} as a bean so the
     * {@code auditorAwareRef} reference is unambiguous even if component
     * scanning is narrowly scoped.
     */
    @Bean
    public AuditAwareImpl auditAwareImpl() {
        return new AuditAwareImpl();
    }
}
