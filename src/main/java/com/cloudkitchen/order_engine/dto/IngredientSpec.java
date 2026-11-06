package com.cloudkitchen.order_engine.dto;

/**
 * One ingredient as classified by the AI: how essential it is to the dish
 * (CORE / SUBSTITUTABLE / OPTIONAL) and a sensible substitute where one exists.
 * Drives role-aware, diet-aware routing.
 */
public class IngredientSpec {

    private String name;
    private String role;        // CORE | SUBSTITUTABLE | OPTIONAL
    private String substitute;  // suggested swap when SUBSTITUTABLE, else blank

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSubstitute() { return substitute; }
    public void setSubstitute(String substitute) { this.substitute = substitute; }
}
