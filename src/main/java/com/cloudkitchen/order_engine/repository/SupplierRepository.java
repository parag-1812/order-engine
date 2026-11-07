package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.supplier.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Long> {

    Optional<SupplierEntity> findByOwnerUsername(String ownerUsername);
}
