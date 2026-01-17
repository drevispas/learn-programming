# Functional Domain Modeling Sample

이 프로젝트는 강의 "Java로 정복하는 함수형 도메인 모델링"의 핵심 개념을 실행 가능한 코드로 구현한 예제입니다.

## 요구사항
- Java 25+
- Maven 3.9+ 또는 Gradle 8.5+

## 빌드 및 테스트

### Maven
```bash
# 프로젝트 루트에서 실행
mvn -q -f examples/functional-domain-modeling/pom.xml test

# 특정 테스트 클래스 실행
mvn -q -f examples/functional-domain-modeling/pom.xml test -Dtest=WorkflowTest
```

### Gradle (KTS)
```bash
# 프로젝트 디렉토리에서 실행
cd examples/functional-domain-modeling
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.ecommerce.sample.WorkflowTest"
```

## 프로젝트 구조

```
src/main/java/com/ecommerce/
├── sample/                    # 교육용 패턴 예제
│   ├── DomainTypes.java       # Value Objects (Money, CustomerId, ProductId 등)
│   ├── Member.java            # Wither 패턴 예제 (불변 객체 수정)
│   ├── Result.java            # Railway-Oriented Programming의 Result 타입
│   ├── Validation.java        # 에러 수집을 위한 Applicative Functor
│   ├── SumTypes.java          # Sum Type 예제 (PaymentMethod, OrderStatus, CouponType)
│   ├── DomainErrors.java      # 도메인 특화 에러 타입 (OrderError)
│   ├── PhantomTypes.java      # Phantom Type 패턴 예제 (Email<Verified>)
│   ├── OrderStateMachine.java # 상태 머신 패턴 예제
│   ├── Workflow.java          # 주문 워크플로우 파이프라인
│   └── ArchitecturePatterns.java # 아키텍처 패턴 데모
├── domain/                    # Bounded Context별 도메인 모델
│   ├── order/                 # 주문 컨텍스트
│   ├── member/                # 회원 컨텍스트
│   ├── product/               # 상품 컨텍스트
│   ├── payment/               # 결제 컨텍스트
│   └── coupon/                # 쿠폰 컨텍스트
├── application/               # Use Case (Imperative Shell)
│   ├── PlaceOrderUseCase.java # 주문 생성 워크플로우
│   ├── PlaceOrderCommand.java # 주문 입력 DTO
│   ├── PlaceOrderError.java   # 주문 에러 Union Type
│   ├── CancelOrderUseCase.java
│   ├── ApplyCouponUseCase.java
│   └── OrderDomainService.java
└── shared/                    # 공통 타입
    ├── Result.java            # 모나딕 Result 타입
    ├── Validation.java        # 에러 수집용 Validation
    └── types/                 # Money, Currency 등
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

### 8. Bounded Contexts (Ch.10)
DDD의 Bounded Context를 패키지로 분리하여 도메인 경계 명확화

```
domain/
├── order/    # Order, OrderId, OrderLine, OrderStatus, OrderError
├── member/   # Member, MemberId, MemberGrade, Points, MemberError
├── product/  # DisplayProduct, InventoryProduct, ProductId, ProductError
├── payment/  # PaymentMethod, PaymentResult, PaymentError, TransactionId
└── coupon/   # Coupon, CouponId, CouponType, CouponStatus, CouponError
```

각 컨텍스트는 독립적인 Value Object, Error Type, Repository를 가짐

## 테스트 코드

### Sample 패키지 테스트
- **LectureConceptTest**: Wither 패턴, Phantom Type 등 기본 개념 검증
- **WorkflowTest**: 전체 주문 워크플로우 및 ROP, Validation 로직 검증
- **ArchitecturePatternsTest**: 아키텍처 패턴 데모

### Domain 패키지 테스트
- **OrderDomainTest**: 주문 도메인 로직 검증
- **MemberDomainTest**: 회원 도메인 로직 검증
- **ProductDomainTest**: 상품 도메인 로직 검증
- **PaymentDomainTest**: 결제 도메인 로직 검증
- **CouponDomainTest**: 쿠폰 도메인 로직 검증

### Application 패키지 테스트
- **PlaceOrderUseCaseTest**: 주문 생성 Use Case 통합 테스트

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
| Ch 10: Bounded Contexts | domain/*, application/* | 구현됨 |
