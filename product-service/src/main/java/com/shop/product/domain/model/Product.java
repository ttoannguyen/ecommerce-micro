package com.shop.product.domain.model;

/**
 * Product aggregate root — plain POJO, no framework annotations.
 *
 * Product owns the stock, so the "never oversell" invariant is enforced HERE.
 * order-service cannot enforce it: all it ever sees is a copy that is already stale.
 *
 * `onHand` is a projection of the ledger, never an independent number. That is why a
 * new product starts at zero and has to be RECEIVED into existence: if on-hand could be
 * conjured at creation time, `SUM(movements) == onHand` would already be false on the
 * first row, and every later audit would be meaningless.
 */
public class Product {

    private final Long id;
    private final String name;
    private final Money price;
    private final int onHand;
    private final int reserved;

    private Product(Long id, String name, Money price, int onHand, int reserved) {
        if (onHand < 0) {
            throw new IllegalArgumentException("onHand không được âm");
        }
        if (reserved < 0 || reserved > onHand) {
            throw new IllegalArgumentException("reserved phải nằm trong khoảng 0..onHand");
        }
        this.id = id;
        this.name = name;
        this.price = price;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    /** Creates a new product (no id yet, and no on-hand until receipt). */
    public static Product create(String name, Money price) {
        return new Product(null, name, price, 0, 0);
    }

    /** Rebuilds from persistence (id already assigned). */
    public static Product rehydrate(Long id, String name, Money price,
                                    int onHand, int reserved) {
        return new Product(id, name, price, onHand, reserved);
    }

    /** Goods arrived: opening balance or a supplier delivery. */
    public StockChange receive(int quantity) {
        requirePositive(quantity);
        return physicalChange(onHand + quantity, reserved,
                StockMovement.receipt(requireId(), quantity));
    }

    /** Hold only changes the logical allocation; no physical stock has moved. */
    public Product hold(int quantity) {
        requirePositive(quantity);
        if (available() < quantity) {
            throw new InsufficientStockException(
                    "Không đủ tồn khả dụng. Còn " + available() + ", cần " + quantity);
        }
        return new Product(id, name, price, onHand, reserved + quantity);
    }

    /** Release/expiry removes one logical allocation and never creates a movement. */
    public Product releaseHold(int quantity) {
        requirePositive(quantity);
        if (reserved < quantity) {
            throw new IllegalStateException("reserved không đủ để release");
        }
        return new Product(id, name, price, onHand, reserved - quantity);
    }

    /** Fulfillment consumes both the hold and physical stock in one operation. */
    public StockChange fulfill(int quantity) {
        requirePositive(quantity);
        if (reserved < quantity) {
            throw new IllegalStateException("reserved không đủ để fulfill");
        }
        return physicalChange(onHand - quantity, reserved - quantity,
                StockMovement.issue(requireId(), quantity));
    }

    /**
     * The shelf and the system disagreed. Record the difference as its own fact with
     * a reason, rather than overwriting the balance and losing the discrepancy.
     */
    public StockChange adjust(int delta, ReasonCode reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("điều chỉnh 0 không phải một sự kiện");
        }
        return physicalChange(onHand + delta, reserved,
                StockMovement.adjustment(requireId(), delta, reason));
    }

    private StockChange physicalChange(int newOnHand, int newReserved,
                                       StockMovement movement) {
        return new StockChange(
                new Product(id, name, price, newOnHand, newReserved), movement);
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

    public int onHand() {
        return onHand;
    }

    public int reserved() {
        return reserved;
    }

    public int available() {
        return onHand - reserved;
    }
}
