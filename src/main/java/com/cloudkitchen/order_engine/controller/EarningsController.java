package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.dto.KitchenEarningsResponse;
import com.cloudkitchen.order_engine.dto.SupplierEarningsResponse;
import com.cloudkitchen.order_engine.order.OrderEntity;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.repository.OrderRepository;
import com.cloudkitchen.order_engine.repository.SourcingRequestRepository;
import com.cloudkitchen.order_engine.repository.SupplierRepository;
import com.cloudkitchen.order_engine.sourcing.SourcingRequestEntity;
import com.cloudkitchen.order_engine.sourcing.SourcingStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Earnings views for the two supply-side partner types. */
@RestController
@RequestMapping("/earnings")
public class EarningsController {

    private final KitchenRepository kitchenRepository;
    private final SupplierRepository supplierRepository;
    private final OrderRepository orderRepository;
    private final SourcingRequestRepository sourcingRepository;

    public EarningsController(KitchenRepository kitchenRepository,
                              SupplierRepository supplierRepository,
                              OrderRepository orderRepository,
                              SourcingRequestRepository sourcingRepository) {
        this.kitchenRepository = kitchenRepository;
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
        this.sourcingRepository = sourcingRepository;
    }

    @GetMapping("/kitchen")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<KitchenEarningsResponse> kitchenEarnings(Authentication auth) {
        Long kitchenId = kitchenRepository.findByOwnerUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Register a kitchen first"))
                .getId();
        List<OrderEntity> orders = orderRepository.findByKitchenId(kitchenId);
        double gross = orders.stream().mapToDouble(OrderEntity::getTotalPrice).sum();
        double commission = orders.stream()
                .mapToDouble(o -> o.getCommissionAmount() == null ? 0 : o.getCommissionAmount()).sum();
        double payout = orders.stream()
                .mapToDouble(o -> o.getKitchenPayout() == null ? 0 : o.getKitchenPayout()).sum();
        return ResponseEntity.ok(new KitchenEarningsResponse(orders.size(), gross, commission, payout));
    }

    @GetMapping("/supplier")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierEarningsResponse> supplierEarnings(Authentication auth) {
        Long supplierId = supplierRepository.findByOwnerUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Register a supplier first"))
                .getId();
        List<SourcingRequestEntity> reqs = sourcingRepository.findBySupplierId(supplierId);
        long delivered = reqs.stream().filter(r -> r.getStatus() == SourcingStatus.DELIVERED).count();
        double payout = reqs.stream().mapToDouble(SourcingRequestEntity::getCost).sum();
        return ResponseEntity.ok(new SupplierEarningsResponse(reqs.size(), delivered, payout));
    }
}
