# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Educational repository demonstrating "Domain Modeling Made Functional" concepts using Java 25. Adapts Scott Wlaschin's F# patterns to modern Java with Records, Sealed Interfaces, and Pattern Matching. The main example is an e-commerce domain.

## Build Commands

### Maven
```bash
# Run all tests (from repository root)
mvn -q -f examples/functional-domain-modeling/pom.xml test

# Run a specific test class
mvn -q -f examples/functional-domain-modeling/pom.xml test -Dtest=WorkflowTest

# Compile only
mvn -q -f examples/functional-domain-modeling/pom.xml compile
```

### Gradle (KTS)
```bash
cd examples/functional-domain-modeling
./gradlew test

# Run a specific test class
./gradlew test --tests "com.ecommerce.sample.WorkflowTest"
```

**Requirements**: Java 25, Maven 3.9+ or Gradle 8.5+

## Repository Structure

```
docs/lectures/           # Lecture documents (Korean)
examples/functional-domain-modeling/
  src/main/java/com/ecommerce/
    sample/              # Educational pattern examples (start here)
    domain/              # Bounded contexts (order, member, product, payment, coupon)
    application/         # Use cases (imperative shell)
    shared/              # Cross-cutting types (Result, Validation, Money)
  src/test/java/         # JUnit 5 tests
```

## Architecture

### Functional Core / Imperative Shell
- **Functional Core**: Pure domain logic in `domain/` packages - no side effects
- **Imperative Shell**: Use cases in `application/` orchestrate side effects and repository calls

### Bounded Contexts
Each domain context (order, member, product, payment, coupon) has:
- Value objects as records with validation in compact constructors
- `*Error` sealed interfaces for domain-specific errors
- `*Repository` interfaces for persistence abstraction

### Key Patterns

**Railway-Oriented Programming**: `Result<S, F>` sealed interface with `map`/`flatMap` for composable error handling. See `PlaceOrderUseCase.execute()`.

**Sum Types**: Sealed interfaces for exhaustive pattern matching:
```java
sealed interface OrderStatus permits Unpaid, Paid, Shipping, Delivered, Cancelled {}
```

**Phantom Types**: Compile-time state enforcement with zero runtime overhead:
```java
Email<Verified> verify(Email<Unverified> email);
```

**Wither Pattern**: Immutable object modification via `withXxx()` methods on records.

**Validation**: Applicative functor in `Validation.java` for collecting all errors rather than fail-fast. Use `Validation.combine4()` to collect all validation errors at once.

### Error Handling Convention
- Business logic uses `Result<S, F>` - no exceptions for expected failures
- Exceptions reserved for infrastructure/programming errors
- Domain errors are sealed hierarchies for exhaustive handling

## Key Entry Points

**Individual Patterns** (`sample/` package):
- `Member.java` - Wither pattern for immutable updates
- `DomainTypes.java` - Value Objects with compact constructor validation
- `SumTypes.java` - Sum types (PaymentMethod, OrderStatus, CouponType)
- `PhantomTypes.java` - Compile-time state enforcement
- `OrderStateMachine.java` - State machine with type-safe transitions
- `Result.java` / `Validation.java` - ROP and error accumulation

**Full Workflow**:
- `PlaceOrderUseCase.java` - Complete workflow integrating all patterns
- `WorkflowTest.java` - Tests demonstrating end-to-end order flow
- `OrderDomainService.java` - Pure domain logic extraction