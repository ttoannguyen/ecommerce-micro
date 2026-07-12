package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ProductResponse;
import com.shop.product.domain.port.in.FindProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inbound adapter REST. Dịch HTTP <-> use case. */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final FindProductsUseCase findProductsUseCase;

    public ProductController(FindProductsUseCase findProductsUseCase) {
        this.findProductsUseCase = findProductsUseCase;
    }

    @GetMapping
    public List<ProductResponse> all() {
        return findProductsUseCase.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> byId(@PathVariable Long id) {
        return findProductsUseCase.findById(id)
                .map(ProductResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
