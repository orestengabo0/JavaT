package com.spring.JavaT.customer;

import com.spring.JavaT.common.EntityStatus;
import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.customer.dto.CreateCustomerRequest;
import com.spring.JavaT.customer.dto.CustomerDto;
import com.spring.JavaT.customer.dto.LinkUserRequest;
import com.spring.JavaT.customer.dto.UpdateCustomerRequest;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.DuplicateResourceException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.user.Role;
import com.spring.JavaT.user.User;
import com.spring.JavaT.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for customer registration and lifecycle management.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository     userRepository;
    private final CustomerMapper     customerMapper;

    // -------------------------------------------------------------------------
    // Create / read / update
    // -------------------------------------------------------------------------

    /**
     * Registers a customer by linking a self-registered portal user.
     * Name, email, and phone are copied from the linked user account.
     */
    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        User user = userRepository.findById(request.getUserId())
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        assertLinkableUser(user);

        String fullNames  = formatFullNames(user);
        String email      = user.getEmail().strip().toLowerCase();
        String phone      = user.getPhone().strip();
        String nationalId = request.getNationalId().strip();
        String address    = request.getAddress().strip();

        assertUniqueForCreate(nationalId, email, phone);

        Customer customer = Customer.builder()
                .user(user)
                .fullNames(fullNames)
                .nationalId(nationalId)
                .email(email)
                .phone(phone)
                .address(address)
                .build();
        customer.setStatus(EntityStatus.ACTIVE);

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public Page<CustomerDto> getAllCustomers(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<Customer> spec = new CustomerSpecification(criteria);
        return customerRepository.findAll(spec, pageable).map(customerMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CustomerDto getCustomerById(UUID id) {
        return customerMapper.toDto(findByIdOrThrow(id));
    }

    @Transactional
    public CustomerDto updateCustomer(UUID id, UpdateCustomerRequest request) {
        Customer customer = findByIdOrThrow(id);

        if (request.getFullNames() != null) {
            customer.setFullNames(request.getFullNames().strip());
        }
        if (request.getPhone() != null) {
            String newPhone = request.getPhone().strip();
            if (!newPhone.equals(customer.getPhone()) && customerRepository.existsByPhone(newPhone)) {
                throw new DuplicateResourceException("Customer", "phone", newPhone);
            }
            customer.setPhone(newPhone);
        }
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().strip().toLowerCase();
            if (!newEmail.equals(customer.getEmail()) && customerRepository.existsByEmail(newEmail)) {
                throw new DuplicateResourceException("Customer", "email", newEmail);
            }
            customer.setEmail(newEmail);
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress().strip());
        }

        return customerMapper.toDto(customerRepository.save(customer));
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Transactional
    public CustomerDto deactivateCustomer(UUID id, String adminEmail) {
        Customer customer = findByIdOrThrow(id);
        customer.softDelete(adminEmail);
        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDto activateCustomer(UUID id) {
        Customer customer = findByIdIncludingDeletedOrThrow(id);
        customer.restore();
        return customerMapper.toDto(customerRepository.save(customer));
    }

    // -------------------------------------------------------------------------
    // Portal user linking
    // -------------------------------------------------------------------------

    @Transactional
    public CustomerDto linkUser(UUID customerId, LinkUserRequest request) {
        Customer customer = findByIdOrThrow(customerId);

        if (customer.getUser() != null) {
            throw new BusinessException(
                    "Customer is already linked to a portal user",
                    HttpStatus.CONFLICT,
                    "CUSTOMER_ALREADY_LINKED"
            );
        }

        User user = userRepository.findByEmail(request.getEmail().strip().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        assertLinkableUser(user);
        customer.setUser(user);
        return customerMapper.toDto(customerRepository.save(customer));
    }

    // -------------------------------------------------------------------------
    // Billing guard — used by BillingService in Phase 5
    // -------------------------------------------------------------------------

    /**
     * Ensures the customer exists, is not soft-deleted, and is {@link EntityStatus#ACTIVE}.
     *
     * @throws BusinessException with {@code CUSTOMER_INACTIVE} when billing is not allowed
     */
    @Transactional(readOnly = true)
    public Customer requireActiveCustomer(UUID customerId) {
        Customer customer = findByIdOrThrow(customerId);
        if (!customer.isActive()) {
            throw new BusinessException(
                    "Inactive customers cannot receive bills",
                    HttpStatus.BAD_REQUEST,
                    "CUSTOMER_INACTIVE"
            );
        }
        return customer;
    }

    /**
     * Resolves the customer record linked to a portal user's email.
     *
     * @throws BusinessException with {@code CUSTOMER_NOT_LINKED} when no customer is linked
     */
    @Transactional(readOnly = true)
    public Customer requireLinkedCustomerForPortalUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return customerRepository.findByUser_Id(user.getId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "No customer record linked to this portal account",
                        HttpStatus.FORBIDDEN,
                        "CUSTOMER_NOT_LINKED"
                ));
    }

    @Transactional(readOnly = true)
    public Customer findByIdOrThrow(UUID id) {
        return customerRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    /** Used by activate — deactivated customers are soft-deleted and hidden from normal lookups. */
    private Customer findByIdIncludingDeletedOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void assertUniqueForCreate(String nationalId, String email, String phone) {
        if (customerRepository.existsByNationalId(nationalId)) {
            throw new DuplicateResourceException("Customer", "nationalId", nationalId);
        }
        if (customerRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Customer", "email", email);
        }
        if (customerRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Customer", "phone", phone);
        }
    }

    private void assertLinkableUser(User user) {
        if (!Role.CUSTOMER.equals(user.getRole())) {
            throw new BusinessException(
                    "Only users with the CUSTOMER role can be linked to a customer record",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_USER_ROLE"
            );
        }
        if (customerRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new DuplicateResourceException("Customer", "userId", user.getId());
        }
    }

    private String formatFullNames(User user) {
        return ("%s %s").formatted(user.getFirstName().strip(), user.getLastName().strip()).strip();
    }
}
