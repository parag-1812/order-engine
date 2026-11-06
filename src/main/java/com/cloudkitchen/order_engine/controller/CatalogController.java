package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.dto.CatalogIngredientResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The shared master ingredient catalog — the vocabulary the AI maps requests
 * onto and kitchens declare their stock against.
 */
@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/ingredients")
    @PreAuthorize("hasAnyRole('USER','KITCHEN','ADMIN')")
    public ResponseEntity<List<CatalogIngredientResponse>> listIngredients() {
        List<CatalogIngredientResponse> body = catalogService.getCatalog().stream()
                .map(c -> new CatalogIngredientResponse(
                        c.getId(), c.getName(), c.getCategory(),
                        c.isVegan(), c.isContainsDairy(), c.isContainsGluten(),
                        c.isContainsNuts(), c.isHighProtein()))
                .toList();
        return ResponseEntity.ok(body);
    }
}
