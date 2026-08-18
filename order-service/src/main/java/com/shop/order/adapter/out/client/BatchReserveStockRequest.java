package com.shop.order.adapter.out.client;

import java.util.List;

public record BatchReserveStockRequest(List<ReserveStockLineRequest> items) {
}
