package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.inventory.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    List<InventoryEntity> findByKitchenId(Long kitchenId);
}
