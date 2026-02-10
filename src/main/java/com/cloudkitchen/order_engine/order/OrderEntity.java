package com.cloudkitchen.order_engine.order;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    private Long kitchenId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private double totalPrice;

    private int totalPrepTime;

    protected OrderEntity() {

    }

    public OrderEntity(
            Long customerId,
            Long kitchenId,
            OrderStatus status,
            double totalPrice,
            int totalPrepTime
    ) {
        this.customerId = customerId;
        this.kitchenId = kitchenId;
        this.status = status;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getKitchenId() {
        return kitchenId;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
