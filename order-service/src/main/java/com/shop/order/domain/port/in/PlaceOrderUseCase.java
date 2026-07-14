package com.shop.order.domain.port.in;

import com.shop.order.domain.model.Order;

/** Inbound port: place an order. */
public interface PlaceOrderUseCase {

    Order placeOrder(PlaceOrderCommand command);
}
