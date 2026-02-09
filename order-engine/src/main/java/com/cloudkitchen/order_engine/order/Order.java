package com.cloudkitchen.order_engine.order;

import java.util.ArrayList;
import java.util.List;

public class Order {

    private Long orderId;
    private Long customerId;
    private Long kitchenId;

    private OrderStatus status;

    private List<OrderItem> items = new ArrayList<>();

    private double totalPrice;
    private int totalPrepTime;

    public Order(Long customerId) {
        this.customerId = customerId;
        this.status = OrderStatus.CREATED;
    }


    public void addItem(OrderItem item) {
        this.items.add(item);
        recalculateTotals();
    }

    public void changeStatus(OrderStatus newStatus) {

        if (!OrderLifecycle.isTransitionAllowed(this.status, newStatus)) {
            throw new IllegalStateException(
                    "Invalid order status transition from "
                            + this.status + " to " + newStatus
            );
        }

        this.status = newStatus;
    }

    public void assignKitchen(Long kitchenId) {
        this.kitchenId = kitchenId;
        changeStatus(OrderStatus.KITCHEN_ASSIGNED);
    }

    private void recalculateTotals() {
        this.totalPrice = items.stream()
                .mapToDouble(OrderItem::getTotalPrice)
                .sum();

        this.totalPrepTime = items.stream()
                .mapToInt(OrderItem::getTotalPrepTime)
                .sum();
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

    public List<OrderItem> getItems() {
        return items;
    }
}