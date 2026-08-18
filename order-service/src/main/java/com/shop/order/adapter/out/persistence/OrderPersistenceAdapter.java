package com.shop.order.adapter.out.persistence;

import com.shop.order.domain.model.Order;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Outbound adapter: implements the load/save ports with Spring Data. */
@Component
public class OrderPersistenceAdapter implements SaveOrderPort, LoadOrderPort {

    private final SpringDataOrderRepository repository;
    private final SpringDataOrderItemRepository itemRepository;
    private final SpringDataOrderStatusHistoryRepository historyRepository;

    public OrderPersistenceAdapter(SpringDataOrderRepository repository,
                                   SpringDataOrderItemRepository itemRepository,
                                   SpringDataOrderStatusHistoryRepository historyRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        boolean newOrder = order.id() == null;
        OrderJpaEntity entity = repository.saveAndFlush(OrderMapper.toEntity(order));
        if (newOrder) {
            itemRepository.saveAll(order.items().stream()
                    .map(item -> OrderMapper.toItemEntity(entity.getId(), order.items().indexOf(item), item))
                    .toList());
        }
        order.history().forEach(transition -> historyRepository
                .findByOrderIdAndSequence(entity.getId(), transition.sequence())
                .orElseGet(() -> historyRepository.save(
                        OrderMapper.toHistoryEntity(entity.getId(), transition))));
        return toDomain(entity);
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll().stream()
                .map(entity -> OrderMapper.toDomain(entity,
                        itemRepository.findByOrderIdOrderByLineNumber(entity.getId()),
                        historyRepository.findByOrderIdOrderBySequence(entity.getId())))
                .toList();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private Order toDomain(OrderJpaEntity entity) {
        return OrderMapper.toDomain(entity,
                itemRepository.findByOrderIdOrderByLineNumber(entity.getId()),
                historyRepository.findByOrderIdOrderBySequence(entity.getId()));
    }
}
