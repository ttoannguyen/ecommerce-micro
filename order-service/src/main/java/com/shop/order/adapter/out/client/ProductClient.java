package com.shop.order.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;
import java.util.List;

/**
 * Calls product-service over HTTP. Order never touches productdb — it has to ask.
 * That is what DB-per-service costs, and why stock must be reserved rather than read.
 * Week 2 drops the hard-coded url in favour of the service name via Eureka.
 */
@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {

    @PostMapping("/products/{id}/reservations")
    ReservationResponse reserve(
            @PathVariable("id") Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Caller-Id") String caller,
            @RequestBody ReserveStockRequest request);

    @PostMapping("/reservations/batch")
    BatchReservationResponse reserveBatch(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Caller-Id") String caller,
            @RequestBody BatchReserveStockRequest request);

    @DeleteMapping("/reservations/{id}")
    void release(@PathVariable("id") UUID reservationId);
}
