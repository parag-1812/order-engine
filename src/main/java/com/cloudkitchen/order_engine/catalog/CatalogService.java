package com.cloudkitchen.order_engine.catalog;

import com.cloudkitchen.order_engine.repository.CatalogIngredientRepository;
import com.cloudkitchen.order_engine.repository.KitchenStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The brain of the operational platform: it turns the AI's free-text ingredients
 * into canonical catalog ids, and scores how well a given kitchen can cover a
 * dish from what it stocks.
 */
@Service
public class CatalogService {

    private final CatalogIngredientRepository catalogRepository;
    private final KitchenStockRepository stockRepository;

    public CatalogService(CatalogIngredientRepository catalogRepository,
                          KitchenStockRepository stockRepository) {
        this.catalogRepository = catalogRepository;
        this.stockRepository = stockRepository;
    }

    /** Normalise a list of free-text ingredient/protein names onto catalog entries. */
    public MatchResult resolve(List<String> rawNames) {
        List<CatalogIngredientEntity> catalog = catalogRepository.findAll();

        List<CatalogIngredientEntity> matched = new ArrayList<>();
        Set<Long> matchedIds = new LinkedHashSet<>();
        List<String> unmatched = new ArrayList<>();

        if (rawNames != null) {
            for (String raw : rawNames) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String norm = normalise(raw);
                CatalogIngredientEntity hit = findBest(norm, catalog);
                if (hit == null) {
                    unmatched.add(raw.trim());
                } else if (matchedIds.add(hit.getId())) {
                    matched.add(hit);
                }
            }
        }
        return new MatchResult(matched, unmatched);
    }

    /**
     * Score how well a kitchen covers the required catalog ingredients:
     * coverage = (required items the kitchen stocks) / (required items), 0-100.
     */
    public Coverage coverageFor(Long kitchenId, List<CatalogIngredientEntity> required) {
        if (required == null || required.isEmpty()) {
            // Nothing concrete to match (e.g. AI returned no catalog ingredients) — treat as full.
            return new Coverage(100, List.of());
        }
        Set<Long> inStock = stockRepository.findByKitchenIdAndInStockTrue(kitchenId).stream()
                .map(KitchenStockEntity::getCatalogIngredientId)
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        int have = 0;
        for (CatalogIngredientEntity item : required) {
            if (inStock.contains(item.getId())) {
                have++;
            } else {
                missing.add(item.getName());
            }
        }
        int score = (int) Math.round((have * 100.0) / required.size());
        return new Coverage(score, missing);
    }

    // ---- Catalog + kitchen-stock management ----

    public List<CatalogIngredientEntity> getCatalog() {
        return catalogRepository.findAll();
    }

    public List<Long> inStockIds(Long kitchenId) {
        return stockRepository.findByKitchenIdAndInStockTrue(kitchenId).stream()
                .map(KitchenStockEntity::getCatalogIngredientId)
                .toList();
    }

    /** Replace a kitchen's in-stock set with exactly these catalog ingredient ids. */
    @Transactional
    public void replaceStock(Long kitchenId, List<Long> ids) {
        stockRepository.deleteAll(stockRepository.findByKitchenId(kitchenId));
        if (ids == null) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>(ids);
        List<KitchenStockEntity> rows = distinct.stream()
                .map(id -> new KitchenStockEntity(kitchenId, id, true))
                .toList();
        stockRepository.saveAll(rows);
    }

    private String normalise(String s) {
        return s.toLowerCase().trim().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private CatalogIngredientEntity findBest(String norm, List<CatalogIngredientEntity> catalog) {
        // 1) exact match on canonical name or any synonym.
        for (CatalogIngredientEntity c : catalog) {
            if (keysOf(c).contains(norm)) {
                return c;
            }
        }
        // 2) containment either direction (guard against trivially short keys).
        for (CatalogIngredientEntity c : catalog) {
            for (String key : keysOf(c)) {
                if (key.length() >= 3 && (norm.contains(key) || key.contains(norm))) {
                    return c;
                }
            }
        }
        return null;
    }

    private Set<String> keysOf(CatalogIngredientEntity c) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(normalise(c.getName()));
        if (c.getSynonyms() != null) {
            for (String syn : c.getSynonyms().split(",")) {
                String n = normalise(syn);
                if (!n.isBlank()) {
                    keys.add(n);
                }
            }
        }
        return keys;
    }

    /** Catalog ingredients a request resolved to, plus any raw names we couldn't place. */
    public record MatchResult(List<CatalogIngredientEntity> matched, List<String> unmatched) {
    }

    /** A kitchen's coverage of a dish: a 0-100 score and the items it's missing. */
    public record Coverage(int score, List<String> missing) {
    }
}
