package com.cloudkitchen.order_engine.dto;

import java.util.List;

public class OrderDetailsResponse {

    private Long orderId;
    private Long customerId;
    private Long kitchenId;
    private String status;
    private double totalPrice;
    private int totalPrepTime;
    private List<OrderItemResponse> items;

    public OrderDetailsResponse(
            Long orderId,
            Long customerId,
            Long kitchenId,
            String status,
            double totalPrice,
            int totalPrepTime,
            List<OrderItemResponse> items
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.kitchenId = kitchenId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public Long getKitchenId() { return kitchenId; }
    public String getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public int getTotalPrepTime() { return totalPrepTime; }
    public List<OrderItemResponse> getItems() { return items; }
}
