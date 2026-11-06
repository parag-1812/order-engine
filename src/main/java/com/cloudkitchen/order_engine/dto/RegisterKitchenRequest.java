package com.cloudkitchen.order_engine.dto;

/** A partner registering their kitchen (cloud kitchen or restaurant). */
public class RegisterKitchenRequest {

    private String name;
    private boolean vegetarianOnly;
    private Double latitude;
    private Double longitude;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isVegetarianOnly() { return vegetarianOnly; }
    public void setVegetarianOnly(boolean vegetarianOnly) { this.vegetarianOnly = vegetarianOnly; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
