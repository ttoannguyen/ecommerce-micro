package com.shop.order.domain.port.out;

import com.shop.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

/** Outbound port: đọc đơn hàng từ store. */
public interface LoadOrderPort {

    List<Order> findAll();

    Optional<Order> findById(Long id);
}
