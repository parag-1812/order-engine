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

    private double latitude;

    private double longitude;

    protected KitchenEntity() {

    }

    public KitchenEntity(Long id, String name, boolean vegetarianOnly, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.vegetarianOnly = vegetarianOnly;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isVegetarianOnly() {
        return vegetarianOnly;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
