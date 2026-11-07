package com.cloudkitchen.order_engine.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An ingredient supplier (wholesaler / dark store / mandi partner) on the
 * network. When a kitchen can't cover a dish, the missing items are sourced
 * from the nearest supplier that stocks them. Bound to a SUPPLIER account.
 */
@Entity
@Table(name = "suppliers")
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double latitude;

    private double longitude;

    @Column(name = "owner_username")
    private String ownerUsername;

    protected SupplierEntity() {
    }

    public SupplierEntity(String name, double latitude, double longitude, String ownerUsername) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getOwnerUsername() { return ownerUsername; }
}
