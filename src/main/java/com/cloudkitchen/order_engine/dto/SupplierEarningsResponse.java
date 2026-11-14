package com.cloudkitchen.order_engine.dto;

/** A supplier's earnings: total/delivered sourcing requests and total payout. */
public record SupplierEarningsResponse(
        long requests,
        long delivered,
        double payout
) {
}
