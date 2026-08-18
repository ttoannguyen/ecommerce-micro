package com.shop.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Order aggregate root. Stock is owned by product-service; lines are snapshots. */
public class Order {

    private final Long id;
    private final List<OrderItem> items;
    private final String idempotencyKey;
    private final Money totalPrice;
    private final OrderStatus status;
    private final Instant createdAt;

    private Order(Long id, List<OrderItem> items, String idempotencyKey,
                  Money totalPrice, OrderStatus status, Instant createdAt) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order phải có ít nhất một item");
        }
        this.id = id;
        this.items = List.copyOf(items);
        this.idempotencyKey = idempotencyKey;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Backward-compatible single-line factory used by the first milestone tests. */
    public static Order place(ReservedProduct product, Quantity quantity,
                              String idempotencyKey) {
        return new Order(null,
                List.of(new OrderItem(product.productId(), product.reservationId(),
                        product.name(), product.price(), quantity)),
                requireKey(idempotencyKey),
                product.price().multiply(quantity.value()),
                OrderStatus.CREATED,
                Instant.now());
    }

    /** Places an order only after product-service has reserved every requested line. */
    public static Order place(List<ReservedProduct> reserved, String idempotencyKey) {
        if (reserved == null || reserved.isEmpty()) {
            throw new IllegalArgumentException("order phải có ít nhất một item");
        }
        List<OrderItem> items = reserved.stream()
                .map(product -> new OrderItem(product.productId(), product.reservationId(),
                        product.name(), product.price(), Quantity.of(product.quantity())))
                .toList();
        return new Order(null, items, requireKey(idempotencyKey), total(items),
                OrderStatus.RESERVED, Instant.now());
    }

    public static Order rehydrate(Long id, Long productId, UUID reservationId,
                                  String idempotencyKey, Quantity quantity,
                                  Money totalPrice, OrderStatus status, Instant createdAt) {
        return rehydrate(id, List.of(new OrderItem(productId, reservationId, "legacy",
                totalPrice.multiply(1), quantity)), idempotencyKey, totalPrice, status, createdAt);
    }

    public static Order rehydrate(Long id, List<OrderItem> items, String idempotencyKey,
                                  Money totalPrice, OrderStatus status, Instant createdAt) {
        return new Order(id, items, idempotencyKey, totalPrice, status, createdAt);
    }

    public boolean matchesRequest(Long requestedProductId, int requestedQuantity) {
        return items.size() == 1
                && productId().equals(requestedProductId)
                && quantity().value() == requestedQuantity;
    }

    public boolean matchesRequest(List<OrderItemDraft> requestedItems) {
        return items.size() == requestedItems.size()
                && items.stream().allMatch(item -> requestedItems.stream().anyMatch(requested ->
                        item.productId().equals(requested.productId())
                                && item.quantity().value() == requested.quantity().value()));
    }

    public Long id() { return id; }
    public List<OrderItem> items() { return items; }
    public String idempotencyKey() { return idempotencyKey; }
    public Money totalPrice() { return totalPrice; }
    public OrderStatus status() { return status; }
    public Instant createdAt() { return createdAt; }

    /** Compatibility projections for clients of the single-SKU API. */
    public Long productId() { return items.get(0).productId(); }
    public UUID reservationId() { return items.get(0).reservationId(); }
    public Quantity quantity() { return items.get(0).quantity(); }

    public Order transitionTo(OrderStatus target) {
        if (!isValidTransition(status, target)) {
            throw new IllegalStateException("không thể chuyển order từ " + status + " sang " + target);
        }
        return new Order(id, items, idempotencyKey, totalPrice, target, createdAt);
    }

    private static boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING_RESERVATION -> to == OrderStatus.RESERVED || to == OrderStatus.FAILED;
            case RESERVED -> to == OrderStatus.PAYMENT_PENDING || to == OrderStatus.CANCELLED;
            case PAYMENT_PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
            case CREATED -> to == OrderStatus.PAYMENT_PENDING || to == OrderStatus.CANCELLED;
            case PAID, CANCELLED, FAILED -> false;
        };
    }

    private static Money total(List<OrderItem> items) {
        BigDecimal amount = items.stream()
                .map(item -> item.totalPrice().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.of(amount);
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key là bắt buộc");
        }
        return key.trim();
    }
}
