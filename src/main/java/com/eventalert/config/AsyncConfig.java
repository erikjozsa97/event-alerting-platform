package com.eventalert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Provides the dedicated thread pool background delivery dispatch runs on.
 */
@Configuration
public class AsyncConfig {

    // Ingestion-triggered deliveries (DeliveryService#deliverForEvent) run here instead
    // of on the scheduler thread — each channel send can involve up to 3 retries with
    // backoff, which would otherwise stall the next poll cycle behind a slow channel.
    @Bean(name = "deliveryExecutor")
    @NonNull
    public Executor deliveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("delivery-");
        executor.initialize();
        return executor;
    }
}
