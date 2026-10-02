package com.example.service;

import com.example.model.OrderStatus;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

@Serdeable
public record OrderEvent(
        Long orderId,
        String customerId,
        OrderStatus status,
        String eventType,
        LocalDateTime timestamp
) {
    public OrderEvent(Long orderId, String customerId, OrderStatus status, String eventType) {
        this(orderId, customerId, status, eventType, LocalDateTime.now());
    }
}