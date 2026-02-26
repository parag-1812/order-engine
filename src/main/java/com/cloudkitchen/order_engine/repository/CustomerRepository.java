package com.cloudkitchen.order_engine.repository;

import com.cloudkitchen.order_engine.customer.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByUsername(String username);
}