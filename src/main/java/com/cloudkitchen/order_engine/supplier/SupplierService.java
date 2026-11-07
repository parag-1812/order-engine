package com.cloudkitchen.order_engine.supplier;

import com.cloudkitchen.order_engine.dto.RegisterSupplierRequest;
import com.cloudkitchen.order_engine.repository.SupplierRepository;
import com.cloudkitchen.order_engine.repository.SupplierStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Supply-side onboarding + stock for ingredient suppliers (mirrors kitchens). */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierStockRepository stockRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           SupplierStockRepository stockRepository) {
        this.supplierRepository = supplierRepository;
        this.stockRepository = stockRepository;
    }

    public Optional<SupplierEntity> findOwnedBy(String username) {
        return supplierRepository.findByOwnerUsername(username);
    }

    @Transactional
    public SupplierEntity register(String username, RegisterSupplierRequest request) {
        if (supplierRepository.findByOwnerUsername(username).isPresent()) {
            throw new IllegalStateException("You have already registered a supplier");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required");
        }
        double lat = request.getLatitude() == null ? 0 : request.getLatitude();
        double lng = request.getLongitude() == null ? 0 : request.getLongitude();

        SupplierEntity saved = supplierRepository.save(
                new SupplierEntity(request.getName().trim(), lat, lng, username));

        // Wholesalers carry broad stock: start with the whole catalog (one bulk insert).
        stockRepository.stockEntireCatalog(saved.getId());
        return saved;
    }

    public List<Long> inStockIds(Long supplierId) {
        return stockRepository.findBySupplierIdAndInStockTrue(supplierId).stream()
                .map(SupplierStockEntity::getCatalogIngredientId)
                .toList();
    }

    /** Make a supplier's in-stock set match {@code ids}, touching only what changed. */
    @Transactional
    public void replaceStock(Long supplierId, List<Long> ids) {
        Set<Long> desired = new LinkedHashSet<>(ids == null ? List.of() : ids);
        List<SupplierStockEntity> existing = stockRepository.findBySupplierId(supplierId);
        Set<Long> existingIds = new LinkedHashSet<>();
        for (SupplierStockEntity row : existing) {
            existingIds.add(row.getCatalogIngredientId());
        }
        List<SupplierStockEntity> toDelete = existing.stream()
                .filter(row -> !desired.contains(row.getCatalogIngredientId()))
                .toList();
        if (!toDelete.isEmpty()) {
            stockRepository.deleteAll(toDelete);
        }
        List<SupplierStockEntity> toAdd = desired.stream()
                .filter(id -> !existingIds.contains(id))
                .map(id -> new SupplierStockEntity(supplierId, id, true))
                .toList();
        if (!toAdd.isEmpty()) {
            stockRepository.saveAll(toAdd);
        }
    }
}
