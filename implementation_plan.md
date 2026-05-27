# Implementation Plan — Spring Boot 3 Maven Project (Terimbere)

This plan details the technical steps to generate, build, test, and document a robust Spring Boot 3 REST API for the **Terimbere Budget Management System** using Java 17, Maven, PostgreSQL, and Swagger UI.

---

## Goal Description

Establish a clean, complete, and production-ready Spring Boot backend for Terimbere. The application will be written using **Java 17**, utilizing **Maven** (via a self-bundled Maven Wrapper `mvnw`), **PostgreSQL** as the primary storage database, **Spring Security + JWT** for user login/sessions, and **Springdoc OpenAPI (Swagger UI)** for interactive API documentation. A comprehensive suite of unit and integration tests will be developed to verify correctness.

---

## User Review Required

Please review the following configuration parameters before we begin generating code:

> [!IMPORTANT]
> **PostgreSQL Configuration Details**
> *   **Database Name**: We will assume a database named `terimbere_db`. You will need to create this in your PostgreSQL instance before running the app (`CREATE DATABASE terimbere_db;`).
> *   **Username**: We will default the connection user to `postgres`. Let us know if your PostgreSQL username is different.
> *   **Password**: Configured to `noah` as requested.
> *   **Port**: Default PostgreSQL port `5432`.

> [!TIP]
> **Testing Strategy**
> *   We will configure **H2 (In-Memory Database)** for test phases. This allows you to run `./mvnw test` locally without needing PostgreSQL active, preventing tests from corrupting or mutating your production database.

---

## Proposed Changes

We will construct the Maven directory structure and place key files under `c:\Users\HP\Desktop\ProductionProjects\Terimbere`.

```
Terimbere
├── pom.xml                               # Maven project configuration
├── mvnw                                  # Maven wrapper execution script (Linux/macOS)
├── mvnw.cmd                              # Maven wrapper execution script (Windows)
├── .mvn/wrapper/maven-wrapper.properties # Configures wrapper binaries
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/terimbere/budget
│   │   │       ├── TerimbereApplication.java
│   │   │       ├── config/               # Security, OpenAPI, Cors Configs
│   │   │       ├── controller/           # REST Controllers
│   │   │       ├── model/                # JPA Entities
│   │   │       ├── repository/           # JPA Repositories
│   │   │       ├── service/              # Core business services
│   │   │       ├── dto/                  # Data Transfer Objects
│   │   │       ├── security/             # JWT filters, providers
│   │   │       └── exception/            # Central exception handling
│   │   └── resources
│   │       ├── application.yml           # App properties & credentials
│   │       └── schema.sql                # Database schema fallback initialization
│   └── test
│       └── java
│           └── com/terimbere/budget      # Unit & Integration tests
```

---

### Key Components to Implement

#### 1. Project Build Configuration (`pom.xml`)
We will configure dependencies:
*   `spring-boot-starter-web` & `spring-boot-starter-validation`
*   `spring-boot-starter-data-jpa` (Hibernate ORM)
*   `spring-boot-starter-security`
*   `postgresql` (JDBC Driver)
*   `springdoc-openapi-starter-webmvc-ui` (v2.5.0 for Swagger 3 UI)
*   `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (v0.12.5 for JWT Token authentication)
*   `lombok` (reducing boilerplate code)
*   `h2` (In-memory DB for isolated unit testing)
*   `spring-boot-starter-test` & `spring-security-test`

#### 2. Stateless Auth (`security` & `controller`)
*   Create registration (`/api/auth/register`) and login (`/api/auth/login`) endpoints.
*   Implement JWT verification filter inside Spring Security config to block unauthorized access to financial data.

#### 3. Core Module Services & CRUDs
*   **Budgeting**: CRUD for named budgets and custom entries (surplus/deficit calculations).
*   **Debts Portal**: CRUD for debtors/creditors, partial payments updating remaining balances, and contract meta linking.
*   **Bills Management**: Track recurring and one-time utility obligations, paid/unpaid status.
*   **Income Planner**: Strategize target amounts and track active pipelines.
*   **Notification Center**: Pull feed events and schedule future alerts.
*   **Downloads / Reports**: Excel export via Apache POI and PDF export.

#### 4. OpenAPI / Swagger Setup
*   Configure OpenAPI meta-information bean to secure endpoints inside the UI (adds a JWT padlock authorization button on the Swagger Dashboard).

---

## Verification Plan

### Automated Verification
We will run terminal commands inside the workspace:
1.  **Code Compilation**:
    ```powershell
    ./mvnw clean compile
    ```
2.  **Test Executions**: Run isolated unit tests for entities, calculators, and controllers using:
    ```powershell
    ./mvnw test
    ```

### Manual Verification
1.  **Running the Service**:
    ```powershell
    ./mvnw spring-boot:run
    ```
2.  **Accessing Swagger UI**: Open the browser to see the interactive documentation and try out API endpoints at:
    ```
    http://localhost:8080/swagger-ui/index.html
    ```
3.  **PostgreSQL Verification**: Log in to PostgreSQL to confirm tables are created by JPA Hibernate automatically when the application boots up.
