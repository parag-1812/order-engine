package com.cloudkitchen.order_engine.dto;

import java.util.List;

/** A sourcing request as shown to suppliers, kitchens and customers. */
public record SourcingRequestResponse(
        Long id,
        Long orderId,
        Long kitchenId,
        String kitchenName,
        Long supplierId,
        String supplierName,
        List<String> items,
        String status
) {
}
