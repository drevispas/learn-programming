# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Educational Spring Boot project demonstrating **Data-Oriented Programming (DOP)** principles in Java 25. Implements a travel/booking platform to showcase modern Java features (Records, Sealed Interfaces, Pattern Matching) with functional programming patterns.

## Build Commands

All commands run from `examples/dop-travel-platform/`:

```bash
# Maven (preferred)
mvn test                           # Run all tests
mvn test -Dtest=Chapter1_DOPPrinciplesTest  # Run single test class
mvn spring-boot:run                # Run application

# Gradle
./gradlew test                     # Run all tests
./gradlew test --tests "Chapter1*" # Run tests matching pattern
./gradlew bootRun                  # Run application
```

**Note:** Java 25 with `--enable-preview` is required. Both build systems configure this automatically.

## Architecture

### DOP 4 Principles (Strictly Enforced)

1. **Separate Code from Data** - Domain objects are `record`s; logic is `static` methods in `*Calculations`/`*Service` classes
2. **Represent Data with Generic Systems** - Standard collections and Records, no complex custom classes
3. **Data is Immutable** - All changes create new instances via Wither pattern (`withStatus()`, `withAddedItem()`)
4. **Schema and Data are Separated** - Flexible data structures

### Key Patterns

- **Functional Core / Imperative Shell**: Pure logic in domain layer, I/O in use cases and repositories
- **Algebraic Data Types**: Sealed interfaces for sum types (e.g., `BookingStatus`), records for product types
- **Result Pattern**: `Result<S,F>` sealed interface for Railway-Oriented error handling (no exceptions for business flows)
- **Wither Pattern**: Immutable updates via `record.withField(newValue)` methods

### Package Structure

```
examples/dop-travel-platform/src/main/java/com/travel/
├── domain/           # Pure domain: Records + static Calculations classes
│   ├── booking/      # Booking aggregate (Booking, BookingStatus, BookingCalculations)
│   ├── member/       # Member domain
│   ├── payment/      # Payment with idempotency
│   ├── coupon/       # Discount management
│   └── ...
├── application/      # Imperative Shell: Use cases orchestrating domain logic
├── infrastructure/   # JPA entities, mappers, repositories
└── shared/           # Result type, value objects (Money, Email, DateRange)
```

### Test Organization

Tests in `src/test/java/com/travel/sample/` are organized by DOP concepts:
- `Chapter1_DOPPrinciplesTest` - Data/behavior separation, immutability
- `Chapter3_CardinalityTest` - Type cardinality and sum types
- `Chapter5_TotalFunctionsTest` - Result type and total functions
- `Chapter7_AlgebraicPropertiesTest` - Mathematical properties
- `Chapter8_RuleEngineTest` - Rule engine pattern

## Code Conventions

- Domain objects: Always `record` types with compact constructor validation
- Business logic: Static methods in `*Calculations` classes (pure functions)
- Error handling: `Result<Success, Failure>` with `flatMap`/`map` chaining
- State changes: Wither methods returning new record instances
- Javadoc: Includes `[Key Point]` sections explaining DOP concepts

## ASCII Diagram Guidelines (for lecture .md files)

When creating or editing ASCII box diagrams in `lectures/*.md`:

1. **Define fixed column width first** - Determine exact character width for each column (e.g., 35 chars between `│` borders)
2. **Calculate padding precisely** - For each line: `padding = fixed_width - content_length`
3. **Never estimate visually** - Always count characters; longer text = less padding
4. **Verify alignment after editing** - Check that all rightmost `│` borders align at the same column
5. **Use ASCII only** - Replace emojis with ASCII equivalents:
   - `❌` → `[X]`
   - `✅` → `[O]`
   - `✓` → `o`
   - `✗` → `x`
   - `↓` → `v`
   - `↑` → `^`
   - `→` → `->`
   - `←` → `<-`
6. **Translate Korean to English** - All diagram text should be in English for consistent character width (Korean characters are double-width in monospace fonts)
7. **Nested boxes require extra care**:
   - When placing two boxes side-by-side: `left_pad + box1 + middle_space + box2 + right_pad = total_width`
   - Deeper nesting = more calculation errors; verify each nesting level
   - Asymmetric content (e.g., `v (SQL)` vs `v (JSON)`) needs explicit character counting
8. **Connector alignment** - `┴` and `┬` connectors must be at the exact same column as the `│` they connect to:
   - Don't estimate visually; count characters or use awk to find column positions
   - When adjusting connector position, compensate elsewhere to maintain total line length
   - Verify with: `awk 'NR==X {for(i=1;i<=length($0);i++) if(substr($0,i,1)=="│") print i}'`
9. **Verify with `awk`** - Run `awk '{print NR": ["length($0)"]"}' file.md` to check all lines have equal length
