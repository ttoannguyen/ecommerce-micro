package com.shop.order.adapter.out.client;

import com.shop.order.domain.model.InsufficientStockException;
import com.shop.order.domain.model.IdempotencyConflictException;
import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.model.OrderItemDraft;
import com.shop.order.domain.port.out.ReserveStockPort;
import feign.FeignException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.List;

/**
 * Anti-corruption boundary. product-service speaks HTTP status codes; the Order domain
 * speaks exceptions. Nothing inside the domain knows that a 409 exists.
 */
@Component
public class ProductClientAdapter implements ReserveStockPort {

    private final ProductClient client;

    public ProductClientAdapter(ProductClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "productService")
    @Bulkhead(name = "productService", type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = "productService")
    public ReservedProduct reserve(Long productId, Quantity quantity, String idempotencyKey) {
        try {
            ReservationResponse reserved =
                    client.reserve(productId, idempotencyKey, "order-service",
                            new ReserveStockRequest(quantity.value()));
            return new ReservedProduct(
                    reserved.productId(),
                    reserved.reservationId(),
                    reserved.name(),
                    Money.of(reserved.price()),
                    reserved.quantity(),
                    reserved.expiresAt());
        } catch (FeignException.Conflict refused) {
            if (refused.contentUTF8().contains("IDEMPOTENCY_CONFLICT")) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key đã được dùng với payload khác");
            }
            // 409: product-service looked at its own row inside its own transaction and
            // said no. It is the only party entitled to give that answer.
            throw new InsufficientStockException(
                    "product-service refused the reservation: " + refused.contentUTF8());
        }
    }

    @Override
    @CircuitBreaker(name = "productService")
    @Bulkhead(name = "productService", type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = "productService")
    public List<ReservedProduct> reserve(List<OrderItemDraft> items, String idempotencyKey) {
        try {
            BatchReservationResponse response = client.reserveBatch(
                    idempotencyKey, "order-service",
                    new BatchReserveStockRequest(items.stream()
                            .map(item -> new ReserveStockLineRequest(item.productId(),
                                    item.quantity().value()))
                            .toList()));
            return response.reservations().stream().map(reserved -> new ReservedProduct(
                    reserved.productId(), reserved.reservationId(), reserved.name(),
                    Money.of(reserved.price()), reserved.quantity(), reserved.expiresAt())).toList();
        } catch (FeignException.Conflict refused) {
            if (refused.contentUTF8().contains("IDEMPOTENCY_CONFLICT")) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key đã được dùng với payload khác");
            }
            throw new InsufficientStockException(
                    "product-service refused the reservation: " + refused.contentUTF8());
        }
    }

    @Override
    @CircuitBreaker(name = "productService")
    @Bulkhead(name = "productService", type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = "productService")
    public void release(UUID reservationId) {
        client.release(reservationId);
    }
}
