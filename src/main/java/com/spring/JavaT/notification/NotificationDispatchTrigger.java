package com.spring.JavaT.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Schedules notification email dispatch to run after the current transaction commits,
 * so DB trigger-inserted notification rows are visible before polling.
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatchTrigger {

    private final NotificationService notificationService;

    public void dispatchAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationService.dispatchPendingEmails();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.dispatchPendingEmails();
            }
        });
    }
}
