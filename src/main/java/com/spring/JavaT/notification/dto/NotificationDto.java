package com.spring.JavaT.notification.dto;

import com.spring.JavaT.notification.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Customer notification")
public class NotificationDto {

    private final UUID id;
    private final UUID billId;
    private final String message;
    private final NotificationChannel channel;
    private final boolean read;
    private final Instant createdAt;
}
