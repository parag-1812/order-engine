package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.dto.RegisterSupplierRequest;
import com.cloudkitchen.order_engine.dto.SupplierStockResponse;
import com.cloudkitchen.order_engine.dto.SupplierSummaryResponse;
import com.cloudkitchen.order_engine.dto.UpdateKitchenStockRequest;
import com.cloudkitchen.order_engine.repository.SupplierRepository;
import com.cloudkitchen.order_engine.supplier.SupplierEntity;
import com.cloudkitchen.order_engine.supplier.SupplierService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ingredient suppliers and their stock — the sourcing side of the marketplace. */
@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final SupplierService supplierService;

    public SupplierController(SupplierRepository supplierRepository, SupplierService supplierService) {
        this.supplierRepository = supplierRepository;
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('KITCHEN','SUPPLIER','ADMIN')")
    public ResponseEntity<List<SupplierSummaryResponse>> list() {
        return ResponseEntity.ok(supplierRepository.findAll().stream().map(this::toSummary).toList());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierSummaryResponse> mine(Authentication auth) {
        return supplierService.findOwnedBy(auth.getName())
                .map(s -> ResponseEntity.ok(toSummary(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierSummaryResponse> register(
            @RequestBody RegisterSupplierRequest request, Authentication auth) {
        return ResponseEntity.ok(toSummary(supplierService.register(auth.getName(), request)));
    }

    @GetMapping("/{supplierId}/stock")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ResponseEntity<SupplierStockResponse> getStock(@PathVariable Long supplierId) {
        return ResponseEntity.ok(new SupplierStockResponse(supplierId, supplierService.inStockIds(supplierId)));
    }

    @PutMapping("/{supplierId}/stock")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierStockResponse> updateStock(
            @PathVariable Long supplierId,
            @RequestBody UpdateKitchenStockRequest request,
            Authentication auth) {
        assertOwned(supplierId, auth);
        supplierService.replaceStock(supplierId, request.getInStockIds());
        return ResponseEntity.ok(new SupplierStockResponse(supplierId, supplierService.inStockIds(supplierId)));
    }

    private void assertOwned(Long supplierId, Authentication auth) {
        SupplierEntity supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        if (!auth.getName().equals(supplier.getOwnerUsername())) {
            throw new AccessDeniedException("You can only manage your own supplier");
        }
    }

    private SupplierSummaryResponse toSummary(SupplierEntity s) {
        return new SupplierSummaryResponse(
                s.getId(), s.getName(), s.getLatitude(), s.getLongitude(),
                supplierService.inStockIds(s.getId()).size());
    }
}
