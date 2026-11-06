package com.cloudkitchen.order_engine.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A canonical ingredient in the platform-owned master catalog.
 * <p>
 * This is the single shared vocabulary the whole platform speaks: the AI maps a
 * customer's free-text request onto these ingredients (via {@code synonyms}),
 * and kitchens declare which of these they stock. Matching a dish to a kitchen
 * is therefore a simple set comparison over catalog ids.
 * <p>
 * Dietary flags power the health/diet wedge (vegan, dairy-free, gluten-free,
 * nut-free, high-protein) without any per-order lookups.
 */
@Entity
@Table(name = "catalog_ingredients")
public class CatalogIngredientEntity {

    @Id
    private Long id;

    private String name;

    /** High-level group e.g. Vegetable, Protein, Dairy, Grain, Spice, Oil, Legume. */
    private String category;

    /** Comma-separated alternate names/spellings used to normalise free text. */
    @Column(length = 500)
    private String synonyms;

    private boolean vegan;
    private boolean containsDairy;
    private boolean containsGluten;
    private boolean containsNuts;
    private boolean highProtein;

    protected CatalogIngredientEntity() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getSynonyms() { return synonyms; }
    public boolean isVegan() { return vegan; }
    public boolean isContainsDairy() { return containsDairy; }
    public boolean isContainsGluten() { return containsGluten; }
    public boolean isContainsNuts() { return containsNuts; }
    public boolean isHighProtein() { return highProtein; }
}
