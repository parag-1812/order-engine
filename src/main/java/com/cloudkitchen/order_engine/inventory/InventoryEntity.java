package com.cloudkitchen.order_engine.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    private Long id;

    private Long kitchenId;
    private Long ingredientId;
    private int availableQuantity;

    protected InventoryEntity() {

    }

    public InventoryEntity(Long id, Long kitchenId, Long ingredientId, int availableQuantity) {
        this.id = id;
        this.kitchenId = kitchenId;
        this.ingredientId = ingredientId;
        this.availableQuantity = availableQuantity;
    }

    public Long getKitchenId() {
        return kitchenId;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void reduceQuantity(int quantity) {
        if (quantity > availableQuantity) {
            throw new IllegalStateException("Insufficient inventory");
        }
        this.availableQuantity -= quantity;
    }

    public void increaseQuantity(int quantity) {
        this.availableQuantity += quantity;
    }
}
