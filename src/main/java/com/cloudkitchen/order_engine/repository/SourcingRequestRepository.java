package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.sourcing.SourcingRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SourcingRequestRepository extends JpaRepository<SourcingRequestEntity, Long> {

    List<SourcingRequestEntity> findBySupplierId(Long supplierId);

    Optional<SourcingRequestEntity> findByOrderId(Long orderId);
}
