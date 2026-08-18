package com.shop.order.domain.port.in;

import com.shop.order.domain.model.Order;

public interface OrderLifecycleUseCase {
    Order pay(Long id);
    Order cancel(Long id);
}
