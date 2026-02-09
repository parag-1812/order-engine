package com.cloudkitchen.order_engine.order;

public class OrderItem {

    private Long ingredientId;
    private int quantity;

    // snapshot values
    private double priceAtOrderTime;
    private int prepTimeAtOrderTime;

    public OrderItem(
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

    public double getTotalPrice() {
        return priceAtOrderTime * quantity;
    }

    public int getTotalPrepTime() {
        return prepTimeAtOrderTime * quantity;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public int getQuantity() {
        return quantity;
    }
}
