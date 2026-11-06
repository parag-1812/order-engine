package com.cloudkitchen.order_engine.dto;

/** A kitchen plus how many catalog items it currently stocks. */
public record KitchenSummaryResponse(
        Long id,
        String name,
        boolean vegetarianOnly,
        double latitude,
        double longitude,
        long stockCount
) {
}
