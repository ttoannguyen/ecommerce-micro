package com.shop.product.adapter.out.persistence;

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

    private String name;
    private BigDecimal price;
    private int stock;

    /**
     * Optimistic lock. Every UPDATE becomes:
     *   UPDATE product SET stock=?, version=v+1 WHERE id=? AND version=v
     * If two transactions both read version=v, only one of them matches a row; the
     * other changes 0 rows and Hibernate raises OptimisticLockingFailure.
     * Reservations take a write lock instead, so this is the backstop for other paths.
     */
    @Version
    private Long version;

    protected ProductJpaEntity() {
    }

    public ProductJpaEntity(Long id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /** Mutates the managed entity in place so dirty-checking bumps the version. */
    void changeStock(int stock) {
        this.stock = stock;
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

    public int getStock() {
        return stock;
    }

    public Long getVersion() {
        return version;
    }
}
