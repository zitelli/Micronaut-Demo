package com.example.repository;

import com.example.model.Order;
import com.example.model.OrderStatus;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.ANSI)
public interface OrderRepository extends PageableRepository<Order, Long> {

    List<Order> findByCustomerId(String customerId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdAndCustomerId(Long id, String customerId);

    // JDBC repositories usam SQL puro, não JPQL - repare que não precisa
    // de nativeQuery=true, isso já é o padrão aqui.
    @Query("SELECT * FROM \"order\" WHERE status = :status AND order_date < :cutoffDate")
    List<Order> findOldOrdersByStatus(OrderStatus status, LocalDateTime cutoffDate);

    @Query("SELECT DISTINCT customer_id FROM \"order\" ORDER BY customer_id")
    List<String> findDistinctCustomerIds();

    long countByStatus(OrderStatus status);
}