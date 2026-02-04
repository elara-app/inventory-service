# Inventory Service

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![Code Coverage](https://img.shields.io/badge/Coverage-80%25-brightgreen.svg)](https://github.com/elara-app/inventory-service)

> A professional Spring Boot microservice that manages inventory items and their integration with units of measure within the Elara application ecosystem.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture & Technology Stack](#architecture--technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [Usage](#usage)
  - [API Documentation](#api-documentation)
  - [API Endpoints](#api-endpoints)
- [Development](#development)
  - [Building the Project](#building-the-project)
  - [Running Tests](#running-tests)
  - [Code Quality & Coverage](#code-quality--coverage)
- [Deployment](#deployment)
- [Project Structure](#project-structure)
- [Best Practices & Design Decisions](#best-practices--design-decisions)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)
- [Contact & Support](#contact--support)

---

## 🎯 Overview

The **Inventory Service** is a critical microservice within the Elara application ecosystem, designed to provide centralized management of inventory items and their lifecycle. This service enables restaurant operations to track inventory items, their costs, quantities, and relationships with units of measure across the platform.

### Why This Service Matters

In enterprise restaurant applications, managing inventory consistently is crucial for:
- **Operational Efficiency**: Ensuring accurate tracking of inventory items across multiple locations
- **Cost Control**: Monitoring standard costs and purchase unit conversions for better financial management
- **Supply Chain Management**: Automated reorder point tracking to prevent stockouts
- **Data Consistency**: Maintaining standardized inventory data across all restaurant services
- **Integration**: Seamless integration with Unit of Measure service for measurement standardization

---

## ✨ Key Features

### Core Functionality
- **📦 Inventory Item Management**: Full CRUD operations for managing inventory items
- **🔗 Unit of Measure Integration**: Seamless integration with UOM service for standardized measurements
- **💰 Cost Management**: Track standard costs and purchase unit conversions accurately
- **📊 Reorder Point Management**: Automated tracking of reorder point quantities to prevent stockouts
- **🔍 Advanced Search & Filtering**: Search by name with pagination support
- **✅ Data Validation**: Comprehensive input validation with detailed error messages

### Technical Features
- **🚀 RESTful API**: Well-documented REST endpoints following industry best practices
- **📊 Pagination Support**: Efficient data retrieval with Spring Data pagination
- **🔐 Configuration Management**: Integration with Spring Cloud Config for centralized configuration
- **🔒 Secure Secrets Management**: HashiCorp Vault integration for sensitive data
- **📈 Service Discovery**: Netflix Eureka client for microservice registration
- **💬 Event Bus**: RabbitMQ integration for asynchronous communication
- **📝 API Documentation**: Interactive Swagger/OpenAPI documentation
- **🏥 Health Monitoring**: Spring Boot Actuator for health checks and metrics
- **🧪 High Test Coverage**: 80%+ code coverage with comprehensive unit and integration tests
- **🎯 Exception Handling**: Centralized error handling with meaningful HTTP status codes

---

## 🏗️ Architecture & Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 | Modern programming language with advanced features |
| **Spring Boot** | 3.5.6 | Enterprise application framework |
| **Spring Data JPA** | 3.5.6 | Data access and persistence layer |
| **PostgreSQL** | 42.7.7 | Production-ready relational database |
| **Maven** | 3.8+ | Build and dependency management |

### Spring Cloud Stack

| Component | Purpose |
|-----------|---------|
| **Spring Cloud Config** | Externalized configuration management |
| **Netflix Eureka Client** | Service discovery and registration |
| **HashiCorp Vault** | Secure secrets and credentials management |
| **Spring Cloud Bus (RabbitMQ)** | Distributed messaging and configuration refresh |

### Supporting Libraries

| Library | Purpose |
|---------|---------|
| **Lombok** | Reduces boilerplate code with annotations |
| **MapStruct** | Type-safe bean mapping |
| **Jakarta Validation** | Bean validation framework |
| **Spring Boot Actuator** | Production-ready monitoring features |

### Architecture Principles

This service follows these architectural principles:
- **Microservice Architecture**: Independently deployable and scalable service
- **Separation of Concerns**: Clear separation between controller, service, repository, and model layers
- **RESTful Design**: Resource-oriented API design with proper HTTP methods and status codes
- **Domain-Driven Design**: Entity models representing business domain concepts
- **SOLID Principles**: Maintainable and extensible code structure
- **12-Factor App**: Cloud-native application design principles

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 21** or higher
  ```bash
  java -version  # Should show version 21+
  ```
- **Apache Maven 3.8+** (or use the included Maven wrapper)
  ```bash
  mvn -version
  ```
- **PostgreSQL 12+** (for production use)
- **Docker** (optional, for containerized dependencies)
- **Git** for version control

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/elara-app/inventory-service.git
   cd inventory-service
   ```

2. **Build the project**
   ```bash
   # Using Maven wrapper (recommended)
   ./mvnw clean install
   
   # Or using system Maven
   mvn clean install
   ```

3. **Run the application**
   ```bash
   # Using Maven Spring Boot plugin
   ./mvnw spring-boot:run
   
   # Or run the JAR directly
   java -jar target/inventory-service-1.2.jar
   ```

The service will start on port `8080` by default (configurable via `application.yml`).

### Configuration

The service uses **Spring Cloud Config** for externalized configuration. Configuration is organized by environment profiles:

#### Application Profiles

- **dev**: Development environment
- **test**: Testing environment
- **prod**: Production environment (configured via Config Server)

#### Local Development Setup

For local development, ensure you have:

1. **Spring Cloud Config Server** running on `http://localhost:8888`
2. **HashiCorp Vault** running on `http://localhost:8200` (for dev profile)
3. **Database Connection** configured in Vault or Config Server:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/inventory_db
       username: your_username
       password: your_password
   ```

#### Environment Variables

Key environment variables you may need to configure:

```bash
SPRING_PROFILES_ACTIVE=dev          # Active profile
SPRING_CLOUD_CONFIG_URI=http://...  # Config server URL
VAULT_TOKEN=your-vault-token        # Vault authentication token
DATABASE_URL=jdbc:postgresql://...  # Database connection string
```

---

## 📖 Usage

### API Documentation

Once the service is running, access the interactive API documentation:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### API Endpoints

#### Inventory Item Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/item/` | Create a new inventory item |
| `GET` | `/item/{id}` | Get inventory item by ID |
| `GET` | `/item/` | Get all inventory items (paginated) |
| `GET` | `/item/search?name={name}` | Search items by name |
| `GET` | `/item/check-name?name={name}` | Check if name is available |
| `PUT` | `/item/{id}` | Update an inventory item |
| `DELETE` | `/item/{id}` | Delete an inventory item |

#### Example Request: Create an Inventory Item

```bash
curl -X POST http://localhost:8080/item/ \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tomato",
    "description": "Fresh red tomatoes",
    "baseUnitOfMeasureId": 1,
    "standardCost": 2.50,
    "unitPerPurchaseUom": 1.0,
    "reorderPointQuantity": 50.0
  }'
```

#### Example Response

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

---

## 🛠️ Development

### Building the Project

Build the project without running tests:
```bash
./mvnw clean package -DskipTests
```

Build with full test suite:
```bash
./mvnw clean install
```

### Running Tests

Run all tests:
```bash
./mvnw test
```

Run specific test class:
```bash
./mvnw test -Dtest=InventoryServiceTest
```

Run integration tests:
```bash
./mvnw verify
```

### Code Quality & Coverage

#### Generate Code Coverage Report
```bash
./mvnw clean test jacoco:report
```

View the coverage report at: `target/site/jacoco/index.html`

**Coverage Requirements:**
- Line Coverage: ≥ 80%
- Branch Coverage: ≥ 70%
- All classes must have coverage (except DTOs and main application class)

#### Run SonarQube Analysis

Start SonarQube server (if using Docker):
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:latest
```

Run analysis:
```bash
./mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=com.elara.app:inventory-service \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token
```

### Code Style

This project follows standard Java coding conventions:
- Use meaningful variable and method names
- Maximum line length: 120 characters
- Use Lombok annotations to reduce boilerplate
- Document public APIs with Javadoc
- Follow REST API naming conventions

---

## 🚢 Deployment

### Docker Deployment

Build Docker image:
```bash
docker build -t elara-app/inventory-service:1.2 .
```

Run container:
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CLOUD_CONFIG_URI=http://config-server:8888 \
  --name inventory-service \
  elara-app/inventory-service:1.2
```

### Kubernetes Deployment

Example deployment manifest:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: inventory-service
  template:
    metadata:
      labels:
        app: inventory-service
    spec:
      containers:
      - name: inventory-service
        image: elara-app/inventory-service:1.2
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
```

### Health Checks

The service exposes Spring Boot Actuator endpoints for monitoring:

- **Health**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`
- **Metrics**: `http://localhost:8080/actuator/metrics`

---

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
│   │   │   │   ├── request/         # Request DTOs
│   │   │   │   ├── response/        # Response DTOs
│   │   │   │   └── update/          # Update DTOs
│   │   │   ├── exceptions/          # Custom exceptions
│   │   │   │   ├── BaseException.java
│   │   │   │   ├── DatabaseException.java
│   │   │   │   ├── InvalidDataException.java
│   │   │   │   ├── ResourceConflictException.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── ServiceUnavailableException.java
│   │   │   │   └── UnexpectedErrorException.java
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   │   └── InventoryItemMapper.java
│   │   │   ├── model/               # JPA entities
│   │   │   │   └── InventoryItem.java
│   │   │   ├── repository/          # Spring Data repositories
│   │   │   │   └── InventoryItemRepository.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── imp/
│   │   │   │   │   ├── InventoryItemImp.java
│   │   │   │   │   └── UomServiceClientImp.java
│   │   │   │   └── interfaces/
│   │   │   │       ├── InventoryItemService.java
│   │   │   │       └── UomServiceClient.java
│   │   │   ├── utils/               # Utility classes
│   │   │   │   ├── ApplicationContextHolder.java
│   │   │   │   ├── ErrorCode.java
│   │   │   │   └── MessageService.java
│   │   │   └── InventoryServiceApplication.java
│   │   └── resources/
│   │       ├── application.yml      # Main configuration
│   │       ├── application-dev.yml  # Development profile
│   │       └── messages.properties  # Internationalization messages
│   └── test/                        # Test classes (mirrors main structure)
├── .mvn/                            # Maven wrapper files
├── target/                          # Build output (generated)
├── .gitignore
├── mvnw                             # Maven wrapper script (Unix)
├── mvnw.cmd                         # Maven wrapper script (Windows)
├── pom.xml                          # Maven project configuration
└── README.md                        # This file
```

### Key Packages

- **`config`**: Configuration classes including global exception handler and error response models
- **`controller`**: REST API endpoints exposing service functionality
- **`dto`**: Data Transfer Objects for API requests, responses, and updates
- **`exceptions`**: Custom exception hierarchy for proper error handling
- **`mapper`**: MapStruct interfaces for entity-DTO conversions
- **`model`**: JPA entities representing database tables
- **`repository`**: Spring Data JPA repositories for data access
- **`service`**: Business logic implementation (interface-based design)
- **`utils`**: Utility classes for common functionality

---

## 🎓 Best Practices & Design Decisions

This project embodies numerous best practices and thoughtful design decisions. For a comprehensive deep-dive into the architectural decisions, development process, and rationale behind each choice, please refer to:

### 📚 Detailed Documentation

- **[Step-by-Step Construction Process](https://bit.ly/4a0ZITO)** - Complete guide covering:
  - Initial project setup and scaffolding
  - Layer-by-layer implementation approach
  - Technology selection rationale
  - Best practices and coding standards
  - Regulatory compliance and security considerations
  - Testing strategy and quality assurance
  - Performance optimization decisions

### Key Design Decisions Highlights

1. **Layered Architecture**: Clean separation of concerns with controller → service → repository → model layers
2. **Interface-Based Services**: All services implement interfaces for better testability and flexibility
3. **DTO Pattern**: Separate DTOs for requests, responses, and updates to prevent over/under-posting
4. **MapStruct for Mapping**: Type-safe, compile-time bean mapping instead of reflection-based solutions
5. **Custom Exception Hierarchy**: Centralized error handling with semantic exception types
6. **Comprehensive Validation**: Bean Validation (Jakarta Validation) at multiple levels
7. **Pagination First**: All list endpoints support pagination for scalability
8. **Stateless Design**: No session state for horizontal scalability
9. **Configuration Management**: Externalized configuration via Spring Cloud Config
10. **Security by Design**: Vault integration for sensitive data, no hardcoded credentials
11. **Microservice Integration**: RESTful integration with Unit of Measure service for standardization
12. **Cost Tracking**: Decimal precision for financial calculations using BigDecimal

---

## 📚 Documentation

### Official Documentation

- **[DeepWiki - Inventory Service](https://deepwiki.com/elara-app/inventory-service)**: Comprehensive project documentation including:
  - Architecture diagrams
  - API specifications
  - Database schema
  - Deployment guides
  - Troubleshooting guides
  - FAQ

### Related Documentation

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Cloud Config](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [MapStruct Documentation](https://mapstruct.org/documentation/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

## 🤝 Contributing

We welcome contributions to the Inventory Service! Here's how you can help:

### Getting Started with Contributions

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/your-username/inventory-service.git
   ```
3. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** following our coding standards
5. **Write or update tests** to cover your changes
6. **Ensure all tests pass**:
   ```bash
   ./mvnw clean verify
   ```
7. **Commit your changes** with clear, descriptive messages:
   ```bash
   git commit -m "Add feature: description of your changes"
   ```
8. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```
9. **Submit a Pull Request** to the main repository

### Contribution Guidelines

- Follow the existing code style and conventions
- Write clear, self-documenting code with appropriate comments
- Include unit tests for new features
- Update documentation as needed
- Ensure your code passes all existing tests
- Keep pull requests focused on a single feature or fix
- Write meaningful commit messages

### Code Review Process

All contributions go through a code review process:
1. Automated checks (build, tests, coverage)
2. Peer review by maintainers
3. Approval required before merging

### Reporting Issues

Found a bug or have a feature request? Please create an issue on GitHub:
- Use a clear and descriptive title
- Provide detailed steps to reproduce (for bugs)
- Include relevant logs or error messages
- Describe the expected behavior

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Elara App

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 📞 Contact & Support

### Project Team

- **Lead Developer**: [Julian Bermudez](https://github.com/julianbetov)
  - Email: julianbetov@gmail.com

### Getting Help

- **Issues**: [GitHub Issues](https://github.com/elara-app/inventory-service/issues)
- **Documentation**: [DeepWiki](https://deepwiki.com/elara-app/inventory-service)
- **Discussions**: [GitHub Discussions](https://github.com/elara-app/inventory-service/discussions)

### Elara Application Ecosystem

This service is part of the larger Elara application ecosystem. For information about other services:

- **Organization**: [Elara App on GitHub](https://github.com/elara-app)
- **Main Repository**: [Elara App](https://github.com/elara-app)

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ by the Elara Team

</div>
