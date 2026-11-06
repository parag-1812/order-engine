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
     * Score how well a kitchen covers a dish:
     * coverage = (required catalog items the kitchen stocks) / (all required items),
     * where "all required" includes ingredients we couldn't place on the catalog —
     * those count honestly as things to source rather than being silently ignored.
     */
    public Coverage coverageFor(Long kitchenId, List<CatalogIngredientEntity> required, List<String> unlisted) {
        List<String> unmatched = unlisted == null ? List.of() : unlisted;
        int total = (required == null ? 0 : required.size()) + unmatched.size();
        if (total == 0) {
            // Nothing concrete to match (e.g. AI returned no ingredients) — treat as full.
            return new Coverage(100, List.of(), List.of());
        }
        Set<Long> inStock = stockRepository.findByKitchenIdAndInStockTrue(kitchenId).stream()
                .map(KitchenStockEntity::getCatalogIngredientId)
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        int have = 0;
        if (required != null) {
            for (CatalogIngredientEntity item : required) {
                if (inStock.contains(item.getId())) {
                    have++;
                } else {
                    missing.add(item.getName());
                }
            }
        }
        missing.addAll(unmatched); // not on the catalog → can't be confirmed in stock
        int score = (int) Math.round((have * 100.0) / total);
        return new Coverage(score, missing, List.of());
    }

    /** Resolve one free-text ingredient name onto the catalog. */
    public CatalogIngredientEntity resolveOne(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return findBest(normalise(name), catalogRepository.findAll());
    }

    /** Resolve AI ingredient specs (name + role + substitute) onto the catalog, once. */
    public List<ResolvedSpec> resolveSpecs(List<SpecInput> specs) {
        List<CatalogIngredientEntity> catalog = catalogRepository.findAll();
        List<ResolvedSpec> out = new ArrayList<>();
        if (specs == null) {
            return out;
        }
        for (SpecInput s : specs) {
            if (s == null || s.name() == null || s.name().isBlank()) {
                continue;
            }
            String role = s.role() == null ? "CORE" : s.role().trim().toUpperCase();
            CatalogIngredientEntity item = findBest(normalise(s.name()), catalog);
            CatalogIngredientEntity sub = (s.substitute() == null || s.substitute().isBlank())
                    ? null : findBest(normalise(s.substitute()), catalog);
            out.add(new ResolvedSpec(
                    s.name().trim(), role,
                    item == null ? null : item.getId(),
                    sub == null ? null : sub.getName(),
                    sub == null ? null : sub.getId()));
        }
        return out;
    }

    /**
     * Role-aware coverage. OPTIONAL items don't count (they can be skipped); a
     * missing SUBSTITUTABLE item still counts as covered if the kitchen stocks its
     * substitute (recording the swap). Everything else missing is "to source".
     */
    public Coverage coverageForSpecs(Long kitchenId, List<ResolvedSpec> specs) {
        List<ResolvedSpec> required = specs.stream()
                .filter(s -> !"OPTIONAL".equals(s.role()))
                .toList();
        if (required.isEmpty()) {
            return new Coverage(100, List.of(), List.of());
        }
        Set<Long> inStock = stockRepository.findByKitchenIdAndInStockTrue(kitchenId).stream()
                .map(KitchenStockEntity::getCatalogIngredientId)
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        List<String> substitutions = new ArrayList<>();
        int have = 0;
        for (ResolvedSpec s : required) {
            if (s.itemId() != null && inStock.contains(s.itemId())) {
                have++;
            } else if ("SUBSTITUTABLE".equals(s.role())
                    && s.substituteId() != null && inStock.contains(s.substituteId())) {
                have++;
                substitutions.add(s.substituteName() + " instead of " + s.name());
            } else {
                missing.add(s.name());
            }
        }
        int score = (int) Math.round((have * 100.0) / required.size());
        return new Coverage(score, missing, substitutions);
    }

    /** AI-provided ingredient classification before catalog resolution. */
    public record SpecInput(String name, String role, String substitute) {
    }

    // ---- Catalog + kitchen-stock management ----

    public List<CatalogIngredientEntity> getCatalog() {
        return catalogRepository.findAll();
    }

    /** Stock a (newly registered) kitchen with the full catalog in one statement. */
    @Transactional
    public void stockAllCatalog(Long kitchenId) {
        stockRepository.stockEntireCatalog(kitchenId);
    }

    public List<Long> inStockIds(Long kitchenId) {
        return stockRepository.findByKitchenIdAndInStockTrue(kitchenId).stream()
                .map(KitchenStockEntity::getCatalogIngredientId)
                .toList();
    }

    /**
     * Make a kitchen's in-stock set exactly match {@code ids}, touching only what
     * changed. A full wipe-and-reinsert meant dozens of statements per save against
     * a remote DB (slow enough to time the client out); diffing keeps a typical
     * toggle down to one or two writes.
     */
    @Transactional
    public void replaceStock(Long kitchenId, List<Long> ids) {
        Set<Long> desired = new LinkedHashSet<>(ids == null ? List.of() : ids);

        List<KitchenStockEntity> existing = stockRepository.findByKitchenId(kitchenId);
        Set<Long> existingIds = existing.stream()
                .map(KitchenStockEntity::getCatalogIngredientId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<KitchenStockEntity> toDelete = existing.stream()
                .filter(row -> !desired.contains(row.getCatalogIngredientId()))
                .toList();
        if (!toDelete.isEmpty()) {
            stockRepository.deleteAll(toDelete);
        }

        List<KitchenStockEntity> toAdd = desired.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> new KitchenStockEntity(kitchenId, id, true))
                .toList();
        if (!toAdd.isEmpty()) {
            stockRepository.saveAll(toAdd);
        }
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

    /**
     * A kitchen's coverage of a dish: a 0-100 score, the items it's missing (to
     * source), and any substitutions it would apply (e.g. "Tofu instead of Paneer").
     */
    public record Coverage(int score, List<String> missing, List<String> substitutions) {
    }

    /** One required ingredient resolved onto the catalog, with its role and substitute. */
    public record ResolvedSpec(
            String name, String role, Long itemId,
            String substituteName, Long substituteId) {
    }
}
