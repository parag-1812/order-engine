package com.cloudkitchen.order_engine.dto;

public record AdminKitchenRow(
        Long id,
        String name,
        boolean vegetarianOnly,
        String area,
        double latitude,
        double longitude,
        long stockCount
) {
}
