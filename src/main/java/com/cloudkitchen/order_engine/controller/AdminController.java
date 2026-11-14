package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.customer.CustomerEntity;
import com.cloudkitchen.order_engine.dto.AdminCustomerRow;
import com.cloudkitchen.order_engine.dto.AdminKitchenRow;
import com.cloudkitchen.order_engine.dto.AdminSupplierRow;
import com.cloudkitchen.order_engine.dto.OrderDetailsResponse;
import com.cloudkitchen.order_engine.ingredient.IngredientEntity;
import com.cloudkitchen.order_engine.inventory.InventoryEntity;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.order.OrderEntity;
import com.cloudkitchen.order_engine.repository.CustomerRepository;
import com.cloudkitchen.order_engine.repository.IngredientRepository;
import com.cloudkitchen.order_engine.repository.InventoryRepository;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.repository.OrderRepository;
import com.cloudkitchen.order_engine.repository.SupplierRepository;
import com.cloudkitchen.order_engine.service.OrderService;
import com.cloudkitchen.order_engine.supplier.SupplierService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IngredientRepository ingredientRepository;
    private final KitchenRepository kitchenRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final CatalogService catalogService;
    private final SupplierService supplierService;

    public AdminController(
            IngredientRepository ingredientRepository,
            KitchenRepository kitchenRepository,
            InventoryRepository inventoryRepository, OrderRepository orderRepository, OrderService orderService,
            SupplierRepository supplierRepository, CustomerRepository customerRepository,
            CatalogService catalogService, SupplierService supplierService
    ) {
        this.ingredientRepository = ingredientRepository;
        this.kitchenRepository = kitchenRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.catalogService = catalogService;
        this.supplierService = supplierService;
    }

    // ===============================
    // Area-wise directories
    // ===============================
    @GetMapping("/kitchens/directory")
    public List<AdminKitchenRow> kitchenDirectory() {
        return kitchenRepository.findAll().stream()
                .map(k -> new AdminKitchenRow(
                        k.getId(), k.getName(), k.isVegetarianOnly(),
                        area(k.getLatitude(), k.getLongitude()),
                        k.getLatitude(), k.getLongitude(),
                        catalogService.inStockIds(k.getId()).size()))
                .sorted(Comparator.comparing(AdminKitchenRow::area).thenComparing(AdminKitchenRow::name))
                .toList();
    }

    @GetMapping("/suppliers/directory")
    public List<AdminSupplierRow> supplierDirectory() {
        return supplierRepository.findAll().stream()
                .map(s -> new AdminSupplierRow(
                        s.getId(), s.getName(),
                        area(s.getLatitude(), s.getLongitude()),
                        s.getLatitude(), s.getLongitude(),
                        supplierService.inStockIds(s.getId()).size()))
                .sorted(Comparator.comparing(AdminSupplierRow::area).thenComparing(AdminSupplierRow::name))
                .toList();
    }

    @GetMapping("/customers/directory")
    public List<AdminCustomerRow> customerDirectory() {
        return customerRepository.findAll().stream()
                .map(this::toCustomerRow)
                .sorted(Comparator.comparing(AdminCustomerRow::area).thenComparing(AdminCustomerRow::username))
                .toList();
    }

    private AdminCustomerRow toCustomerRow(CustomerEntity c) {
        List<OrderEntity> orders = orderRepository.findByCustomerId(c.getId());
        double spent = orders.stream().mapToDouble(OrderEntity::getTotalPrice).sum();
        String area = orders.stream()
                .filter(o -> o.getCustomerLat() != null && o.getCustomerLng() != null)
                .max(Comparator.comparing(OrderEntity::getId))
                .map(o -> area(o.getCustomerLat(), o.getCustomerLng()))
                .orElse("Unknown");
        return new AdminCustomerRow(c.getId(), c.getUsername(), area, orders.size(), spent);
    }

    /** Derive a coarse area label from coordinates (relative to the city centre). */
    private String area(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return "Unknown";
        }
        double cLat = 22.7196, cLng = 75.8577;
        double dLat = lat - cLat, dLng = lng - cLng;
        if (Math.sqrt(dLat * dLat + dLng * dLng) < 0.008) {
            return "Central";
        }
        return (dLat >= 0 ? "North" : "South") + "-" + (dLng >= 0 ? "East" : "West");
    }

    @PostMapping("/ingredients")
    public IngredientEntity createIngredient(@RequestBody IngredientEntity ingredient) {
        return ingredientRepository.save(ingredient);
    }

    @GetMapping("/ingredients")
    public List<IngredientEntity> getIngredients() {
        return ingredientRepository.findAll();
    }

    @DeleteMapping("/ingredients/{id}")
    public void deleteIngredient(@PathVariable Long id) {
        ingredientRepository.deleteById(id);
    }

    @PostMapping("/kitchens")
    public KitchenEntity createKitchen(@RequestBody KitchenEntity kitchen) {
        return kitchenRepository.save(kitchen);
    }

    @GetMapping("/kitchens")
    public List<KitchenEntity> getKitchens() {
        return kitchenRepository.findAll();
    }

    @PostMapping("/inventory")
    public InventoryEntity createInventory(@RequestBody InventoryEntity inventory) {
        return inventoryRepository.save(inventory);
    }

    @GetMapping("/inventory")
    public List<InventoryEntity> getInventory() {
        return inventoryRepository.findAll();
    }

    @GetMapping("/orders")
    public List<OrderDetailsResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {

        Map<String, Object> stats = new HashMap<>();
        List<OrderEntity> orders = orderRepository.findAll();

        double gmv = orders.stream().mapToDouble(OrderEntity::getTotalPrice).sum();
        double platformRevenue = orders.stream()
                .mapToDouble(o -> o.getCommissionAmount() == null ? 0 : o.getCommissionAmount()).sum();
        double kitchenPayouts = orders.stream()
                .mapToDouble(o -> o.getKitchenPayout() == null ? 0 : o.getKitchenPayout()).sum();

        stats.put("totalOrders", orders.size());
        stats.put("totalRevenue", gmv);          // GMV (kept key for backward compat)
        stats.put("gmv", gmv);
        stats.put("platformRevenue", platformRevenue);
        stats.put("kitchenPayouts", kitchenPayouts);

        return stats;
    }
}
