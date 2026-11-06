package com.cloudkitchen.order_engine.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A kitchen's declaration that it stocks a given catalog ingredient.
 * <p>
 * Deliberately boolean, not weight-based: small kitchens will never keep
 * gram-level inventory accurate, but they will keep a simple "do I have this?"
 * checklist current. {@code inStock} lets a kitchen temporarily mark something
 * out without deleting it from their capability list.
 */
@Entity
@Table(name = "kitchen_stock")
public class KitchenStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long kitchenId;

    private Long catalogIngredientId;

    private boolean inStock = true;

    protected KitchenStockEntity() {
    }

    public KitchenStockEntity(Long kitchenId, Long catalogIngredientId, boolean inStock) {
        this.kitchenId = kitchenId;
        this.catalogIngredientId = catalogIngredientId;
        this.inStock = inStock;
    }

    public Long getId() { return id; }
    public Long getKitchenId() { return kitchenId; }
    public Long getCatalogIngredientId() { return catalogIngredientId; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
}
