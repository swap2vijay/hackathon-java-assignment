# Java Hackathon Assignment - 6 Hour Edition

This is a **hackathon-style code assignment** designed to be completed in approximately **6 hours**. It covers API design, persistence, testing patterns, and transaction management in a real-world Quarkus application.

## Quick Start

```bash
# 1. Start the application in dev mode
./mvnw quarkus:dev

# 2. Access Swagger UI
open http://localhost:8080/q/swagger-ui

# 3. Run tests
./mvnw test

# 4. Compile and package
./mvnw package
```

## Before You Begin

Read [BRIEFING.md](BRIEFING.md) for domain context, then [CODE_ASSIGNMENT.md](CODE_ASSIGNMENT.md) for your tasks.

---

## Architecture

This codebase follows **Hexagonal Architecture** (Ports & Adapters) with:

- Domain use cases isolated from REST and database concerns
- CDI events for post-commit integration calls
- OpenAPI-generated REST layer for the Warehouse API
- Hand-coded REST endpoints for Stores and Products

---

## Technologies

- **Java 17+**
- **Quarkus 3.13.3**
- **PostgreSQL** (via Docker or Quarkus Dev Services)
- **JUnit 5** + **Testcontainers** + **Mockito**
- **OpenAPI** (code generation for Warehouse API)

---

## API Reference

### Warehouse Endpoints

> Defined via OpenAPI spec (`warehouse-openapi.yaml`), code-generated.

| Method | Path | Description | Status Codes |
|--------|------|-------------|--------------|
| `GET` | `/warehouse` | List all warehouse units | `200` |
| `POST` | `/warehouse` | Create a new warehouse unit | `201`, `400` |
| `GET` | `/warehouse/{id}` | Get warehouse by business unit code | `200`, `404` |
| `DELETE` | `/warehouse/{id}` | Archive a warehouse unit | `204`, `400`, `404` |
| `POST` | `/warehouse/{businessUnitCode}/replacement` | Replace an active warehouse | `200`, `400`, `404` |

**Example requests:**

```bash
# List all warehouses
curl http://localhost:8080/warehouse

# Create a warehouse
curl -X POST http://localhost:8080/warehouse \
  -H "Content-Type: application/json" \
  -d '{
    "businessUnitCode": "WH-001",
    "location": "AMSTERDAM-001",
    "capacity": 50,
    "stock": 10
  }'

# Get a warehouse by business unit code
curl http://localhost:8080/warehouse/WH-001

# Archive a warehouse
curl -X DELETE http://localhost:8080/warehouse/WH-001

# Replace a warehouse
curl -X POST http://localhost:8080/warehouse/WH-001/replacement \
  -H "Content-Type: application/json" \
  -d '{
    "location": "ZWOLLE-001",
    "capacity": 30,
    "stock": 15
  }'
```

### Store Endpoints

> Hand-coded REST resource. Store mutations fire CDI events for legacy system synchronization.

| Method | Path | Description | Status Codes |
|--------|------|-------------|--------------|
| `GET` | `/store` | List all stores (sorted by name) | `200` |
| `GET` | `/store/{id}` | Get store by ID | `200`, `404` |
| `POST` | `/store` | Create a new store | `201`, `422` |
| `PUT` | `/store/{id}` | Fully update a store | `200`, `404`, `422` |
| `PATCH` | `/store/{id}` | Partially update a store | `200`, `404`, `422` |
| `DELETE` | `/store/{id}` | Delete a store | `204`, `404` |

**Example requests:**

```bash
# List all stores
curl http://localhost:8080/store

# Get a store by ID
curl http://localhost:8080/store/1

# Create a store
curl -X POST http://localhost:8080/store \
  -H "Content-Type: application/json" \
  -d '{"name": "NEW-STORE", "quantityProductsInStock": 25}'

# Update a store
curl -X PUT http://localhost:8080/store/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "UPDATED-STORE", "quantityProductsInStock": 50}'

# Patch a store
curl -X PATCH http://localhost:8080/store/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "PATCHED-STORE", "quantityProductsInStock": 30}'

# Delete a store
curl -X DELETE http://localhost:8080/store/1
```

### Product Endpoints

> Hand-coded REST resource with standard CRUD operations.

| Method | Path | Description | Status Codes |
|--------|------|-------------|--------------|
| `GET` | `/product` | List all products (sorted by name) | `200` |
| `GET` | `/product/{id}` | Get product by ID | `200`, `404` |
| `POST` | `/product` | Create a new product | `201`, `422` |
| `PUT` | `/product/{id}` | Update a product | `200`, `404`, `422` |
| `DELETE` | `/product/{id}` | Delete a product | `204`, `404` |

**Example requests:**

```bash
# List all products
curl http://localhost:8080/product

# Get a product by ID
curl http://localhost:8080/product/1

# Create a product
curl -X POST http://localhost:8080/product \
  -H "Content-Type: application/json" \
  -d '{"name": "NEW-PRODUCT", "stock": 100}'

# Update a product
curl -X PUT http://localhost:8080/product/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "UPDATED-PRODUCT", "description": "A fine product", "price": 29.99, "stock": 75}'

# Delete a product
curl -X DELETE http://localhost:8080/product/1
```

---

## Running the Code

```bash
# Compile and run tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Run integration tests (concurrency + testcontainers)
./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT

# Start development mode
./mvnw quarkus:dev

# Access Swagger UI
open http://localhost:8080/q/swagger-ui
```

### (Optional) Run in JVM mode

First compile:

```bash
./mvnw package
```

Start a PostgreSQL instance:

```bash
docker run -it --rm=true --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

Then run:

```bash
java -jar ./target/quarkus-app/quarkus-run.jar
```

Navigate to <http://localhost:8080/index.html>

---

## Database Configuration

The application uses profile-based datasource configuration:

| Profile | Database | Details |
|---------|----------|---------|
| `%prod` | PostgreSQL | Connects to `localhost:15432`, requires a running PostgreSQL instance |
| `%dev` | H2 (in-memory) | No external dependencies needed, works out of the box |
| `%test` | H2 (in-memory) | Used by `./mvnw test`, no setup required |

To switch dev mode to PostgreSQL (requires Docker):

```properties
# In application.properties, replace the %dev lines with:
%dev.quarkus.datasource.db-kind=postgresql
%dev.quarkus.datasource.devservices.enabled=true
```

Quarkus Dev Services will automatically start a PostgreSQL container when running `./mvnw quarkus:dev`.

---

## Available Locations

| Identifier | Max Warehouses | Max Capacity |
|---|---|---|
| ZWOLLE-001 | 1 | 40 |
| ZWOLLE-002 | 2 | 50 |
| AMSTERDAM-001 | 5 | 100 |
| AMSTERDAM-002 | 3 | 75 |
| TILBURG-001 | 1 | 40 |
| HELMOND-001 | 1 | 45 |
| EINDHOVEN-001 | 2 | 70 |
| VETSBY-001 | 1 | 90 |

---

**Good luck and have fun!** This is about demonstrating your understanding of production-grade patterns, not just writing code under pressure.
