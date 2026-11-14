package com.cloudkitchen.order_engine.dto;

public record AdminSupplierRow(
        Long id,
        String name,
        String area,
        double latitude,
        double longitude,
        long stockCount
) {
}
