package com.cloudkitchen.order_engine.dto;

import java.util.List;

/** The catalog ingredient ids a kitchen currently marks as in stock. */
public record KitchenStockResponse(
        Long kitchenId,
        List<Long> inStockIds
) {
}
