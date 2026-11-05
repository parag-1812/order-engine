package com.cloudkitchen.order_engine.dto;

import java.util.List;

/**
 * A custom dish coming from the AI recipe service (the ParsedDish shape),
 * plus a quantity. order-engine prices it and routes it to a compatible kitchen.
 */
public class CreateCustomOrderRequest {

    private String dishType;      // VEG / NON_VEG
    private String protein;
    private List<String> ingredients;
    private String cookingStyle;
    private String spiceLevel;
    private String oilLevel;
    private List<String> dietaryTags;
    private String specialInstructions;
    private String summary;
    private int quantity = 1;
    private Double customerLat;
    private Double customerLng;

    public String getDishType() { return dishType; }
    public void setDishType(String dishType) { this.dishType = dishType; }

    public String getProtein() { return protein; }
    public void setProtein(String protein) { this.protein = protein; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public String getCookingStyle() { return cookingStyle; }
    public void setCookingStyle(String cookingStyle) { this.cookingStyle = cookingStyle; }

    public String getSpiceLevel() { return spiceLevel; }
    public void setSpiceLevel(String spiceLevel) { this.spiceLevel = spiceLevel; }

    public String getOilLevel() { return oilLevel; }
    public void setOilLevel(String oilLevel) { this.oilLevel = oilLevel; }

    public List<String> getDietaryTags() { return dietaryTags; }
    public void setDietaryTags(List<String> dietaryTags) { this.dietaryTags = dietaryTags; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Double getCustomerLat() { return customerLat; }
    public void setCustomerLat(Double customerLat) { this.customerLat = customerLat; }

    public Double getCustomerLng() { return customerLng; }
    public void setCustomerLng(Double customerLng) { this.customerLng = customerLng; }
}
