package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.dto.OrderDetailsResponse;
import com.cloudkitchen.order_engine.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;

    public AdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public List<OrderDetailsResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/revenue")
    public double getTotalRevenue() {
        return orderService.getTotalRevenue();
    }

    @GetMapping("/orders/count")
    public long getOrderCount() {
        return orderService.getOrderCount();
    }
}