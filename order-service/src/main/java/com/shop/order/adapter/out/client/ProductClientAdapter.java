package com.shop.order.adapter.out.client;

import com.shop.order.domain.model.InsufficientStockException;
import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.port.out.ReserveStockPort;
import feign.FeignException;
import org.springframework.stereotype.Component;

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
    public ReservedProduct reserve(Long productId, Quantity quantity) {
        try {
            ReservationResponse reserved =
                    client.reserve(productId, new ReserveStockRequest(quantity.value()));
            return new ReservedProduct(
                    reserved.productId(),
                    reserved.name(),
                    Money.of(reserved.price()));
        } catch (FeignException.Conflict refused) {
            // 409: product-service looked at its own row inside its own transaction and
            // said no. It is the only party entitled to give that answer.
            throw new InsufficientStockException(
                    "product-service refused the reservation: " + refused.contentUTF8());
        }
    }

    @Override
    public void release(Long productId, Quantity quantity) {
        client.release(productId, new ReserveStockRequest(quantity.value()));
    }
}
