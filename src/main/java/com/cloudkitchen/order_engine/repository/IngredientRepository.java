package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.ingredient.IngredientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {
}
