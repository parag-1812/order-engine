package com.cloudkitchen.order_engine.dto;

public record AdminCustomerRow(
        Long id,
        String username,
        String area,
        long orders,
        double totalSpent
) {
}
