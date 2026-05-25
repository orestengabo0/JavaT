package com.spring.JavaT.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration.
 *
 * <p>{@code @EnableAsync} activates Spring's {@code @Async} annotation processing.
 * Without this class, {@code @Async} methods run synchronously.
 *
 * <p>A dedicated thread pool is defined for email sending so that:
 * <ul>
 *   <li>Email threads are isolated from the main request-handling threads.</li>
 *   <li>Pool size is tunable via {@code application.properties} without code changes.</li>
 *   <li>Thread names are prefixed for easy identification in logs and thread dumps.</li>
 * </ul>
 *
 * <p>The bean is named {@code "emailTaskExecutor"} and referenced explicitly in
 * {@link com.spring.JavaT.notification.EmailService} via
 * {@code @Async("emailTaskExecutor")}.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size:5}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${app.async.thread-name-prefix:async-email-}")
    private String threadNamePrefix;

    /**
     * Thread pool executor dedicated to async email sending.
     *
     * <p>Pool sizing rationale:
     * <ul>
     *   <li>{@code corePoolSize=2}   — always-on threads; handles normal load.</li>
     *   <li>{@code maxPoolSize=5}    — burst capacity for spikes.</li>
     *   <li>{@code queueCapacity=100}— tasks queue here before new threads are spawned.</li>
     * </ul>
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Email task executor initialized — core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}
