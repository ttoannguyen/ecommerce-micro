package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;

/** Dịch giữa aggregate miền và bản ghi JPA. */
final class ProductMapper {

    private ProductMapper() {
    }

    static ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(
                product.id(),
                product.name(),
                product.price().amount(),
                product.stock());
    }

    static Product toDomain(ProductJpaEntity entity) {
        return Product.rehydrate(
                entity.getId(),
                entity.getName(),
                Money.of(entity.getPrice()),
                entity.getStock());
    }
}
