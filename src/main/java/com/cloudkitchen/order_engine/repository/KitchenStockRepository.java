package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.catalog.KitchenStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KitchenStockRepository extends JpaRepository<KitchenStockEntity, Long> {

    List<KitchenStockEntity> findByKitchenId(Long kitchenId);

    List<KitchenStockEntity> findByKitchenIdAndInStockTrue(Long kitchenId);
}
