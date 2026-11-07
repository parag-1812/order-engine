package com.cloudkitchen.order_engine.dto;

import java.util.List;

/** The catalog ingredient ids a supplier currently marks as in stock. */
public record SupplierStockResponse(
        Long supplierId,
        List<Long> inStockIds
) {
}
