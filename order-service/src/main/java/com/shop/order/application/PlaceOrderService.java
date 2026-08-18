package com.shop.order.application;

import com.shop.order.domain.model.InsufficientStockException;
import com.shop.order.domain.model.IdempotencyConflictException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.port.in.PlaceOrderCommand;
import com.shop.order.domain.port.in.PlaceOrderUseCase;
import com.shop.order.domain.port.out.ReserveStockPort;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * A two-step saga across two databases.
 *
 * There is no distributed transaction here. @Transactional would roll back orderdb,
 * but it cannot roll back an HTTP call that productdb has already committed. So when
 * saving the order fails after the hold was committed, we release that reservation.
 */
@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderService.class);

    private final ReserveStockPort reserveStockPort;
    private final SaveOrderPort saveOrderPort;
    private final LoadOrderPort loadOrderPort;

    public PlaceOrderService(ReserveStockPort reserveStockPort,
                             SaveOrderPort saveOrderPort,
                             LoadOrderPort loadOrderPort) {
        this.reserveStockPort = reserveStockPort;
        this.saveOrderPort = saveOrderPort;
        this.loadOrderPort = loadOrderPort;
    }

    @Override
    public Order placeOrder(PlaceOrderCommand command) {
        String key = requireIdempotencyKey(command.idempotencyKey());
        Order replay = loadOrderPort.findByIdempotencyKey(key).orElse(null);
        if (replay != null) {
            return replay(command, replay);
        }

        Quantity quantity = Quantity.of(command.quantity());

        // Step 1: product-service creates an expiring hold, or refuses.
        ReservedProduct reserved = reserve(command.productId(), quantity, key);

        // Step 2: record the reservation reference. If this fails, release the hold.
        try {
            return saveOrderPort.save(Order.place(reserved, quantity, key));
        } catch (DataIntegrityViolationException concurrentReplay) {
            Order winner = loadOrderPort.findByIdempotencyKey(key).orElse(null);
            if (winner != null && winner.matchesRequest(
                    command.productId(), command.quantity())) {
                return winner;
            }
            compensate(reserved.reservationId(), concurrentReplay);
            throw concurrentReplay;
        } catch (RuntimeException saveFailed) {
            compensate(reserved.reservationId(), saveFailed);
            throw saveFailed;
        }
    }

    private ReservedProduct reserve(Long productId, Quantity quantity, String key) {
        try {
            return reserveStockPort.reserve(productId, quantity, key);
        } catch (InsufficientStockException refused) {
            // A clean "no". product-service decided and committed nothing. Safe to give up.
            throw refused;
        } catch (RuntimeException ambiguous) {
            // Timeout, reset connection, bad deserialisation: the call failed, but we do
            // NOT know whether product-service committed before it did. We cannot release
            // without the UUID, but retrying this order with the same key returns the same
            // reservation; if nobody retries, its TTL is the recovery path.
            log.error("Reservation outcome UNKNOWN; retry with the same idempotency key: "
                            + "productId={} quantity={}",
                    productId, quantity.value(), ambiguous);
            throw ambiguous;
        }
    }

    private void compensate(java.util.UUID reservationId, RuntimeException cause) {
        try {
            reserveStockPort.release(reservationId);
        } catch (RuntimeException releaseFailed) {
            // The expiry worker is the backstop when compensation itself fails.
            log.error("Compensation failed, reservation remains HELD: reservationId={}",
                    reservationId, releaseFailed);
            cause.addSuppressed(releaseFailed);
        }
    }

    private static String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key phải có 1..128 ký tự");
        }
        return key.trim();
    }

    private static Order replay(PlaceOrderCommand command, Order existing) {
        if (!existing.matchesRequest(command.productId(), command.quantity())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key đã được dùng với payload khác");
        }
        return existing;
    }
}
