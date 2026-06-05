package com.spring.JavaT.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for unsent notification rows and dispatches them as emails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEmailScheduler {

    private final NotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.notification.email-poll-ms:30000}")
    public void pollAndDispatchEmails() {
        try {
            notificationService.dispatchPendingEmails();
        } catch (Exception e) {
            log.error("Notification email dispatch failed: {}", e.getMessage(), e);
        }
    }
}
