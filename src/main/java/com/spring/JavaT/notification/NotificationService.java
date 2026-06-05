package com.spring.JavaT.notification;

import com.spring.JavaT.common.filter.SearchCriteria;
import com.spring.JavaT.customer.Customer;
import com.spring.JavaT.customer.CustomerRepository;
import com.spring.JavaT.exception.BusinessException;
import com.spring.JavaT.exception.ResourceNotFoundException;
import com.spring.JavaT.notification.dto.NotificationDto;
import com.spring.JavaT.user.User;
import com.spring.JavaT.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads in-app notifications and dispatches unsent rows as emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository   customerRepository;
    private final UserRepository         userRepository;
    private final NotificationMapper     notificationMapper;
    private final EmailService           emailService;

    @Value("${app.notification.email-batch-size:50}")
    private int emailBatchSize;

    // -------------------------------------------------------------------------
    // Read / mark read
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForAdmin(Boolean read, Pageable pageable) {
        Specification<Notification> spec = new NotificationSpecification(buildCriteria(read));
        return notificationRepository.findAll(spec, pageable).map(notificationMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsForCustomer(String customerEmail, Boolean read, Pageable pageable) {
        Customer customer = requireCustomerForPortalUser(customerEmail);
        Specification<Notification> spec = new NotificationSpecification(buildCriteria(read), customer.getId());
        return notificationRepository.findAll(spec, pageable).map(notificationMapper::toDto);
    }

    @Transactional
    public NotificationDto markAsRead(UUID id, String customerEmail) {
        Customer customer = requireCustomerForPortalUser(customerEmail);
        Notification notification = findByIdOrThrow(id);

        if (!notification.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException(
                    "You do not have access to this notification",
                    HttpStatus.FORBIDDEN,
                    "NOTIFICATION_ACCESS_DENIED"
            );
        }

        notification.setRead(true);
        return notificationMapper.toDto(notificationRepository.save(notification));
    }

    // -------------------------------------------------------------------------
    // Email dispatch — polled by scheduler and triggered after bill/payment commits
    // -------------------------------------------------------------------------

    /**
     * Sends emails for DB-inserted notifications that have not yet been emailed.
     *
     * @return number of notifications processed in this batch
     */
    @Transactional
    public int dispatchPendingEmails() {
        List<Notification> pending = notificationRepository.findByEmailSentFalseOrderByCreatedAtAsc(
                PageRequest.of(0, emailBatchSize)
        );

        for (Notification notification : pending) {
            dispatchEmail(notification);
            notification.setEmailSent(true);
            notificationRepository.save(notification);
        }

        if (!pending.isEmpty()) {
            log.info("Dispatched {} bill/payment notification email(s)", pending.size());
        }

        return pending.size();
    }

    private void dispatchEmail(Notification notification) {
        Customer customer = notification.getCustomer();
        emailService.sendBillNotificationEmail(
                customer.getEmail(),
                customer.getFullNames(),
                notification.getMessage()
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<SearchCriteria> buildCriteria(Boolean read) {
        List<SearchCriteria> criteria = new ArrayList<>();
        if (read != null) {
            criteria.add(new SearchCriteria("read", SearchCriteria.Op.EQ, read));
        }
        return criteria;
    }

    private Customer requireCustomerForPortalUser(String email) {
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

    private Notification findByIdOrThrow(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
    }
}
