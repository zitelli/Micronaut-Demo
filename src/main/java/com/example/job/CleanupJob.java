package com.example.job;

import com.example.service.OrderService;
import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Requires(property = "scheduled.tasks.cleanup.cron")
public class CleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CleanupJob.class);

    private final OrderService orderService;

    public CleanupJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "${scheduled.tasks.cleanup.cron}")
    void cleanupOldPendingOrders() {
        log.info("Starting cleanup of old pending orders");
        try {
            orderService.cleanupOldPendingOrders();
            log.info("Cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during cleanup", e);
        }
    }
}