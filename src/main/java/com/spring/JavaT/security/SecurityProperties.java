package com.spring.JavaT.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typed binding for {@code app.security.*} properties.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * Paths that bypass JWT authentication entirely.
     * Configured as a comma-separated list in {@code application.properties}.
     */
    private List<String> publicPaths = List.of();
}
