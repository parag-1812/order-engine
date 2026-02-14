package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.dto.CreateOrderRequest;
import com.cloudkitchen.order_engine.dto.OrderDetailsResponse;
import com.cloudkitchen.order_engine.dto.OrderResponse;
import com.cloudkitchen.order_engine.order.Order;
import com.cloudkitchen.order_engine.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestParam Long customerId,
            @RequestBody CreateOrderRequest request
    ) {

        Order order = orderService.createOrder(customerId, request);

        OrderResponse response = new OrderResponse(
                order.getKitchenId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getTotalPrepTime()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailsResponse> getOrder(@PathVariable Long orderId) {

        var orderEntity = orderService.getOrderById(orderId);

        return ResponseEntity.ok(orderEntity);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderDetailsResponse>> getOrdersByCustomer(
            @PathVariable Long customerId
    ) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

}
