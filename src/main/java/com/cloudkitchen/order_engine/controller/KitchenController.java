package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.dto.KitchenStockResponse;
import com.cloudkitchen.order_engine.dto.KitchenSummaryResponse;
import com.cloudkitchen.order_engine.dto.RegisterKitchenRequest;
import com.cloudkitchen.order_engine.dto.UpdateKitchenStockRequest;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.service.KitchenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    private final KitchenService kitchenService;

    public KitchenController(KitchenRepository kitchenRepository,
                             CatalogService catalogService,
                             KitchenService kitchenService) {
        this.kitchenRepository = kitchenRepository;
        this.catalogService = catalogService;
        this.kitchenService = kitchenService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','KITCHEN','ADMIN')")
    public ResponseEntity<List<KitchenSummaryResponse>> listKitchens() {
        List<KitchenSummaryResponse> body = kitchenRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(body);
    }

    /** The kitchen the signed-in partner owns, or 404 if they haven't registered one. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<KitchenSummaryResponse> myKitchen(Authentication auth) {
        return kitchenService.findOwnedBy(auth.getName())
                .map(k -> ResponseEntity.ok(toSummary(k)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Register (onboard) a kitchen bound to the signed-in partner account. */
    @PostMapping
    @PreAuthorize("hasRole('KITCHEN')")
    public ResponseEntity<KitchenSummaryResponse> register(
            @RequestBody RegisterKitchenRequest request, Authentication auth) {
        KitchenEntity saved = kitchenService.register(auth.getName(), request);
        return ResponseEntity.ok(toSummary(saved));
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
            @RequestBody UpdateKitchenStockRequest request,
            Authentication auth) {
        assertOwned(kitchenId, auth);
        catalogService.replaceStock(kitchenId, request.getInStockIds());
        return ResponseEntity.ok(
                new KitchenStockResponse(kitchenId, catalogService.inStockIds(kitchenId)));
    }

    /** A partner may only edit the stock of a kitchen they own. */
    private void assertOwned(Long kitchenId, Authentication auth) {
        KitchenEntity kitchen = kitchenRepository.findById(kitchenId)
                .orElseThrow(() -> new IllegalArgumentException("Kitchen not found"));
        if (!auth.getName().equals(kitchen.getOwnerUsername())) {
            throw new AccessDeniedException("You can only manage your own kitchen");
        }
    }

    private KitchenSummaryResponse toSummary(KitchenEntity k) {
        return new KitchenSummaryResponse(
                k.getId(), k.getName(), k.isVegetarianOnly(),
                k.getLatitude(), k.getLongitude(),
                catalogService.inStockIds(k.getId()).size());
    }
}
