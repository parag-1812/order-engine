package com.cloudkitchen.order_engine.dto;

import java.util.List;

/** Replace a kitchen's in-stock set with this full list of catalog ingredient ids. */
public class UpdateKitchenStockRequest {

    private List<Long> inStockIds;

    public List<Long> getInStockIds() { return inStockIds; }
    public void setInStockIds(List<Long> inStockIds) { this.inStockIds = inStockIds; }
}
