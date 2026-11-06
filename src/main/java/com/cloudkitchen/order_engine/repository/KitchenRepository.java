package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KitchenRepository extends JpaRepository<KitchenEntity, Long> {

    Optional<KitchenEntity> findByOwnerUsername(String ownerUsername);
}