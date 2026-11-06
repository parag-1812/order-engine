package com.cloudkitchen.order_engine.kitchen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A fulfilment partner — a cloud kitchen or a restaurant (one generic type).
 * <p>
 * Seeded demo kitchens have a null {@code ownerUsername} (platform-owned,
 * part of the network). A kitchen a partner registers for themselves is bound
 * to their account via {@code ownerUsername}, so they manage only their own.
 */
@Entity
@Table(name = "kitchens")
public class KitchenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean vegetarianOnly;

    private double latitude;

    private double longitude;

    /** The partner account that owns this kitchen; null for seeded network kitchens. */
    @Column(name = "owner_username")
    private String ownerUsername;

    protected KitchenEntity() {

    }

    /** Register a new partner-owned kitchen. */
    public KitchenEntity(String name, boolean vegetarianOnly, double latitude, double longitude, String ownerUsername) {
        this.name = name;
        this.vegetarianOnly = vegetarianOnly;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ownerUsername = ownerUsername;
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

    public String getOwnerUsername() {
        return ownerUsername;
    }
}
