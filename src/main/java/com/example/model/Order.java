package com.example.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@MappedEntity("order")
@Serdeable
@Introspected
public class Order {

    @Id
    @GeneratedValue(GeneratedValue.Type.IDENTITY)
    private Long id;

    @NotBlank
    private String customerId;

    @NotNull
    private LocalDateTime orderDate;

    @NotNull
    @PositiveOrZero
    private BigDecimal totalAmount;

    @TypeDef(type = DataType.STRING)
    private OrderStatus status;

    // Não persistido - preenchido manualmente pelo OrderService, já que
    // Micronaut Data JDBC não faz cascade/hidratação automática de
    // relacionamentos como o Hibernate fazia.
    @Transient
    private List<OrderItem> items;

    public Order() {}

    public Order(String customerId, LocalDateTime orderDate) {
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}