# Payment Web Service

A Spring Boot REST API for customer management, account operations, and payment processing via external Payment Service Providers (PSP). Currently integrates with **Stripe** for card and SEPA payment flows.

## Overview

This service provides a complete payment lifecycle: creating customers, managing their accounts with balance tracking, registering customers at a PSP, and executing card or SEPA payments with concurrency-safe locking and Kafka-based notifications.

## Tech Stack

- **Java 17+** with **Spring Boot 3.x**
- **Spring Data JPA / Hibernate** for persistence
- **PostgreSQL** (production) / **H2** (tests)
- **Stripe Java SDK** for payment processing
- **Apache Kafka** for payment event notifications
- **Redis** (via LockManager) for distributed locking on payment operations
- **ModelMapper** for entity/DTO mapping
- **Swagger / OpenAPI 3** for API documentation
- **JUnit 5 + MockMvc + Mockito** for integration testing

## Project Structure

```
com.alpian.paymentws
├── controller
│   ├── CustomerController          # Customer CRUD endpoints
│   ├── CustomerAccountController   # Account creation and balance operations
│   └── PaymentController           # PSP registration and payment endpoints
├── service
│   ├── CustomerService             # Customer business logic
│   ├── CustomerAccountService      # Account balance management
│   ├── PaymentService              # Payment orchestration with locking
│   ├── NotificationService         # Kafka event publishing
│   └── psp
│       ├── ProviderPaymentService  # PSP interface
│       ├── StripePaymentService    # Stripe implementation
│       ├── PaymentServiceFactory   # PSP provider resolution
│       └── CreatePaymentIntentResult
├── domain
│   ├── Customer                    # Customer entity
│   ├── CustomerAccount             # Account entity with balance
│   ├── PaymentIntent               # Payment intent entity
│   ├── PaymentRequest              # Outbound PSP request log
│   ├── PaymentResponse             # Inbound PSP webhook/response log
│   └── PaymentIntentStatus         # CREATED, SUCCEEDED, FAILED, ...
├── dto
│   ├── CustomerDTO
│   ├── CustomerAccountDTO
│   ├── CreateCardPaymentDTO
│   ├── CreateSEPAPaymentDTO
│   ├── PaymentIntentDTO
│   └── PaymentNotificationDTO
├── exception
│   ├── CustomerNotFoundException
│   ├── CustomerNotAttachedException
│   ├── CustomerAlreadyAttachedException
│   ├── CustomerAccountNotFoundException
│   ├── CustomerAccountAlreadyExistsException
│   ├── InsufficientBalanceException
│   ├── PaymentAmountException
│   └── PaymentProviderMissingException
├── repository
│   ├── CustomerRepository
│   ├── CustomerAccountRepository
│   └── PaymentIntentRepository
└── util
    └── LockManager                 # Distributed lock abstraction (Redis-backed)
```

## API Endpoints

### Customers — `/api/customers`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/customers` | Create a new customer |
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get customer by ID |
| GET | `/api/customers/search?firstName=...&lastName=...` | Search by name |
| PUT | `/api/customers/{id}` | Update a customer |
| DELETE | `/api/customers/{id}` | Delete a customer |

### Customer Accounts — `/api/accounts`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/accounts/customer/{id}` | Create account for customer (one per customer) |
| PUT | `/api/accounts/customer/{id}/add/{amount}` | Add funds to customer account |

### Payments — `/api/payments`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/payments/customer/{id}/register` | Register customer at PSP (Stripe) |
| POST | `/api/payments/customer/{id}/payCard` | Create a card payment intent |
| POST | `/api/payments/customer/{id}/paySepa` | Create a SEPA payment intent |

## Configuration

### Required Environment Variables

| Variable | Description |
|----------|-------------|
| `STRIPE_API_KEY` | Stripe secret API key |

### Application Properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/paymentws
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

# PSP provider
payment.provider=Stripe

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
```

## Payment Flow

### Card Payment

1. Customer must exist and be registered at the PSP (`/register` endpoint).
2. A `POST /payCard` request triggers a distributed lock keyed by `customer_id + amount`.
3. If no existing payment intent is found for that key, the service calls Stripe to create a PaymentIntent.
4. The PaymentIntent is persisted, and a Kafka notification is published.
5. Duplicate concurrent requests with the same customer and amount are deduplicated by the lock — only one PSP call is made.

### SEPA Payment

1. Customer must exist, be registered at the PSP, and have a funded account.
2. A `POST /paySepa` request first validates the account balance is sufficient.
3. A distributed lock keyed by `customer_id + destination_iban + amount` prevents duplicate processing.
4. The service calls Stripe to create a SEPA PaymentIntent, persists the result, deducts the balance, and publishes a notification.
5. Balance deduction uses `SELECT ... FOR UPDATE` (pessimistic locking) to prevent lost-update race conditions under concurrent access.

## Running Locally

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL
- Redis (for distributed locking)
- Kafka broker on `localhost:9092`

### Build and Run

```bash
# Build
mvn clean package

# Run
export STRIPE_API_KEY=sk_test_...
mvn spring-boot:run
```

### API Documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

## Testing

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=PaymentControllerIntegrationTest
```

### Test Strategy

The project uses **integration tests** with `@SpringBootTest` and `MockMvc` that exercise the full request pipeline (validation, serialization, service, repository). External dependencies are isolated:

- **PSP (Stripe)** — mocked via `@MockitoBean` on `PaymentServiceFactory`
- **Kafka** — mocked via `@MockitoBean` on `NotificationService`
- **Database** — H2 in-memory (test profile)

Concurrency tests verify that the `LockManager` correctly deduplicates simultaneous payment requests, ensuring only one PSP call, one balance deduction, and one notification per unique payment.

## License

Proprietary — Alpian SA. All rights reserved.
