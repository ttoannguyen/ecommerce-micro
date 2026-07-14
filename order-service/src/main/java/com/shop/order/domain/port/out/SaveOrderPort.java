package com.shop.order.domain.port.out;

import com.shop.order.domain.model.Order;

/** Outbound port: saves an order. */
public interface SaveOrderPort {

    Order save(Order order);
}
