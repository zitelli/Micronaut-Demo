package com.example.service;

import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.OrderStatus;
import com.example.repository.OrderItemRepository;
import com.example.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    OrderEventPublisher eventPublisher;

    @Mock
    ObjectMapper objectMapper;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, eventPublisher, objectMapper);
    }

    @Test
    void createOrderCalculatesTotalAndPublishesEvent() throws Exception {
        OrderItem item = new OrderItem("p1", "Widget", 2, new BigDecimal("9.99"));

        // 1. orderRepository.save(order) - insere o pedido "vazio" primeiro
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        // 2. orderItemRepository.save(item) - insere cada item já com orderId
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });

        // 3. orderRepository.update(order) - grava o total calculado
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 4. orderItemRepository.findByOrderId(1L) - recarrega os itens pra devolver na resposta
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));

        when(objectMapper.writeValueAsString(any(OrderEvent.class)))
                .thenReturn("{\"orderId\":1,\"status\":\"PENDING\"}");

        Order result = orderService.createOrder("cust-1", List.of(item));

        assertEquals(0, new BigDecimal("19.98").compareTo(result.getTotalAmount()));
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(eventPublisher).publishOrderCreated(eq("1"), anyString());
    }

    @Test
    void updateStatusThrowsWhenOrderMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.updateStatus(99L, OrderStatus.PROCESSING));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void cancelOrderPublishesCancelledEvent() throws Exception {
        Order existing = new Order("cust-1", java.time.LocalDateTime.now());
        existing.setId(5L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(orderRepository.update(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objectMapper.writeValueAsString(any(OrderEvent.class)))
                .thenReturn("{\"orderId\":5,\"status\":\"CANCELLED\"}");

        orderService.cancelOrder(5L);

        assertEquals(OrderStatus.CANCELLED, existing.getStatus());
        verify(eventPublisher).publishOrderCancelled(eq("5"), anyString());
    }
}