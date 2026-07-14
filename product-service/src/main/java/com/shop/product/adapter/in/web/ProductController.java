package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ProductResponse;
import com.shop.product.adapter.in.web.dto.ReservationResponse;
import com.shop.product.adapter.in.web.dto.ReserveStockRequest;
import com.shop.product.domain.port.in.FindProductsUseCase;
import com.shop.product.domain.port.in.ReserveStockCommand;
import com.shop.product.domain.port.in.ReserveStockUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inbound REST adapter. Translates HTTP into use cases. */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final FindProductsUseCase findProductsUseCase;
    private final ReserveStockUseCase reserveStockUseCase;

    public ProductController(FindProductsUseCase findProductsUseCase,
                             ReserveStockUseCase reserveStockUseCase) {
        this.findProductsUseCase = findProductsUseCase;
        this.reserveStockUseCase = reserveStockUseCase;
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

    /** Reserves stock immediately. 409 when there is not enough. */
    @PostMapping("/{id}/reservations")
    public ReservationResponse reserve(@PathVariable Long id,
                                       @Valid @RequestBody ReserveStockRequest request) {
        return ReservationResponse.from(
                reserveStockUseCase.reserve(new ReserveStockCommand(id, request.quantity())));
    }

    /** Compensation: put the stock back after the caller failed. */
    @DeleteMapping("/{id}/reservations")
    public ReservationResponse release(@PathVariable Long id,
                                       @Valid @RequestBody ReserveStockRequest request) {
        return ReservationResponse.from(
                reserveStockUseCase.release(new ReserveStockCommand(id, request.quantity())));
    }
}
