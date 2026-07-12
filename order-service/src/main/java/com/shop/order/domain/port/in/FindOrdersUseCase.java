package com.shop.order.domain.port.in;

import com.shop.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

/** Inbound port: đọc đơn hàng. */
public interface FindOrdersUseCase {

    List<Order> findAll();

    Optional<Order> findById(Long id);
}
