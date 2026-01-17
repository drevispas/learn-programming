# Functional Domain Modeling Sample

이 프로젝트는 강의 "Java로 정복하는 함수형 도메인 모델링"의 핵심 개념을 실행 가능한 코드로 구현한 예제입니다.

## 요구사항
- Java 21+
- Maven 3.9+

## 빌드 및 테스트

```bash
mvn -q -f docs/samples/functional-domain-modeling/pom.xml test
```

## 프로젝트 구조

```
src/main/java/com/ecommerce/sample/
├── DomainTypes.java        # Value Objects (Money, CustomerId, ProductId 등)
├── Result.java             # Railway-Oriented Programming의 Result 타입
├── Validation.java         # 에러 수집을 위한 Applicative Functor
├── SumTypes.java           # Sum Type 예제 (PaymentMethod, OrderStatus, CouponType)
├── DomainErrors.java       # 도메인 특화 에러 타입 (OrderError)
├── OrderStateMachine.java  # 상태 머신 패턴 예제
└── Workflow.java           # 주문 워크플로우 파이프라인
```

## 핵심 개념

### 1. Value Objects (Ch.2)
원시 타입을 의미 있는 도메인 타입으로 감싸 컴파일 타임 안전성 확보

```java
record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be >= 0");
        }
    }
}

record CustomerId(long value) {
    public CustomerId {
        if (value <= 0) throw new IllegalArgumentException("CustomerId must be positive");
    }
}
```

### 2. Sum Types (Ch.3)
`sealed interface`를 통한 OR 타입 정의 - 컴파일러가 모든 케이스 처리 강제

```java
// 결제 수단은 정확히 4가지 중 하나
sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points, SimplePay {}

// Pattern Matching으로 exhaustive check
String describe(PaymentMethod method) {
    return switch (method) {
        case CreditCard c -> "Card: " + c.cardNumber();
        case BankTransfer b -> "Bank: " + b.bankCode();
        case Points p -> "Points: " + p.amount();
        case SimplePay s -> "SimplePay: " + s.provider();
    };
}
```

### 3. 상태 머신 패턴 (Ch.4)
타입으로 상태 전이를 강제하여 불가능한 상태 제거

```java
// 각 상태가 별도 타입 - 잘못된 전이 불가능
UnpaidOrder -> pay() -> PaidOrder -> startShipping() -> ShippingOrder -> completeDelivery() -> DeliveredOrder

// 예: PaidOrder만 배송 시작 가능
record PaidOrder(...) {
    ShippingOrder startShipping(String trackingNumber) { ... }
}
```

### 4. Domain Error Types (Ch.6)
String 대신 구체적인 에러 타입으로 컴파일 타임 에러 처리 강제

```java
sealed interface OrderError permits
    EmptyOrder, InvalidCustomerId, OutOfStock, PaymentFailed {}

// 모든 에러 케이스 처리 강제됨
String handleError(OrderError error) {
    return switch (error) {
        case EmptyOrder e -> "주문 상품이 없습니다";
        case InvalidCustomerId e -> "유효하지 않은 고객: " + e.customerId();
        case OutOfStock e -> "재고 부족: " + e.productId();
        case PaymentFailed e -> "결제 실패: " + e.reason();
    };
}
```

### 5. Railway-Oriented Programming (Ch.6)
`Result<S, F>` 타입으로 에러를 값으로 다루기

```java
public Result<OrderPlaced, String> execute(PlaceOrderCommand command) {
    return validateOrder(command)
        .map(this::priceOrder)
        .flatMap(this::processPayment)
        .map(this::createOrderPlaced);
}
```

### 6. Validation으로 에러 수집 (Ch.7)
`Validation.combine4()`로 모든 검증 에러 한번에 수집

```java
Result<ValidatedOrder, List<OrderError>> result = Validation.combine4(
    validateCustomerId(cmd.customerId()),
    validateOrderLines(cmd.lines()),
    validateShippingAddress(cmd.shippingAddress()),
    validateCouponCode(cmd.couponCode()),
    ValidatedOrder::new
).fold(Result::success, Result::failure);
```

## 테스트 코드

`WorkflowTest.java`에서 22개의 단위 테스트로 모든 핵심 기능을 검증합니다:
- 기존 Workflow (String 에러)
- 개선된 Workflow (Validation + 도메인 에러)
- Sum Type (PaymentMethod, OrderStatus, CouponType)
- Domain Error 처리
- Result 타입 연산 (map, flatMap, mapError)
- Validation 타입 연산 (combine, combine4, fold)

## 강의 문서와의 대응

| 강의 챕터 | 구현 파일 | 구현 상태 |
|----------|----------|----------|
| Ch 2: Value Objects | DomainTypes.java | 구현됨 |
| Ch 3: Sum Type | SumTypes.java | 구현됨 |
| Ch 4: 상태 머신 | OrderStateMachine.java | 구현됨 |
| Ch 6: Result 타입 | Result.java | 구현됨 |
| Ch 6: Domain Error | DomainErrors.java | 구현됨 |
| Ch 7: Validation | Validation.java, Workflow.java | 구현됨 |
