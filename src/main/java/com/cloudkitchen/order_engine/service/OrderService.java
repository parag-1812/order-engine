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
            OrderRepository orderRepository
    ) {
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
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Ingredient not found: " + itemRequest.getIngredientId()
                            )
                    );

            if (!ingredient.isVegetarian()) {
                orderIsVegetarian = false;
            }

            OrderItem orderItem = new OrderItem(
                    ingredient.getId(),
                    itemRequest.getQuantity(),
                    ingredient.getPrice(),
                    ingredient.getPrepTimeInMinutes()
            );

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
                order.getTotalPrepTime()
        );

        order.getItems().forEach(domainItem -> {

            OrderItemEntity itemEntity = new OrderItemEntity(
                    domainItem.getIngredientId(),
                    domainItem.getQuantity(),
                    domainItem.getTotalPrice() / domainItem.getQuantity(),
                    domainItem.getTotalPrepTime() / domainItem.getQuantity(),
                    orderEntity
            );

            orderEntity.addItem(itemEntity);
        });

        orderRepository.save(orderEntity);

        return order;
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrderById(Long orderId) {

        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        List<OrderItemResponse> itemResponses = orderEntity.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getIngredientId(),
                        item.getQuantity(),
                        item.getPriceAtOrderTime(),
                        item.getPrepTimeAtOrderTime()
                ))
                .toList();

        return new OrderDetailsResponse(
                orderEntity.getId(),
                orderEntity.getCustomerId(),
                orderEntity.getKitchenId(),
                orderEntity.getStatus().name(),
                orderEntity.getTotalPrice(),
                orderEntity.getTotalPrepTime(),
                itemResponses
        );
    }

    @Transactional(readOnly = true)
    public List<OrderDetailsResponse> getOrdersByCustomer(Long customerId) {

        List<OrderEntity> orders = orderRepository.findByCustomerId(customerId);

        return orders.stream()
                .map(orderEntity -> {

                    List<OrderItemResponse> items = orderEntity.getItems()
                            .stream()
                            .map(item -> new OrderItemResponse(
                                    item.getIngredientId(),
                                    item.getQuantity(),
                                    item.getPriceAtOrderTime(),
                                    item.getPrepTimeAtOrderTime()
                            ))
                            .collect(java.util.stream.Collectors.toList());

                    return new OrderDetailsResponse(
                            orderEntity.getId(),
                            orderEntity.getCustomerId(),
                            orderEntity.getKitchenId(),
                            orderEntity.getStatus().name(),
                            orderEntity.getTotalPrice(),
                            orderEntity.getTotalPrepTime(),
                            items
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDetailsResponse> getOrdersByKitchen(Long kitchenId) {

        List<OrderEntity> orders = orderRepository.findByKitchenId(kitchenId);

        return orders.stream()
                .map(orderEntity -> {

                    List<OrderItemResponse> items = orderEntity.getItems()
                            .stream()
                            .map(item -> new OrderItemResponse(
                                    item.getIngredientId(),
                                    item.getQuantity(),
                                    item.getPriceAtOrderTime(),
                                    item.getPrepTimeAtOrderTime()
                            ))
                            .collect(java.util.stream.Collectors.toList());

                    return new OrderDetailsResponse(
                            orderEntity.getId(),
                            orderEntity.getCustomerId(),
                            orderEntity.getKitchenId(),
                            orderEntity.getStatus().name(),
                            orderEntity.getTotalPrice(),
                            orderEntity.getTotalPrepTime(),
                            items
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {

        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderStatus currentStatus = orderEntity.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
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

        List<InventoryEntity> inventoryList =
                inventoryRepository.findByKitchenId(kitchenId);

        for (OrderItemEntity item : orderEntity.getItems()) {

            InventoryEntity inventory = inventoryList.stream()
                    .filter(i -> i.getIngredientId().equals(item.getIngredientId()))
                    .findFirst()
                    .orElseThrow();

            inventory.increaseQuantity(item.getQuantity());
            inventoryRepository.save(inventory);
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

        List<InventoryEntity> inventoryList =
                inventoryRepository.findByKitchenId(kitchen.getId());

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

        List<InventoryEntity> inventoryList =
                inventoryRepository.findByKitchenId(kitchen.getId());

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
}
