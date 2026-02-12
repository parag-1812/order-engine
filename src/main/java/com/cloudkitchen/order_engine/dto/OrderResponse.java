package com.cloudkitchen.order_engine.dto;

public class OrderResponse {

    private Long kitchenId;
    private String status;
    private double totalPrice;
    private int totalPrepTime;

    public OrderResponse(Long kitchenId, String status, double totalPrice, int totalPrepTime) {
        this.kitchenId = kitchenId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
    }

    public Long getKitchenId() {
        return kitchenId;
    }

    public String getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public int getTotalPrepTime() {
        return totalPrepTime;
    }
}
