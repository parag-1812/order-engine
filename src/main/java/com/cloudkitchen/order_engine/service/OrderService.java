package com.cloudkitchen.order_engine.service;

import com.cloudkitchen.order_engine.catalog.CatalogIngredientEntity;
import com.cloudkitchen.order_engine.catalog.CatalogService;
import com.cloudkitchen.order_engine.dto.*;
import com.cloudkitchen.order_engine.ingredient.IngredientEntity;
import com.cloudkitchen.order_engine.inventory.InventoryEntity;
import com.cloudkitchen.order_engine.kitchen.KitchenEntity;
import com.cloudkitchen.order_engine.order.*;
import com.cloudkitchen.order_engine.repository.IngredientRepository;
import com.cloudkitchen.order_engine.repository.InventoryRepository;
import com.cloudkitchen.order_engine.repository.KitchenRepository;
import com.cloudkitchen.order_engine.repository.OrderRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderService {

    private final IngredientRepository ingredientRepository;
    private final KitchenRepository kitchenRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final CatalogService catalogService;

    public OrderService(
            IngredientRepository ingredientRepository,
            KitchenRepository kitchenRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            CatalogService catalogService) {
        this.ingredientRepository = ingredientRepository;
        this.kitchenRepository = kitchenRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
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

        // What does this dish actually require? Resolve the AI's free-text
        // ingredients (plus the protein) onto the shared canonical catalog.
        List<String> rawRequired = new ArrayList<>();
        if (request.getIngredients() != null) {
            rawRequired.addAll(request.getIngredients());
        }
        if (request.getProtein() != null && !request.getProtein().isBlank()) {
            rawRequired.add(request.getProtein());
        }
        List<CatalogIngredientEntity> required = catalogService.resolve(rawRequired).matched();

        boolean hasLocation = request.getCustomerLat() != null && request.getCustomerLng() != null;
        double lat = hasLocation ? request.getCustomerLat() : 0;
        double lng = hasLocation ? request.getCustomerLng() : 0;

        // Rank kitchens by how much of the dish they can cover, then by distance.
        // Best coverage wins; a tie breaks toward the nearest kitchen.
        KitchenEntity kitchen = null;
        CatalogService.Coverage bestCoverage = null;
        double bestDistance = Double.MAX_VALUE;
        for (KitchenEntity k : compatible) {
            CatalogService.Coverage cov = catalogService.coverageFor(k.getId(), required);
            double dist = hasLocation
                    ? haversineKm(lat, lng, k.getLatitude(), k.getLongitude())
                    : 0;
            boolean better = bestCoverage == null
                    || cov.score() > bestCoverage.score()
                    || (cov.score() == bestCoverage.score() && dist < bestDistance);
            if (better) {
                kitchen = k;
                bestCoverage = cov;
                bestDistance = dist;
            }
        }

        Double distanceKm = hasLocation
                ? Math.round(haversineKm(lat, lng, kitchen.getLatitude(), kitchen.getLongitude()) * 10.0) / 10.0
                : null;

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
        orderEntity.setCustomerLat(request.getCustomerLat());
        orderEntity.setCustomerLng(request.getCustomerLng());
        orderEntity.setMatchScore(bestCoverage.score());
        orderEntity.setMissingItemsCsv(
                bestCoverage.missing().isEmpty() ? null : String.join(", ", bestCoverage.missing()));

        orderRepository.save(orderEntity);

        return mapToDetailsResponse(orderEntity);
    }

    /** Throws 403 if the order doesn't belong to this customer. */
    @Transactional(readOnly = true)
    public void assertOwnedBy(Long orderId, Long customerId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getCustomerId() == null || !order.getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You can only access your own orders");
        }
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
    public OrderDetailsResponse rateOrder(Long orderId, int rating, String review) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("You can only rate an order once it's delivered");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        order.setRating(rating);
        order.setReview(review);
        orderRepository.save(order);
        return mapToDetailsResponse(order);
    }

    @Transactional
    public OrderDetailsResponse setFavorite(Long orderId, boolean favorite) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setFavorite(favorite);
        orderRepository.save(order);
        return mapToDetailsResponse(order);
    }

    @Transactional
    public OrderDetailsResponse reorder(Long orderId, Long customerId) {
        OrderEntity src = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (src.getDishType() == null) {
            throw new IllegalStateException("Only AI dishes can be reordered");
        }
        CreateCustomOrderRequest req = new CreateCustomOrderRequest();
        req.setDishType(src.getDishType());
        req.setProtein(src.getProtein());
        req.setIngredients(src.getIngredientsCsv() == null ? null
                : Arrays.stream(src.getIngredientsCsv().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList());
        req.setCookingStyle(src.getCookingStyle());
        req.setSpiceLevel(src.getSpiceLevel());
        req.setOilLevel(src.getOilLevel());
        req.setSpecialInstructions(src.getSpecialInstructions());
        req.setSummary(src.getDishSummary());
        req.setQuantity(src.getQuantity() == null ? 1 : src.getQuantity());
        req.setCustomerLat(src.getCustomerLat());
        req.setCustomerLng(src.getCustomerLng());
        return createCustomOrder(customerId, req);
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

        List<String> missingItems = orderEntity.getMissingItemsCsv() == null ? null
                : Arrays.stream(orderEntity.getMissingItemsCsv().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

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
                orderEntity.getPaymentStatus(),
                orderEntity.getRating(),
                orderEntity.getReview(),
                orderEntity.isFavorite(),
                orderEntity.getMatchScore(),
                missingItems);
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
