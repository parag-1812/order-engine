# Order Engine

The `order-engine` is the transactional heart of the Cloud Kitchen backend. It receives order requests, validates requested ingredients, computes price and preparation time from live ingredient data, assigns the best-fit kitchen, reserves inventory atomically, and persists the final order with item snapshots for reliable downstream processing.

This service is built for one core promise: an order should either be created completely and consistently, or not created at all.

## Why This Service Exists

Food ordering looks simple from the UI, but backend order processing has to solve several problems at once:

- Validate that the customer request is meaningful.
- Resolve ingredient references from master data.
- Determine whether the order is vegetarian or non-vegetarian.
- Choose a kitchen that is compatible with the order.
- Ensure inventory is available before confirming the order.
- Reserve stock in the same transaction that saves the order.
- Keep a stable historical snapshot of price and prep time even if ingredient values change later.
- Enforce order status transitions safely over time.

`order-engine` handles those responsibilities in one place.

## What Is Implemented Today

- Customer order creation
- Per-item ingredient lookup
- Quantity validation
- Total price calculation
- Total preparation time calculation
- Kitchen assignment based on dietary compatibility and inventory
- Inventory reservation during order creation
- Order and order-item persistence
- Order lookup by ID
- Order lookup by customer
- Order lookup by kitchen
- Admin endpoints for ingredients, kitchens, inventory, order listing, and stats
- Order status updates with transition rules
- Order cancellation with inventory restoration
- JWT-based request authentication
- Role-based method security
- Swagger/OpenAPI documentation

## Core Business Ideas

### 1. Snapshot-Based Orders

An order stores item-level price and prep-time snapshots at the time of placement. That means future changes to ingredient pricing do not rewrite historical orders.

### 2. Kitchen Compatibility

Vegetarian orders can be prepared in any kitchen. Non-vegetarian orders are only assigned to kitchens that are not marked as `vegetarianOnly`.

### 3. Inventory Safety

The service checks whether a kitchen has enough stock for every requested ingredient before assigning it. If stock is insufficient, that kitchen is skipped. When a kitchen is selected, inventory is reduced inside the same transaction as order persistence.

### 4. Status Discipline

Orders cannot move arbitrarily through the lifecycle. Only valid transitions are accepted by the service.

## High-Level Request Flow

1. A client sends a `CreateOrderRequest`.
2. The service validates that the request contains items and that all quantities are greater than zero.
3. Each ingredient is loaded from the database.
4. The domain order is built item by item.
5. Total price and total prep time are computed.
6. The service finds the first compatible kitchen with sufficient inventory.
7. Inventory is reserved.
8. The order and order items are persisted.
9. The created order is returned to the caller.

If any step fails, the transaction rolls back.

## Architecture

The project follows a clean layered structure:

- `controller`: REST endpoints
- `service`: transactional business orchestration
- `dto`: request and response contracts
- `order`: core order domain and lifecycle rules
- `ingredient`, `inventory`, `kitchen`, `customer`: persistence models by subdomain
- `repository`: Spring Data JPA repositories
- `config`: Spring Security configuration
- `security`: JWT parsing and authentication filter
- `exception`: API error handling

## Domain Model at a Glance

### Order

The main aggregate that owns:

- customer identity
- assigned kitchen
- lifecycle status
- total price
- total preparation time
- order items

### Order Item

Each item stores:

- ingredient ID
- requested quantity
- price at order time
- prep time at order time

### Kitchen

Each kitchen has:

- an ID and name
- a `vegetarianOnly` capability flag

### Inventory

Inventory is tracked per kitchen and per ingredient using available quantity.

## Order Lifecycle

Current statuses in code:

- `CREATED`
- `VALIDATED`
- `KITCHEN_ASSIGNED`
- `COOKING`
- `READY`
- `DELIVERED`
- `CANCELLED`

Valid forward transitions currently enforced:

- `KITCHEN_ASSIGNED -> COOKING`
- `COOKING -> READY`
- `READY -> DELIVERED`

Cancellation rules:

- An order can be cancelled before it reaches active fulfillment stages.
- Orders in `COOKING`, `READY`, or `DELIVERED` cannot be cancelled.
- Cancellation restores reserved inventory.

## API Overview

Base URL:

```text
http://localhost:8080
```

### Customer / Kitchen Order APIs

| Method  | Endpoint                              | Access            | Purpose                             |
| ------- | ------------------------------------- | ----------------- | ----------------------------------- |
| `POST`  | `/orders`                             | `USER`            | Create a new order                  |
| `GET`   | `/orders/{orderId}`                   | `USER`, `KITCHEN` | Get one order                       |
| `GET`   | `/orders/customer`                    | `USER`            | Get all orders for current customer |
| `GET`   | `/orders/kitchen/{kitchenId}`         | `KITCHEN`         | Get orders for one kitchen          |
| `PATCH` | `/orders/{orderId}/status?status=...` | `KITCHEN`         | Advance order status                |
| `PATCH` | `/orders/{orderId}/cancel`            | `USER`            | Cancel an order                     |

### Admin APIs

| Method   | Endpoint                  | Access  | Purpose                           |
| -------- | ------------------------- | ------- | --------------------------------- |
| `POST`   | `/admin/ingredients`      | `ADMIN` | Create ingredient                 |
| `GET`    | `/admin/ingredients`      | `ADMIN` | List ingredients                  |
| `DELETE` | `/admin/ingredients/{id}` | `ADMIN` | Delete ingredient                 |
| `POST`   | `/admin/kitchens`         | `ADMIN` | Create kitchen                    |
| `GET`    | `/admin/kitchens`         | `ADMIN` | List kitchens                     |
| `POST`   | `/admin/inventory`        | `ADMIN` | Create inventory row              |
| `GET`    | `/admin/inventory`        | `ADMIN` | List inventory                    |
| `GET`    | `/admin/orders`           | `ADMIN` | List all orders                   |
| `GET`    | `/admin/stats`            | `ADMIN` | Get total order count and revenue |

## Example Request and Response

### Create Order Request

```json
{
  "items": [
    {
      "ingredientId": 1,
      "quantity": 2
    },
    {
      "ingredientId": 2,
      "quantity": 1
    }
  ]
}
```

### Create Order Response

```json
{
  "kitchenId": 1,
  "status": "KITCHEN_ASSIGNED",
  "totalPrice": 300.0,
  "totalPrepTime": 15
}
```

### Order Details Response

```json
{
  "orderId": 10,
  "customerId": 5,
  "kitchenId": 1,
  "status": "KITCHEN_ASSIGNED",
  "totalPrice": 300.0,
  "totalPrepTime": 15,
  "items": [
    {
      "ingredientId": 1,
      "quantity": 2,
      "priceAtOrderTime": 100.0,
      "prepTimeAtOrderTime": 5
    }
  ]
}
```

## Security Model

The service uses a JWT filter plus Spring method security.

### How Authentication Works

- The service reads the `Authorization` header.
- It expects a bearer token in the format `Bearer <jwt>`.
- The JWT subject becomes the authenticated username.
- The JWT `role` claim is converted into a Spring Security role.

Expected role values:

- `USER`
- `KITCHEN`
- `ADMIN`

### Important Note

This service does not currently issue JWTs itself. It expects a valid token from an upstream auth/login service or from a manually generated development token that uses the same secret.

### Public Endpoints

These endpoints are intentionally public for developer usability:

- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/h2-console/**`

All business APIs still require authentication and appropriate roles.

## Swagger / OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

Raw OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

Swagger is useful for:

- viewing all available endpoints
- checking schemas
- testing requests when authorization is supplied

## CORS Configuration

Current allowed frontend origins in code:

- `http://localhost:5173` for order endpoints
- `http://localhost:5175` for admin endpoints

If your frontend runs on a different port, update the relevant `@CrossOrigin` or shared CORS configuration.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- PostgreSQL driver
- H2 console starter
- JWT via `jjwt`
- Springdoc OpenAPI / Swagger UI
- Maven

## Database Notes

The current application properties are configured for PostgreSQL-style JPA settings:

- username: `postgres`
- password: `postgres`
- dialect: `PostgreSQLDialect`
- `ddl-auto=update`

Current seed data in `data.sql` inserts:

- ingredients
- kitchens
- inventory

Seeded examples include:

- `Paneer`
- `Cheese`
- one vegetarian kitchen
- one all-food kitchen

## Local Setup

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL available locally if you want a persistent database

### Run the Service

From the `backend/order-engine` directory:

```bash
mvn spring-boot:run
```

Or build first:

```bash
mvn clean package
java -jar target/order-engine-0.0.1-SNAPSHOT.jar
```

### Useful Environment Variable

```text
JWT_SECRET
```

If not supplied, the service falls back to:

```text
dev-secret-key-change-me-32bytes-min
```

For real environments, always override that value.

## What Makes This Order Engine Strong

- Transaction-first design instead of best-effort order placement
- Inventory reservation tied directly to order creation
- Stable historical pricing through item snapshots
- Explicit lifecycle rules
- Clear separation of controller, service, domain, and persistence concerns
- Practical admin APIs for kitchen operations and debugging
- Ready-to-use Swagger documentation

## Current Gaps and Natural Next Steps

A strong README should also be honest about what can be improved next:

- Add a dedicated auth service or token issuance flow for easier Swagger testing
- Add ownership checks so users can only read or cancel their own orders
- Add optimistic or pessimistic locking for high-concurrency inventory contention
- Add pagination for admin listing endpoints
- Add integration tests for transactional edge cases
- Add Docker support for one-command local startup
- Add environment-specific config profiles
- Add audit logging and metrics
- Add API examples for every role in Swagger

## Suggested Project Structure

```text
backend/order-engine
|-- src/main/java/com/cloudkitchen/order_engine
|   |-- config
|   |-- controller
|   |-- customer
|   |-- dto
|   |-- exception
|   |-- ingredient
|   |-- inventory
|   |-- kitchen
|   |-- order
|   |-- repository
|   |-- security
|   `-- service
|-- src/main/resources
|   |-- application.properties
|   `-- data.sql
|-- pom.xml
`-- README.md
```

## For Collaborators

If you are integrating with this service:

- send a valid JWT with `sub` and `role`
- use Swagger to discover request and response shapes
- seed kitchens and inventory before creating real orders
- expect authorization failures if you call protected APIs without the correct role

## Summary

`order-engine` is more than a CRUD service. It is a transactional workflow engine for order acceptance, kitchen assignment, and inventory-safe fulfillment in a cloud kitchen environment. Its strength comes from enforcing consistency where it matters most: pricing, stock, kitchen selection, and lifecycle state.
