package com.example.controller;

import com.example.model.OrderItem;
import com.example.service.OrderEventPublisher;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class OrderControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    // Without this, OrderService.createOrder() tries to publish to a real
    // Kafka broker at localhost:9092 during the test and the request fails
    // with 500. This test is about HTTP/security behavior, not Kafka
    // integration, so the publisher is mocked out here.
    @MockBean(OrderEventPublisher.class)
    OrderEventPublisher orderEventPublisher() {
        return Mockito.mock(OrderEventPublisher.class);
    }

    @Test
    void ordersEndpointRejectsUnauthenticatedRequests() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/api/v1/orders")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void loginThenCreateOrderSucceeds() {
        // 1. Log in to get a JWT (AuthenticationProviderUserPassword accepts user/password)
        Map<String, Object> credentials = Map.of("username", "user", "password", "password");
        Map<String, Object> loginResponse = client.toBlocking()
                .retrieve(HttpRequest.POST("/login", credentials), Map.class);
        String token = (String) loginResponse.get("access_token");

        // 2. Use the token to create an order
        OrderItem item = new OrderItem("p1", "Widget", 1, new BigDecimal("5.00"));
        HttpRequest<?> request = HttpRequest.POST("/api/v1/orders?customerId=cust-1", List.of(item))
                .bearerAuth(token);

        var response = client.toBlocking().exchange(request, Argument.of(Map.class));
        assertEquals(HttpStatus.CREATED, response.getStatus());
    }

    @Test
    void healthzIsPubliclyAccessible() {
        var response = client.toBlocking().exchange(HttpRequest.GET("/healthz"), Argument.of(Map.class));
        assertEquals(HttpStatus.OK, response.getStatus());
    }
}