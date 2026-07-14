package com.shop.order.application;

import com.shop.order.domain.model.InsufficientStockException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.model.ReservedProduct;
import com.shop.order.domain.port.in.PlaceOrderCommand;
import com.shop.order.domain.port.in.PlaceOrderUseCase;
import com.shop.order.domain.port.out.ReserveStockPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A two-step saga across two databases.
 *
 * There is no distributed transaction here. @Transactional would roll back orderdb,
 * but it cannot roll back an HTTP call that productdb has already committed. So when
 * saving the order fails after the stock was taken, we hand the stock back ourselves.
 */
@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderService.class);

    private final ReserveStockPort reserveStockPort;
    private final SaveOrderPort saveOrderPort;

    public PlaceOrderService(ReserveStockPort reserveStockPort, SaveOrderPort saveOrderPort) {
        this.reserveStockPort = reserveStockPort;
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    public Order placeOrder(PlaceOrderCommand command) {
        Quantity quantity = Quantity.of(command.quantity());

        // Step 1: product-service takes the stock, or refuses. No stock, no order.
        ReservedProduct reserved = reserve(command.productId(), quantity);

        // Step 2: record the order. If this fails the stock is already gone, so give it back.
        try {
            return saveOrderPort.save(Order.place(reserved, quantity));
        } catch (RuntimeException saveFailed) {
            compensate(command.productId(), quantity, saveFailed);
            throw saveFailed;
        }
    }

    private ReservedProduct reserve(Long productId, Quantity quantity) {
        try {
            return reserveStockPort.reserve(productId, quantity);
        } catch (InsufficientStockException refused) {
            // A clean "no". product-service decided and committed nothing. Safe to give up.
            throw refused;
        } catch (RuntimeException ambiguous) {
            // Timeout, reset connection, bad deserialisation: the call failed, but we do
            // NOT know whether product-service committed the decrement before it did. We
            // cannot compensate — releasing stock that was never taken would invent stock.
            //
            // No try/catch can close this. The fix is a reservation with a TTL, so an
            // unclaimed one expires on its own, or an idempotency key so the call can be
            // safely retried. Until then, log it and let a human reconcile.
            log.error("Reservation outcome UNKNOWN, stock may be leaked: productId={} quantity={}",
                    productId, quantity.value(), ambiguous);
            throw ambiguous;
        }
    }

    private void compensate(Long productId, Quantity quantity, RuntimeException cause) {
        try {
            reserveStockPort.release(productId, quantity);
        } catch (RuntimeException releaseFailed) {
            // Stock is now stranded: taken, but no order exists to justify it. Nothing
            // left to do in-process. This is why real systems reserve with a TTL rather
            // than forever — an expiry job is the backstop when compensation itself fails.
            log.error("Compensation failed, stock leaked: productId={} quantity={}",
                    productId, quantity.value(), releaseFailed);
            cause.addSuppressed(releaseFailed);
        }
    }
}
