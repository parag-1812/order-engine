package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.catalog.KitchenStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KitchenStockRepository extends JpaRepository<KitchenStockEntity, Long> {

    List<KitchenStockEntity> findByKitchenId(Long kitchenId);

    List<KitchenStockEntity> findByKitchenIdAndInStockTrue(Long kitchenId);

    /** Stock a kitchen with the entire catalog in a single statement (fast onboarding). */
    @Modifying
    @Query(value = "INSERT INTO kitchen_stock (kitchen_id, catalog_ingredient_id, in_stock) "
            + "SELECT :kitchenId, id, true FROM catalog_ingredients", nativeQuery = true)
    void stockEntireCatalog(@Param("kitchenId") Long kitchenId);
}
