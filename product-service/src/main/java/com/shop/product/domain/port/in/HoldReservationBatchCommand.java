package com.shop.product.domain.port.in;

import java.util.List;

public record HoldReservationBatchCommand(List<HoldReservationLine> lines,
                                          String caller,
                                          String idempotencyKey) {
}
