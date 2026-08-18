package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ReservationResponse;
import com.shop.product.adapter.in.web.dto.BatchReservationResponse;
import com.shop.product.adapter.in.web.dto.BatchReserveStockRequest;
import com.shop.product.domain.port.in.HoldReservationBatchCommand;
import com.shop.product.domain.port.in.HoldReservationLine;
import com.shop.product.domain.port.in.ReservationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationUseCase reservationUseCase;

    public ReservationController(ReservationUseCase reservationUseCase) {
        this.reservationUseCase = reservationUseCase;
    }

    @PostMapping("/batch")
    public BatchReservationResponse reserveBatch(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Caller-Id", defaultValue = "api") String caller,
            @Valid @RequestBody BatchReserveStockRequest request) {
        return BatchReservationResponse.from(reservationUseCase.holdBatch(
                new HoldReservationBatchCommand(
                        request.items().stream()
                                .map(item -> new HoldReservationLine(item.productId(), item.quantity()))
                                .toList(),
                        caller,
                        idempotencyKey)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> byId(@PathVariable UUID id) {
        return reservationUseCase.findById(id)
                .map(ReservationResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable UUID id) {
        return ReservationResponse.from(reservationUseCase.confirm(id));
    }

    @DeleteMapping("/{id}")
    public ReservationResponse release(@PathVariable UUID id) {
        return ReservationResponse.from(reservationUseCase.release(id));
    }
}
