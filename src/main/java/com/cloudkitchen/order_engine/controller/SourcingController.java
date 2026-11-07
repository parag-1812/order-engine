package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.dto.SourcingRequestResponse;
import com.cloudkitchen.order_engine.repository.SupplierRepository;
import com.cloudkitchen.order_engine.sourcing.SourcingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Sourcing requests — the supplier's work queue and their lifecycle. */
@RestController
@RequestMapping("/sourcing")
public class SourcingController {

    private final SourcingService sourcingService;
    private final SupplierRepository supplierRepository;

    public SourcingController(SourcingService sourcingService, SupplierRepository supplierRepository) {
        this.sourcingService = sourcingService;
        this.supplierRepository = supplierRepository;
    }

    /** The signed-in supplier's incoming sourcing requests. */
    @GetMapping("/supplier")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<List<SourcingRequestResponse>> myQueue(Authentication auth) {
        Long supplierId = supplierRepository.findByOwnerUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Register a supplier first"))
                .getId();
        return ResponseEntity.ok(sourcingService.queueForSupplier(supplierId));
    }

    /** Advance one of the supplier's requests (REQUESTED → ACCEPTED → DISPATCHED → DELIVERED). */
    @PatchMapping("/{requestId}/advance")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SourcingRequestResponse> advance(
            @PathVariable Long requestId, Authentication auth) {
        return ResponseEntity.ok(sourcingService.advance(requestId, auth.getName()));
    }

    /** The sourcing request attached to an order (for customer/kitchen display). */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('USER','KITCHEN','ADMIN')")
    public ResponseEntity<SourcingRequestResponse> forOrder(@PathVariable Long orderId) {
        SourcingRequestResponse r = sourcingService.forOrder(orderId);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }
}
