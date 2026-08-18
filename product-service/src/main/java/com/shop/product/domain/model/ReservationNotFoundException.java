package com.shop.product.domain.model;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(UUID id) {
        super("Không tìm thấy reservation " + id);
    }
}
