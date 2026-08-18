package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ProductResponse;
import com.shop.product.adapter.in.web.dto.ReconciliationResponse;
import com.shop.product.adapter.in.web.dto.ReservationResponse;
import com.shop.product.adapter.in.web.dto.ReserveStockRequest;
import com.shop.product.domain.port.in.FindProductsUseCase;
import com.shop.product.domain.port.in.HoldReservationCommand;
import com.shop.product.domain.port.in.ReconcileInventoryUseCase;
import com.shop.product.domain.port.in.ReservationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inbound REST adapter. Translates HTTP into use cases. */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final FindProductsUseCase findProductsUseCase;
    private final ReservationUseCase reservationUseCase;
    private final ReconcileInventoryUseCase reconcileInventoryUseCase;

    public ProductController(FindProductsUseCase findProductsUseCase,
                             ReservationUseCase reservationUseCase,
                             ReconcileInventoryUseCase reconcileInventoryUseCase) {
        this.findProductsUseCase = findProductsUseCase;
        this.reservationUseCase = reservationUseCase;
        this.reconcileInventoryUseCase = reconcileInventoryUseCase;
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

    /** Creates an idempotent, time-bound hold. */
    @PostMapping("/{id}/reservations")
    public ResponseEntity<ReservationResponse> reserve(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Caller-Id", defaultValue = "api") String caller,
            @Valid @RequestBody ReserveStockRequest request) {
        ReservationResponse response = ReservationResponse.from(
                reservationUseCase.hold(new HoldReservationCommand(
                        id, request.quantity(), caller, idempotencyKey)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/reconciliation")
    public ReconciliationResponse reconcile(@PathVariable Long id) {
        return ReconciliationResponse.from(reconcileInventoryUseCase.reconcile(id));
    }
}
