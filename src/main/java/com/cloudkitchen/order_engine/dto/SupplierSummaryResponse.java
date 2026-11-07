package com.cloudkitchen.order_engine.dto;

/** A supplier plus how many catalog items it currently stocks. */
public record SupplierSummaryResponse(
        Long id,
        String name,
        double latitude,
        double longitude,
        long stockCount
) {
}
