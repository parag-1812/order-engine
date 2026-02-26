package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.customer.CustomerEntity;
import com.cloudkitchen.order_engine.dto.CreateOrderRequest;
import com.cloudkitchen.order_engine.dto.OrderDetailsResponse;
import com.cloudkitchen.order_engine.dto.OrderResponse;
import com.cloudkitchen.order_engine.order.Order;
import com.cloudkitchen.order_engine.order.OrderStatus;
import com.cloudkitchen.order_engine.repository.CustomerRepository;
import com.cloudkitchen.order_engine.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;

    public OrderController(OrderService orderService,
                           CustomerRepository customerRepository) {
        this.orderService = orderService;
        this.customerRepository = customerRepository;
    }

    // ===============================
    // CREATE ORDER (USER ONLY)
    // ===============================
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {

        Long customerId = resolveCustomerId(authentication);

        Order order = orderService.createOrder(customerId, request);

        OrderResponse response = new OrderResponse(
                order.getKitchenId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                order.getTotalPrepTime()
        );

        return ResponseEntity.ok(response);
    }

    // ===============================
    // GET SINGLE ORDER
    // ===============================
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('USER','KITCHEN')")
    public ResponseEntity<OrderDetailsResponse> getOrder(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    // ===============================
    // GET CUSTOMER ORDERS (USER ONLY)
    // ===============================
    @GetMapping("/customer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrderDetailsResponse>> getOrdersByCustomer(
            Authentication authentication
    ) {

        Long customerId = resolveCustomerId(authentication);

        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    // ===============================
    // GET KITCHEN ORDERS
    // ===============================
    @GetMapping("/kitchen/{kitchenId}")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<List<OrderDetailsResponse>> getOrdersByKitchen(
            @PathVariable Long kitchenId
    ) {
        return ResponseEntity.ok(orderService.getOrdersByKitchen(kitchenId));
    }

    // ===============================
    // UPDATE STATUS (KITCHEN ONLY)
    // ===============================
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok("Order status updated to " + status);
    }

    // ===============================
    // CANCEL ORDER (USER ONLY)
    // ===============================
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId
    ) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok("Order cancelled successfully");
    }

    // ===============================
    // PRIVATE HELPER METHOD
    // ===============================
    private Long resolveCustomerId(Authentication authentication) {

        String username = authentication.getName();

        CustomerEntity customer = customerRepository
                .findByUsername(username)
                .orElseGet(() ->
                        customerRepository.save(new CustomerEntity(username))
                );

        return customer.getId();
    }
}