package com.example.service;

import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.OrderStatus;
import com.example.repository.OrderItemRepository;
import com.example.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        OrderEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(String customerId, List<OrderItem> items) {
        Order order = new Order(customerId, LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            item.setId(null);
            item.setOrderId(savedOrder.getId());
            OrderItem savedItem = orderItemRepository.save(item);
            total = total.add(savedItem.getSubtotal());
        }
        savedOrder.setTotalAmount(total);
        savedOrder = orderRepository.update(savedOrder);
        savedOrder.setItems(orderItemRepository.findByOrderId(savedOrder.getId()));

        publishEvent(savedOrder, "CREATED");
        return savedOrder;
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id).map(this::withItems);
    }

    public Optional<Order> findByIdAndCustomer(Long id, String customerId) {
        return orderRepository.findByIdAndCustomerId(id, customerId).map(this::withItems);
    }

    public List<Order> findByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(this::withItems).toList();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream().map(this::withItems).toList();
    }

    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::withItems);
    }

    public List<String> findAllCustomerIds() {
        return orderRepository.findDistinctCustomerIds();
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.update(order);
        updatedOrder.setItems(orderItemRepository.findByOrderId(id));
        publishEvent(updatedOrder, "STATUS_CHANGED");
        return updatedOrder;
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.update(order);
        publishEvent(order, "CANCELLED");
    }

    public long getOrderCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Transactional
    public void cleanupOldPendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<Order> oldOrders = orderRepository.findOldOrdersByStatus(OrderStatus.PENDING, cutoff);
        oldOrders.forEach(order -> {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.update(order);
        });
    }

    private Order withItems(Order order) {
        order.setItems(orderItemRepository.findByOrderId(order.getId()));
        return order;
    }

    private void publishEvent(Order order, String eventType) {
        OrderEvent event = new OrderEvent(order.getId(), order.getCustomerId(), order.getStatus(), eventType);
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            switch (eventType) {
                case "CREATED" -> eventPublisher.publishOrderCreated(order.getId().toString(), eventJson);
                case "STATUS_CHANGED" -> eventPublisher.publishOrderStatusChanged(order.getId().toString(), eventJson);
                case "CANCELLED" -> eventPublisher.publishOrderCancelled(order.getId().toString(), eventJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize order event", e);
        }
    }
}