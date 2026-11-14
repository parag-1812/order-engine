package com.cloudkitchen.order_engine.sourcing;

import com.cloudkitchen.order_engine.catalog.CatalogIngredientEntity;
import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.dto.SourcingRequestResponse;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.repository.SourcingRequestRepository;
import com.cloudkitchen.order_engine.repository.SupplierRepository;
import com.cloudkitchen.order_engine.supplier.SupplierEntity;
import com.cloudkitchen.order_engine.supplier.SupplierService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Turns a kitchen's "to source" gap into an actual fulfilment step: pick the
 * nearest supplier that stocks the most of the missing items and raise a
 * sourcing request the supplier can accept and deliver.
 */
@Service
public class SourcingService {

    private final SourcingRequestRepository sourcingRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierService supplierService;
    private final KitchenRepository kitchenRepository;
    private final CatalogService catalogService;
    private final Clock clock = Clock.systemUTC();

    /** What a supplier is paid per sourced ingredient (Rs). */
    private static final double COST_PER_ITEM = 40.0;

    public SourcingService(SourcingRequestRepository sourcingRepository,
                           SupplierRepository supplierRepository,
                           SupplierService supplierService,
                           KitchenRepository kitchenRepository,
                           CatalogService catalogService) {
        this.sourcingRepository = sourcingRepository;
        this.supplierRepository = supplierRepository;
        this.supplierService = supplierService;
        this.kitchenRepository = kitchenRepository;
        this.catalogService = catalogService;
    }

    /**
     * Raise a sourcing request for the items a kitchen is missing. Routes to the
     * supplier covering the most of them, breaking ties by distance to the kitchen.
     */
    @Transactional
    public double createForOrder(Long orderId, Long kitchenId, List<String> missingItems) {
        if (missingItems == null || missingItems.isEmpty()) {
            return 0;
        }
        // Resolve the missing item names to catalog ids where possible.
        List<Long> neededIds = missingItems.stream()
                .map(catalogService::resolveOne)
                .filter(c -> c != null)
                .map(CatalogIngredientEntity::getId)
                .toList();

        KitchenEntity kitchen = kitchenRepository.findById(kitchenId).orElse(null);
        double kLat = kitchen == null ? 0 : kitchen.getLatitude();
        double kLng = kitchen == null ? 0 : kitchen.getLongitude();

        SupplierEntity best = null;
        int bestHave = 0;
        double bestDist = Double.MAX_VALUE;
        for (SupplierEntity s : supplierRepository.findAll()) {
            var stock = new java.util.HashSet<>(supplierService.inStockIds(s.getId()));
            int have = (int) neededIds.stream().filter(stock::contains).count();
            double dist = haversineKm(kLat, kLng, s.getLatitude(), s.getLongitude());
            if (have > bestHave || (have == bestHave && dist < bestDist)) {
                best = s;
                bestHave = have;
                bestDist = dist;
            }
        }

        String itemsCsv = String.join(", ", missingItems);
        boolean fulfilled = best != null && bestHave > 0;
        SourcingStatus status = fulfilled ? SourcingStatus.REQUESTED : SourcingStatus.UNFULFILLED;
        Long supplierId = fulfilled ? best.getId() : null;
        // The supplier is paid only for the items it can actually cover.
        double cost = fulfilled ? bestHave * COST_PER_ITEM : 0;

        sourcingRepository.save(new SourcingRequestEntity(
                orderId, kitchenId, supplierId, itemsCsv, status, cost, clock.instant()));
        return cost;
    }

    @Transactional(readOnly = true)
    public List<SourcingRequestResponse> queueForSupplier(Long supplierId) {
        return sourcingRepository.findBySupplierId(supplierId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SourcingRequestResponse forOrder(Long orderId) {
        return sourcingRepository.findByOrderId(orderId).map(this::toResponse).orElse(null);
    }

    /** A supplier advances one of its own requests through the lifecycle. */
    @Transactional
    public SourcingRequestResponse advance(Long requestId, String username) {
        SourcingRequestEntity req = sourcingRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Sourcing request not found"));
        SupplierEntity supplier = req.getSupplierId() == null ? null
                : supplierRepository.findById(req.getSupplierId()).orElse(null);
        if (supplier == null || !username.equals(supplier.getOwnerUsername())) {
            throw new AccessDeniedException("You can only manage your own sourcing requests");
        }
        req.setStatus(next(req.getStatus()));
        sourcingRepository.save(req);
        return toResponse(req);
    }

    private SourcingStatus next(SourcingStatus current) {
        return switch (current) {
            case REQUESTED -> SourcingStatus.ACCEPTED;
            case ACCEPTED -> SourcingStatus.DISPATCHED;
            case DISPATCHED -> SourcingStatus.DELIVERED;
            default -> throw new IllegalStateException("Request can't advance from " + current);
        };
    }

    private SourcingRequestResponse toResponse(SourcingRequestEntity r) {
        String kitchenName = r.getKitchenId() == null ? null
                : kitchenRepository.findById(r.getKitchenId()).map(KitchenEntity::getName).orElse(null);
        String supplierName = r.getSupplierId() == null ? null
                : supplierRepository.findById(r.getSupplierId()).map(SupplierEntity::getName).orElse(null);
        List<String> items = r.getItemsCsv() == null ? List.of()
                : java.util.Arrays.stream(r.getItemsCsv().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new SourcingRequestResponse(
                r.getId(), r.getOrderId(), r.getKitchenId(), kitchenName,
                r.getSupplierId(), supplierName, items, r.getStatus().name());
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
