package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: implements the load/save ports with Spring Data. */
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
    public Optional<Product> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(ProductMapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Product save(Product product) {
        if (product.id() == null) {
            return ProductMapper.toDomain(repository.save(ProductMapper.toEntity(product)));
        }

        // Update: do NOT build a fresh detached entity and merge it. That throws the
        // @Version away and quietly disables the lock. Mutate the managed entity instead
        // (already in the persistence context from the load earlier in this transaction),
        // so Hibernate dirty-checks it and emits WHERE version = ?
        ProductJpaEntity managed = repository.findById(product.id())
                .orElseThrow(() -> new ProductNotFoundException(product.id()));
        managed.changeStock(product.stock());
        return ProductMapper.toDomain(repository.save(managed));
    }
}
