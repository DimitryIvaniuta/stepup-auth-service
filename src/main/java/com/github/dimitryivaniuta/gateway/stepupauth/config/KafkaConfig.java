package com.github.dimitryivaniuta.gateway.stepupauth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Kafka topic definitions. */
@Configuration
public class KafkaConfig {
    /** Monitoring topic used by outbox publisher. */
    @Bean
    public NewTopic monitoringTopic(@Value("${app.kafka.topics.monitoring}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
