package com.shop.product.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/** JPA record for Product. Keeps the original table name "product". */
@Entity
@Table(name = "product")
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Explicit, because V1__init.sql states the same thing. Left implicit, Hibernate
    // would assume nullable varchar(255) and numeric(38,2) — and `validate` does not
    // compare length, precision or nullability, so the drift would go unnoticed.
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    /**
     * Optimistic lock. Every UPDATE becomes:
     *   UPDATE product SET on_hand=?, reserved=?, version=v+1
     *   WHERE id=? AND version=v
     * If two transactions both read version=v, only one of them matches a row; the
     * other changes 0 rows and Hibernate raises OptimisticLockingFailure.
     * Reservations take a write lock instead, so this is the backstop for other paths.
     */
    @Version
    private Long version;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(Long id, String name, BigDecimal price,
                            int onHand, int reserved) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    /** Mutates the managed entity in place so dirty-checking bumps the version. */
    void changeBalance(int onHand, int reserved) {
        this.onHand = onHand;
        this.reserved = reserved;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public Long getVersion() {
        return version;
    }
}
