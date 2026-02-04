# Inventory Service

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/license/mit)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-12+-blue.svg)](https://www.postgresql.org/)

A comprehensive Spring Boot microservice for managing inventory items and their units of measure within the Elara application ecosystem. This service handles inventory lifecycle, integrates with the unit-of-measure service, and provides robust RESTful APIs for inventory management in restaurant operations.

## 📚 Documentation

- **Deep Wiki**: [Project Documentation](https://deepwiki.com/elara-app/inventory-service)
- **Build Process & Architecture Decisions**: [Step-by-step Construction Guide](https://bit.ly/4a0ZITO)

## 🎯 Overview

The Inventory Service is a critical component of the Elara application ecosystem, designed to manage inventory items, their quantities, costs, and relationships with units of measure. Built with Spring Boot 3.5.6 and Java 21, it follows microservices architecture principles and integrates seamlessly with other services through Spring Cloud components.

### Key Features

- **Inventory Item Management**: Full CRUD operations for inventory items
- **Unit of Measure Integration**: Integration with external UOM service for standardized measurements
- **Advanced Search**: Search and filter inventory items by name with pagination support
- **Cost Management**: Track standard costs and purchase unit conversions
- **Reorder Point Management**: Automated tracking of reorder point quantities
- **RESTful API**: Well-documented REST endpoints following industry best practices
- **Service Discovery**: Integration with Netflix Eureka for service registration and discovery
- **Centralized Configuration**: Integration with Spring Cloud Config Server and HashiCorp Vault
- **Event Bus**: RabbitMQ integration for distributed event-driven architecture
- **Validation**: Comprehensive input validation using Jakarta Bean Validation
- **Exception Handling**: Centralized exception handling with detailed error responses
- **Health Monitoring**: Spring Boot Actuator endpoints for application health and metrics

## 🏗️ Architecture

This service follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│                 Controller Layer                     │
│          (REST API Endpoints & Validation)           │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│                 Service Layer                        │
│         (Business Logic & Orchestration)             │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│               Repository Layer                       │
│          (Data Access & Persistence)                 │
└───────────────────┬─────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────┐
│              PostgreSQL Database                     │
└─────────────────────────────────────────────────────┘
```

### Integration Points

- **UOM Service**: Validates unit of measure references
- **Config Server**: Centralized configuration management
- **Eureka Server**: Service discovery and registration
- **HashiCorp Vault**: Secure secrets management
- **RabbitMQ**: Event-driven messaging and configuration refresh

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot**: 3.5.6
- **Java**: 21
- **Build Tool**: Maven

### Spring Modules
- **Spring Web**: RESTful web services
- **Spring Data JPA**: Data persistence and ORM
- **Spring Validation**: Bean validation
- **Spring Boot Actuator**: Monitoring and health checks

### Cloud & Microservices
- **Spring Cloud**: 2025.0.0
- **Netflix Eureka Client**: Service discovery
- **Spring Cloud Config**: Centralized configuration
- **Spring Cloud Vault**: Secrets management
- **Spring Cloud Bus**: Configuration change propagation (RabbitMQ)

### Database
- **PostgreSQL**: 12+ (Production database)
- **PostgreSQL JDBC Driver**: 42.7.7
- **JPA/Hibernate**: ORM layer

### Development Tools
- **Lombok**: 1.18.38 - Reduces boilerplate code
- **MapStruct**: 1.6.3 - Type-safe bean mapping

## 📋 Prerequisites

Before running this service, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 21 or higher
- **Maven**: Version 3.6+ (or use included Maven wrapper)
- **PostgreSQL**: Version 12+ (for production)
- **Docker** (Optional): For containerized deployment
- **Git**: For version control

### Required External Services

The following services must be running for full functionality:

1. **Spring Cloud Config Server** (default: `http://localhost:8888`)
2. **HashiCorp Vault** (default: `http://localhost:8200`)
3. **Netflix Eureka Server** (for service discovery)
4. **RabbitMQ** (for Spring Cloud Bus)
5. **PostgreSQL Database** (production environment)
6. **Unit of Measure (UOM) Service** (for UOM validation)

## 🚀 Getting Started

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/elara-app/inventory-service.git
   cd inventory-service
   ```

2. **Build the project**:
   ```bash
   # Using Maven wrapper (recommended)
   ./mvnw clean install
   
   # Or using Maven
   mvn clean install
   ```

### Configuration

The service uses a layered configuration approach:

#### 1. Application Configuration (`application.yml`)
```yaml
spring:
  application:
    name: inventory-service
  config:
    import: configserver:http://localhost:8888
  profiles:
    active: dev
```

#### 2. Development Configuration (`application-dev.yml`)
```yaml
spring:
  config:
    import: vault://secret/inventory-service/dev
  cloud:
    vault:
      uri: http://localhost:8200
      authentication: TOKEN
      token: 00000000-0000-0000-0000-000000000000
```

#### 3. Required Vault Secrets

Store the following in Vault at `secret/inventory-service/dev`:

```json
{
  "spring.datasource.url": "jdbc:postgresql://localhost:5432/inventory_db",
  "spring.datasource.username": "your_username",
  "spring.datasource.password": "your_password",
  "eureka.client.service-url.defaultZone": "http://localhost:8761/eureka/"
}
```

### Running the Application

#### Development Mode

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Using Maven
mvn spring-boot:run
```

#### Production Mode

```bash
# Build JAR
./mvnw clean package

# Run JAR
java -jar target/inventory-service-1.2.jar
```

#### Using Docker

```bash
# Build Docker image
docker build -t elara/inventory-service:1.2 .

# Run container
docker run -p 8080:8080 elara/inventory-service:1.2
```

The service will start on the default port (typically 8080, configured in Config Server).

## 📖 API Documentation

### Base URL
```
http://localhost:8080/item/
```

### Endpoints

#### Create Inventory Item
```http
POST /item/
Content-Type: application/json

{
  "name": "Tomato",
  "description": "Fresh red tomatoes",
  "baseUnitOfMeasureId": 1,
  "standardCost": 2.50,
  "unitPerPurchaseUom": 1.0,
  "reorderPointQuantity": 50.0
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "name": "Tomato",
  "description": "Fresh red tomatoes",
  "baseUnitOfMeasure": {
    "id": 1,
    "code": "KG",
    "name": "Kilogram"
  },
  "standardCost": 2.50,
  "unitPerPurchaseUom": 1.0,
  "reorderPointQuantity": 50.0
}
```

#### Get Inventory Item by ID
```http
GET /item/{id}
```

**Response**: `200 OK`

#### Get All Inventory Items (Paginated)
```http
GET /item/?page=0&size=20&sort=name
```

**Response**: `200 OK` with paginated results

#### Search Inventory Items by Name
```http
GET /item/search?name=Tomato
```

**Response**: `200 OK` with filtered results

#### Check if Name is Taken
```http
GET /item/check-name?name=Tomato
```

**Response**: `200 OK` with boolean value

#### Update Inventory Item
```http
PUT /item/{id}
Content-Type: application/json

{
  "name": "Organic Tomato",
  "description": "Fresh organic red tomatoes",
  "standardCost": 3.00
}
```

**Response**: `200 OK`

#### Delete Inventory Item
```http
DELETE /item/{id}
```

**Response**: `204 No Content`

### Error Responses

The API uses standardized error responses:

```json
{
  "timestamp": "2026-02-04T20:38:04.279Z",
  "status": 404,
  "error": "Not Found",
  "message": "Inventory item not found with id: 999",
  "path": "/item/999"
}
```

## 🧪 Testing

### Run All Tests
```bash
./mvnw test
```

### Run Tests with Coverage
```bash
./mvnw verify
```

### Test Structure
- **Unit Tests**: Service layer business logic
- **Integration Tests**: Controller and repository layers
- **Validation Tests**: Input validation scenarios

## 📁 Project Structure

```
inventory-service/
├── src/
│   ├── main/
│   │   ├── java/com/elara/app/inventory_service/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── AppConfig.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── controller/          # REST controllers
│   │   │   │   └── InventoryItemController.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   ├── response/
│   │   │   │   └── update/
│   │   │   ├── exceptions/          # Custom exceptions
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   │   └── InventoryItemMapper.java
│   │   │   ├── model/               # JPA entities
│   │   │   │   └── InventoryItem.java
│   │   │   ├── repository/          # Spring Data repositories
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── interfaces/
│   │   │   │   └── imp/
│   │   │   ├── utils/               # Utility classes
│   │   │   └── InventoryServiceApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── messages.properties
│   └── test/
│       └── java/                    # Test classes
├── .gitignore
├── mvnw                             # Maven wrapper
├── mvnw.cmd
├── pom.xml                          # Maven configuration
└── README.md
```

## 🔗 Dependencies

### Core Dependencies
- **spring-boot-starter-web**: RESTful web services
- **spring-boot-starter-data-jpa**: JPA/Hibernate ORM
- **spring-boot-starter-validation**: Bean validation
- **spring-boot-starter-actuator**: Health checks and metrics

### Cloud Dependencies
- **spring-cloud-starter-netflix-eureka-client**: Service discovery
- **spring-cloud-starter-config**: Centralized configuration
- **spring-cloud-starter-vault-config**: Secrets management
- **spring-cloud-starter-bus-amqp**: Event bus with RabbitMQ

### Database
- **postgresql**: PostgreSQL JDBC driver

### Development Tools
- **lombok**: Code generation
- **mapstruct**: Bean mapping
- **spring-boot-starter-test**: Testing framework

## 🏢 Microservices Ecosystem

This service is part of the **Elara Application Ecosystem**, which includes:

- **Inventory Service** (this service) - Inventory management
- **Unit of Measure Service** - UOM definitions and conversions
- **Config Server** - Centralized configuration
- **Eureka Server** - Service discovery
- **API Gateway** - Routing and load balancing

## 🔐 Security Considerations

1. **Secrets Management**: All sensitive configuration stored in HashiCorp Vault
2. **Validation**: Input validation on all endpoints using Jakarta Bean Validation
3. **Exception Handling**: Detailed error responses without exposing sensitive information
4. **Database**: Prepared statements via JPA to prevent SQL injection

## 📊 Monitoring & Health

The service exposes Actuator endpoints for monitoring:

- **Health**: `/actuator/health` - Application health status
- **Info**: `/actuator/info` - Application information
- **Metrics**: `/actuator/metrics` - Application metrics

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/your-feature-name`
3. **Commit your changes**: `git commit -m 'Add some feature'`
4. **Push to the branch**: `git push origin feature/your-feature-name`
5. **Open a Pull Request**

### Coding Standards
- Follow Java coding conventions
- Write meaningful commit messages
- Include unit tests for new features
- Update documentation as needed
- Use Lombok annotations to reduce boilerplate
- Follow REST API design best practices

## 📝 Best Practices Implemented

### Code Quality
- **Clean Architecture**: Clear separation of concerns
- **SOLID Principles**: Single responsibility, dependency injection
- **DRY Principle**: Reusable components and utilities
- **Validation**: Comprehensive input validation at multiple layers
- **Exception Handling**: Centralized error handling with meaningful messages

### Spring Boot Best Practices
- **Spring Data JPA**: Repository pattern for data access
- **DTO Pattern**: Separation of entity and API models using MapStruct
- **Pagination**: Built-in support for paginated queries
- **Logging**: Structured logging with SLF4J
- **Configuration**: Externalized configuration with profiles

### Microservices Patterns
- **Service Discovery**: Automatic service registration with Eureka
- **Centralized Configuration**: Config server for environment-specific settings
- **Health Checks**: Actuator endpoints for monitoring
- **API Versioning**: Versioned artifact for backward compatibility

### Database Design
- **Entity Validation**: Database-level and application-level constraints
- **Optimistic Locking**: Preventing concurrent update conflicts
- **Indexes**: Optimized queries with proper indexing
- **Foreign Keys**: Referential integrity with external services

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](https://opensource.org/license/mit) for details.

```
MIT License

Copyright (c) 2026 Elara App

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## 📞 Contact & Support

- **Developer**: Julian Bermudez
- **Email**: julianbetov@gmail.com
- **GitHub**: [@julianbetov](https://github.com/julianbetov)
- **Repository**: [elara-app/inventory-service](https://github.com/elara-app/inventory-service)

## 🔗 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Project Deep Wiki](https://deepwiki.com/elara-app/inventory-service)
- [Architecture & Design Decisions](https://bit.ly/4a0ZITO)

---

**Version**: 1.2  
**Last Updated**: February 2026  
**Status**: Active Development

Made with ❤️ by the Elara Team
