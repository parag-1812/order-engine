package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.dto.KitchenStockResponse;
import com.cloudkitchen.order_engine.dto.KitchenSummaryResponse;
import com.cloudkitchen.order_engine.dto.UpdateKitchenStockRequest;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kitchens (cloud kitchens or restaurants — one generic partner type) and their
 * boolean stock checklist. This is the supply side of the marketplace.
 */
@RestController
@RequestMapping("/kitchens")
public class KitchenController {

    private final KitchenRepository kitchenRepository;
    private final CatalogService catalogService;

    public KitchenController(KitchenRepository kitchenRepository, CatalogService catalogService) {
        this.kitchenRepository = kitchenRepository;
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','KITCHEN','ADMIN')")
    public ResponseEntity<List<KitchenSummaryResponse>> listKitchens() {
        List<KitchenSummaryResponse> body = kitchenRepository.findAll().stream()
                .map(k -> new KitchenSummaryResponse(
                        k.getId(), k.getName(), k.isVegetarianOnly(),
                        k.getLatitude(), k.getLongitude(),
                        catalogService.inStockIds(k.getId()).size()))
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{kitchenId}/stock")
    @PreAuthorize("hasAnyRole('KITCHEN','ADMIN')")
    public ResponseEntity<KitchenStockResponse> getStock(@PathVariable Long kitchenId) {
        return ResponseEntity.ok(
                new KitchenStockResponse(kitchenId, catalogService.inStockIds(kitchenId)));
    }

    @PutMapping("/{kitchenId}/stock")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<KitchenStockResponse> updateStock(
            @PathVariable Long kitchenId,
            @RequestBody UpdateKitchenStockRequest request) {
        catalogService.replaceStock(kitchenId, request.getInStockIds());
        return ResponseEntity.ok(
                new KitchenStockResponse(kitchenId, catalogService.inStockIds(kitchenId)));
    }
}
