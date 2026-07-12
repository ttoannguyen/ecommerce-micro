package com.shop.order.adapter.out.persistence;

import com.shop.order.domain.model.Order;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: hiện thực port lưu/đọc bằng Spring Data. */
@Component
public class OrderPersistenceAdapter implements SaveOrderPort, LoadOrderPort {

    private final SpringDataOrderRepository repository;

    public OrderPersistenceAdapter(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        return OrderMapper.toDomain(repository.save(OrderMapper.toEntity(order)));
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll().stream().map(OrderMapper::toDomain).toList();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return repository.findById(id).map(OrderMapper::toDomain);
    }
}
