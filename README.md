# 🍽️ Order Creation Engine – Cloud Kitchen Backend

A **production-grade Order Creation Engine** built as the core backend module for a **Cloud Kitchen application**.

This service handles **custom food orders**, validates ingredients, calculates price & preparation time, assigns a suitable kitchen, and safely manages inventory — all in a **transactionally consistent** manner.

---

## 🚀 Features

- ✅ Custom order creation with ingredient-level granularity
- ✅ Deterministic pricing using snapshot-based order items
- ✅ Order lifecycle management (state machine)
- ✅ Intelligent kitchen assignment (veg / non-veg compatible)
- ✅ Inventory validation and reservation (no negative stock)
- ✅ Transaction-safe order placement
- ✅ Persistent storage using JPA + H2 (dev)
- ✅ Clean domain-driven design (DDD-inspired)

---

## 🧠 High-Level Flow

Customer Request

        ↓

Request Validation

        ↓

Ingredient Lookup (DB)

        ↓

Order Creation (Domain)

        ↓

Price & Prep-Time Calculation

        ↓

Kitchen Selection

        ↓

Inventory Check & Reservation

        ↓

Order Persistence

        ↓

Order Assigned to Kitchen


---

## 🏗️ Architecture Overview

### Domain vs Persistence Separation

| Layer | Responsibility |
|-----|----------------|
| DTO | API boundary, input validation |
| Domain (`Order`, `OrderItem`) | Business rules & lifecycle |
| Entity (`OrderEntity`, `IngredientEntity`) | Database persistence |
| Service (`OrderService`) | Orchestration & transactions |
| Repository | Data access (JPA) |

This separation ensures:
- Clean business logic
- Testability
- Long-term maintainability

---

## 🧩 Core Domain Model

### Order
- Aggregate root
- Owns `OrderItem`
- Controls lifecycle transitions
- Holds final price & prep time

### OrderItem
- Ingredient snapshot (price, prep time)
- Quantity-based calculations
- Immutable after creation

### Kitchen
- Veg-only or all-food capability
- Owns inventory

### Inventory
- Per kitchen + ingredient
- Prevents overselling
- Updated atomically with order creation

---

## 🔄 Order Lifecycle

CREATED

        ↓

VALIDATED

        ↓

KITCHEN_ASSIGNED

        ↓

COOKING

        ↓

READY

        ↓

DELIVERED


Invalid transitions are **explicitly blocked** by design.

---

## 🛡️ Inventory Safety

- Inventory is checked **before** kitchen assignment
- Stock is reserved **inside the same transaction**
- If anything fails → **entire transaction rolls back**
- Inventory never goes negative

---

## 🗄️ Database (Development)

- **H2 (In-Memory)**
- Schema auto-generated via JPA
- Seed data via `data.sql`
- H2 Console enabled for inspection

### H2 Console
http://localhost:8080/h2-console

---

**JDBC URL**

jdbc:h2:mem:orderdb


---

## ⚙️ Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Data JPA**
- **Spring Security** (configured for dev)
- **H2 Database**
- **Maven**

---

## 📂 Project Structure

com.cloudkitchen.orderengine

├── order // Domain + Order lifecycle

├── ingredient // Ingredient master data

├── kitchen // Kitchen entity

├── inventory // Inventory management

├── service // Order orchestration logic

├── repository // JPA repositories

├── dto // Request DTOs

└── config // Security & configuration


---

## 🧪 Current Status

- ✔ Order creation engine complete
- ✔ Kitchen assignment & inventory locking complete
- ✔ Orders persisted successfully

### Upcoming (Planned)
- REST APIs (`/orders`)
- Role-based order access (Customer / Kitchen / Admin)
- Order item persistence
- Status updates (COOKING → READY → DELIVERED)
- PostgreSQL integration

---

## 🧑‍💻 Author

**Parag Gupta**  
Backend Engineer | Java | Spring Boot

This project is part of a larger **Cloud Kitchen platform** being built with production readiness in mind.

---

## 📜 License

This project is for learning and portfolio purposes.


