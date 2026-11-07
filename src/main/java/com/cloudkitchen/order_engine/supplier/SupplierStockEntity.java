package com.cloudkitchen.order_engine.supplier;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** What a supplier stocks, declared against the shared catalog (boolean). */
@Entity
@Table(name = "supplier_stock")
public class SupplierStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long supplierId;

    private Long catalogIngredientId;

    private boolean inStock = true;

    protected SupplierStockEntity() {
    }

    public SupplierStockEntity(Long supplierId, Long catalogIngredientId, boolean inStock) {
        this.supplierId = supplierId;
        this.catalogIngredientId = catalogIngredientId;
        this.inStock = inStock;
    }

    public Long getId() { return id; }
    public Long getSupplierId() { return supplierId; }
    public Long getCatalogIngredientId() { return catalogIngredientId; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
}
