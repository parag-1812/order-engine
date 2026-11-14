package com.cloudkitchen.order_engine.sourcing;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A request to source the ingredients a kitchen was missing for an order, routed
 * to the nearest supplier that stocks them. This turns the "to source" gap into
 * an actual fulfilment step with its own lifecycle.
 */
@Entity
@Table(name = "sourcing_requests")
public class SourcingRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long kitchenId;

    private Long supplierId;         // null if nothing could cover it

    @Column(length = 1000)
    private String itemsCsv;         // the missing items being sourced

    @Enumerated(EnumType.STRING)
    private SourcingStatus status;

    private double cost;             // what the supplier is paid for this request

    private Instant createdAt;

    protected SourcingRequestEntity() {
    }

    public SourcingRequestEntity(Long orderId, Long kitchenId, Long supplierId,
                                 String itemsCsv, SourcingStatus status, double cost, Instant createdAt) {
        this.orderId = orderId;
        this.kitchenId = kitchenId;
        this.supplierId = supplierId;
        this.itemsCsv = itemsCsv;
        this.status = status;
        this.cost = cost;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getKitchenId() { return kitchenId; }
    public Long getSupplierId() { return supplierId; }
    public String getItemsCsv() { return itemsCsv; }
    public SourcingStatus getStatus() { return status; }
    public void setStatus(SourcingStatus status) { this.status = status; }
    public double getCost() { return cost; }
    public Instant getCreatedAt() { return createdAt; }
}
