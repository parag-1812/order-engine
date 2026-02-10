package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenRepository extends JpaRepository<KitchenEntity, Long> {
}