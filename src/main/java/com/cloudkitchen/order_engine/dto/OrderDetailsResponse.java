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

    // Custom AI dish details (null for legacy ingredient-id orders)
    private String dishType;
    private String protein;
    private List<String> ingredients;
    private String cookingStyle;
    private String spiceLevel;
    private String oilLevel;
    private String specialInstructions;
    private String dishSummary;
    private Integer quantity;

    public OrderDetailsResponse(
            Long orderId,
            Long customerId,
            Long kitchenId,
            String status,
            double totalPrice,
            int totalPrepTime,
            List<OrderItemResponse> items,
            String dishType,
            String protein,
            List<String> ingredients,
            String cookingStyle,
            String spiceLevel,
            String oilLevel,
            String specialInstructions,
            String dishSummary,
            Integer quantity
    ) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.kitchenId = kitchenId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
        this.items = items;
        this.dishType = dishType;
        this.protein = protein;
        this.ingredients = ingredients;
        this.cookingStyle = cookingStyle;
        this.spiceLevel = spiceLevel;
        this.oilLevel = oilLevel;
        this.specialInstructions = specialInstructions;
        this.dishSummary = dishSummary;
        this.quantity = quantity;
    }

    public Long getOrderId() { return orderId; }
    public Long getCustomerId() { return customerId; }
    public Long getKitchenId() { return kitchenId; }
    public String getStatus() { return status; }
    public double getTotalPrice() { return totalPrice; }
    public int getTotalPrepTime() { return totalPrepTime; }
    public List<OrderItemResponse> getItems() { return items; }

    public String getDishType() { return dishType; }
    public String getProtein() { return protein; }
    public List<String> getIngredients() { return ingredients; }
    public String getCookingStyle() { return cookingStyle; }
    public String getSpiceLevel() { return spiceLevel; }
    public String getOilLevel() { return oilLevel; }
    public String getSpecialInstructions() { return specialInstructions; }
    public String getDishSummary() { return dishSummary; }
    public Integer getQuantity() { return quantity; }
}
