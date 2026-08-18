package com.shop.product.domain.port.in;

public record HoldReservationCommand(Long productId, int quantity,
                                     String caller, String idempotencyKey) {
}
