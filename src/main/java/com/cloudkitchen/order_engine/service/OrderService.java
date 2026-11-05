package com.cloudkitchen.order_engine.service;

import com.cloudkitchen.order_engine.dto.*;
import com.cloudkitchen.order_engine.ingredient.IngredientEntity;
import com.cloudkitchen.order_engine.inventory.InventoryEntity;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.order.*;
import com.cloudkitchen.order_engine.repository.IngredientRepository;
import com.cloudkitchen.order_engine.repository.InventoryRepository;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderService {

    private final IngredientRepository ingredientRepository;
    private final KitchenRepository kitchenRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public OrderService(
            IngredientRepository ingredientRepository,
            KitchenRepository kitchenRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository) {
        this.ingredientRepository = ingredientRepository;
        this.kitchenRepository = kitchenRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(Long customerId, CreateOrderRequest request) {

        validateRequest(request);

        Order order = new Order(customerId);
        boolean orderIsVegetarian = true;

        for (CreateOrderItemRequest itemRequest : request.getItems()) {

            IngredientEntity ingredient = ingredientRepository
                    .findById(itemRequest.getIngredientId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Ingredient not found: " + itemRequest.getIngredientId()));

            if (!ingredient.isVegetarian()) {
                orderIsVegetarian = false;
            }

            OrderItem orderItem = new OrderItem(
                    ingredient.getId(),
                    itemRequest.getQuantity(),
                    ingredient.getPrice(),
                    ingredient.getPrepTimeInMinutes());

            order.addItem(orderItem);
        }

        order.changeStatus(OrderStatus.VALIDATED);

        KitchenEntity assignedKitchen = assignKitchen(order, orderIsVegetarian);
        order.assignKitchen(assignedKitchen.getId());

        OrderEntity orderEntity = new OrderEntity(
                customerId,
                order.getKitchenId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getTotalPrepTime());

        order.getItems().forEach(domainItem -> {

            OrderItemEntity itemEntity = new OrderItemEntity(
                    domainItem.getIngredientId(),
                    domainItem.getQuantity(),
                    domainItem.getTotalPrice() / domainItem.getQuantity(),
                    domainItem.getTotalPrepTime() / domainItem.getQuantity(),
                    orderEntity);

            orderEntity.addItem(itemEntity);
        });

        orderRepository.save(orderEntity);

        return order;
    }

    /**
     * Create an order from a custom AI-parsed dish. The dish is priced from a
     * formula (ingredient + cooking + packaging + platform fee + delivery) and
     * routed to the first kitchen compatible with its veg/non-veg type.
     */
    @Transactional
    public OrderDetailsResponse createCustomOrder(Long customerId, CreateCustomOrderRequest request) {

        int quantity = request.getQuantity() <= 0 ? 1 : request.getQuantity();
        boolean isVegetarian = !"NON_VEG".equalsIgnoreCase(request.getDishType());

        List<KitchenEntity> compatible = kitchenRepository.findAll().stream()
                .filter(k -> isKitchenCompatible(k, isVegetarian))
                .toList();
        if (compatible.isEmpty()) {
            throw new IllegalStateException(
                    "No kitchen available for a " + (isVegetarian ? "veg" : "non-veg") + " dish");
        }

        // Route to the nearest compatible kitchen when the customer shared a location.
        KitchenEntity kitchen;
        Double distanceKm = null;
        if (request.getCustomerLat() != null && request.getCustomerLng() != null) {
            double lat = request.getCustomerLat();
            double lng = request.getCustomerLng();
            kitchen = compatible.stream()
                    .min(Comparator.comparingDouble(
                            k -> haversineKm(lat, lng, k.getLatitude(), k.getLongitude())))
                    .orElseThrow();
            distanceKm = Math.round(
                    haversineKm(lat, lng, kitchen.getLatitude(), kitchen.getLongitude()) * 10.0) / 10.0;
        } else {
            kitchen = compatible.get(0);
        }

        int ingredientCount = request.getIngredients() == null ? 0 : request.getIngredients().size();
        double totalPrice = computePrice(request.getProtein(), ingredientCount, request.getCookingStyle()) * quantity;
        int totalPrepTime = computePrepTime(request.getProtein(), ingredientCount, request.getCookingStyle(), quantity);

        OrderEntity orderEntity = new OrderEntity(
                customerId,
                kitchen.getId(),
                OrderStatus.KITCHEN_ASSIGNED,
                totalPrice,
                totalPrepTime);

        orderEntity.setDishType(isVegetarian ? "VEG" : "NON_VEG");
        orderEntity.setProtein(request.getProtein());
        orderEntity.setIngredientsCsv(
                request.getIngredients() == null ? null : String.join(", ", request.getIngredients()));
        orderEntity.setCookingStyle(request.getCookingStyle());
        orderEntity.setSpiceLevel(request.getSpiceLevel());
        orderEntity.setOilLevel(request.getOilLevel());
        orderEntity.setSpecialInstructions(request.getSpecialInstructions());
        orderEntity.setDishSummary(request.getSummary());
        orderEntity.setQuantity(quantity);
        orderEntity.setDistanceKm(distanceKm);

        orderRepository.save(orderEntity);

        return mapToDetailsResponse(orderEntity);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderById(Long orderId) {
        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return mapToDetailsResponse(orderEntity);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailsResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDetailsResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDetailsResponse> getOrdersByKitchen(Long kitchenId) {
        return orderRepository.findByKitchenId(kitchenId).stream()
                .map(this::mapToDetailsResponse)
                .toList();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {

        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderStatus currentStatus = orderEntity.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        orderEntity.setStatus(newStatus);
        orderRepository.save(orderEntity);
    }

    @Transactional
    public void cancelOrder(Long orderId) {

        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderStatus currentStatus = orderEntity.getStatus();

        if (currentStatus == OrderStatus.COOKING ||
                currentStatus == OrderStatus.READY ||
                currentStatus == OrderStatus.DELIVERED) {

            throw new IllegalStateException("Order cannot be cancelled at this stage");
        }

        Long kitchenId = orderEntity.getKitchenId();

        List<InventoryEntity> inventoryList = inventoryRepository.findByKitchenId(kitchenId);

        for (OrderItemEntity item : orderEntity.getItems()) {

            InventoryEntity inventory = inventoryList.stream()
                    .filter(i -> i.getIngredientId().equals(item.getIngredientId()))
                    .findFirst()
                    .orElse(null);

            if (inventory != null) {
                inventory.increaseQuantity(item.getQuantity());
                inventoryRepository.save(inventory);
            }
        }

        orderEntity.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(orderEntity);
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {

        return switch (current) {
            case KITCHEN_ASSIGNED -> next == OrderStatus.COOKING;
            case COOKING -> next == OrderStatus.READY;
            case READY -> next == OrderStatus.DELIVERED;
            default -> false;
        };
    }

    private KitchenEntity assignKitchen(Order order, boolean orderIsVegetarian) {

        List<KitchenEntity> kitchens = kitchenRepository.findAll();

        for (KitchenEntity kitchen : kitchens) {

            if (!isKitchenCompatible(kitchen, orderIsVegetarian)) {
                continue;
            }

            if (hasSufficientInventory(kitchen, order)) {
                reserveInventory(kitchen, order);
                return kitchen;
            }
        }

        throw new IllegalStateException("No suitable kitchen found for this order");
    }

    private boolean isKitchenCompatible(KitchenEntity kitchen, boolean orderIsVegetarian) {
        if (orderIsVegetarian) {
            return true;
        }
        return !kitchen.isVegetarianOnly();
    }

    private boolean hasSufficientInventory(KitchenEntity kitchen, Order order) {

        List<InventoryEntity> inventoryList = inventoryRepository.findByKitchenId(kitchen.getId());

        for (OrderItem item : order.getItems()) {

            InventoryEntity inventory = inventoryList.stream()
                    .filter(i -> i.getIngredientId().equals(item.getIngredientId()))
                    .findFirst()
                    .orElse(null);

            if (inventory == null || inventory.getAvailableQuantity() < item.getQuantity()) {
                return false;
            }
        }

        return true;
    }

    private void reserveInventory(KitchenEntity kitchen, Order order) {

        List<InventoryEntity> inventoryList = inventoryRepository.findByKitchenId(kitchen.getId());

        for (OrderItem item : order.getItems()) {

            InventoryEntity inventory = inventoryList.stream()
                    .filter(i -> i.getIngredientId().equals(item.getIngredientId()))
                    .findFirst()
                    .orElseThrow();

            inventory.reduceQuantity(item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        request.getItems().forEach(item -> {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
        });
    }

    // ---- Pricing for custom dishes (all amounts in Rs.) ----

    private double computePrice(String protein, int ingredientCount, String cookingStyle) {
        double base = 120;
        double proteinCost = proteinCost(protein);
        double ingredientsCost = ingredientCount * 15.0;
        double cookingCost = cookingCost(cookingStyle);
        double subtotal = base + proteinCost + ingredientsCost + cookingCost;

        double packaging = 20;
        double platformFee = Math.round(subtotal * 0.05);
        double delivery = 30;

        return subtotal + packaging + platformFee + delivery;
    }

    private double proteinCost(String protein) {
        if (protein == null || protein.isBlank()) return 0;
        return switch (protein.trim().toLowerCase()) {
            case "chicken", "mutton", "fish", "prawn" -> 90;
            case "paneer" -> 60;
            case "tofu", "soya" -> 50;
            case "egg" -> 40;
            default -> 0;
        };
    }

    private double cookingCost(String cookingStyle) {
        if (cookingStyle == null) return 20;
        return switch (cookingStyle.trim().toLowerCase()) {
            case "tandoor", "grilled", "smoked" -> 40;
            case "baked", "roasted" -> 30;
            case "fried" -> 25;
            case "curry" -> 20;
            default -> 15;
        };
    }

    private int computePrepTime(String protein, int ingredientCount, String cookingStyle, int quantity) {
        int base = 15;
        int ingredients = ingredientCount * 2;
        int style = switch (cookingStyle == null ? "" : cookingStyle.trim().toLowerCase()) {
            case "tandoor", "grilled" -> 15;
            case "smoked" -> 18;
            case "baked", "roasted" -> 12;
            case "curry", "steamed" -> 10;
            case "fried" -> 8;
            case "dry" -> 6;
            default -> 10;
        };
        int proteinTime = proteinCost(protein) > 0 && !"paneer".equalsIgnoreCase(protein == null ? "" : protein.trim()) ? 5 : 0;
        return base + ingredients + style + proteinTime + (quantity - 1) * 2;
    }

    // ---- Response mapping (single source of truth, includes custom dish fields) ----

    private OrderDetailsResponse mapToDetailsResponse(OrderEntity orderEntity) {

        List<OrderItemResponse> itemResponses = orderEntity.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getIngredientId(),
                        item.getQuantity(),
                        item.getPriceAtOrderTime(),
                        item.getPrepTimeAtOrderTime()))
                .toList();

        List<String> ingredients = orderEntity.getIngredientsCsv() == null ? null
                : Arrays.stream(orderEntity.getIngredientsCsv().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        String kitchenName = orderEntity.getKitchenId() == null ? null
                : kitchenRepository.findById(orderEntity.getKitchenId())
                        .map(KitchenEntity::getName)
                        .orElse(null);

        return new OrderDetailsResponse(
                orderEntity.getId(),
                orderEntity.getCustomerId(),
                orderEntity.getKitchenId(),
                orderEntity.getStatus().name(),
                orderEntity.getTotalPrice(),
                orderEntity.getTotalPrepTime(),
                itemResponses,
                orderEntity.getDishType(),
                orderEntity.getProtein(),
                ingredients,
                orderEntity.getCookingStyle(),
                orderEntity.getSpiceLevel(),
                orderEntity.getOilLevel(),
                orderEntity.getSpecialInstructions(),
                orderEntity.getDishSummary(),
                orderEntity.getQuantity(),
                kitchenName,
                orderEntity.getDistanceKm(),
                orderEntity.getPaymentStatus());
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

    @Transactional(readOnly = true)
    public List<OrderDetailsResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToDetailsResponse)
                .toList();
    }

    public double getTotalRevenue() {
        return orderRepository.findAll()
                .stream()
                .mapToDouble(OrderEntity::getTotalPrice)
                .sum();
    }

    public long getOrderCount() {
        return orderRepository.count();
    }
}
