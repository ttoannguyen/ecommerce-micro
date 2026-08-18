package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ReservationResponse;
import com.shop.product.domain.port.in.ReservationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
