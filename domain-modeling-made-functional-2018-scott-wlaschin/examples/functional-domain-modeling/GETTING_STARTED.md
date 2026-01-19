# 함수형 도메인 모델링 입문 가이드

이 문서는 `examples/functional-domain-modeling/` 프로젝트를 처음 읽는 사람들을 위한 학습 가이드입니다.

## 0. 이 문서의 목적

이 프로젝트는 Scott Wlaschin의 "Domain Modeling Made Functional" 책의 F# 패턴을 Java 25로 구현한 교육용 코드베이스입니다. 이 가이드는 다음을 목표로 합니다:

- 어디서부터 읽기 시작해야 하는지 안내
- 각 패턴이 왜 필요한지 맥락 제공
- 테스트를 문서처럼 활용하는 방법 소개

## 1. 사전 지식

### 필수
- Java 기본 문법 (records, sealed interfaces, switch expressions)
- 객체지향 기본 개념 (Entity, Value Object)

### 권장
- 함수형 프로그래밍 기초 (불변성, 순수 함수)
- Optional, Stream API 기본 사용법

### 없어도 됨
- F# 경험 (이 프로젝트가 Java로 번역해줍니다)
- 고급 함수형 개념 (Monad, Applicative - 코드를 보면서 배웁니다)

## 2. 프로젝트 빌드 및 테스트 실행

### Maven (권장)
```bash
# 저장소 루트에서 실행
cd examples/functional-domain-modeling

# 전체 테스트 실행
mvn -q test

# 특정 테스트 클래스 실행
mvn -q test -Dtest=WorkflowTest

# 컴파일만
mvn -q compile
```

### Gradle
```bash
cd examples/functional-domain-modeling
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.ecommerce.sample.WorkflowTest"
```

**요구사항**: Java 25, Maven 3.9+ 또는 Gradle 8.5+

## 3. 학습 로드맵 개요

```
Phase 1: 기반 타입         Phase 2: 패턴 학습         Phase 3: 실무 적용
    │                          │                          │
    ▼                          ▼                          ▼
┌─────────┐              ┌─────────────┐            ┌───────────────┐
│ shared/ │───────────▶ │   sample/   │─────────▶ │    domain/    │
│         │              │             │            │  application/ │
│ Result  │              │ DomainTypes │            │               │
│Validation│             │  SumTypes   │            │ Order, Member │
│  Money  │              │PhantomTypes │            │PlaceOrderUseCase│
└─────────┘              │OrderStateMachine│        └───────────────┘
                         └─────────────┘
```

**핵심 원칙**: 각 Phase의 개념이 다음 Phase에서 실제로 사용되므로, 순서대로 학습하는 것이 효과적입니다.

## 4. Phase 1: 기반 타입 (shared 패키지)

모든 도메인 로직의 기반이 되는 타입들입니다.

### 1-1. Result.java - Railway-Oriented Programming

**파일**: `src/main/java/com/ecommerce/shared/Result.java`
**테스트**: `WorkflowTest.ResultTests`

**핵심 개념**:
- 예외(Exception) 대신 타입으로 에러를 표현
- `map()`: 성공 값 변환 (A → B)
- `flatMap()`: 실패 가능한 연산 체이닝 (첫 실패 시 중단)
- `mapError()`: 에러 타입 변환

```java
// Before: try-catch 중첩
try {
    var validated = validate(order);
    var priced = price(validated);
} catch (Exception e) { ... }

// After: flatMap 체인
return validate(order)
    .flatMap(this::checkInventory)
    .flatMap(this::calculatePrice);
```

**읽는 순서**:
1. `Success`와 `Failure` 레코드 정의 확인
2. `map()` 메서드의 switch 표현식
3. `flatMap()` 메서드의 short-circuit 동작

---

### 1-2. Validation.java - Applicative 에러 수집

**파일**: `src/main/java/com/ecommerce/shared/Validation.java`
**테스트**: `WorkflowTest.ValidationTests`

**핵심 개념**:
- Result는 첫 에러에서 중단 (fail-fast)
- Validation은 모든 에러를 수집 (error accumulation)
- 폼 입력 검증에 적합

```java
// 4개 필드를 독립적으로 검증하고 모든 에러 수집
Validation.combine4(
    validateName(name),      // "이름은 2자 이상"
    validateEmail(email),    // "이메일 형식 오류"
    validatePhone(phone),    // "전화번호 형식 오류"
    validateAge(age),        // "나이는 0 이상"
    User::new
);
```

**읽는 순서**:
1. `Valid`와 `Invalid` 레코드 정의
2. `combine()` 메서드의 4가지 경우 처리
3. `combine3()`, `combine4()`가 `combine()`을 재귀 사용하는 방식

---

### 1-3. Money.java - Value Object 패턴

**파일**: `src/main/java/com/ecommerce/shared/types/Money.java`

**핵심 개념**:
- 불변 객체 + 자기 검증
- 통화 안전성 (같은 통화끼리만 연산)
- 정적 팩토리 메서드

```java
// compact constructor에서 불변식 검증
record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be >= 0");
        }
    }
}
```

## 5. Phase 2: 패턴 학습 (sample 패키지)

독립적인 예제로 각 패턴을 학습합니다.

### 2-1. DomainTypes.java - Value Object와 Compact Constructor

**파일**: `src/main/java/com/ecommerce/sample/DomainTypes.java`

**핵심 개념**:
- **Simple Value**: 원시 타입 래핑으로 타입 안전성 확보
- **Compact Constructor**: 생성 시점에 불변식 검증
- **Primitive Obsession 방지**

```java
// Before: 순서 바꿔도 컴파일됨!
void findOrder(long customerId, long orderId);

// After: 타입으로 구분
void findOrder(CustomerId customerId, OrderId orderId);  // 컴파일 에러
```

**읽는 순서**:
1. `CustomerId`, `ProductId` - Simple Value Objects
2. `OrderLine` - Compound Value Object
3. `ValidatedOrder`, `PricedOrder` - 워크플로우 상태 타입

---

### 2-2. Member.java - Wither 패턴

**파일**: `src/main/java/com/ecommerce/sample/Member.java`
**테스트**: `LectureConceptTest` - "Ch 1.3: Wither 패턴으로 불변 객체 속성 변경"

**핵심 개념**:
- 불변 객체에서 일부 필드만 변경한 새 인스턴스 생성
- `setXxx()` vs `withXxx()` 네이밍 컨벤션
- 체이닝 가능

```java
var m1 = new Member("Kim", 30, "kim@test.com");
var m2 = m1.withAge(31);  // m1은 여전히 age=30

// 체이닝
var m3 = m1.withAge(31).withEmail("new@test.com");
```

---

### 2-3. SumTypes.java - sealed interface

**파일**: `src/main/java/com/ecommerce/sample/SumTypes.java`
**테스트**: `WorkflowTest.SumTypeTests`

**핵심 개념**:
- **Sum Type**: "A 이거나 B 이거나 C" - 유한 개의 선택지
- **Exhaustive Pattern Matching**: 컴파일러가 모든 케이스 처리 강제
- **Make Illegal States Unrepresentable**

```java
// 결제 수단: 4가지 중 정확히 하나
sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points, SimplePay {}

// 컴파일러가 모든 케이스 처리 강제
String description = switch (paymentMethod) {
    case CreditCard c -> "카드 " + c.cardNumber();
    case BankTransfer b -> "계좌 " + b.accountNumber();
    case Points p -> "포인트 " + p.amount();
    case SimplePay s -> s.provider() + " 페이";
    // 새 결제 수단 추가 시 컴파일 에러 발생!
};
```

**읽는 순서**:
1. `PaymentMethod` - 각 variant가 필요한 데이터만 정의
2. `OrderStatus` - 상태별 다른 데이터
3. `CouponType` - `calculateDiscount()` 메서드 포함

---

### 2-4. PhantomTypes.java - 컴파일 타임 상태 검증

**파일**: `src/main/java/com/ecommerce/sample/PhantomTypes.java`
**테스트**: `LectureConceptTest` - "Ch 4.4: Phantom Type으로 상태 전이 강제"

**핵심 개념**:
- 제네릭 타입 파라미터로 상태 구분 (런타임 오버헤드 없음)
- 컴파일 타임에 잘못된 사용 방지

```java
// 상태 마커 인터페이스
sealed interface EmailState permits Unverified, Verified {}

// Phantom Type 적용
record Email<S extends EmailState>(String value) {}

// 검증된 이메일만 register에 전달 가능
Member register(Email<Verified> email, String name);

// ❌ 컴파일 에러!
Email<Unverified> unverified = Email.of("test@example.com");
register(unverified, "name");  // 컴파일 안 됨
```

---

### 2-5. OrderStateMachine.java - 타입 기반 상태 머신

**파일**: `src/main/java/com/ecommerce/sample/OrderStateMachine.java`

**핵심 개념**:
- 각 상태가 별도 타입 (UnpaidOrder, PaidOrder, ShippingOrder 등)
- 잘못된 상태 전이가 컴파일 에러
- 비즈니스 규칙이 타입에 인코딩됨

```
상태 전이 흐름:
UnvalidatedOrder → ValidatedOrder → PricedOrder → UnpaidOrder → PaidOrder → ShippingOrder → DeliveredOrder
                                                  ↓                ↓
                                            CancelledOrder   CancelledOrder (24h 내)
```

```java
// Before: 런타임 검증
void startShipping(Order order) {
    if (order.status != PAID) throw new IllegalStateException();
}

// After: 타입으로 강제
ShippingOrder startShipping(PaidOrder order) {
    // PaidOrder만 받으므로 상태 검증 불필요
}
```

**읽는 순서**:
1. `UnpaidOrder.pay()` → `PaidOrder` 전이
2. `PaidOrder.startShipping()` → `ShippingOrder` 전이
3. `ShippingOrder`에 `cancel()` 메서드가 없음 주목 (배송 중 취소 불가)
4. `DeliveredOrder`, `CancelledOrder`에 메서드 없음 (최종 상태)

## 6. Phase 3: 실무 적용 (domain + application)

Phase 1, 2에서 학습한 패턴이 실제 도메인에 적용된 코드입니다.

### 3-1. Order.java - 상태 전이 구현

**파일**: `src/main/java/com/ecommerce/domain/order/Order.java`
**테스트**: `OrderDomainTest`

**핵심 개념**:
- `OrderStatus` Sum Type으로 상태 관리
- 각 메서드가 `Result<Order, OrderError>` 반환
- 비즈니스 규칙이 타입 시스템에 인코딩됨

**테스트 DisplayName 예시**:
- `@DisplayName("결제 완료 주문 취소 (24시간 내)")`
- `@DisplayName("배송 중 주문 취소 불가")`

---

### 3-2. Member.java - 등급별 할인

**파일**: `src/main/java/com/ecommerce/domain/member/Member.java`
**테스트**: `MemberDomainTest`

**핵심 개념**:
- `MemberGrade` Sum Type (Bronze, Silver, Gold, Vip)
- 각 등급별 할인율, 무료배송 여부
- Wither 패턴으로 상태 변경

**테스트 DisplayName 예시**:
- `@DisplayName("VIP 등급: 할인율 10%, 무료배송 있음")`
- `@DisplayName("포인트 사용 실패 - 잔액 부족")`

---

### 3-3. OrderDomainService.java - Functional Core

**파일**: `src/main/java/com/ecommerce/application/OrderDomainService.java`
**테스트**: `PlaceOrderUseCaseTest.OrderDomainServiceTests`

**핵심 개념**:
- 순수 함수만 포함 (외부 의존성 없음)
- 입력/출력만으로 테스트 가능 (Mock 불필요)
- 가격 계산, 할인 적용 등 비즈니스 로직

---

### 3-4. PlaceOrderUseCase.java - Imperative Shell

**파일**: `src/main/java/com/ecommerce/application/PlaceOrderUseCase.java`
**테스트**: `PlaceOrderUseCaseTest`

**핵심 개념**:
- **Functional Core / Imperative Shell 패턴**
- I/O(Repository, Gateway)와 순수 로직(DomainService) 분리
- Result 체이닝으로 에러 흐름 관리

```
execute() 메서드 구조:
┌────────────────────────────────────────────────────────────┐
│ 1. [I/O] 데이터 조회 - memberRepository.findById()         │
│ 2. [I/O] 상품/재고 조회 - productRepository.find...        │
│ 3. [I/O] 쿠폰 조회 - couponRepository.findByCode()         │
│ 4. [PURE] 가격 계산 - domainService.calculatePricing()     │
│ 5. [I/O] 결제 처리 - paymentGateway.charge()               │
│ 6. [PURE] 주문 생성 - domainService.createOrder()          │
│ 7. [I/O] 저장 - orderRepository.save()                     │
└────────────────────────────────────────────────────────────┘
```

**읽는 순서**:
1. `execute()` 메서드의 전체 흐름
2. `[I/O]`와 `[PURE]` 주석 구분
3. `Result` 체이닝 패턴

## 7. 테스트가 문서다 - 테스트 읽는 순서

이 프로젝트의 테스트는 한국어 `@DisplayName`으로 비즈니스 요구사항을 명확히 설명합니다.

### 테스트 클래스별 역할

| 테스트 클래스 | 검증 대상 | 주요 DisplayName 예시 |
|--------------|----------|---------------------|
| `WorkflowTest.ResultTests` | Result 타입 동작 | "flatMap_chains_successful_operations" |
| `WorkflowTest.ValidationTests` | Validation 에러 수집 | "combine_collects_all_errors" |
| `WorkflowTest.SumTypeTests` | Sum Type 패턴 매칭 | "payment_method_exhaustive_switch" |
| `LectureConceptTest` | Wither, Phantom Type | "Ch 1.3: Wither 패턴으로 불변 객체 속성 변경" |
| `OrderDomainTest` | 주문 상태 전이 | "배송 중 주문 취소 불가" |
| `MemberDomainTest` | 회원 등급/포인트 | "VIP 등급: 할인율 10%, 무료배송 있음" |
| `PlaceOrderUseCaseTest` | 주문 워크플로우 통합 | "쿠폰 적용하여 주문 성공" |

### 테스트 읽기 팁

1. **DisplayName 먼저 읽기**: 무엇을 검증하는지 파악
2. **Given-When-Then 패턴 확인**: 테스트 구조 이해
3. **실패 케이스 주목**: 비즈니스 규칙의 경계 조건

```java
@Test
@DisplayName("배송 중 주문 취소 불가")  // ← 비즈니스 규칙
void cancel_shipping_notAllowed() {
    // Given: 배송 중인 주문
    Order shippingOrder = ...;

    // When: 취소 시도
    Result<Order, OrderError> result = shippingOrder.cancel(...);

    // Then: 실패
    assertTrue(result.isFailure());
    assertTrue(result.error() instanceof OrderError.CannotCancel);
}
```

## 8. 실습 과제

### 초급: 패턴 이해

1. **Result 체이닝 따라가기**
   - `Workflow.execute()` 메서드에서 `flatMap` 체인 따라가기
   - 어느 단계에서 실패하면 어떻게 처리되는지 확인

2. **Sum Type 패턴 매칭**
   - `OrderStatus` switch 표현식 작성해보기
   - 새로운 상태 추가 시 컴파일 에러 확인

### 중급: 패턴 적용

3. **새 Value Object 추가**
   - `EmailAddress` record 생성 (이메일 형식 검증 포함)
   - `PhoneNumber` record 생성 (한국 휴대폰 형식 검증)

4. **상태 전이 추가**
   - `Order`에 "교환 요청" 상태 추가
   - `Delivered` 상태에서만 교환 요청 가능하도록 구현

### 고급: 아키텍처 확장

5. **새 Bounded Context 추가**
   - `inventory` 도메인 분리
   - `InventoryError` sealed interface 생성
   - `PlaceOrderError`에 새 에러 타입 통합

6. **Functional Core 확장**
   - `OrderDomainService`에 "복합 할인 계산" 로직 추가
   - 단위 테스트 작성 (Mock 없이)

## 9. 다음 단계

### 추가 학습 자료

- **원서**: "Domain Modeling Made Functional" - Scott Wlaschin
- **강의 자료**: `docs/lectures/` 디렉토리 (한국어)

### 관련 프로젝트

- 이 프로젝트의 `docs/lectures/` 폴더에 있는 강의 문서들을 읽으면 각 패턴의 이론적 배경을 더 깊이 이해할 수 있습니다.

### 실무 적용 팁

1. **점진적 도입**: 기존 코드에 한 번에 모든 패턴을 적용하지 말고, Result나 Value Object부터 시작
2. **팀 합의**: sealed interface, pattern matching 등은 팀원들의 이해가 필요
3. **테스트 먼저**: 패턴 도입 전에 충분한 테스트 커버리지 확보

---

**문의사항이나 개선 제안**은 이 저장소의 Issues에 남겨주세요.
