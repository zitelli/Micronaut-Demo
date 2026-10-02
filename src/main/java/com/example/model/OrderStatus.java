package com.example.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum OrderStatus {
    PENDING, PROCESSING, COMPLETED, CANCELLED
}
