package com.shop.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.order.OrderServiceApplication;
import com.shop.product.ProductServiceApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 2 black-box test: two real PostgreSQL databases, two real Spring
 * applications and the order -> product OpenFeign call over HTTP.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Milestone2IntegrationTest {

    static final PostgreSQLContainer<?> productDb = postgres("productdb");

    static final PostgreSQLContainer<?> orderDb = postgres("orderdb");

    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    private static ConfigurableApplicationContext productContext;
    private static ConfigurableApplicationContext orderContext;
    private static HttpClient http;
    private static ObjectMapper json;
    private static String productBaseUrl;
    private static String orderBaseUrl;

    @BeforeAll
    static void startApplications() {
        kafka.start();
        productDb.start();
        orderDb.start();

        productContext = new SpringApplicationBuilder(ProductServiceApplication.class)
                .properties(
                        "spring.config.location=optional:classpath:/integration-product-only.yml",
                        "spring.application.name=product-service-integration",
                        "server.port=0",
                        "spring.datasource.url=" + productDb.getJdbcUrl(),
                        "spring.datasource.username=" + productDb.getUsername(),
                        "spring.datasource.password=" + productDb.getPassword(),
                        "spring.flyway.locations=classpath:db/migration/product",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "inventory.reservation.ttl=PT15M",
                        "inventory.reservation.expiry-interval=PT5S",
                        "inventory.reservation.expiry-batch-size=100",
                        "inventory.reservation.expiry-enabled=false")
                .run();

        int productPort = productContext.getEnvironment().getProperty("local.server.port", Integer.class);
        productBaseUrl = "http://localhost:" + productPort;

        orderContext = new SpringApplicationBuilder(OrderServiceApplication.class)
                .properties(
                        "spring.config.location=optional:classpath:/integration-order-only.yml",
                        "spring.application.name=order-service-integration",
                        "server.port=0",
                        "spring.datasource.url=" + orderDb.getJdbcUrl(),
                        "spring.datasource.username=" + orderDb.getUsername(),
                        "spring.datasource.password=" + orderDb.getPassword(),
                        "spring.flyway.locations=classpath:db/migration/order",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "product-service.url=" + productBaseUrl,
                        "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),
                        "messaging.outbox.enabled=true",
                        "messaging.outbox.poll-interval-ms=250",
                        "messaging.topic.orders=order-events",
                        "messaging.consumer.group=integration-notification",
                        "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
                        "management.endpoint.health.probes.enabled=true",
                        "management.health.livenessstate.enabled=true",
                        "management.health.readinessstate.enabled=true")
                .run();

        int orderPort = orderContext.getEnvironment().getProperty("local.server.port", Integer.class);
        orderBaseUrl = "http://localhost:" + orderPort;
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        json = new ObjectMapper().findAndRegisterModules();
    }

    @AfterAll
    static void stopApplications() {
        if (orderContext != null) {
            orderContext.close();
        }
        if (productContext != null) {
            productContext.close();
        }
        orderDb.stop();
        productDb.stop();
        kafka.stop();
    }

    @Test
    @Order(1)
    void productReservationUsesPostgresAndIsIdempotent() throws Exception {
        String key = "integration-product-" + UUID.randomUUID();
        String body = "{\"quantity\":2}";

        HttpResponse<String> first = send(request(productBaseUrl + "/products/1/reservations")
                .header("Idempotency-Key", key)
                .header("X-Caller-Id", "integration-test")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        HttpResponse<String> replay = send(request(productBaseUrl + "/products/1/reservations")
                .header("Idempotency-Key", key)
                .header("X-Caller-Id", "integration-test")
                .POST(HttpRequest.BodyPublishers.ofString(body)));

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(json.readTree(replay.body()).get("reservationId").asText())
                .isEqualTo(json.readTree(first.body()).get("reservationId").asText());

        JsonNode product = get(productBaseUrl + "/products/1");
        assertThat(product.get("onHand").asInt()).isEqualTo(10);
        assertThat(product.get("reserved").asInt()).isEqualTo(2);
        assertThat(product.get("available").asInt()).isEqualTo(8);

        JsonNode reconciliation = get(productBaseUrl + "/products/1/reconciliation");
        assertThat(reconciliation.get("consistent").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void orderUsesOpenFeignAndOrderIdempotencyAcrossBothServices() throws Exception {
        String key = "integration-order-" + UUID.randomUUID();
        String correlationId = "corr-" + UUID.randomUUID();
        String body = "{\"productId\":2,\"quantity\":3}";

        HttpResponse<String> first = send(request(orderBaseUrl + "/orders")
                .header("Idempotency-Key", key)
                .header("X-Correlation-Id", correlationId)
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        HttpResponse<String> replay = send(request(orderBaseUrl + "/orders")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body)));

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.headers().firstValue("X-Correlation-Id").orElseThrow())
                .isEqualTo(correlationId);
        assertThat(replay.statusCode()).isEqualTo(201);
        JsonNode created = json.readTree(first.body());
        JsonNode repeated = json.readTree(replay.body());
        assertThat(repeated.get("id").asLong()).isEqualTo(created.get("id").asLong());
        assertThat(created.get("reservationId").asText()).isNotBlank();

        JsonNode product = get(productBaseUrl + "/products/2");
        assertThat(product.get("reserved").asInt()).isEqualTo(3);

        HttpResponse<String> conflict = send(request(orderBaseUrl + "/orders")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":2,\"quantity\":4}")));
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(json.readTree(conflict.body()).get("code").asText())
                .isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(count("select count(*) from outbox_events where correlation_id is not null"))
                .isEqualTo(1);
    }

    @Test
    @Order(3)
    void insufficientStockIsRejectedByProductAndDoesNotCreateOrder() throws Exception {
        String key = "integration-insufficient-" + UUID.randomUUID();
        HttpResponse<String> response = send(request(orderBaseUrl + "/orders")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":3,\"quantity\":99}")));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(json.readTree(response.body()).get("code").asText())
                .isEqualTo("INSUFFICIENT_STOCK");
        assertThat(get(productBaseUrl + "/products/3").get("reserved").asInt()).isZero();
    }

    @Test
    @Order(4)
    void batchOrderReservesAllLinesOrNothing() throws Exception {
        String key = "integration-batch-" + UUID.randomUUID();
        HttpResponse<String> created = send(request(orderBaseUrl + "/orders/batch")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"items\":[{\"productId\":1,\"quantity\":2},"
                                + "{\"productId\":2,\"quantity\":4}]}")));

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode order = json.readTree(created.body());
        assertThat(order.get("status").asText()).isEqualTo("RESERVED");
        assertThat(order.get("items")).hasSize(2);
        assertThat(get(productBaseUrl + "/products/1").get("reserved").asInt()).isEqualTo(4);
        assertThat(get(productBaseUrl + "/products/2").get("reserved").asInt()).isEqualTo(7);

        String failingKey = "integration-batch-fail-" + UUID.randomUUID();
        HttpResponse<String> failed = send(request(orderBaseUrl + "/orders/batch")
                .header("Idempotency-Key", failingKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"items\":[{\"productId\":1,\"quantity\":1},"
                                + "{\"productId\":3,\"quantity\":99}]}")));

        assertThat(failed.statusCode()).isEqualTo(409);
        assertThat(get(productBaseUrl + "/products/1").get("reserved").asInt()).isEqualTo(4);
        assertThat(get(productBaseUrl + "/products/3").get("reserved").asInt()).isZero();
    }

    @Test
    @Order(5)
    void orderPayAndCancelPersistStateTransitions() throws Exception {
        HttpResponse<String> payable = send(request(orderBaseUrl + "/orders/batch")
                .header("Idempotency-Key", "integration-pay-" + UUID.randomUUID())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"items\":[{\"productId\":2,\"quantity\":1}]}")));
        assertThat(payable.statusCode()).isEqualTo(201);
        long payableId = json.readTree(payable.body()).get("id").asLong();

        JsonNode paid = json.readTree(send(request(orderBaseUrl + "/orders/" + payableId + "/pay")
                .POST(HttpRequest.BodyPublishers.noBody())).body());
        assertThat(paid.get("status").asText()).isEqualTo("PAID");
        assertThat(paid.get("transitions")).hasSize(2);

        HttpResponse<String> cancellable = send(request(orderBaseUrl + "/orders/batch")
                .header("Idempotency-Key", "integration-cancel-" + UUID.randomUUID())
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"items\":[{\"productId\":1,\"quantity\":1}]}")));
        long cancellableId = json.readTree(cancellable.body()).get("id").asLong();
        JsonNode cancelled = json.readTree(send(
                request(orderBaseUrl + "/orders/" + cancellableId + "/cancel")
                        .POST(HttpRequest.BodyPublishers.noBody())).body());
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(get(productBaseUrl + "/products/1").get("reserved").asInt()).isEqualTo(4);
    }

    @Test
    @Order(6)
    void outboxPublishesOrderEventsAndInboxDeduplicatesReplay() throws Exception {
        assertThat(count("select count(*) from outbox_events"))
                .as("six order transitions should have created outbox events")
                .isEqualTo(6);
        waitFor(() -> count("select count(*) from outbox_events where status = 'PUBLISHED'") >= 6,
                Duration.ofSeconds(20));
        waitFor(() -> count("select count(*) from notification_log") >= 6,
                Duration.ofSeconds(20));

        String event = firstEventPayload();
        KafkaTemplate<String, String> template = orderContext.getBean(KafkaTemplate.class);
        template.send("order-events", "replay", event).get(10, TimeUnit.SECONDS);

        Thread.sleep(1500);
        assertThat(count("select count(*) from notification_log")).isEqualTo(6);
        assertThat(count("select count(*) from inbox_events")).isEqualTo(6);
        assertThat(count("select count(*) from outbox_events where correlation_id is not null"))
                .isEqualTo(6);
    }

    @Test
    @Order(7)
    void exposesSeparateLivenessReadinessAndPrometheusMetrics() throws Exception {
        assertThat(send(request(orderBaseUrl + "/actuator/health/liveness").GET()).statusCode())
                .isEqualTo(200);
        assertThat(send(request(orderBaseUrl + "/actuator/health/readiness").GET()).statusCode())
                .isEqualTo(200);
        HttpResponse<String> metrics = send(request(orderBaseUrl + "/actuator/prometheus").GET());
        assertThat(metrics.statusCode()).isEqualTo(200);
        assertThat(metrics.body()).contains("http_server_requests_seconds");
        assertThat(metrics.body()).contains("messaging_outbox_events");
    }

    private static long count(String sql) throws Exception {
        try (Connection connection = DriverManager.getConnection(orderDb.getJdbcUrl(),
                orderDb.getUsername(), orderDb.getPassword());
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String firstEventPayload() throws Exception {
        try (Connection connection = DriverManager.getConnection(orderDb.getJdbcUrl(),
                orderDb.getUsername(), orderDb.getPassword());
             PreparedStatement statement = connection.prepareStatement(
                     "select payload from outbox_events order by occurred_at limit 1");
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    private static void waitFor(CheckedCondition condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.matches()) {
                return;
            }
            Thread.sleep(250);
        }
        assertThat(condition.matches()).as("condition within " + timeout).isTrue();
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean matches() throws Exception;
    }

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    private static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode get(String url) throws Exception {
        HttpResponse<String> response = send(request(url).GET());
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body());
    }

    private static PostgreSQLContainer<?> postgres(String database) {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName(database)
                .withUsername("postgres")
                .withPassword("postgres");
    }
}
