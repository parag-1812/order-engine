package com.cloudkitchen.order_engine.dto;

public class OrderItemResponse {

    private Long ingredientId;
    private int quantity;
    private double priceAtOrderTime;
    private int prepTimeAtOrderTime;

    public OrderItemResponse(
            Long ingredientId,
            int quantity,
            double priceAtOrderTime,
            int prepTimeAtOrderTime
    ) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.priceAtOrderTime = priceAtOrderTime;
        this.prepTimeAtOrderTime = prepTimeAtOrderTime;
    }

    public Long getIngredientId() { return ingredientId; }
    public int getQuantity() { return quantity; }
    public double getPriceAtOrderTime() { return priceAtOrderTime; }
    public int getPrepTimeAtOrderTime() { return prepTimeAtOrderTime; }
}
