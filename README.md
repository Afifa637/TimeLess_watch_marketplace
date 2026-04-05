# ⌚ Timeless Marketplace

> A full-stack luxury watch resale platform built with **Spring Boot 3**, featuring JWT-secured REST APIs, server-rendered Thymeleaf views, role-based dashboards, PostgreSQL persistence, Liquibase migrations, and a Docker-first local setup with full CI/CD to Render.

---

## 📋 Table of Contents

1. [Project Overview](#-project-overview)
2. [Live Demo](#-live-demo)
3. [Demo Credentials](#-demo-credentials)
4. [Tech Stack](#-tech-stack)
5. [Prerequisites](#-prerequisites)
6. [Quick Start (Docker)](#-quick-start-docker)
7. [Environment Variables](#-environment-variables)
8. [Architecture Overview](#-architecture-overview)
9. [ER Diagram](#-er-diagram)
10. [Project Structure](#-project-structure)
11. [API Reference](#-api-reference)
12. [Authentication Flow](#-authentication-flow)
13. [Role-Based Access](#-role-based-access)
14. [Running Tests](#-running-tests)
15. [CI/CD Pipeline](#-cicd-pipeline)
16. [Branch Strategy](#-branch-strategy)
17. [Render Deployment](#-render-deployment)
18. [Seed Data](#-seed-data)
19. [Security Notes](#-security-notes)
20. [Troubleshooting](#-troubleshooting)

---

## 📌 Project Overview

**Timeless Marketplace** is a luxury watch resale platform where:

- **Buyers** browse, wishlist, cart, purchase, and review watches.
- **Sellers** list watches, manage inventory, and track orders.
- **Admins** oversee users, approve/reject listings, manage orders, and view platform statistics.

The backend exposes a full REST API documented via Swagger UI, while Thymeleaf delivers server-rendered pages for each role's dashboard. Authentication is stateless using JWT tokens. Database migrations are handled by Liquibase, and the entire stack runs in Docker with a single command.

---

## 🌐 Live Demo

| Resource | URL |
|---|---|
| Application | [https://timeless-watch-marketplace.onrender.com/](https://timeless-watch-marketplace.onrender.com/) |
| Swagger UI | [https://timeless-watch-marketplace.onrender.com/swagger-ui/index.html](https://timeless-watch-marketplace.onrender.com/swagger-ui/index.html) |
| GitHub Repo | [https://github.com/Afifa637/TimeLess_watch_marketplace](https://github.com/Afifa637/TimeLess_watch_marketplace) |

---

## 🔑 Demo Credentials

These accounts are seeded automatically when `BOOTSTRAP_ENABLED=true`:

| Role | Email | Password | Access |
|---|---|---|---|
| Admin | `admin@timeless.com` | `Admin123!` | Full platform control |
| Seller | `seller1@timeless.com` | `Seller123!` | List and manage watches |
| Buyer | `buyer1@timeless.com` | `Buyer123!` | Browse, buy, review |

---

## 🛠 Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 17 | Core application language |
| Framework | Spring Boot 3.3.4 | Application backbone |
| Security | Spring Security + JWT + BCrypt | Auth, role guards, password hashing |
| Database | PostgreSQL 16 (prod) / H2 (tests) | Persistence |
| ORM | Spring Data JPA / Hibernate | Object-relational mapping |
| Migrations | Liquibase | Schema version control |
| Views | Thymeleaf + Spring Security Extras | Server-rendered HTML pages |
| API Docs | springdoc-openapi / Swagger UI | Interactive API explorer |
| Testing | JUnit 5 + Mockito + MockMvc | Unit and integration tests |
| Build | Maven 3.9 | Dependency management and packaging |
| Containerization | Docker + Docker Compose | Local and CI environment |
| CI/CD | GitHub Actions + Render | Automated build, test, and deploy |

---

## ✅ Prerequisites

Before you begin, make sure the following are installed on your machine:

| Tool | Minimum Version | Check Command |
|---|---|---|
| Docker | 24.x | `docker --version` |
| Docker Compose | 2.x (bundled with Docker Desktop) | `docker compose version` |
| Git | 2.x | `git --version` |
| Java (optional, for local dev) | 17 | `java -version` |
| Maven (optional, for local dev) | 3.9 | `mvn -version` |

> If you only want to **run** the app, you only need **Docker** and **Git**. Java and Maven are only needed if you plan to develop or run tests outside of Docker.

---

## 🚀 Quick Start (Docker)

This is the fastest way to get the entire application running locally.

### Step 1 — Clone the repository

```bash
git clone [https://github.com/your-username/timeless-marketplace.git](https://github.com/Afifa637/TimeLess_watch_marketplace)
cd timeless-marketplace
```

### Step 2 — Create the environment file

Copy the example environment file and fill in your values:

```bash
cp .env.example .env
```

Open `.env` in any text editor and set the values (see [Environment Variables](#-environment-variables) below). The defaults in the example file work out of the box for local Docker use.

### Step 3 — Build and start everything

```bash
docker compose up --build
```

This single command will:
1. Pull the PostgreSQL 16 image
2. Build the Spring Boot application image using the multi-stage `Dockerfile`
3. Start the `db` container and wait for it to pass its health check
4. Start the `app` container once the database is ready
5. Run Liquibase migrations to create all tables
6. Seed demo data (admin, sellers, buyers, watches, orders, reviews)

### Step 4 — Open the application

| Resource | URL |
|---|---|
| Application homepage | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| OpenAPI JSON | http://localhost:8081/v3/api-docs |

> The app is mapped to port **8081** on your host machine (container port 8080). If you need a different host port, update the `ports` section in `docker-compose.yml`.

### Stopping the application

```bash
# Stop containers but keep volumes (data is preserved)
docker compose down

# Stop containers AND delete all data (fresh start)
docker compose down -v
```

---

### Variable Reference

| Variable | Required | Description |
|---|---|---|
| `POSTGRES_DB` | Yes | PostgreSQL database name |
| `POSTGRES_USER` | Yes | PostgreSQL username |
| `POSTGRES_PASSWORD` | Yes | PostgreSQL password |
| `DB_URL` | Yes | Full JDBC connection URL for the app |
| `DB_USER` | Yes | DB username passed to Spring |
| `DB_PASS` | Yes | DB password passed to Spring |
| `JWT_SECRET` | Yes | Secret key for signing JWT tokens (≥32 chars) |
| `JWT_EXPIRY` | Yes | Token lifetime in milliseconds |
| `BOOTSTRAP_ENABLED` | No | `true` seeds demo data, `false` skips it |
| `APP_ADMIN_EMAIL` | Yes | Email for the bootstrapped admin account |
| `APP_ADMIN_PASSWORD` | Yes | Password for the bootstrapped admin account |
| `SERVER_PORT` | No | Internal container port (default: 8080) |

---

## 🏗 Architecture Overview

```
┌────────────────────────────────────────────────────────────────┐
│                        Browser / Client                        │
│   • Thymeleaf server-rendered pages (HTML + CSS + JS)          │
│   • JS fetch() calls to REST API for dynamic interactions      │
│   • JWT stored in localStorage + mirrored to cookie            │
└───────────────────────────┬────────────────────────────────────┘
                            │  HTTP/HTTPS
                            ▼
┌────────────────────────────────────────────────────────────────┐
│                  Spring Security Filter Chain                  │
│   • JwtAuthenticationFilter — reads Bearer token from header   │
│   • Authenticates principal on every request                   │
│   • Role-based URL guards + @PreAuthorize method security      │
└───────────────────────────┬────────────────────────────────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
┌──────────────┐  ┌──────────────────┐  ┌────────────────────┐
│ PageController│  │  REST Controllers │  │  AdminController   │
│ (Thymeleaf   │  │  /api/**          │  │  /api/admin/**     │
│  pages)      │  │  Auth, Watches,   │  │  Users, Listings,  │
│              │  │  Orders, Payments,│  │  Stats, Reviews    │
│              │  │  Cart, Wishlist,  │  │                    │
│              │  │  Reviews          │  │                    │
└──────┬───────┘  └────────┬─────────┘  └─────────┬──────────┘
       │                   │                       │
       └───────────────────┼───────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────────┐
│                        Service Layer                           │
│   AuthService │ WatchService │ OrderService │ PaymentService   │
│   CartService │ WishlistService │ ReviewService                │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│              Repository Layer (Spring Data JPA)                │
│   UserAccountRepository │ WatchRepository │ OrderRepository    │
│   PaymentRepository │ CartItemRepository │ WishlistRepository  │
│   ReviewRepository                                             │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│               PostgreSQL 16  +  Liquibase Migrations           │
│   Tables: users, watches, orders, payments, reviews,           │
│           user_watch_cart, user_watch_wishlist                 │
└────────────────────────────────────────────────────────────────┘
```

**Key design decisions:**
- **Stateless auth** — JWT eliminates the need for server-side sessions.
- **Dual rendering** — Thymeleaf pages call the same REST API that external clients use; no duplicate business logic.
- **Migration-first schema** — Liquibase tracks every DDL change, making rollbacks and team collaboration safe.
- **Health-check dependency** — Docker Compose only starts the app after PostgreSQL passes its health check, preventing startup race conditions.

---

## 📊 ER Diagram

```
┌──────────────────┐         ┌──────────────────┐
│     users        │ 1     ∞ │     watches       │
│──────────────────│─────────│──────────────────│
│ id          PK   │         │ id          PK   │
│ email       UNIQUE         │ seller_id   FK → users
│ password_hash    │         │ name             │
│ full_name        │         │ brand            │
│ phone            │         │ category         │
│ address          │         │ condition        │
│ role             │         │ description      │
│ enabled          │         │ price            │
│ email_verified   │         │ stock_quantity   │
│ created_at       │         │ status           │
└────────┬─────────┘         │ image_url        │
         │                   │ reference_number │
         │                   │ year             │
         │                   │ created_at       │
         │                   └────────┬─────────┘
         │                            │
         │ 1                          │ 1
         │           ┌────────────────┘
         ▼ ∞         ▼ ∞
┌──────────────────┐       ┌──────────────────────┐
│     orders       │ 1   1 │      payments         │
│──────────────────│───────│──────────────────────│
│ id          PK   │       │ id            PK     │
│ buyer_id    FK   │       │ order_id      FK UNIQUE
│ watch_id    FK   │       │ buyer_id      FK     │
│ status           │       │ amount               │
│ total_amount     │       │ method               │
│ tracking_number  │       │ status               │
│ created_at       │       │ transaction_ref      │
│ updated_at       │       │ paid_at              │
└────────┬─────────┘       │ created_at           │
         │ 1               └──────────────────────┘
         │
         ▼ 1
┌──────────────────┐
│     reviews      │
│──────────────────│
│ id          PK   │
│ buyer_id    FK   │
│ watch_id    FK   │
│ order_id    FK UNIQUE
│ rating           │
│ comment          │
│ created_at       │
│ UNIQUE(buyer_id, watch_id)
└──────────────────┘

┌──────────────────────┐     ┌──────────────────────┐
│  user_watch_wishlist │     │   user_watch_cart     │
│──────────────────────│     │──────────────────────│
│ user_id   PK/FK      │     │ id        PK         │
│ watch_id  PK/FK      │     │ user_id   FK         │
│ added_at             │     │ watch_id  FK         │
└──────────────────────┘     │ quantity             │
                             │ added_at             │
                             │ UNIQUE(user_id, watch_id)
                             └──────────────────────┘
```

**Relationships summary:**
- `users` → `watches`: One seller can list many watches (1:M)
- `users` → `orders`: One buyer can place many orders (1:M)
- `watches` → `orders`: One watch per order (1:M)
- `orders` → `payments`: One order has at most one payment (1:1)
- `orders` → `reviews`: One order can generate one review (1:1)
- `users` ↔ `watches` (via cart): Many-to-many through `user_watch_cart`
- `users` ↔ `watches` (via wishlist): Many-to-many through `user_watch_wishlist`

---

## 📁 Project Structure

```
timeless-marketplace/
│
├── .github/
│   └── workflows/
│       └── ci-cd.yml              # GitHub Actions pipeline
│
├── src/
│   ├── main/
│   │   ├── java/com/timeless/app/
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java         # Spring Security + JWT filter
│   │   │   │   ├── DataBootstrapConfig.java    # Seed data on startup
│   │   │   │   └── WebConfig.java              # MVC configuration
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java         # POST /api/auth/**
│   │   │   │   ├── WatchController.java        # CRUD /api/watches/**
│   │   │   │   ├── OrderController.java        # /api/orders/**
│   │   │   │   ├── PaymentController.java      # /api/payments/**
│   │   │   │   ├── CartController.java         # /api/cart/**
│   │   │   │   ├── WishlistController.java     # /api/wishlist/**
│   │   │   │   ├── ReviewController.java       # /api/reviews/**
│   │   │   │   ├── AdminController.java        # /api/admin/**
│   │   │   │   ├── PageController.java         # Thymeleaf page routes
│   │   │   │   ├── FallbackPageController.java # SPA-style fallback
│   │   │   │   └── ErrorPageController.java    # Custom error pages
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── WatchService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── PaymentService.java
│   │   │   │   ├── CartService.java
│   │   │   │   ├── WishlistService.java
│   │   │   │   ├── ReviewService.java
│   │   │   │   └── FileStorageService.java
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── UserAccount.java
│   │   │   │   ├── Watch.java
│   │   │   │   ├── Order.java
│   │   │   │   ├── Payment.java
│   │   │   │   ├── Review.java
│   │   │   │   ├── CartItem.java
│   │   │   │   ├── WishlistItem.java
│   │   │   │   └── (enums: Role, OrderStatus, WatchStatus, etc.)
│   │   │   │
│   │   │   ├── repository/          # Spring Data JPA interfaces
│   │   │   ├── dto/
│   │   │   │   ├── request/         # Incoming payload POJOs
│   │   │   │   └── response/        # Outgoing response POJOs
│   │   │   │
│   │   │   ├── exception/           # Custom exceptions + global handler
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── UserPrincipal.java
│   │   │   └── util/
│   │   │       └── SecurityUtils.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                      # Main configuration
│   │       ├── db/changelog/
│   │       │   └── db.changelog-master.yaml         # Liquibase migrations
│   │       ├── templates/                           # Thymeleaf HTML pages
│   │       │   ├── index.html
│   │       │   ├── login.html
│   │       │   ├── register.html
│   │       │   ├── watches.html
│   │       │   ├── watch-detail.html
│   │       │   ├── admin/
│   │       │   ├── seller/
│   │       │   ├── buyer/
│   │       │   ├── error/
│   │       │   └── layout/
│   │       └── static/
│   │           ├── css/timeless.css
│   │           └── js/timeless.js
│   │
│   └── test/
│       ├── java/com/timeless/app/
│       │   ├── service/
│       │   │   ├── AuthServiceTest.java    # 6 unit tests
│       │   │   ├── WatchServiceTest.java   # 8 unit tests
│       │   │   └── OrderServiceTest.java   # 8 unit tests
│       │   └── controller/
│       │       ├── AuthControllerIT.java   # 5 integration tests
│       │       ├── WatchControllerIT.java  # 5 integration tests
│       │       └── OrderControllerIT.java  # 5 integration tests
│       └── resources/
│           └── application-test.yml        # H2 in-memory test config
│
├── Dockerfile                  # Multi-stage build (Maven → JRE)
├── docker-compose.yml          # App + PostgreSQL services
├── .env.example                # Template for environment variables
├── pom.xml                     # Maven dependencies and plugins
└── README.md                   # This file
```

---

## 📡 API Reference

All REST endpoints are fully documented and testable at `http://localhost:8081/swagger-ui.html`.

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new BUYER or SELLER account |
| `POST` | `/api/auth/login` | Public | Log in and receive a JWT token |

### Watches

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `GET` | `/api/watches` | Public | Any | Browse active watches (supports filters + pagination) |
| `GET` | `/api/watches/{id}` | Public | Any | View full watch details |
| `POST` | `/api/watches` | ✅ | SELLER | Create a new watch listing |
| `PUT` | `/api/watches/{id}` | ✅ | SELLER / ADMIN | Update a watch listing |
| `DELETE` | `/api/watches/{id}` | ✅ | SELLER / ADMIN | Delete a watch listing |
| `GET` | `/api/watches/my` | ✅ | SELLER | List the current seller's own watches |

### Orders

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `POST` | `/api/orders` | ✅ | BUYER | Place a new order |
| `GET` | `/api/orders/my` | ✅ | BUYER | List the current buyer's orders |
| `GET` | `/api/orders/seller` | ✅ | SELLER | List orders for the seller's watches |
| `GET` | `/api/orders/{id}` | ✅ | Any | View a specific order (if authorized) |
| `PATCH` | `/api/orders/{id}/status` | ✅ | Any | Update order status (role rules apply) |
| `GET` | `/api/orders` | ✅ | ADMIN | List all orders on the platform |

### Payments

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `POST` | `/api/payments` | ✅ | BUYER | Initiate payment (simulated instant approval) |
| `GET` | `/api/payments/order/{orderId}` | ✅ | BUYER / ADMIN | Get payment details for an order |
| `POST` | `/api/payments/{id}/refund` | ✅ | ADMIN | Refund a payment and cancel the order |
| `GET` | `/api/payments` | ✅ | ADMIN | List all payments on the platform |

### Cart

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `GET` | `/api/cart` | ✅ | BUYER | View cart contents |
| `POST` | `/api/cart/{watchId}` | ✅ | BUYER | Add a watch to cart |
| `DELETE` | `/api/cart/{watchId}` | ✅ | BUYER | Remove a specific item from cart |
| `DELETE` | `/api/cart` | ✅ | BUYER | Clear entire cart |
| `POST` | `/api/cart/checkout` | ✅ | BUYER | Create orders from all cart items |

### Wishlist

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `GET` | `/api/wishlist` | ✅ | BUYER | View wishlist |
| `POST` | `/api/wishlist/{watchId}` | ✅ | BUYER | Add a watch to wishlist |
| `DELETE` | `/api/wishlist/{watchId}` | ✅ | BUYER | Remove a watch from wishlist |
| `POST` | `/api/wishlist/{watchId}/move-to-cart` | ✅ | BUYER | Move item from wishlist to cart |

### Reviews

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `POST` | `/api/reviews` | ✅ | BUYER | Leave a review (requires completed order) |
| `GET` | `/api/reviews/watch/{watchId}` | Public | Any | List all reviews for a watch |
| `DELETE` | `/api/reviews/{id}` | ✅ | ADMIN | Remove a review |

### Admin

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| `GET` | `/api/admin/users` | ✅ | ADMIN | List all users (optional role filter) |
| `PATCH` | `/api/admin/users/{id}/toggle` | ✅ | ADMIN | Enable or disable a user account |
| `GET` | `/api/admin/watches` | ✅ | ADMIN | List all watches (optional status filter) |
| `GET` | `/api/admin/watches/pending` | ✅ | ADMIN | List watches awaiting review |
| `PATCH` | `/api/admin/watches/{id}/approve` | ✅ | ADMIN | Approve a listing |
| `PATCH` | `/api/admin/watches/{id}/reject` | ✅ | ADMIN | Reject a listing |
| `PATCH` | `/api/admin/watches/{id}/deactivate` | ✅ | ADMIN | Deactivate a listing |
| `DELETE` | `/api/admin/watches/{id}` | ✅ | ADMIN | Permanently delete a listing |
| `DELETE` | `/api/admin/reviews/{id}` | ✅ | ADMIN | Delete any review |
| `GET` | `/api/admin/stats` | ✅ | ADMIN | Marketplace statistics dashboard data |

---

## 🔒 Authentication Flow

```
Client                          Server
  │                               │
  │──POST /api/auth/login──────►  │
  │  { email, password }          │  1. Load user from DB
  │                               │  2. Verify BCrypt password
  │                               │  3. Generate signed JWT
  │◄── 200 OK  { token, role } ── │
  │                               │
  │  (Store token in localStorage)│
  │                               │
  │──GET /api/orders/my ────────► │
  │  Authorization: Bearer <JWT>  │  4. JwtAuthenticationFilter
  │                               │     parses and validates token
  │                               │  5. Sets SecurityContext
  │                               │  6. @PreAuthorize checks role
  │◄── 200 OK  [ orders... ] ──── │
```

- Tokens are stored client-side in `localStorage` for API calls.
- A copy is also mirrored to a regular browser cookie so Thymeleaf pages can read the authenticated user's identity for server-rendered navigation.
- Passwords are never stored in plain text — BCrypt is applied at registration.
- The admin account cannot be created through the public `/api/auth/register` endpoint.

---

## 👥 Role-Based Access

| Feature | PUBLIC | BUYER | SELLER | ADMIN |
|---|:---:|:---:|:---:|:---:|
| Browse watches | ✅ | ✅ | ✅ | ✅ |
| Register / Login | ✅ | ✅ | ✅ | ✅ |
| View watch details | ✅ | ✅ | ✅ | ✅ |
| Read reviews | ✅ | ✅ | ✅ | ✅ |
| Manage cart | ❌ | ✅ | ❌ | ❌ |
| Manage wishlist | ❌ | ✅ | ❌ | ❌ |
| Place orders | ❌ | ✅ | ❌ | ❌ |
| Make payments | ❌ | ✅ | ❌ | ❌ |
| Write reviews | ❌ | ✅ | ❌ | ❌ |
| Create listings | ❌ | ❌ | ✅ | ❌ |
| Manage own listings | ❌ | ❌ | ✅ | ✅ |
| View own orders | ❌ | ✅ | ✅ | ✅ |
| Approve/reject listings | ❌ | ❌ | ❌ | ✅ |
| Manage all users | ❌ | ❌ | ❌ | ✅ |
| Issue refunds | ❌ | ❌ | ❌ | ✅ |
| View platform stats | ❌ | ❌ | ❌ | ✅ |

---

## 🧪 Running Tests

### Run only unit tests

```bash
mvn test
```

### Run full verification (unit + integration)

```bash
mvn verify
```

### Test breakdown

| Test Class | Type | Tests | What it covers |
|---|---|---|---|
| `AuthServiceTest` | Unit | 6 | Registration, login, duplicate email, bad credentials |
| `WatchServiceTest` | Unit | 8 | CRUD, ownership, status transitions, not-found errors |
| `OrderServiceTest` | Unit | 8 | Place order, status updates, role-based transitions, validations |
| `AuthControllerIT` | Integration | 5 | Register and login endpoints via MockMvc |
| `WatchControllerIT` | Integration | 5 | Browse, create, update, delete via HTTP layer |
| `OrderControllerIT` | Integration | 5 | Place order, list orders, status update via HTTP layer |
| **Total** | | **37** | |

**Unit tests** use `@ExtendWith(MockitoExtension.class)` — no Spring context, no database. All dependencies are mocked with Mockito for fast, isolated testing.

**Integration tests** use `@SpringBootTest` + `MockMvc` + an in-memory **H2** database (configured via `application-test.yml`). Liquibase is disabled in test mode; Hibernate's `create-drop` strategy builds the schema from JPA annotations.

### Test environment configuration

Tests use `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  liquibase:
    enabled: false
app:
  jwt:
    secret: test-secret-key-for-unit-testing-only-must-be-32-chars
    expiration-ms: 3600000
  bootstrap:
    enabled: false
```

---

## ⚙️ CI/CD Pipeline

The pipeline is defined in `.github/workflows/ci-cd.yml` and runs automatically on every push or pull request.

```
Push / PR ──► GitHub Actions ──► build-and-test ──► deploy (main only)
                                       │                     │
                              mvn clean verify         Render deploy hook
                              (unit + integration)     (rebuild + restart)
```

### Pipeline YAML

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"
          cache: maven

      - name: Build and test
        run: mvn clean verify -B

  deploy:
    needs: build-and-test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
      - name: Deploy to Render
        run: curl -X POST "${{ secrets.RENDER_DEPLOY_HOOK_URL }}"
```

### How the pipeline works step by step

1. **Trigger** — Any push to `main` or `develop`, or any pull request targeting `main`.
2. **Checkout** — The `actions/checkout@v4` action clones the repository into the runner.
3. **Java setup** — Temurin 17 JDK is installed. Maven dependencies are cached between runs to speed up subsequent builds.
4. **Build and test** — `mvn clean verify -B` compiles the project, runs all 37 tests (unit + integration), and fails the job if any test fails.
5. **Deploy** (main push only) — A `curl` call to the Render deploy hook URL stored as a GitHub secret triggers a new Render deployment. The deploy job only runs if `build-and-test` passes.

### Required GitHub Secrets

Go to **Settings → Secrets and Variables → Actions** in your GitHub repository and add:

| Secret Name | Value |
|---|---|
| `RENDER_DEPLOY_HOOK_URL` | Your Render deploy hook URL (from Render dashboard) |

---

## 🌿 Branch Strategy

```
main           ← production-ready, protected
  │
  └── develop  ← active integration branch
        │
        ├── feature/add-watch-listing
        ├── feature/payment-flow
        ├── feature/buyer-dashboard
        └── hotfix/fix-jwt-expiry   ← cut from main for urgent fixes
```

### Rules

| Rule | Details |
|---|---|
| Direct push to `main` | ❌ Prohibited — PR required |
| Merging to `main` | Requires at least 1 review approval and CI passing |
| Feature branches | Branch off `develop`, merge back to `develop` |
| Hotfix branches | Branch off `main`, merge to both `main` and `develop` |
| Branch naming | `feature/short-description`, `hotfix/short-description` |

### Typical workflow for a new feature

```bash
# 1. Start from develop
git checkout develop
git pull origin develop

# 2. Create a feature branch
git checkout -b feature/add-review-moderation

# 3. Work and commit
git add .
git commit -m "feat: add review moderation endpoint for admins"

# 4. Push and open a PR to develop
git push origin feature/add-review-moderation
# → Open PR on GitHub: feature/add-review-moderation → develop

# 5. After review and approval, merge to develop
# 6. When develop is stable, open PR: develop → main
# 7. After merge to main, CI auto-deploys to Render
```

---

## ☁️ Render Deployment

### Services to create

| Service Type | Purpose |
|---|---|
| **Web Service** | Runs the Spring Boot application JAR |
| **Managed PostgreSQL** | Production database (Render-hosted) |

### Build and start commands

| Setting | Value |
|---|---|
| Build Command | `mvn clean package -DskipTests` |
| Start Command | `java -jar target/timeless-marketplace-1.0.0.jar` |

### Environment variables for Render

In the Render Web Service dashboard under **Environment**, add the following:

| Variable | Example Value | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://<render-db-host>:5432/timeless` | From Render Postgres dashboard |
| `DB_USER` | `timeless` | Your Render Postgres username |
| `DB_PASS` | `your-secure-password` | Your Render Postgres password |
| `JWT_SECRET` | `a-very-long-random-secret-min-32-chars` | Generate with a password manager |
| `JWT_EXPIRY` | `86400000` | 24 hours in milliseconds |
| `APP_ADMIN_EMAIL` | `admin@yourdomain.com` | Admin account email |
| `APP_ADMIN_PASSWORD` | `StrongPass123!` | Admin account password |
| `BOOTSTRAP_ENABLED` | `true` | Set to `false` after first deploy |
| `SERVER_PORT` | `10000` | Render's internal port requirement |

### Step-by-step Render setup

1. Push your code to GitHub and make sure CI passes on `main`.
2. In the Render dashboard, create a new **PostgreSQL** service and copy the connection details.
3. Create a new **Web Service**, connect your GitHub repository, and set the branch to `main`.
4. Fill in the build/start commands and all environment variables listed above.
5. Copy the **Deploy Hook URL** from the Render Web Service settings.
6. Add it as a GitHub secret named `RENDER_DEPLOY_HOOK_URL`.
7. Every future push to `main` that passes CI will automatically redeploy.

---

## 🌱 Seed Data

When `BOOTSTRAP_ENABLED=true` and the database is empty (fresh start), the application seeds the following data automatically:

| Data | Count | Details |
|---|---|---|
| Admin account | 1 | `admin@timeless.com` |
| Seller accounts | 2 | `seller1@timeless.com`, `seller2@timeless.com` |
| Buyer accounts | 2 | `buyer1@timeless.com`, `buyer2@timeless.com` |
| Watch listings | 8 | Mix of brands (Rolex, Patek, AP, etc.), active status |
| Completed orders | 2 | Belonging to `buyer1@timeless.com` |
| Payment records | 2 | Linked to the completed orders |
| Reviews | 2 | Demo reviews on seeded watches |

> Set `BOOTSTRAP_ENABLED=false` in production after the first deployment to prevent re-seeding on restarts.

---

## 🔐 Security Notes

| Area | Implementation |
|---|---|
| Password storage | BCrypt hashing — plain text passwords are never stored |
| Token format | Signed JWT (HS256) with configurable expiry |
| Token storage | `localStorage` (API calls) + browser cookie (page navigation) |
| Method security | `@PreAuthorize` annotations enforce role rules at the service boundary |
| Admin creation | Admins are only created via bootstrap config, not the public registration endpoint |
| Payment processing | **Prototype simulation only** — no real payment gateway, no card data stored |
| Credentials in code | All secrets are injected via environment variables; no credentials are hardcoded |

---

## 📄 License

This project is submitted as academic coursework. All rights reserved by the author.
