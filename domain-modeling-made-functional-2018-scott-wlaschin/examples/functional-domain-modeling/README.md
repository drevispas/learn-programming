# Functional Domain Modeling Sample

이 프로젝트는 강의 "Java로 정복하는 함수형 도메인 모델링"의 핵심 개념을 실행 가능한 코드로 구현한 예제입니다.

## 요구사항
- Java 21+
- Maven 3.9+

## 빌드 및 테스트

```bash
mvn -q -f examples/functional-domain-modeling/pom.xml test
```

## 프로젝트 구조

```
src/main/java/com/ecommerce/sample/
├── DomainTypes.java        # Value Objects (Money, CustomerId, ProductId 등)
├── Member.java             # Wither 패턴 예제 (불변 객체 수정)
├── Result.java             # Railway-Oriented Programming의 Result 타입
├── Validation.java         # 에러 수집을 위한 Applicative Functor
├── SumTypes.java           # Sum Type 예제 (PaymentMethod, OrderStatus, CouponType)
├── DomainErrors.java       # 도메인 특화 에러 타입 (OrderError)
├── PhantomTypes.java       # Phantom Type 패턴 예제 (Email<Verified>)
├── OrderStateMachine.java  # 상태 머신 패턴 예제
└── Workflow.java           # 주문 워크플로우 파이프라인
```

## 핵심 개념

### 0. Wither 패턴 (Ch.1)
Java 25 `with` 표현식 미지원으로 인한 수동 상태 변경 패턴

```java
// 불변 객체의 일부 속성만 변경하여 새 객체 반환
Member member = new Member("Alice", 30, "a@b.com");
Member updated = member.withAge(31); // 31살이 된 새로운 Member 객체
```

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
```

### 2. Sum Types (Ch.3)
`sealed interface`를 통한 OR 타입 정의 - 컴파일러가 모든 케이스 처리 강제

```java
// 결제 수단은 정확히 4가지 중 하나
sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points, SimplePay {}
```

### 3. 상태 머신 패턴 (Ch.4)
타입으로 상태 전이를 강제하여 불가능한 상태 제거

```java
// 각 상태가 별도 타입 - 잘못된 전이 불가능
UnpaidOrder -> pay() -> PaidOrder -> startShipping() -> ShippingOrder -> completeDelivery() -> DeliveredOrder
```

### 4. Phantom Types (Ch.4)
런타임 오버헤드 없이 제네릭 타입 파라미터로 상태를 강제

```java
// Unverified 상태만 입력 가능
Email<Verified> verify(Email<Unverified> email);

// Verified 상태만 입력 가능 (컴파일 타임 체크)
void register(Email<Verified> email);
```

### 5. Domain Error Types (Ch.6)
String 대신 구체적인 에러 타입으로 컴파일 타임 에러 처리 강제

```java
sealed interface OrderError permits
    EmptyOrder, InvalidCustomerId, OutOfStock, PaymentFailed {}
```

### 6. Railway-Oriented Programming (Ch.6)
`Result<S, F>` 타입으로 에러를 값으로 다루기

```java
public Result<OrderPlaced, String> execute(PlaceOrderCommand command) {
    return validateOrder(command)
        .map(this::priceOrder)
        .flatMap(this::processPayment)
        .map(this::createOrderPlaced);
}
```

### 7. Validation으로 에러 수집 (Ch.7)
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

`WorkflowTest.java`와 `LectureConceptTest.java`에서 모든 핵심 기능을 검증합니다:
- **LectureConceptTest**: Wither 패턴, Phantom Type 등 기본 개념 검증
- **WorkflowTest**: 전체 주문 워크플로우 및 ROP, Validation 로직 검증

## 강의 문서와의 대응

| 강의 챕터 | 구현 파일 | 구현 상태 |
|----------|----------|----------|
| Ch 1: Wither Pattern | Member.java | 구현됨 |
| Ch 2: Value Objects | DomainTypes.java | 구현됨 |
| Ch 3: Sum Type | SumTypes.java | 구현됨 |
| Ch 4: 상태 머신 | OrderStateMachine.java | 구현됨 |
| Ch 4: Phantom Type | PhantomTypes.java | 구현됨 |
| Ch 6: Result 타입 | Result.java | 구현됨 |
| Ch 6: Domain Error | DomainErrors.java | 구현됨 |
| Ch 7: Validation | Validation.java, Workflow.java | 구현됨 |
