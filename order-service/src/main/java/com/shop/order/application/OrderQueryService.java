package com.shop.order.application;

import com.shop.order.domain.model.Order;
import com.shop.order.domain.port.in.FindOrdersUseCase;
import com.shop.order.domain.port.out.LoadOrderPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Read-side use case for orders. */
@Service
@Transactional(readOnly = true)
public class OrderQueryService implements FindOrdersUseCase {

    private final LoadOrderPort loadOrderPort;

    public OrderQueryService(LoadOrderPort loadOrderPort) {
        this.loadOrderPort = loadOrderPort;
    }

    @Override
    public List<Order> findAll() {
        return loadOrderPort.findAll();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return loadOrderPort.findById(id);
    }
}
