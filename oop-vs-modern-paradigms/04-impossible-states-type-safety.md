# 04. Impossible States & Type Safety (불가능한 상태와 타입 안전성)

> **Sources**: DMMF Ch.4 (Impossible States), DOP Ch.4 (Make Illegal States Unrepresentable)

---

## 1. Make Illegal States Unrepresentable (불가능한 상태를 표현 불가능하게)

### 핵심 개념
- **관련 키워드**: Illegal States, Type-Driven Design, Compile-Time Safety, State Machine
- **통찰**: 런타임 if 검증 대신, 타입 시스템으로 불가능한 상태를 아예 표현하지 못하게 만든다.
- **설명**: "결제됐는데 취소됨", "배송됐는데 미결제"와 같은 모순 상태가 코드로 작성조차 되지 않으면, 런타임 검증이 필요 없습니다. 이는 "경비원을 더 잘 훈련시키자"가 아니라 "처음부터 자물쇠를 설치하자"는 접근입니다.

```
RUNTIME VALIDATION (Guard)        COMPILE-TIME SAFETY (Lock)
==========================        ==========================

  +--------+                        +--------+
  |  Door  | <-- anyone can enter   | Locked | <-- key required
  |  open  |     guard may miss     |  Door  |     impossible without key
  +--------+                        +--------+

  if (!isPaid) throw ...            ship(PaidOrder order) {
  if (isCanceled) throw ...           // only PaidOrder can enter
  // guard may forget!              }
                                    // UnpaidOrder? COMPILE ERROR
```

### 개념이 아닌 것
- **"모든 검증을 타입으로"**: 비즈니스 규칙 중 일부는 런타임 검증이 필요 (금액 한도 등)
- **"타입이 많으면 복잡해진다"**: 타입은 복잡성을 추가하는 게 아니라, 도메인의 복잡성을 명시적으로 드러냄

### Before: Traditional OOP
```java
// [X] 런타임 검증에 의존: 개발자가 검증을 빠뜨리면 버그
public class OrderService {
    public void processOrder(Order order) {
        if (order.getStatus() == null)
            throw new IllegalStateException("상태 없음");
        if ("PAID".equals(order.getStatus()) && order.isCanceled())
            throw new IllegalStateException("결제됐는데 취소?");
        if ("SHIPPED".equals(order.getStatus()) && !"PAID".equals(order.getPreviousStatus()))
            throw new IllegalStateException("결제 없이 배송?");
        // ... 수십 줄의 검증 로직 후에야 비즈니스 로직 시작
        doProcess(order);
    }

    public void shipOrder(Order order) {
        // 또 같은 검증 반복...
        if (!"PAID".equals(order.getStatus()))
            throw new IllegalStateException("결제 안 됨");
        order.setStatus("SHIPPED");
    }
}
```
- **의도 및 코드 설명**: 모든 메서드에서 상태 검증을 반복하는 방어적 프로그래밍
- **뭐가 문제인가**:
  - 검증 코드가 비즈니스 로직보다 많음
  - 개발자가 하나라도 빠뜨리면 런타임 버그
  - 검증 로직 중복 (DRY 위반)
  - 새 상태 추가 시 모든 검증 코드를 수동으로 업데이트

### After: Modern Approach
```java
// [O] 타입으로 불가능한 상태 차단: 검증 없이도 안전
public sealed interface OrderStatus {
    record Unpaid(LocalDateTime createdAt) implements OrderStatus {}
    record Paid(LocalDateTime paidAt, PaymentId paymentId) implements OrderStatus {}
    record Shipped(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
    record Canceled(LocalDateTime canceledAt, CancelReason reason) implements OrderStatus {}
}

public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {
    public Order { items = List.copyOf(items); }
}

// 상태 전이 함수: 타입이 규칙을 강제
public class OrderTransitions {
    // PaidOrder에서만 배송 가능: Unpaid를 넣으면 컴파일 에러
    public static Order ship(Order order, TrackingNumber tracking) {
        return switch (order.status()) {
            case OrderStatus.Paid paid ->
                new Order(order.id(), order.items(),
                    new OrderStatus.Shipped(LocalDateTime.now(), tracking));
            case OrderStatus.Unpaid u ->
                throw new IllegalStateException("결제되지 않은 주문");
            case OrderStatus.Shipped s ->
                throw new IllegalStateException("이미 배송됨");
            case OrderStatus.Delivered d ->
                throw new IllegalStateException("이미 배달 완료");
            case OrderStatus.Canceled c ->
                throw new IllegalStateException("취소된 주문");
        };
    }
}
```
- **의도 및 코드 설명**: sealed interface로 상태를 정의하고, switch expression으로 모든 케이스를 망라적으로 처리
- **무엇이 좋아지나**:
  - "결제됐는데 취소"와 같은 모순 상태가 타입으로 표현 불가능
  - 새 상태(Refunded 등) 추가 시 모든 switch에서 컴파일 에러
  - 상태별 데이터가 정확히 타입에 맞음
  - 비즈니스 로직에서 방어 코드 대폭 감소

### 이해를 위한 부가 상세

**Boolean 필드 vs Sum Type 변환 가이드**:

| Before (Boolean) | After (Sum Type) | 효과 |
|-------------------|-------------------|------|
| `isVerified` + `verifiedAt` | `sealed{Unverified, Verified(at)}` | null 조합 제거 |
| `isPaid` + `paymentId` | `sealed{Unpaid, Paid(paymentId)}` | 모순 상태 제거 |
| `isActive` + `deactivatedAt` | `sealed{Active, Inactive(at)}` | 불일치 방지 |

### 틀리기/놓치기 쉬운 부분
- **"항상 타입으로 강제해야 하나?"**: 비용 대비 효과를 고려. 상태가 2개뿐이면 boolean도 OK
- **"IllegalStateException은 여전히 있잖아?"**: 타입이 모든 경우를 다 막지는 못함. 하지만 "가능한 상태" 중에서만 분기하므로 검증 코드가 최소화됨
- **"sealed interface는 확장이 불가능한데?"**: 그것이 장점. 가능한 상태가 명확히 정의됨

### 꼭 기억할 것
1. **원칙**: 불가능한 상태는 타입으로 표현 자체를 막음
2. **도구**: Sealed Interface + Record + Pattern Matching
3. **효과**: 런타임 검증 코드 대폭 감소, 컴파일 타임 안전성 확보
4. **한계**: 모든 비즈니스 규칙을 타입으로 표현할 수는 없음 → Result/Validator 병행

---

## 2. 상태별 Entity 패턴 (State-per-Entity Pattern)

### 핵심 개념
- **관련 키워드**: Typestate, State Machine, Type-Safe Transition, Workflow as Types
- **통찰**: 함수의 시그니처가 "이 상태에서만 이 연산이 가능함"을 표현하면, 잘못된 순서의 호출이 컴파일 에러가 된다.
- **설명**: `ship()` 함수가 `PaidOrder` 타입만 받으면, 결제 안 된 주문을 배송하려는 시도는 컴파일 단계에서 차단됩니다. 각 상태가 별도 타입이므로, 상태 전이는 "한 타입에서 다른 타입으로의 변환 함수"가 됩니다.

```
State Machine as Types:

  UnpaidOrder ---pay()--> PaidOrder ---ship()--> ShippedOrder
       |                       |                      |
       |                       |                      |
  (no ship())            (no pay())             (no cancel())
  COMPILE ERROR!         COMPILE ERROR!         COMPILE ERROR!
```

### 개념이 아닌 것
- **"모든 Entity를 이렇게 만들어야 한다"**: 상태 전이가 복잡한 핵심 도메인에만 적용
- **"상태가 많으면 타입도 많아진다"**: 그게 장점. 각 상태의 계약이 명확해짐

### Before: Traditional OOP
```java
// [X] 상태를 String으로 관리: 잘못된 전이가 런타임에만 발견됨
public class Order {
    private String status = "UNPAID";

    public void pay(PaymentInfo payment) {
        if (!"UNPAID".equals(status))
            throw new IllegalStateException("이미 결제됨");
        this.status = "PAID";
        this.paymentInfo = payment;
    }

    public void ship(TrackingNumber tracking) {
        // 개발자가 이 검증을 빠뜨리면?
        // if (!"PAID".equals(status)) throw ...
        this.status = "SHIPPED";  // 결제 안 됐는데 배송! 런타임 버그!
    }
}
```
- **의도 및 코드 설명**: String 기반 상태 관리로 전이 규칙을 메서드 내부에서 검증
- **뭐가 문제인가**:
  - 개발자가 검증을 빠뜨리면 잘못된 전이 발생
  - `ship()`을 아무 상태에서나 호출 가능
  - 상태별로 다른 데이터를 관리하기 어려움

### After: Modern Approach
```java
// [O] 상태별 타입: 함수 시그니처가 전이 규칙을 강제
public sealed interface OrderState
    permits UnpaidOrder, PaidOrder, ShippedOrder, DeliveredOrder {}

public record UnpaidOrder(OrderId id, List<OrderItem> items, Money total)
    implements OrderState {
    public UnpaidOrder { items = List.copyOf(items); }
}

public record PaidOrder(OrderId id, List<OrderItem> items, Money total,
    PaymentId paymentId, LocalDateTime paidAt) implements OrderState {
    public PaidOrder { items = List.copyOf(items); }
}

public record ShippedOrder(OrderId id, PaymentId paymentId,
    TrackingNumber tracking, LocalDateTime shippedAt) implements OrderState {}

public record DeliveredOrder(OrderId id, PaymentId paymentId,
    TrackingNumber tracking, LocalDateTime deliveredAt) implements OrderState {}

// 전이 함수: 타입이 규칙을 강제
public class OrderWorkflow {
    // UnpaidOrder만 결제 가능
    public static PaidOrder pay(UnpaidOrder order, PaymentId paymentId) {
        return new PaidOrder(order.id(), order.items(), order.total(),
            paymentId, LocalDateTime.now());
    }

    // PaidOrder만 배송 가능
    public static ShippedOrder ship(PaidOrder order, TrackingNumber tracking) {
        return new ShippedOrder(order.id(), order.paymentId(),
            tracking, LocalDateTime.now());
    }

    // ShippedOrder만 배달 완료 가능
    public static DeliveredOrder deliver(ShippedOrder order) {
        return new DeliveredOrder(order.id(), order.paymentId(),
            order.tracking(), LocalDateTime.now());
    }
}

// 컴파일 에러!
UnpaidOrder unpaid = new UnpaidOrder(id, items, total);
OrderWorkflow.ship(unpaid, tracking);  // Type mismatch: UnpaidOrder != PaidOrder
```
- **의도 및 코드 설명**: 각 상태가 별도 Record, 전이 함수가 입출력 타입으로 규칙 강제
- **무엇이 좋아지나**:
  - 잘못된 순서의 호출이 컴파일 에러
  - 각 상태가 자신에게 필요한 데이터만 보유
  - 런타임 검증 코드 불필요 (타입이 보장)
  - 코드가 도메인 상태 기계를 정확히 반영

### 틀리기/놓치기 쉬운 부분
- **"JPA Entity와 어떻게 공존?"**: JPA Entity는 DB 매핑용, 도메인 Record는 비즈니스 로직용. 변환 계층 필요
- **"취소는 모든 상태에서 가능한데?"**: 다형적 메서드나 별도 함수로 처리

### 꼭 기억할 것
1. **상태 = 타입**: 각 상태가 별도 Record
2. **전이 = 함수**: `pay(UnpaidOrder) → PaidOrder`처럼 입출력 타입이 규칙
3. **잘못된 순서 = 컴파일 에러**: 타입이 맞지 않으면 호출 불가
4. **적용 범위**: 상태 전이가 복잡한 핵심 Aggregate에만 적용

---

## 3. Phantom Type 패턴 (팬텀 타입)

### 핵심 개념
- **관련 키워드**: Phantom Type, Type Parameter, Zero-Cost Abstraction, State Tag
- **통찰**: 타입 파라미터를 런타임에 사용하지 않으면서도, 컴파일 타임에 상태를 추적할 수 있다.
- **설명**: `Email<Unverified>`와 `Email<Verified>`는 런타임에는 같은 구조이지만, 컴파일러는 이들을 다른 타입으로 취급합니다. 인증된 이메일만 받는 함수에 미인증 이메일을 전달하면 컴파일 에러가 발생합니다.

```
Phantom Type = "invisible stamp"

  Email<Unverified>  ---verify()--->  Email<Verified>
  (same data at runtime)              (same data at runtime)
  (different type at compile-time!)   (can call sendWelcome())
```

### 개념이 아닌 것
- **"Phantom Type = 제네릭 활용"**: 제네릭은 보통 실제 데이터를 담지만, Phantom Type은 타입 파라미터가 필드에 사용되지 않음
- **"복잡한 기법"**: 실제로는 marker interface + 제네릭일 뿐

### Before: Traditional OOP
```java
// [X] boolean 플래그로 인증 상태 관리
public class EmailService {
    public void sendWelcomeEmail(String email, boolean isVerified) {
        if (!isVerified)
            throw new IllegalStateException("인증된 이메일에만 전송 가능");
        // 전송 로직
    }
}

// 호출 시 실수: verified 파라미터를 잘못 전달해도 컴파일 OK
emailService.sendWelcomeEmail("test@example.com", false);  // 런타임 에러
```
- **의도 및 코드 설명**: boolean 파라미터로 인증 여부를 전달
- **뭐가 문제인가**:
  - 잘못된 boolean 값을 넣어도 컴파일 성공
  - 검증 누락 시 미인증 이메일에 메일 전송

### After: Modern Approach
```java
// [O] Phantom Type: 타입 파라미터로 상태를 컴파일 타임에 추적
public sealed interface EmailState permits Unverified, Verified {}
public record Unverified() implements EmailState {}
public record Verified() implements EmailState {}

public record Email<S extends EmailState>(String value) {
    public Email {
        if (value == null || !value.contains("@"))
            throw new IllegalArgumentException("유효하지 않은 이메일");
    }
}

// 인증 함수: Unverified → Verified로 상태 전환
public class EmailVerification {
    public static Email<Verified> verify(Email<Unverified> email, String code) {
        // 인증 코드 확인 로직...
        return new Email<>(email.value());  // 같은 데이터, 다른 타입
    }
}

// 인증된 이메일만 받는 함수
public class NotificationService {
    public static void sendWelcome(Email<Verified> email) {
        // 인증 체크 불필요! 타입이 보장
        send(email.value(), "Welcome!");
    }
}

// 컴파일 에러!
Email<Unverified> unverified = new Email<>("test@example.com");
NotificationService.sendWelcome(unverified);
// Type mismatch: Email<Unverified> != Email<Verified>
```
- **의도 및 코드 설명**: 타입 파라미터 `S`는 런타임에 사용되지 않지만, 컴파일러가 상태를 구분
- **무엇이 좋아지나**:
  - 미인증 이메일을 인증 전용 함수에 전달 불가 (컴파일 에러)
  - 런타임 오버헤드 없음 (Zero-cost abstraction)
  - 인증 상태를 타입으로 명시적 추적

### 틀리기/놓치기 쉬운 부분
- **"Java의 Type Erasure 때문에 의미 없지 않나?"**: 런타임에는 지워지지만, 컴파일 타임 검증이 목적
- **"너무 복잡하지 않나?"**: 상태별 Entity 패턴이 더 직관적인 경우가 많음. Phantom Type은 "같은 구조, 다른 상태"일 때 유용

### 꼭 기억할 것
1. **Phantom Type**: 타입 파라미터가 필드에 사용되지 않지만 컴파일 타임 상태 추적
2. **장점**: Zero-cost abstraction, 런타임 오버헤드 없음
3. **적용 범위**: 데이터 구조는 같지만 처리 단계(상태)가 다를 때
4. **대안**: 데이터 구조가 다르면 상태별 Entity 패턴이 더 적합

---

## 4. Optional의 올바른 사용 (Proper Use of Optional)

### 핵심 개념
- **관련 키워드**: Optional, Null Safety, Billion Dollar Mistake, Explicit Absence
- **통찰**: NULL은 "값이 없음"을 암묵적으로 표현하지만, Optional은 "없을 수도 있음"을 타입으로 명시한다.
- **설명**: Tony Hoare가 "10억 달러짜리 실수"라고 자인한 NULL은 타입 시스템에서 "없음"을 구분하지 못합니다. Optional은 "값이 있거나 없다"를 타입으로 강제하여, 개발자가 "없음"을 반드시 처리하도록 합니다.

### 개념이 아닌 것
- **"Optional = null의 래퍼"**: Optional은 "부재의 가능성"을 타입으로 표현하는 도구
- **"Optional을 필드에 쓰면 안 된다"**: 엄격한 규칙은 아니지만, Record 필드에는 피하는 게 관례

### Before: Traditional OOP
```java
// [X] null로 부재 표현: NullPointerException 지뢰밭
public class Customer {
    private String nickname;  // null일 수 있음? 없음? 시그니처에 안 보임

    public String getDisplayName() {
        return nickname.toUpperCase();  // NPE 가능!
    }
}

// 방어적 코딩 필요
if (customer.getNickname() != null) {
    // 사용
} else {
    // null인 경우 처리
}
```
- **의도 및 코드 설명**: null로 값의 부재를 표현하지만, 시그니처에 드러나지 않음
- **뭐가 문제인가**:
  - 어떤 필드가 null일 수 있는지 코드를 읽어야 앎
  - null 체크를 빠뜨리면 NPE
  - "의도적 null"과 "실수로 null"을 구분 불가

### After: Modern Approach
```java
// [O] Optional 또는 Sum Type으로 부재를 타입에 명시
// 방법 1: Optional (간단한 경우)
public record Customer(
    CustomerId id,
    String name,
    Optional<String> nickname  // "없을 수도 있음"이 타입에 명시
) {
    public String displayName() {
        return nickname.orElse(name);  // 컴파일러가 처리를 강제
    }
}

// 방법 2: Sum Type (상태에 따라 데이터가 다른 경우)
public sealed interface ContactInfo {
    record EmailOnly(EmailAddress email) implements ContactInfo {}
    record PhoneOnly(PhoneNumber phone) implements ContactInfo {}
    record Both(EmailAddress email, PhoneNumber phone) implements ContactInfo {}
}
// "이메일은 있는데 전화번호는 없고..." → 타입으로 명확히 표현
```
- **의도 및 코드 설명**: Optional은 부재 가능성을 명시, Sum Type은 가능한 조합을 열거
- **무엇이 좋아지나**:
  - 부재 가능성이 시그니처에 드러남
  - 컴파일러가 "없는 경우" 처리를 강제 (orElse, map 등)
  - NPE 위험 제거

### 틀리기/놓치기 쉬운 부분
- **`Optional.get()` 사용 금지**: NPE와 다를 바 없음. `orElse()`, `map()`, `ifPresent()` 사용
- **"Optional<Optional<T>>"은 냄새**: 설계 재고 필요
- **메서드 파라미터에 Optional 피하기**: 호출자가 `Optional.ofNullable(x)`를 해야 해서 불편

### 꼭 기억할 것
1. **NULL의 문제**: 타입에 드러나지 않는 암묵적 부재
2. **Optional**: "없을 수도 있음"을 타입으로 명시
3. **Sum Type**: 복합적인 부재 조합을 타입으로 표현
4. **규칙**: `get()` 금지, `orElse()`/`map()` 사용
