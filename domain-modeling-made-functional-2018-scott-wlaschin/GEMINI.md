# Functional Domain Modeling in Java

## Project Overview
This project is a Java implementation of the concepts from the book/lecture "Domain Modeling Made Functional" by Scott Wlaschin. It demonstrates how to apply functional programming principles (immutability, type systems, composition) in a standard Java 21+ environment.

## Architecture
The project follows the "Functional Core, Imperative Shell" pattern:

- **`com.ecommerce.domain` (Functional Core):**
  - Contains pure domain logic, entities, and value objects.
  - No dependencies on external frameworks or side effects (DB, API).
  - Heavy use of `record`, `sealed interface` (Sum Types), and custom types for type safety.
  - **Key Files:** `Order.java`, `OrderDomainService.java`.

- **`com.ecommerce.application` (Imperative Shell):**
  - Orchestrates the domain logic.
  - Handles side effects like database access (Repositories) and external systems (PaymentGateway).
  - Returns `Result` types to the caller.
  - **Key Files:** `PlaceOrderUseCase.java`.

- **`com.ecommerce.sample` (Concept Playground):**
  - Isolated examples of specific functional concepts taught in the lecture.
  - simplified implementations of patterns like "Wither", "Phantom Types", and "State Machines".
  - **Key Files:** `Workflow.java` (demonstrates Railway Oriented Programming), `Member.java` (Wither pattern).

- **`com.ecommerce.shared`:**
  - Reusable functional control structures.
  - **Key Files:** `Result.java` (Success/Failure monad), `Validation.java` (Error accumulation).

## Key Patterns & Conventions

### 1. Error Handling (Railway Oriented Programming)
Instead of exceptions, the project uses a `Result<T, E>` type.
- **Success:** `Result.success(value)`
- **Failure:** `Result.failure(error)`
- **Chaining:** Use `.map()`, `.flatMap()` to chain operations.
- **Example:**
  ```java
  public Result<OrderPlaced, PlaceOrderError> execute(Command cmd) {
      return validate(cmd)
          .map(this::calculatePrice)
          .flatMap(this::processPayment) // Returns Result
          .map(this::createEvent);
  }
  ```

### 2. Validation
For validating input where multiple errors should be collected (e.g., form submission), use `Validation`.
- **Combine:** `Validation.combine4(...)` to aggregate errors from multiple checks.

### 3. Type Safety
- **Value Objects:** Use `record` to wrap primitives (e.g., `CustomerId`, `Money`).
- **Sum Types:** Use `sealed interface` to define a closed set of possibilities (e.g., `PaymentMethod`, `OrderError`).
- **Phantom Types:** Use generic parameters to encode state in the type system (e.g., `Email<Verified>`).

## Building and Running

The project is a standard Maven project.

```bash
# Run all tests
mvn clean test

# Run tests for the specific module
mvn -f examples/functional-domain-modeling/pom.xml test
```

## Directory Structure
- `docs/`: Lecture notes and references.
- `examples/functional-domain-modeling/`: Main Java source code.
