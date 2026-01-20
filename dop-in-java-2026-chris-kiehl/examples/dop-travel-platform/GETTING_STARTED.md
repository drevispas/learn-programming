# Data-Oriented Programming 입문 가이드

이 문서는 `dop-travel-platform` 프로젝트를 처음 읽는 사람들을 위한 학습 가이드입니다.

## 0. 이 문서의 목적

이 프로젝트는 Chris Kiehl의 "Data-Oriented Programming" 책의 패턴을 Java 25로 구현한 교육용 코드베이스입니다. 이 가이드는 다음을 목표로 합니다:

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
- DOP 원서 경험 (이 프로젝트가 Java로 패턴을 보여줍니다)
- 고급 함수형 개념 (Monad, Applicative - 코드를 보면서 배웁니다)

## 2. 프로젝트 빌드 및 테스트 실행

### Maven (권장)
```bash
# 프로젝트 디렉토리로 이동
cd examples/dop-travel-platform

# 전체 테스트 실행
mvn test

# 특정 Chapter 테스트 실행
mvn test -Dtest=Chapter1_DOPPrinciplesTest

# 컴파일만
mvn compile

# 애플리케이션 실행
mvn spring-boot:run
```

### Gradle
```bash
cd examples/dop-travel-platform

# 전체 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "Chapter1*"

# 애플리케이션 실행
./gradlew bootRun
```

**요구사항**: Java 25, Maven 3.9+ 또는 Gradle 8.5+

> **Note**: `--enable-preview` 플래그가 필요하지만, 두 빌드 시스템 모두 자동으로 설정합니다.

## 3. 학습 로드맵 개요

```
┌─────────────────┐       ┌─────────────────────┐       ┌───────────────────────┐
│  Phase 1        │       │  Phase 2            │       │  Phase 3              │
│  기반 타입      │──────▶│  DOP 원칙 학습      │──────▶│  실무 적용            │
│  (shared/)      │       │  (sample/ - 테스트) │       │  (domain/application) │
├─────────────────┤       ├─────────────────────┤       ├───────────────────────┤
│ • Result        │       │ • Chapter 1: 원칙   │       │ • Booking 도메인      │
│ • Validation    │       │ • Chapter 3: 타입   │       │ • CreateBookingUseCase│
│ • Money         │       │ • Chapter 5: Total  │       │ • 샌드위치 아키텍처   │
│ • DateRange     │       │ • Chapter 7: 대수   │       │                       │
│ • Email         │       │ • Chapter 8: 규칙   │       │                       │
└─────────────────┘       └─────────────────────┘       └───────────────────────┘
```

**핵심 원칙**: 각 Phase의 개념이 다음 Phase에서 실제로 사용되므로, 순서대로 학습하는 것이 효과적입니다.

---

## 4. Phase 1: 기반 타입 (shared 패키지)

모든 도메인 로직의 기반이 되는 타입들입니다.

### 4-1. Result.java - Railway-Oriented Programming

**파일**: `src/main/java/com/travel/shared/Result.java`
**테스트**: `Chapter5_TotalFunctionsTest.ResultTypeUsage`

**핵심 개념**:
- 예외(Exception) 대신 타입으로 에러를 표현
- `map()`: 성공 값 변환 (A → B)
- `flatMap()`: 실패 가능한 연산 체이닝 (첫 실패 시 중단)
- `fold()`: 양쪽 케이스를 처리하여 값 추출

```java
// Before: try-catch 중첩
try {
    var member = memberRepository.findById(id);
    var booking = createBooking(member);
} catch (Exception e) { ... }

// After: flatMap 체인
return validateCommand(command)
    .flatMap(this::loadMember)
    .flatMap(this::createBooking);
```

**읽는 순서**:
1. `Success`와 `Failure` 레코드 정의 확인 (line 64-81)
2. `map()` 메서드의 switch 표현식 (line 138-143)
3. `flatMap()` 메서드의 short-circuit 동작 (line 182-187)

---

### 4-2. Validation.java - Applicative 에러 수집

**파일**: `src/main/java/com/travel/shared/Validation.java`
**테스트**: `Chapter5_TotalFunctionsTest.ValidationApplicative`

**핵심 개념**:
- Result는 첫 에러에서 중단 (fail-fast)
- Validation은 모든 에러를 수집 (fail-slow)
- 폼 입력 검증에 적합

```java
// 3개 필드를 독립적으로 검증하고 모든 에러 수집
Validation.combine(
    validateName(name),      // "이름은 필수입니다"
    validateEmail(email),    // "유효한 이메일이 아닙니다"
    validateAge(age),        // "유효한 나이가 아닙니다"
    (n, e, a) -> new User(n, e, a)
);
// → 3개 오류 모두 수집됨
```

**읽는 순서**:
1. `Valid`와 `Invalid` 레코드 정의
2. `ap()` 메서드의 4가지 경우 처리
3. `combine()` 헬퍼가 `ap()`를 사용하는 방식

---

### 4-3. Value Objects - Money, DateRange, Email

**파일들**:
- `src/main/java/com/travel/shared/types/Money.java`
- `src/main/java/com/travel/shared/types/DateRange.java`
- `src/main/java/com/travel/shared/types/Email.java`

**핵심 개념**:
- 불변 객체 + 자기 검증 (compact constructor)
- 통화 안전성 (같은 통화끼리만 연산)
- Primitive Obsession 방지

```java
// compact constructor에서 불변식 검증
record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be >= 0");
        }
    }

    // Wither 패턴 - 새 객체 반환
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
}
```

---

## 5. Phase 2: DOP 원칙 학습 (Chapter별 테스트)

`src/test/java/com/travel/sample/` 패키지에서 Chapter별로 DOP 원칙을 학습합니다.

### 5-1. Chapter 1: DOP 4대 원칙

**테스트**: `Chapter1_DOPPrinciplesTest.java`

**학습 목표**:
- 데이터와 동작의 분리 이해
- God Class의 문제점 인식
- 불변 데이터의 이점 체험
- 순수 함수의 테스트 용이성 확인

**핵심 예제**:

```java
// [데이터] Booking record = 순수 데이터만
var booking = Booking.create(memberId, items);

// [동작] BookingCalculations = 순수 함수 (static 메서드)
Money total = BookingCalculations.calculateTotalAmount(items);
Money refund = BookingCalculations.calculateRefundAmount(paidAmount, 7);

// [Key Point] 순수 함수는 Mock 없이 테스트 가능!
// 같은 입력 → 항상 같은 출력
```

**테스트 DisplayName 예시**:
- `[Ch 1.1] 데이터와 동작의 분리`
- `[Ch 1.2] 불변성의 이점`
- `[Ch 1.3] 순수 함수의 테스트 용이성`

---

### 5-2. Chapter 3: Cardinality - Sum/Product Type

**테스트**: `Chapter3_CardinalityTest.java`

**학습 목표**:
- Sum Type으로 불가능한 상태 제거
- Cardinality(경우의 수)로 타입 설계 검증
- 패턴 매칭의 exhaustiveness

**핵심 예제 - BookingStatus**:

```java
// [Before] boolean 필드들 사용:
boolean isPending, isConfirmed, isPaid, isCancelled, isCompleted;
// → 2^5 = 32가지 상태 가능 (대부분 무의미/불가능)
// 예: isPending=true && isCompleted=true → 말이 안 됨

// [After] sealed interface 사용:
sealed interface BookingStatus permits Pending, Confirmed, Cancelled, Completed, NoShow {}
// → 정확히 5가지 상태만 가능

// 각 상태가 해당 상태에서만 의미 있는 데이터 보유:
record Pending(Instant createdAt, Instant expiresAt) implements BookingStatus {}
record Confirmed(String paymentId, Instant confirmedAt) implements BookingStatus {}
record Cancelled(String reason, Instant cancelledAt, Money refundAmount) implements BookingStatus {}
```

**패턴 매칭 (컴파일러가 모든 케이스 검사)**:

```java
String description = switch (status) {
    case Pending p -> "결제 대기 중, 만료: " + p.expiresAt();
    case Confirmed c -> "확정됨, 결제 ID: " + c.paymentId();
    case Cancelled c -> "취소됨, 사유: " + c.reason();
    case Completed c -> "완료, 체크아웃: " + c.completedAt();
    case NoShow n -> "노쇼, 발생일: " + n.occurredAt();
    // 새 상태 추가 시 여기서 컴파일 에러!
};
```

---

### 5-3. Chapter 5: Total Functions

**테스트**: `Chapter5_TotalFunctionsTest.java`

**학습 목표**:
- Total Function vs Partial Function 이해
- Result 타입으로 오류 처리
- Railway-Oriented Programming 패턴
- Validation (Applicative)으로 오류 수집

**핵심 개념 - Total Function**:

```java
// [Partial Function] 일부 입력에서 예외 발생 (안티패턴)
User findUser(String id) throws UserNotFoundException;
// → 호출자가 예외 처리를 잊을 수 있음

// [Total Function] 모든 입력에 대해 정의된 출력 반환 (권장)
Result<User, UserNotFound> findUser(String id);
// → 반환값 처리 시 Success/Failure 모두 다뤄야 함
```

**Railway-Oriented Programming**:

```
     Success Track ─────────────────────────────→
          │               │               │
        map()          flatMap()        map()
          │               │               │
     Failure Track ─────────────────────────────→

[Key Point] 한 번 실패하면 이후 연산은 건너뛰고 실패 결과만 전달
```

---

### 5-4. Chapter 7: Algebraic Properties

**테스트**: `Chapter7_AlgebraicPropertiesTest.java`

**학습 목표**:
- 결합법칙 (Associativity)
- 항등원 (Identity Element)
- 멱등성 (Idempotence)

**핵심 개념**:

```java
// 금액 계산은 결합법칙을 만족:
// (item1 + item2) + item3 = item1 + (item2 + item3)
// → 순서에 관계없이 동일한 결과 보장
// → 병렬 처리 가능!

// 항등원 (Identity Element):
Money.ZERO_KRW.add(anyMoney) == anyMoney
// → reduce 연산의 시작값으로 사용

// [Ch 7] reduce 연산 - 항등원(ZERO)에서 시작하여 결합
return items.stream()
    .map(BookingItem::basePrice)
    .reduce(Money.ZERO_KRW, Money::add);
```

---

### 5-5. Chapter 8: Rule Engine

**테스트**: `Chapter8_RuleEngineTest.java`

**학습 목표**:
- 규칙을 데이터로 표현
- Interpreter 패턴
- 비즈니스 규칙의 동적 조합

**핵심 파일**:
- `src/main/java/com/travel/domain/promotion/PromotionRule.java`
- `src/main/java/com/travel/domain/promotion/PromotionRuleEngine.java`

---

## 6. Phase 3: 실무 적용 (domain + application)

Phase 1, 2에서 학습한 패턴이 실제 도메인에 적용된 코드입니다.

### 6-1. Booking.java + BookingStatus.java - 상태 머신

**파일들**:
- `src/main/java/com/travel/domain/booking/Booking.java`
- `src/main/java/com/travel/domain/booking/BookingStatus.java`

**핵심 개념**:
- `BookingStatus` Sum Type으로 상태 관리
- 각 상태 전이 메서드가 새 상태 객체 반환 (불변성)
- 잘못된 상태 전이 시 컴파일/런타임 에러

**상태 전이 다이어그램**:

```
┌─────────┐   confirm(paymentId)   ┌───────────┐   complete()   ┌───────────┐
│ Pending │───────────────────────▶│ Confirmed │───────────────▶│ Completed │
└─────────┘                        └───────────┘                └───────────┘
     │                                   │
     │ cancel()                          │ cancel()
     │                                   │
     ▼                                   ▼
┌───────────┐                      ┌───────────┐
│ Cancelled │                      │ Cancelled │
└───────────┘                      └───────────┘
                                         │
                                         │ markNoShow()
                                         ▼
                                   ┌──────────┐
                                   │  NoShow  │
                                   └──────────┘
```

---

### 6-2. BookingCalculations.java - Functional Core

**파일**: `src/main/java/com/travel/domain/booking/BookingCalculations.java`
**테스트**: `BookingCalculationsTest.java`

**핵심 개념**:
- 순수 함수만 포함 (외부 의존성 없음)
- 입력/출력만으로 테스트 가능 (Mock 불필요)
- 가격 계산, 할인 적용, 환불 계산 등 비즈니스 로직

```java
public final class BookingCalculations {
    // 인스턴스화 방지
    private BookingCalculations() {}

    // [FC] 순수 함수: 입력이 같으면 출력도 항상 같음
    public static Money calculateTotalAmount(List<BookingItem> items) { ... }
    public static Money calculateRefundAmount(Money paidAmount, long daysBeforeUse) { ... }
    public static Money calculateBestDiscount(Money total, Money fixed, int percent) { ... }
}
```

---

### 6-3. CreateBookingUseCase.java - Imperative Shell (샌드위치 아키텍처)

**파일**: `src/main/java/com/travel/application/booking/CreateBookingUseCase.java`

**핵심 개념**:
- **Functional Core / Imperative Shell 패턴**
- I/O(Repository, Gateway)와 순수 로직(Calculations) 분리
- Result 체이닝으로 에러 흐름 관리

**샌드위치 구조**:

```
┌──────────────────────────────────────────────────────────┐
│  Top Bun (IS): 데이터 수집                                │
│  - memberRepository.findById()                           │
│  - couponRepository.findById()                           │
│  - checkAvailability()                                   │
├──────────────────────────────────────────────────────────┤
│  Meat (FC): 순수 비즈니스 로직                            │
│  - Booking.create()                                      │
│  - BookingDomainService.applyDiscount()                  │
│  - 모든 입력이 준비된 상태에서 순수 계산                   │
├──────────────────────────────────────────────────────────┤
│  Bottom Bun (IS): 부수효과 실행                           │
│  - bookingRepository.save()                              │
│  - markCouponAsUsed()                                    │
│  - eventPublisher.publish() (TODO)                       │
└──────────────────────────────────────────────────────────┘
```

**코드 예시**:

```java
@Transactional
public Result<Booking, BookingError> execute(CreateBookingCommand command) {
    // === Top Bun (IS): 데이터 수집 ===
    Result<Member, BookingError> memberResult = loadMember(command.memberId());
    if (memberResult.isFailure()) {
        return Result.failure(memberResult.errorOrNull());
    }
    // ...

    // === Meat (FC): 순수 비즈니스 로직 ===
    Booking booking = Booking.create(command.memberId(), command.items());
    // ...

    // === Bottom Bun (IS): 부수효과 실행 ===
    Booking savedBooking = bookingRepository.save(booking);
    return Result.success(savedBooking);
}
```

---

## 7. 테스트가 문서다 - 테스트 읽는 순서

이 프로젝트의 테스트는 한국어 `@DisplayName`으로 비즈니스 요구사항을 명확히 설명합니다.

### 테스트 클래스별 역할

| 테스트 클래스 | 검증 대상 | 주요 DisplayName 예시 |
|--------------|----------|---------------------|
| `Chapter1_DOPPrinciplesTest` | DOP 4대 원칙 | "Booking(데이터)과 BookingCalculations(동작)이 분리되어 있다" |
| `Chapter3_CardinalityTest` | Sum/Product Type | "sealed interface로 불가능한 상태 제거" |
| `Chapter5_TotalFunctionsTest` | Result, Validation | "flatMap으로 성공 케이스를 체이닝한다" |
| `Chapter7_AlgebraicPropertiesTest` | 대수적 속성 | "결합법칙: (a+b)+c = a+(b+c)" |
| `Chapter8_RuleEngineTest` | 규칙 엔진 | "규칙을 데이터로 표현하고 해석" |
| `BookingCalculationsTest` | 예약 계산 로직 | "환불 금액 계산은 결정적(deterministic)이다" |

### 테스트 읽기 팁

1. **DisplayName 먼저 읽기**: 무엇을 검증하는지 파악
2. **Given-When-Then 패턴 확인**: 테스트 구조 이해
3. **실패 케이스 주목**: 비즈니스 규칙의 경계 조건

```java
@Test
@DisplayName("체인 중간에 실패하면 이후는 건너뛴다")
void failure_short_circuits_the_chain() {
    // Given
    Result<Integer, String> start = Result.success(10);

    // When: 중간에 실패
    Result<Integer, String> result = start
            .flatMap(x -> Result.success(x * 2))                    // 20
            .flatMap(x -> Result.<Integer, String>failure("오류!"))  // 실패!
            .flatMap(x -> Result.success(x + 100));  // 실행 안 됨

    // Then: 최종 결과는 실패
    assertTrue(result.isFailure());
    assertEquals("오류!", result.errorOrNull());
}
```

---

## 8. 핵심 파일 빠른 참조 표

| 패턴 | 파일 | 역할 |
|------|------|------|
| **Sum Type** | `BookingStatus.java` | 5개 상태만 허용, 불가능한 상태 제거 |
| **Result** | `Result.java` | Railway-Oriented 에러 처리 |
| **Validation** | `Validation.java` | 모든 에러 수집 (Applicative) |
| **Value Object** | `Money.java`, `Email.java` | 불변 + 자기 검증 |
| **Functional Core** | `BookingCalculations.java` | 순수 함수 (static 메서드) |
| **Imperative Shell** | `CreateBookingUseCase.java` | I/O 조율, 샌드위치 구조 |
| **Wither 패턴** | `Booking.java` | 불변 객체 업데이트 (`withStatus()`) |
| **상태 머신** | `BookingStatus.java` | 타입 안전 상태 전이 |

---

## 9. 실습 과제

### 초급: 패턴 이해

1. **Result 체이닝 따라가기**
   - `CreateBookingUseCase.execute()` 메서드에서 `flatMap` 체인 따라가기
   - 어느 단계에서 실패하면 어떻게 처리되는지 확인

2. **Sum Type 패턴 매칭**
   - `BookingStatus` switch 표현식 작성해보기
   - 새로운 상태 `OnHold` 추가 시 컴파일 에러 확인

### 중급: 패턴 적용

3. **새 Value Object 추가**
   - `PhoneNumber` record 생성 (한국 휴대폰 형식 검증)
   - compact constructor에서 정규식 검증

4. **상태 전이 추가**
   - `BookingStatus`에 "환불 대기(RefundPending)" 상태 추가
   - `Cancelled` 상태에서만 `RefundPending`으로 전이 가능하도록 구현

### 고급: 아키텍처 확장

5. **새 Bounded Context 추가**
   - `review` 도메인 분리 (리뷰 작성/수정/삭제)
   - `ReviewError` sealed interface 생성
   - `CreateReviewUseCase` 구현 (샌드위치 아키텍처)

6. **Functional Core 확장**
   - `BookingCalculations`에 "복합 할인 계산" 로직 추가
   - 쿠폰 + 등급 할인 + 프로모션을 조합하는 순수 함수
   - 단위 테스트 작성 (Mock 없이)

---

## 10. 다음 단계

### 추가 학습 자료

- **원서**: "Data-Oriented Programming" - Chris Kiehl
- **관련 서적**: "Domain Modeling Made Functional" - Scott Wlaschin

### 관련 프로젝트

- 이 저장소의 `docs/lectures/` 폴더에 있는 강의 문서들을 읽으면 각 패턴의 이론적 배경을 더 깊이 이해할 수 있습니다.
- `domain-modeling-made-functional-2018-scott-wlaschin/examples/functional-domain-modeling/` 프로젝트도 참고하세요.

### 실무 적용 팁

1. **점진적 도입**: 기존 코드에 한 번에 모든 패턴을 적용하지 말고, Result나 Value Object부터 시작
2. **팀 합의**: sealed interface, pattern matching 등은 팀원들의 이해가 필요
3. **테스트 먼저**: 패턴 도입 전에 충분한 테스트 커버리지 확보

---

**문의사항이나 개선 제안**은 이 저장소의 Issues에 남겨주세요.
