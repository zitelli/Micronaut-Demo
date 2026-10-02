package com.example.service;

import com.example.model.Order;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
public interface OrderEventPublisher {

    @Topic("order-events")
    void publishOrderCreated(@KafkaKey String key, String eventJson);

    @Topic("order-events")
    void publishOrderStatusChanged(@KafkaKey String key, String eventJson);

    @Topic("order-events")
    void publishOrderCancelled(@KafkaKey String key, String eventJson);

}