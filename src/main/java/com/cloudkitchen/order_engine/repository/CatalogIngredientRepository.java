package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.catalog.CatalogIngredientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogIngredientRepository extends JpaRepository<CatalogIngredientEntity, Long> {
}
