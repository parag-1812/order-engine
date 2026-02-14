package com.cloudkitchen.order_engine.order;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

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


    public void addItem(OrderItemEntity item) {
        items.add(item);
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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

    public double getTotalPrice() {
        return totalPrice;
    }

    public int getTotalPrepTime() {
        return totalPrepTime;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }
}
