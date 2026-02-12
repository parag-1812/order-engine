package com.cloudkitchen.order_engine.order;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ingredientId;

    private int quantity;

    private double priceAtOrderTime;

    private int prepTimeAtOrderTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    protected OrderItemEntity() {

    }

    public OrderItemEntity(
            Long ingredientId,
            int quantity,
            double priceAtOrderTime,
            int prepTimeAtOrderTime,
            OrderEntity order
    ) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.priceAtOrderTime = priceAtOrderTime;
        this.prepTimeAtOrderTime = prepTimeAtOrderTime;
        this.order = order;
    }
}
