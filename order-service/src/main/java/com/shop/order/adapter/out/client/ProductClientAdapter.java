package com.shop.order.adapter.out.client;

import com.shop.order.domain.model.InsufficientStockException;
import com.shop.order.domain.model.IdempotencyConflictException;
import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.port.out.ReserveStockPort;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
    public void release(UUID reservationId) {
        client.release(reservationId);
    }
}
