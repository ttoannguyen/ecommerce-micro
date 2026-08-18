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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private static ConfigurableApplicationContext productContext;
    private static ConfigurableApplicationContext orderContext;
    private static HttpClient http;
    private static ObjectMapper json;
    private static String productBaseUrl;
    private static String orderBaseUrl;

    @BeforeAll
    static void startApplications() {
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
                        "product-service.url=" + productBaseUrl)
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
        String body = "{\"productId\":2,\"quantity\":3}";

        HttpResponse<String> first = send(request(orderBaseUrl + "/orders")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body)));
        HttpResponse<String> replay = send(request(orderBaseUrl + "/orders")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body)));

        assertThat(first.statusCode()).isEqualTo(201);
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
