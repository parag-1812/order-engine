package com.cloudkitchen.order_engine.order;

import java.util.EnumSet;
import java.util.Set;

public class OrderLifecycle {

    public static boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {

        switch (from) {
            case CREATED:
                return to == OrderStatus.VALIDATED
                        || to == OrderStatus.CANCELLED;

            case VALIDATED:
                return to == OrderStatus.KITCHEN_ASSIGNED
                        || to == OrderStatus.CANCELLED;

            case KITCHEN_ASSIGNED:
                return to == OrderStatus.COOKING
                        || to == OrderStatus.CANCELLED;

            case COOKING:
                return to == OrderStatus.READY
                        || to == OrderStatus.CANCELLED;

            case READY:
                return to == OrderStatus.DELIVERED;

            case DELIVERED:
                return false;

            case CANCELLED:
                return false;

            default:
                return false;
        }
    }
}
