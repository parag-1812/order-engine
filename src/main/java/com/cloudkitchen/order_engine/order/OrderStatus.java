package com.cloudkitchen.order_engine.order;

public enum OrderStatus {
    CREATED,          // Order object created, nothing validated yet

    VALIDATED,        // Ingredients & quantities verified

    KITCHEN_ASSIGNED, // Kitchen selected and locked

    COOKING,          // Kitchen started preparing food

    READY,            // Food prepared, waiting for delivery/pickup

    DELIVERED,        // Order completed successfully

    CANCELLED         // Order cancelled at any stage (with rules)
}
