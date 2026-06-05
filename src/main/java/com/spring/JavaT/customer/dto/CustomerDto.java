package com.spring.JavaT.customer.dto;

import com.spring.JavaT.common.EntityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Customer profile data")
public class CustomerDto {

    @Schema(example = "1")
    private final UUID id;

    @Schema(example = "Jean Pierre Uwimana")
    private final String fullNames;

    @Schema(example = "1199887766554433")
    private final String nationalId;

    @Schema(example = "jean.uwimana@example.com")
    private final String email;

    @Schema(example = "+250788123456")
    private final String phone;

    @Schema(example = "KG 123 St, Kigali, Rwanda")
    private final String address;

    @Schema(example = "ACTIVE")
    private final EntityStatus status;

    @Schema(description = "Linked portal user id, if any", example = "5")
    private final UUID userId;

    private final Instant createdAt;

    private final Instant updatedAt;
}
