package com.shop.order.application;

import com.shop.order.domain.model.Order;
import com.shop.order.domain.model.ProductSnapshot;
import com.shop.order.domain.model.Quantity;
import com.shop.order.domain.port.in.PlaceOrderCommand;
import com.shop.order.domain.port.in.PlaceOrderUseCase;
import com.shop.order.domain.port.out.LoadProductPort;
import com.shop.order.domain.port.out.SaveOrderPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use case điều phối: hỏi product -> dựng aggregate -> lưu. Không chứa luật miền. */
@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveOrderPort saveOrderPort;

    public PlaceOrderService(LoadProductPort loadProductPort, SaveOrderPort saveOrderPort) {
        this.loadProductPort = loadProductPort;
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        ProductSnapshot product = loadProductPort.loadProduct(command.productId());
        Order order = Order.place(product, Quantity.of(command.quantity()));
        return saveOrderPort.save(order);
    }
}
