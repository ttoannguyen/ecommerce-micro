package com.shop.product.adapter.out.persistence;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.model.StockChange;
import com.shop.product.domain.model.StockMovement;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.LoadStockLedgerPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: implements the load/save ports with Spring Data. */
@Component
public class ProductPersistenceAdapter
        implements LoadProductPort, SaveProductPort, LoadStockLedgerPort {

    private final SpringDataProductRepository repository;
    private final SpringDataStockMovementRepository movements;

    public ProductPersistenceAdapter(SpringDataProductRepository repository,
                                     SpringDataStockMovementRepository movements) {
        this.repository = repository;
        this.movements = movements;
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
    public int balanceOf(Long productId) {
        return movements.balanceOf(productId);
    }

    @Override
    public Product save(Product product) {
        if (product.id() == null) {
            return ProductMapper.toDomain(repository.save(ProductMapper.toEntity(product)));
        }
        return ProductMapper.toDomain(repository.save(managed(product.id())));
    }

    @Override
    public Product updateBalance(Product product) {
        ProductJpaEntity managed = managed(product.id());
        managed.changeBalance(product.onHand(), product.reserved());
        return ProductMapper.toDomain(repository.save(managed));
    }

    /**
     * Both writes, one transaction. The caller's @Transactional spans this method, so
     * the ledger line and the new balance commit together or not at all — which is the
     * only thing that keeps SUM(movements) == product.onHand true under failure.
     */
    @Override
    public Product apply(StockChange change) {
        Product product = change.product();
        StockMovement movement = change.movement();

        movements.save(StockMovementMapper.toEntity(movement));

        // Update: do NOT build a fresh detached entity and merge it. That throws the
        // @Version away and quietly disables the lock. Mutate the managed entity instead
        // (already in the persistence context from the load earlier in this transaction),
        // so Hibernate dirty-checks it and emits WHERE version = ?
        ProductJpaEntity managed = managed(product.id());
        managed.changeBalance(product.onHand(), product.reserved());
        return ProductMapper.toDomain(repository.save(managed));
    }

    private ProductJpaEntity managed(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
