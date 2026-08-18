package com.shop.order.application;

import com.shop.order.domain.model.InvalidOrderTransitionException;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.OrderNotFoundException;
import com.shop.order.domain.model.OrderStatus;
import com.shop.order.domain.port.in.OrderLifecycleUseCase;
import com.shop.order.domain.port.out.LoadOrderPort;
import com.shop.order.domain.port.out.ReserveStockPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.springframework.stereotype.Service;

@Service
public class OrderLifecycleService implements OrderLifecycleUseCase {

    private final LoadOrderPort loadOrderPort;
    private final SaveOrderPort saveOrderPort;
    private final ReserveStockPort reserveStockPort;

    public OrderLifecycleService(LoadOrderPort loadOrderPort, SaveOrderPort saveOrderPort,
                                 ReserveStockPort reserveStockPort) {
        this.loadOrderPort = loadOrderPort;
        this.saveOrderPort = saveOrderPort;
        this.reserveStockPort = reserveStockPort;
    }

    @Override
    public Order pay(Long id) {
        return saveTransition(id, OrderStatus.PAID);
    }

    @Override
    public Order cancel(Long id) {
        Order current = load(id);
        if (current.status() == OrderStatus.CANCELLED) {
            return current;
        }
        if (current.status() != OrderStatus.RESERVED
                && current.status() != OrderStatus.PAYMENT_PENDING
                && current.status() != OrderStatus.CREATED) {
            throw new InvalidOrderTransitionException(
                    "không thể hủy order ở trạng thái " + current.status());
        }
        current.items().forEach(item -> reserveStockPort.release(item.reservationId()));
        return saveOrderPort.save(current.transitionTo(OrderStatus.CANCELLED));
    }

    private Order saveTransition(Long id, OrderStatus target) {
        Order current = load(id);
        return saveOrderPort.save(current.transitionTo(target));
    }

    private Order load(Long id) {
        return loadOrderPort.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
