package com.spring.JavaT.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByNationalId(String nationalId);

    Optional<Customer> findByUser_Id(UUID userId);

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
