package com.cloudkitchen.order_engine.order;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    private Long kitchenId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private double totalPrice;

    private int totalPrepTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    // ---- Custom AI dish fields (null for legacy ingredient-id orders) ----
    private String dishType;          // VEG / NON_VEG
    private String protein;
    @Column(length = 1000)
    private String ingredientsCsv;    // comma-joined ingredient names
    private String cookingStyle;
    private String spiceLevel;
    private String oilLevel;
    @Column(length = 1000)
    private String specialInstructions;
    @Column(length = 1000)
    private String dishSummary;
    private Integer quantity;
    private Double distanceKm;

    protected OrderEntity() {

    }

    public OrderEntity(
            Long customerId,
            Long kitchenId,
            OrderStatus status,
            double totalPrice,
            int totalPrepTime
    ) {
        this.customerId = customerId;
        this.kitchenId = kitchenId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
    }


    public void addItem(OrderItemEntity item) {
        items.add(item);
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getKitchenId() {
        return kitchenId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public int getTotalPrepTime() {
        return totalPrepTime;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    // ---- Custom dish accessors ----
    public String getDishType() { return dishType; }
    public void setDishType(String dishType) { this.dishType = dishType; }

    public String getProtein() { return protein; }
    public void setProtein(String protein) { this.protein = protein; }

    public String getIngredientsCsv() { return ingredientsCsv; }
    public void setIngredientsCsv(String ingredientsCsv) { this.ingredientsCsv = ingredientsCsv; }

    public String getCookingStyle() { return cookingStyle; }
    public void setCookingStyle(String cookingStyle) { this.cookingStyle = cookingStyle; }

    public String getSpiceLevel() { return spiceLevel; }
    public void setSpiceLevel(String spiceLevel) { this.spiceLevel = spiceLevel; }

    public String getOilLevel() { return oilLevel; }
    public void setOilLevel(String oilLevel) { this.oilLevel = oilLevel; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public String getDishSummary() { return dishSummary; }
    public void setDishSummary(String dishSummary) { this.dishSummary = dishSummary; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}
