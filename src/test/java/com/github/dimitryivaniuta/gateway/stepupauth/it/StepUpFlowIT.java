package com.github.dimitryivaniuta.gateway.stepupauth.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration test: risky authorize -> OTP verify -> event published. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class StepUpFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
        .withDatabaseName("stepup").withUsername("stepup").withPassword("stepup");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        r.add("app.otp.dev-preview", () -> true);
        r.add("app.outbox.enabled", () -> true);
        r.add("app.outbox.publish-interval", () -> "500ms");
        r.add("app.security.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret-test-secret");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired StringRedisTemplate redisTemplate;

    static KafkaConsumer<String, String> consumer;

    @BeforeAll
    static void setupConsumer() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(p);
        consumer.subscribe(Collections.singletonList("risk.monitoring"));
    }

    @AfterAll
    static void closeConsumer() {
        if (consumer != null) consumer.close();
    }

    @Test
    void fullFlow() throws Exception {
        mvc.perform(post("/api/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
            .andExpect(status().isNoContent());

        String loginJson = mvc.perform(post("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode login = mapper.readTree(loginJson);
        String token = login.get("token").asText();
        assertThat(token).isNotBlank();

        String authzJson = mvc.perform(post("/api/transactions/authorize")
                .header("Authorization", "Bearer " + token)
                .header("X-Device-Id", "device-new-999")
                .header("X-Country", "US")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionType\":\"TRANSFER\",\"amount\":\"5000.00\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode authz = mapper.readTree(authzJson);
        assertThat(authz.get("decision").asText()).isEqualTo("STEP_UP_REQUIRED");
        String challengeId = authz.get("challengeId").asText();

        String otp = redisTemplate.opsForValue().get("otp:" + challengeId);
        assertThat(otp).isNotBlank();

        mvc.perform(post("/api/stepup/" + challengeId + "/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otp\":\"" + otp + "\"}"))
            .andExpect(status().isOk());

        var records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThan(0);
    }
}
