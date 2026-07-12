package com.shop.order.domain.port.out;

import com.shop.order.domain.model.Order;

/** Outbound port: lưu đơn hàng. */
public interface SaveOrderPort {

    Order save(Order order);
}
