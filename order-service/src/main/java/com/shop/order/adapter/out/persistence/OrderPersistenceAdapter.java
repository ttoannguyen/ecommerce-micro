package com.shop.order.adapter.out.persistence;

import com.shop.order.domain.model.Order;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: implements the load/save ports with Spring Data. */
@Component
public class OrderPersistenceAdapter implements SaveOrderPort, LoadOrderPort {

    private final SpringDataOrderRepository repository;
    private final SpringDataOrderItemRepository itemRepository;

    public OrderPersistenceAdapter(SpringDataOrderRepository repository,
                                   SpringDataOrderItemRepository itemRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = repository.saveAndFlush(OrderMapper.toEntity(order));
        itemRepository.saveAll(order.items().stream()
                .map(item -> OrderMapper.toItemEntity(entity.getId(), order.items().indexOf(item), item))
                .toList());
        return OrderMapper.toDomain(entity, itemRepository.findByOrderIdOrderByLineNumber(entity.getId()));
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll().stream()
                .map(entity -> OrderMapper.toDomain(entity,
                        itemRepository.findByOrderIdOrderByLineNumber(entity.getId())))
                .toList();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return repository.findById(id).map(entity -> OrderMapper.toDomain(entity,
                itemRepository.findByOrderIdOrderByLineNumber(entity.getId())));
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(entity -> OrderMapper.toDomain(entity,
                itemRepository.findByOrderIdOrderByLineNumber(entity.getId())));
    }
}
