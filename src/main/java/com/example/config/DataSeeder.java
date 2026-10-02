package com.example.config;

import com.example.model.Order;
import com.example.model.OrderItem;
import com.example.model.OrderStatus;
import com.example.repository.OrderItemRepository;
import com.example.repository.OrderRepository;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Popula o banco com dados de exemplo no startup, cobrindo os 4 valores de
 * OrderStatus. Usa o repositório diretamente (não OrderService.createOrder)
 * para não depender do Kafka estar disponível já na inicialização.
 * Idempotente: só semeia se a tabela estiver vazia, então não duplica dados
 * em reinícios com um banco persistente (Postgres); com H2 em memória isso
 * roda a cada start, já que o banco é recriado do zero de qualquer forma.
 */
@Singleton
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public DataSeeder(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @EventListener
    @Transactional
    void onStartup(StartupEvent event) {
        if (orderRepository.count() > 0) {
            log.info("Seed de dados pulado - já existem pedidos no banco");
            return;
        }

        seedOrder("carlos", OrderStatus.PENDING,
                new OrderItem("p1", "Teclado Mecânico", 1, new BigDecimal("350.00")),
                new OrderItem("p2", "Mouse Gamer", 2, new BigDecimal("120.00")));

        seedOrder("manuel", OrderStatus.PROCESSING,
                new OrderItem("p3", "Monitor 27\"", 1, new BigDecimal("1200.00")));

        seedOrder("ana", OrderStatus.COMPLETED,
                new OrderItem("p4", "Notebook", 1, new BigDecimal("4500.00")),
                new OrderItem("p5", "Mochila", 1, new BigDecimal("150.00")),
                new OrderItem("p6", "Suporte para Notebook", 1, new BigDecimal("90.00")));

        seedOrder("beatriz", OrderStatus.CANCELLED,
                new OrderItem("p7", "Cadeira Gamer", 1, new BigDecimal("980.00")));

        log.info("Seed de dados concluído: 4 pedidos de exemplo criados");
    }

    private void seedOrder(String customerId, OrderStatus status, OrderItem... items) {
        Order order = new Order(customerId, LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            item.setOrderId(savedOrder.getId());
            OrderItem savedItem = orderItemRepository.save(item);
            total = total.add(savedItem.getSubtotal());
        }
        savedOrder.setStatus(status);
        savedOrder.setTotalAmount(total);
        orderRepository.update(savedOrder);
    }
}