package com.cloudkitchen.order_engine.dto;

/** A canonical catalog ingredient exposed to kitchens and (read-only) the UI. */
public record CatalogIngredientResponse(
        Long id,
        String name,
        String category,
        boolean vegan,
        boolean containsDairy,
        boolean containsGluten,
        boolean containsNuts,
        boolean highProtein
) {
}
