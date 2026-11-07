package com.cloudkitchen.order_engine.sourcing;

/** Lifecycle of a request to source missing ingredients from a supplier. */
public enum SourcingStatus {
    REQUESTED,   // created, waiting for a supplier to accept
    ACCEPTED,    // supplier agreed to supply
    DISPATCHED,  // on the way to the kitchen
    DELIVERED,   // reached the kitchen
    UNFULFILLED  // no supplier could cover the items
}
