package com.cloudkitchen.order_engine.ingredient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class IngredientEntity {

    @Id
    private Long id;

    private String name;

    private double price;

    @Column(name = "prep_time_in_minutes")
    private int prepTimeInMinutes;

    private boolean vegetarian;

    protected IngredientEntity() {

    }

    public IngredientEntity(Long id, String name, double price, int prepTimeInMinutes, boolean vegetarian) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.prepTimeInMinutes = prepTimeInMinutes;
        this.vegetarian = vegetarian;
    }

    public Long getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public int getPrepTimeInMinutes() {
        return prepTimeInMinutes;
    }

    public boolean isVegetarian() {
        return vegetarian;
    }
}
