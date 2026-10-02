package com.example.controller;

import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.OrderStatus;
import com.example.service.OrderService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@Controller("/api/v1/orders")
@Secured(SecurityRule.IS_AUTHENTICATED)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Post
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided items")
    @ApiResponse(responseCode = "201", description = "Order created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public HttpResponse<Order> createOrder(
            @Parameter(description = "Customer ID") @QueryValue String customerId,
            @Body @Valid List<OrderItem> items) {
        Order order = orderService.createOrder(customerId, items);
        return HttpResponse.created(order);
    }

    @Get("/{id}")
    @Operation(summary = "Get order by ID")
    public HttpResponse<Order> getOrder(@PathVariable Long id) {
        return orderService.findById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get
    @Operation(summary = "Get orders with pagination")
    public HttpResponse<Page<Order>> getOrders(
            @Parameter(description = "Page number") @QueryValue(defaultValue = "0") int page,
            @Parameter(description = "Page size") @QueryValue(defaultValue = "20") int size) {
        return HttpResponse.ok(orderService.findAll(Pageable.from(page, size)));
    }

    @Get("/customer/{customerId}")
    @Operation(summary = "Get orders by customer")
    public HttpResponse<List<Order>> getOrdersByCustomer(@PathVariable String customerId) {
        return HttpResponse.ok(orderService.findByCustomer(customerId));
    }

    @Get("/customers")
    @Operation(summary = "List all customer IDs", description = "Returns the distinct customer IDs that have placed at least one order")
    public HttpResponse<List<String>> getAllCustomerIds() {
        return HttpResponse.ok(orderService.findAllCustomerIds());
    }

    @Get("/status/{status}")
    @Operation(summary = "Get orders by status")
    public HttpResponse<List<Order>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return HttpResponse.ok(orderService.findByStatus(status));
    }

    @Put("/{id}/status")
    @Operation(summary = "Update order status")
    public HttpResponse<Order> updateStatus(
            @PathVariable Long id,
            @Body @Valid StatusUpdateRequest request) {
        return HttpResponse.ok(orderService.updateStatus(id, request.status()));
    }

    @Delete("/{id}")
    @Operation(summary = "Cancel order")
    public HttpResponse<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return HttpResponse.noContent();
    }

    @Get("/stats")
    @Operation(summary = "Get order statistics")
    @Secured("ADMIN")
    public HttpResponse<OrderStats> getStats() {
        return HttpResponse.ok(new OrderStats(
                orderService.getOrderCountByStatus(OrderStatus.PENDING),
                orderService.getOrderCountByStatus(OrderStatus.PROCESSING),
                orderService.getOrderCountByStatus(OrderStatus.COMPLETED),
                orderService.getOrderCountByStatus(OrderStatus.CANCELLED)
        ));
    }

    @Serdeable
    public record StatusUpdateRequest(OrderStatus status) {}
    @Serdeable
    public record OrderStats(long pending, long processing, long completed, long cancelled) {}
}