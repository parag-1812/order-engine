package com.cloudkitchen.order_engine.kitchen;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kitchens")
public class KitchenEntity {

    @Id
    private Long id;

    private String name;

    private boolean vegetarianOnly;

    protected KitchenEntity() {

    }

    public KitchenEntity(Long id, String name, boolean vegetarianOnly) {
        this.id = id;
        this.name = name;
        this.vegetarianOnly = vegetarianOnly;
    }

    public Long getId() {
        return id;
    }

    public boolean isVegetarianOnly() {
        return vegetarianOnly;
    }
}
