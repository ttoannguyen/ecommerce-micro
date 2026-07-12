package com.shop.order.domain.port.in;

import com.shop.order.domain.model.Order;

/** Inbound port: đặt hàng. */
public interface PlaceOrderUseCase {

    Order placeOrder(PlaceOrderCommand command);
}
