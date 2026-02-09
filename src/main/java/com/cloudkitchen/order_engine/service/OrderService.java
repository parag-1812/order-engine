package com.cloudkitchen.order_engine.service;

import com.cloudkitchen.order_engine.dto.CreateOrderRequest;
import com.cloudkitchen.order_engine.dto.CreateOrderItemRequest;
import com.cloudkitchen.order_engine.ingredient.Ingredient;
import com.cloudkitchen.order_engine.order.Order;
import com.cloudkitchen.order_engine.order.OrderItem;
import com.cloudkitchen.order_engine.order.OrderStatus;

import java.util.List;

public class OrderService {

    public Order createOrder(Long customerId, CreateOrderRequest request) {

        validateRequest(request);

        Order order = new Order(customerId);

        for (CreateOrderItemRequest itemRequest : request.getItems()) {

            Ingredient ingredient = fetchIngredient(itemRequest.getIngredientId());

            OrderItem orderItem = new OrderItem(
                    ingredient.getId(),
                    itemRequest.getQuantity(),
                    ingredient.getPrice(),
                    ingredient.getPrepTimeInMinutes()
            );

            order.addItem(orderItem);
        }

        order.changeStatus(OrderStatus.VALIDATED);

        return order;
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (CreateOrderItemRequest item : request.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero");
            }
        }
    }

    private Ingredient fetchIngredient(Long ingredientId) {
        return new Ingredient(
                ingredientId,
                "Dummy Ingredient",
                100.0,
                5,
                true
        );
    }
}
