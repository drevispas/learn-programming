# DOP Travel Platform - Project Context

## Project Overview
This project is an educational implementation of a **Travel Platform** designed to demonstrate **Data-Oriented Programming (DOP)** principles in Java. It accompanies the "Data-Oriented Programming in Java" lecture series.

The core philosophy is the strict separation of code (behavior) and data, utilizing Java's modern features like Records, Sealed Types, and Pattern Matching.

## Key Technologies
*   **Language:** Java 25 (with `--enable-preview` features enabled).
*   **Framework:** Spring Boot 3.4.1.
*   **Build Tools:** Maven (`pom.xml`) & Gradle (`build.gradle.kts`).
*   **Database:** H2 Database (In-memory).
*   **Testing:** JUnit 5, Spring Boot Test.

## Architectural Principles (DOP)
This project adheres to the 4 Principles of Data-Oriented Programming:
1.  **Separate Code from Data:** Data is represented by immutable `record`s (e.g., `Booking`). Logic is strictly in static methods/classes (e.g., `BookingCalculations`).
2.  **Represent Data with Generic Systems:** Uses standard collections (List, Map) and Records rather than complex custom classes.
3.  **Data is Immutable:** All domain objects are immutable. "Changes" are handled by creating new instances (Wither pattern).
4.  **Schema and Data are Separated:** (Implied usage of flexible data structures).

### Design Patterns Used
*   **Functional Core / Imperative Shell:** Pure business logic (Functional Core) is isolated from side effects like DB or Network calls (Imperative Shell).
*   **Algebraic Data Types (ADTs):** Uses Sealed Interfaces for Sum Types (e.g., `BookingStatus`) and Records for Product Types.
*   **Result Pattern:** Uses `Result<T, E>` types for error handling instead of Exceptions for business flows.
*   **Wither Pattern:** Methods like `withStatus(...)` that return a new instance of the record.

## Directory Structure
*   `examples/dop-travel-platform`: The main Spring Boot application source code.
    *   `src/main/java/com/travel/domain`: Core domain models (Records) and pure logic.
    *   `src/main/java/com/travel/application`: Use Cases and Application Services (Imperative Shell).
    *   `src/main/java/com/travel/infrastructure`: DB adapters, configuration.
*   `docs`: Lecture notes and documentation drafts.

## Build and Run
**Prerequisite:** Java 25 must be installed and configured.

### Maven (Preferred)
```bash
cd examples/dop-travel-platform
# Run the application
mvn spring-boot:run

# Run tests
mvn test
```

### Gradle
```bash
cd examples/dop-travel-platform
# Run the application
./gradlew bootRun

# Run tests
./gradlew test
```

## Development Conventions
*   **Domain Objects:** Must be `record`s.
*   **Logic:** Must be `static` functions, usually in a class suffixed with `Calculations` or `Service`.
*   **Validation:** Performed inside Record constructors (Compact Constructor) or static factory methods.
*   **Comments:** Extensive Javadoc explaining DOP concepts (e.g., "Key Point" sections).
