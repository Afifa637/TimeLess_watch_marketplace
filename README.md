# Timeless Marketplace

Timeless is a Spring Boot 3 luxury watch resale marketplace with server-rendered Thymeleaf pages, JWT-secured REST APIs, PostgreSQL persistence via JPA, Liquibase migrations, role-based dashboards, and Docker-first local setup.

## Demo Credentials

| Role | Email | Password |
|---|---|---|
| Admin | `admin@timeless.com` | `Admin123!` |
| Seller | `seller1@timeless.com` | `Seller123!` |
| Buyer | `buyer1@timeless.com` | `Buyer123!` |

## Architecture Overview

```text
+-------------------+
| Browser / Client  |
| - Thymeleaf pages |
| - JS fetch calls  |
+---------+---------+
          |
          v
+------------------------------+
| Spring Security Filter Chain |
| - JWT Authentication Filter  |
| - Role-based method guards   |
+---------------+--------------+
                |
                v
+----------------------------------------------+
| Controllers                                   |
| - PageController (server-rendered pages)      |
| - REST Controllers (/api/**)                  |
+--------------------------+-------------------+
                           |
                           v
+----------------------------------------------+
| Service Layer                                  |
| - AuthService                                  |
| - WatchService                                 |
| - OrderService                                 |
| - PaymentService                               |
| - CartService / WishlistService / ReviewService|
+--------------------------+-------------------+
                           |
                           v
+----------------------------------------------+
| Repository Layer (Spring Data JPA)            |
+--------------------------+-------------------+
                           |
                           v
+----------------------------------------------+
| PostgreSQL + Liquibase                         |
| - schema migrations                            |
| - persistent marketplace data                  |
+----------------------------------------------+
```

## ER Diagram

```text
+-------------------+       +-------------------+
| users             |1-----<| watches           |
|-------------------|       |-------------------|
| id PK             |       | id PK             |
| email UNIQUE      |       | seller_id FK      |
| password_hash     |       | name              |
| full_name         |       | brand             |
| phone             |       | category          |
| address           |       | condition         |
| role              |       | description       |
| enabled           |       | price             |
| email_verified    |       | stock_quantity    |
| created_at        |       | status            |
+-------------------+       | image_url         |
        |   ^               | reference_number  |
        |   |               | year              |
        |   |               | created_at        |
        |   |               | updated_at        |
        |   |               +-------------------+
        |   |
        |   +-----------------------+
        |                           |
        v                           v
+-------------------+       +---------------------------+
| orders            |>-----1| payments                  |
|-------------------|       |---------------------------|
| id PK             |       | id PK                     |
| buyer_id FK       |       | order_id FK UNIQUE        |
| watch_id FK       |       | buyer_id FK               |
| status            |       | amount                    |
| total_amount      |       | method                    |
| tracking_number   |       | status                    |
| created_at        |       | transaction_ref           |
| updated_at        |       | paid_at                   |
+-------------------+       | created_at                |
        |
        |1
        v
+-------------------+
| reviews           |
|-------------------|
| id PK             |
| buyer_id FK       |
| watch_id FK       |
| order_id FK UNIQUE|
| rating            |
| comment           |
| created_at        |
| UNIQUE(buyer,watch)|
+-------------------+

+---------------------------+
| user_watch_wishlist       |
|---------------------------|
| user_id PK/FK -> users    |
| watch_id PK/FK -> watches |
| added_at                  |
+---------------------------+

+---------------------------+
| user_watch_cart           |
|---------------------------|
| id PK                     |
| user_id FK -> users       |
| watch_id FK -> watches    |
| quantity                  |
| added_at                  |
| UNIQUE(user_id, watch_id) |
+---------------------------+
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL (runtime), H2 (tests) |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Liquibase |
| Views | Thymeleaf + Spring Security Extras |
| API Docs | springdoc-openapi / Swagger UI |
| Testing | JUnit 5, Mockito, MockMvc |
| Containerization | Docker + docker compose |
| CI/CD | GitHub Actions + Render deploy hook |

## API Endpoints

| Method | Path | Auth | Role | Description |
|---|---|---|---|---|
| POST | `/api/auth/register` | Public | Public | Register a buyer or seller account |
| POST | `/api/auth/login` | Public | Public | Login and receive JWT |
| GET | `/api/watches` | Public | Public | Browse active watches with filters and pagination |
| GET | `/api/watches/{id}` | Public | Public | View watch details |
| POST | `/api/watches` | Required | SELLER | Create a watch listing |
| PUT | `/api/watches/{id}` | Required | SELLER or ADMIN | Update a watch listing |
| DELETE | `/api/watches/{id}` | Required | SELLER or ADMIN | Delete a watch listing |
| GET | `/api/watches/my` | Required | SELLER | List current seller watches |
| POST | `/api/orders` | Required | BUYER | Place an order |
| GET | `/api/orders/my` | Required | BUYER | List current buyer orders |
| GET | `/api/orders/seller` | Required | SELLER | List orders for current seller watches |
| GET | `/api/orders/{id}` | Required | Any authenticated | View order if authorized |
| PATCH | `/api/orders/{id}/status` | Required | Any authenticated | Update order status according to role rules |
| GET | `/api/orders` | Required | ADMIN | List all orders |
| POST | `/api/payments` | Required | BUYER | Prototype payment initiation and instant simulated approval |
| GET | `/api/payments/order/{orderId}` | Required | BUYER or ADMIN | Get payment by order |
| POST | `/api/payments/{id}/refund` | Required | ADMIN | Refund payment and cancel order |
| GET | `/api/payments` | Required | ADMIN | List all payments |
| GET | `/api/cart` | Required | BUYER | View cart |
| POST | `/api/cart/{watchId}` | Required | BUYER | Add a watch to cart |
| DELETE | `/api/cart/{watchId}` | Required | BUYER | Remove a cart item |
| DELETE | `/api/cart` | Required | BUYER | Clear cart |
| POST | `/api/cart/checkout` | Required | BUYER | Create orders from cart items |
| GET | `/api/wishlist` | Required | BUYER | View wishlist |
| POST | `/api/wishlist/{watchId}` | Required | BUYER | Add a watch to wishlist |
| DELETE | `/api/wishlist/{watchId}` | Required | BUYER | Remove from wishlist |
| POST | `/api/wishlist/{watchId}/move-to-cart` | Required | BUYER | Move item to cart |
| POST | `/api/reviews` | Required | BUYER | Create a review after delivery/completion |
| GET | `/api/reviews/watch/{watchId}` | Public | Public | List reviews for a watch |
| DELETE | `/api/reviews/{id}` | Required | ADMIN | Delete review |
| GET | `/api/admin/users` | Required | ADMIN | List users, optionally by role |
| PATCH | `/api/admin/users/{id}/toggle` | Required | ADMIN | Enable or disable a user |
| GET | `/api/admin/watches` | Required | ADMIN | List all watches or filter by status |
| GET | `/api/admin/watches/pending` | Required | ADMIN | List pending review watches |
| PATCH | `/api/admin/watches/{id}/approve` | Required | ADMIN | Approve listing |
| PATCH | `/api/admin/watches/{id}/reject` | Required | ADMIN | Reject listing |
| PATCH | `/api/admin/watches/{id}/deactivate` | Required | ADMIN | Deactivate listing |
| DELETE | `/api/admin/watches/{id}` | Required | ADMIN | Delete listing |
| GET | `/api/admin/orders` | Required | ADMIN | List all orders |
| DELETE | `/api/admin/reviews/{id}` | Required | ADMIN | Delete a review |
| GET | `/api/admin/stats` | Required | ADMIN | Marketplace dashboard statistics |

## Running the Project

1. Start everything:

```bash
docker compose up --build
```

2. Open the app:

```text
http://localhost:8080
```

3. Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Local Runtime Notes

- The app container waits for PostgreSQL health before startup.
- Liquibase creates the schema automatically.
- Bootstrap data is enabled by default through `BOOTSTRAP_ENABLED=true`.
- JWT is stored client-side in `localStorage` and mirrored to a regular cookie for page navigation convenience.

## CI/CD Pipeline

The GitHub Actions pipeline defined in `.github/workflows/ci-cd.yml` has two jobs:

1. **build-and-test**
   - checks out source code
   - installs Java 17 via Temurin
   - restores Maven cache
   - runs `mvn clean verify -B`
   - uploads Surefire reports even when tests fail

2. **deploy**
   - runs only for direct pushes to `main`
   - depends on successful build-and-test
   - triggers Render deployment through `RENDER_DEPLOY_HOOK_URL`

## Branch Strategy

```text
main        -> protected production branch
develop     -> integration branch for active work
feature/*   -> short-lived feature branches
hotfix/*    -> urgent fixes cut from main when needed
```

Recommended rules:

- no direct pushes to `main`
- pull requests required for `main`
- CI must pass before merge
- develop is used for staging integration
- feature branches are rebased or merged back into develop

## Render Deployment

### Suggested Render Services

- **Web Service** for the Spring Boot application
- **Managed PostgreSQL** for production persistence

### Build / Start Commands

```text
Build Command: mvn clean package -DskipTests
Start Command: java -jar target/timeless-marketplace-1.0.0.jar
```

### Environment Variables to Configure

| Variable | Example |
|---|---|
| `DB_URL` | `jdbc:postgresql://<render-db-host>:5432/timeless` |
| `DB_USER` | `timeless` |
| `DB_PASS` | `your-strong-password` |
| `JWT_SECRET` | `a-very-long-random-secret-at-least-32-characters` |
| `JWT_EXPIRY` | `86400000` |
| `APP_ADMIN_EMAIL` | `admin@timeless.com` |
| `APP_ADMIN_PASSWORD` | `Admin123!` |
| `BOOTSTRAP_ENABLED` | `true` or `false` |
| `SERVER_PORT` | `10000` if required by your Render setup |

### Deploy Flow

1. Push to `main`
2. GitHub Actions validates the build
3. Deploy job calls the Render deploy hook
4. Render rebuilds the application image / artifact
5. Service starts with the configured environment variables

## Running Tests

Run only the test suite:

```bash
mvn test
```

Run the full verification lifecycle:

```bash
mvn verify
```

## Seed Data Overview

When bootstrap is enabled and the database is empty, Timeless creates:

- 1 admin account
- 2 seller accounts
- 2 buyer accounts
- 8 active watch listings
- 2 completed demo orders for `buyer1@timeless.com`
- completed prototype payment records
- demo review data for showcase screens

## Security Notes

- passwords are stored with BCrypt
- JWT auth is stateless at the API layer
- method-level `@PreAuthorize` guards enforce role rules
- admins cannot self-register through the public registration endpoint
- payment processing is a **prototype simulation** and stores no real card data
