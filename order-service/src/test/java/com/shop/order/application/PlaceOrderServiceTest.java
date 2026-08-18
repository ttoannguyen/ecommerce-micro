package com.shop.order.application;

import com.shop.order.domain.model.IdempotencyConflictException;
import com.shop.order.domain.model.Money;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderStatus;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.port.in.PlaceOrderCommand;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.ReserveStockPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceOrderServiceTest {

    private FakeInventory inventory;
    private FakeOrderStore orders;
    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        inventory = new FakeInventory();
        orders = new FakeOrderStore();
        service = new PlaceOrderService(inventory, orders, orders);
    }

    @Test
    void storesReservationReferenceReturnedByInventory() {
        Order order = service.placeOrder(new PlaceOrderCommand(1L, 2, "order-1"));

        assertThat(order.reservationId()).isEqualTo(inventory.reservationId);
        assertThat(order.idempotencyKey()).isEqualTo("order-1");
        assertThat(inventory.reserveCalls).isEqualTo(1);
        assertThat(inventory.releasedId).isNull();
    }

    @Test
    void replayReturnsExistingOrderWithoutAnotherHold() {
        Order existing = persisted("replay-key", 1L, 2);
        orders.saved.add(existing);

        Order replay = service.placeOrder(new PlaceOrderCommand(1L, 2, "replay-key"));

        assertThat(replay).isSameAs(existing);
        assertThat(inventory.reserveCalls).isZero();
        assertThat(orders.saveCalls).isZero();
    }

    @Test
    void replayWithDifferentPayloadIsRejected() {
        orders.saved.add(persisted("conflict-key", 1L, 2));

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand(1L, 3, "conflict-key")))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(inventory.reserveCalls).isZero();
    }

    @Test
    void failedOrderSaveReleasesByReservationId() {
        orders.failSave = true;

        assertThatThrownBy(() -> service.placeOrder(
                new PlaceOrderCommand(1L, 2, "failed-save")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(inventory.releasedId).isEqualTo(inventory.reservationId);
    }

    private static Order persisted(String key, Long productId, int quantity) {
        return Order.rehydrate(1L, productId, UUID.randomUUID(), key,
                Quantity.of(quantity), Money.of(new BigDecimal("2000")),
                OrderStatus.CREATED, Instant.now());
    }

    private static final class FakeInventory implements ReserveStockPort {
        private final UUID reservationId = UUID.randomUUID();
        private int reserveCalls;
        private UUID releasedId;

        @Override
        public ReservedProduct reserve(Long productId, Quantity quantity, String key) {
            reserveCalls++;
            return new ReservedProduct(productId, reservationId, "Keyboard",
                    Money.of(new BigDecimal("1000")), Instant.now().plusSeconds(900));
        }

        @Override
        public void release(UUID id) {
            releasedId = id;
        }
    }

    private static final class FakeOrderStore implements SaveOrderPort, LoadOrderPort {
        private final List<Order> saved = new ArrayList<>();
        private int saveCalls;
        private boolean failSave;

        @Override
        public Order save(Order order) {
            saveCalls++;
            if (failSave) {
                throw new IllegalStateException("orderdb down");
            }
            saved.add(order);
            return order;
        }

        @Override
        public List<Order> findAll() {
            return List.copyOf(saved);
        }

        @Override
        public Optional<Order> findById(Long id) {
            return saved.stream().filter(order -> id.equals(order.id())).findFirst();
        }

        @Override
        public Optional<Order> findByIdempotencyKey(String key) {
            return saved.stream().filter(order -> key.equals(order.idempotencyKey())).findFirst();
        }
    }
}
