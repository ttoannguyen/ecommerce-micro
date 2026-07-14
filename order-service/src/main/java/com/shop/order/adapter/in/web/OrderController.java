package com.shop.order.adapter.in.web;

import com.shop.order.adapter.in.web.dto.CreateOrderRequest;
import com.shop.order.adapter.in.web.dto.OrderResponse;
import com.shop.order.domain.model.Order;
import com.shop.order.domain.port.in.FindOrdersUseCase;
import com.shop.order.domain.port.in.PlaceOrderCommand;
import com.shop.order.domain.port.in.PlaceOrderUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inbound REST adapter. Translates HTTP into use cases; holds no domain rules. */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final FindOrdersUseCase findOrdersUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase, FindOrdersUseCase findOrdersUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.findOrdersUseCase = findOrdersUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order order = placeOrderUseCase.placeOrder(
                new PlaceOrderCommand(request.productId(), request.quantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping
    public List<OrderResponse> all() {
        return findOrdersUseCase.findAll().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> byId(@PathVariable Long id) {
        return findOrdersUseCase.findById(id)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
