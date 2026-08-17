package com.shop.product.domain.model;

/**
 * Product aggregate root — plain POJO, no framework annotations.
 *
 * Product owns the stock, so the "never oversell" invariant is enforced HERE.
 * order-service cannot enforce it: all it ever sees is a copy that is already stale.
 *
 * `stock` is a projection of the ledger, never an independent number. That is why a
 * new product starts at zero and has to be RECEIVED into existence: if stock could be
 * conjured at creation time, `SUM(movements) == stock` would already be false on the
 * first row, and every later audit would be meaningless.
 */
public class Product {

    private final Long id;
    private final String name;
    private final Money price;
    private final int stock;

    private Product(Long id, String name, Money price, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("stock không được âm");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /** Creates a new product (no id yet, and no stock until something is received). */
    public static Product create(String name, Money price) {
        return new Product(null, name, price, 0);
    }

    /** Rebuilds from persistence (id already assigned). */
    public static Product rehydrate(Long id, String name, Money price, int stock) {
        return new Product(id, name, price, stock);
    }

    /** Goods arrived: opening balance or a supplier delivery. */
    public StockChange receive(int quantity) {
        requirePositive(quantity);
        return change(stock + quantity, StockMovement.receipt(requireId(), quantity));
    }

    /** Reserve: take the stock off the shelf now. A decision, not a question. */
    public StockChange reserve(int quantity) {
        requirePositive(quantity);
        if (stock < quantity) {
            throw new InsufficientStockException(
                    "Không đủ tồn kho. Còn " + stock + ", cần " + quantity);
        }
        return change(stock - quantity, StockMovement.issue(requireId(), quantity));
    }

    /** Compensation: put the stock back after the caller failed. */
    public StockChange release(int quantity) {
        requirePositive(quantity);
        return change(stock + quantity, StockMovement.release(requireId(), quantity));
    }

    /**
     * The shelf and the system disagreed. Record the difference as its own fact with
     * a reason, rather than overwriting the balance and losing the discrepancy.
     */
    public StockChange adjust(int delta, ReasonCode reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("điều chỉnh 0 không phải một sự kiện");
        }
        return change(stock + delta, StockMovement.adjustment(requireId(), delta, reason));
    }

    private StockChange change(int newStock, StockMovement movement) {
        return new StockChange(new Product(id, name, price, newStock), movement);
    }

    private Long requireId() {
        if (id == null) {
            throw new IllegalStateException("product chưa được lưu, không thể ghi ledger");
        }
        return id;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Money price() {
        return price;
    }

    public int stock() {
        return stock;
    }
}
