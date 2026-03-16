package com.cloudkitchen.order_engine.controller;

import com.cloudkitchen.order_engine.dto.OrderDetailsResponse;
import com.cloudkitchen.order_engine.ingredient.IngredientEntity;
import com.cloudkitchen.order_engine.inventory.InventoryEntity;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.order.OrderEntity;
import com.cloudkitchen.order_engine.repository.IngredientRepository;
import com.cloudkitchen.order_engine.repository.InventoryRepository;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.repository.OrderRepository;
import com.cloudkitchen.order_engine.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    public AdminController(
            IngredientRepository ingredientRepository,
            KitchenRepository kitchenRepository,
            InventoryRepository inventoryRepository, OrderRepository orderRepository , OrderService orderService
    ) {
        this.ingredientRepository = ingredientRepository;
        this.kitchenRepository = kitchenRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
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

        stats.put("totalOrders", orderRepository.count());
        stats.put("totalRevenue",
                orderRepository.findAll()
                        .stream()
                        .mapToDouble(OrderEntity::getTotalPrice)
                        .sum()
        );

        return stats;
    }
}
