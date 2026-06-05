package com.spring.JavaT.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration for the utility billing API.
 *
 * <p>Endpoints are grouped by domain via {@code @Tag} on each controller.
 * A single OpenAPI document is used so Swagger UI lists every endpoint.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "WASAC Utility Billing API",
                version     = "1.0",
                description = """
                        End-to-end utility billing system — customer & meter management, \
                        meter readings, tariff configuration, bill generation & approval, \
                        payments, and notifications.
                        """,
                contact     = @Contact(name = "WASAC Utility Billing")
        )
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
}
