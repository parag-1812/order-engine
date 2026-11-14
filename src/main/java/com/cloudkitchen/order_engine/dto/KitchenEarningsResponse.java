package com.cloudkitchen.order_engine.dto;

/** A kitchen's earnings: gross order value, platform commission, and net payout. */
public record KitchenEarningsResponse(
        long orders,
        double gross,
        double commission,
        double payout
) {
}
