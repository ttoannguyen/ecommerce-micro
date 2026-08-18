package com.shop.product.adapter.in.web.dto;

import com.shop.product.domain.model.ReservationBatchDetails;

import java.util.List;

public record BatchReservationResponse(List<ReservationResponse> reservations) {
    public static BatchReservationResponse from(ReservationBatchDetails details) {
        return new BatchReservationResponse(details.reservations().stream()
                .map(ReservationResponse::from).toList());
    }
}
