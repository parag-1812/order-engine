package com.cloudkitchen.order_engine.service;

import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.dto.RegisterKitchenRequest;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Supply-side onboarding: a partner registers and manages their own kitchen. */
@Service
public class KitchenService {

    private final KitchenRepository kitchenRepository;
    private final CatalogService catalogService;

    public KitchenService(KitchenRepository kitchenRepository, CatalogService catalogService) {
        this.kitchenRepository = kitchenRepository;
        this.catalogService = catalogService;
    }

    public Optional<KitchenEntity> findOwnedBy(String username) {
        return kitchenRepository.findByOwnerUsername(username);
    }

    @Transactional
    public KitchenEntity register(String username, RegisterKitchenRequest request) {
        if (kitchenRepository.findByOwnerUsername(username).isPresent()) {
            throw new IllegalStateException("You have already registered a kitchen");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Kitchen name is required");
        }
        double lat = request.getLatitude() == null ? 0 : request.getLatitude();
        double lng = request.getLongitude() == null ? 0 : request.getLongitude();

        KitchenEntity saved = kitchenRepository.save(
                new KitchenEntity(request.getName().trim(), request.isVegetarianOnly(), lat, lng, username));

        // Start stocked with the whole catalog so the kitchen is immediately
        // competitive in routing; they can untick what they don't keep. Done as a
        // single bulk INSERT...SELECT so onboarding isn't 45 network round-trips.
        catalogService.stockAllCatalog(saved.getId());

        return saved;
    }
}
