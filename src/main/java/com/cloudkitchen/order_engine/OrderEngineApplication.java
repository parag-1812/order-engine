package com.cloudkitchen.order_engine;

import com.cloudkitchen.order_engine.dto.CreateOrderItemRequest;
import com.cloudkitchen.order_engine.dto.CreateOrderRequest;
import com.cloudkitchen.order_engine.order.Order;
import com.cloudkitchen.order_engine.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class OrderEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderEngineApplication.class, args);
	}

	@Bean
	CommandLineRunner testOrderCreation(OrderService orderService) {
		return args -> {

			CreateOrderItemRequest item1 = new CreateOrderItemRequest();
			item1.setIngredientId(1L);
			item1.setQuantity(2);

			CreateOrderItemRequest item2 = new CreateOrderItemRequest();
			item2.setIngredientId(2L);
			item2.setQuantity(1);

			CreateOrderRequest request = new CreateOrderRequest();
			request.setItems(List.of(item1, item2));

			Order order = orderService.createOrder(101L, request);

			System.out.println("Order created for customer: " + order.getCustomerId());
			System.out.println("Order status: " + order.getStatus());
			System.out.println("Total price: " + order.getTotalPrice());
			System.out.println("Total prep time: " + order.getTotalPrepTime());
		};
	}

}

