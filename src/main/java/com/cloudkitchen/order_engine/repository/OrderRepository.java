package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}