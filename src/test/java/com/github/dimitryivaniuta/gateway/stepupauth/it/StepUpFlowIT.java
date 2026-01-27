package com.github.dimitryivaniuta.gateway.stepupauth.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dimitryivaniuta.gateway.stepupauth.service.outbox.OutboxPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: risky authorize -> OTP verify -> monitoring event published to Kafka.
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>Runs with Testcontainers (Postgres + Redis + Kafka).</li>
 *   <li>Disables scheduling to avoid flakiness; triggers outbox publishing explicitly.</li>
 *   <li>Uses OTP preview if enabled, otherwise falls back to Redis.</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class StepUpFlowIT {

    private static final String TOPIC_MONITORING = "risk.monitoring";

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("stepup")
                    .withUsername("stepup")
                    .withPassword("stepup");

    @SuppressWarnings("resource")
    @Container
    static ConfluentKafkaContainer kafka =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OutboxPublisher outboxPublisher;

    private static KafkaConsumer<String, String> consumer;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // Make the test deterministic: outbox publish is triggered manually.
        r.add("spring.task.scheduling.enabled", () -> "false");

        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Return OTP in authorize response for test convenience.
        r.add("app.otp.dev-preview", () -> true);

        // Enable outbox in tests; publisher is invoked explicitly.
        r.add("app.outbox.enabled", () -> true);

        // Stable JWT signing for ITs.
        r.add("app.security.jwt.secret", () -> "test-secret-test-secret-test-secret-test-secret-test-secret");
    }

    @BeforeAll
    static void setupConsumer() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumer = new KafkaConsumer<>(p);
        consumer.subscribe(Collections.singletonList(TOPIC_MONITORING));

        // Ensure subscription is active before the test produces messages.
        consumer.poll(Duration.ofMillis(200));
    }

    @AfterAll
    static void closeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void fullFlow() throws Exception {
        // 1) Register
        mvc.perform(post("/api/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
                .andExpect(status().isNoContent());

        // 2) Login -> JWT
        String loginJson = mvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo12345\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode login = mapper.readTree(loginJson);
        String token = login.get("token").asText();
        assertThat(token).isNotBlank();

        // 3) Authorize (risky) -> expects step-up
        String authzJson = mvc.perform(post("/api/transactions/authorize")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Device-Id", "device-new-999")
                        .header("X-Country", "US")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actionType\":\"TRANSFER\",\"amount\":\"5000.00\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode authz = mapper.readTree(authzJson);
        assertThat(authz.get("decision").asText()).isEqualTo("STEP_UP_REQUIRED");

        String challengeId = authz.get("challengeId").asText();
        assertThat(challengeId).isNotBlank();

        // Prefer response preview if available (dev-preview mode), fallback to Redis.
        String otp = authz.path("otpPreview").asText("");
        if (otp.isBlank()) {
            otp = redisTemplate.opsForValue().get("otp:" + challengeId);
        }
        assertThat(otp).isNotBlank();

        // 4) Verify OTP
        mvc.perform(post("/api/stepup/" + challengeId + "/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());

        // 5) Publish outbox now (scheduling disabled for determinism)
        outboxPublisher.publishDueEvents();

        // 6) Assert monitoring event arrived
        boolean gotMessage = false;
        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline && !gotMessage) {
            var records = consumer.poll(Duration.ofMillis(500));
            gotMessage = records.count() > 0;
        }

        assertThat(gotMessage).isTrue();
    }
}
