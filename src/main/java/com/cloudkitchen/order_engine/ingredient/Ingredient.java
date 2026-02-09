package com.cloudkitchen.order_engine.ingredient;

public class Ingredient {

    private Long id;
    private String name;

    private double price;
    private int prepTimeInMinutes;

    private boolean vegetarian;

    public Ingredient(
            Long id,
            String name,
            double price,
            int prepTimeInMinutes,
            boolean vegetarian
    ) {
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

