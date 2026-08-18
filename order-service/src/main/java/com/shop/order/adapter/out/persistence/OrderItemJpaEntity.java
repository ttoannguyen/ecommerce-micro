package com.shop.order.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private int lineNumber;
    private Long productId;
    private UUID reservationId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;

    protected OrderItemJpaEntity() {
    }

    public OrderItemJpaEntity(Long id, Long orderId, int lineNumber, Long productId,
                              UUID reservationId, String productName,
                              BigDecimal unitPrice, int quantity) {
        this.id = id;
        this.orderId = orderId;
        this.lineNumber = lineNumber;
        this.productId = productId;
        this.reservationId = reservationId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getOrderId() { return orderId; }
    public int getLineNumber() { return lineNumber; }
    public Long getProductId() { return productId; }
    public UUID getReservationId() { return reservationId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
