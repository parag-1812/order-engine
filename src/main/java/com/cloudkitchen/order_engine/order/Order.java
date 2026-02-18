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

        if (this.status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Delivered order cannot be modified");
        }

        switch (this.status) {

            case VALIDATED -> {
                if (newStatus != OrderStatus.KITCHEN_ASSIGNED) {
                    throw new IllegalStateException("Invalid transition from VALIDATED");
                }
            }

            case KITCHEN_ASSIGNED -> {
                if (newStatus != OrderStatus.COOKING) {
                    throw new IllegalStateException("Invalid transition from KITCHEN_ASSIGNED");
                }
            }

            case COOKING -> {
                if (newStatus != OrderStatus.READY) {
                    throw new IllegalStateException("Invalid transition from COOKING");
                }
            }

            case READY -> {
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new IllegalStateException("Invalid transition from READY");
                }
            }

            default -> throw new IllegalStateException("Invalid state transition");
        }

        this.status = newStatus;
    }


    private boolean isValidTransition(OrderStatus current, OrderStatus next) {

        return switch (current) {
            case CREATED -> next == OrderStatus.VALIDATED;
            case VALIDATED -> next == OrderStatus.KITCHEN_ASSIGNED;
            case KITCHEN_ASSIGNED -> next == OrderStatus.COOKING;
            case COOKING -> next == OrderStatus.READY;
            case READY -> next == OrderStatus.DELIVERED;
            default -> false;
        };
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