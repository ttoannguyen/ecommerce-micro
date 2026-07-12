package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: hiện thực port lưu/đọc bằng Spring Data. */
@Component
public class ProductPersistenceAdapter implements LoadProductPort, SaveProductPort {

    private final SpringDataProductRepository repository;

    public ProductPersistenceAdapter(SpringDataProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Product> findAll() {
        return repository.findAll().stream().map(ProductMapper::toDomain).toList();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return repository.findById(id).map(ProductMapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Product save(Product product) {
        return ProductMapper.toDomain(repository.save(ProductMapper.toEntity(product)));
    }
}
