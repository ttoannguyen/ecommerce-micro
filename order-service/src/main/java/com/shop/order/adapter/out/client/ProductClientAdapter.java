package com.shop.order.adapter.out.client;

import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.ProductSnapshot;
import com.shop.order.domain.port.out.LoadProductPort;
import org.springframework.stereotype.Component;

/** Outbound adapter: dịch wire DTO Product -> value object miền (anti-corruption). */
@Component
public class ProductClientAdapter implements LoadProductPort {

    private final ProductClient client;

    public ProductClientAdapter(ProductClient client) {
        this.client = client;
    }

    @Override
    public ProductSnapshot loadProduct(Long productId) {
        ProductResponse response = client.getProduct(productId);
        return new ProductSnapshot(
                response.getId(),
                response.getName(),
                Money.of(response.getPrice()),
                response.getStock());
    }
}
