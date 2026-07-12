package com.shop.product.application;

import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.in.FindProductsUseCase;
import com.shop.product.domain.port.out.LoadProductPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Use case đọc catalog. */
@Service
@Transactional(readOnly = true)
public class ProductQueryService implements FindProductsUseCase {

    private final LoadProductPort loadProductPort;

    public ProductQueryService(LoadProductPort loadProductPort) {
        this.loadProductPort = loadProductPort;
    }

    @Override
    public List<Product> findAll() {
        return loadProductPort.findAll();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return loadProductPort.findById(id);
    }
}
