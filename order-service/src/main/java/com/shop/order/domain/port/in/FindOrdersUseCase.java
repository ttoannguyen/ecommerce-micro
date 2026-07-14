package com.shop.order.domain.port.in;

import com.shop.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

/** Inbound port: read orders. */
public interface FindOrdersUseCase {

    List<Order> findAll();

    Optional<Order> findById(Long id);
}
