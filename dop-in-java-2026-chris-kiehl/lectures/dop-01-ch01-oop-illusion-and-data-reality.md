# Chapter 1: 객체 지향의 환상과 데이터의 실체 (The Illusion of OOP and the Reality of Data)

## 학습 목표 (Learning Objectives)
1. OOP의 캡슐화가 대규모 시스템에서 야기하는 문제를 설명할 수 있다
2. DOP의 4대 원칙을 이해하고 각각의 의미를 설명할 수 있다
3. 데이터와 로직 분리의 장점을 코드로 보여줄 수 있다
4. God Class 안티패턴을 인식하고 리팩토링 방향을 제시할 수 있다
5. DOP와 OOP의 차이점을 표로 정리할 수 있다

---

## 1.1 왜 우리는 고통스러운가? (Why Do We Suffer?)

> **다른 말로 (In other words):**
> - "캡슐화라는 이름으로 데이터와 로직을 한 곳에 모으면 복잡성이 집중되어 변경이 어려워진다"
> - "God Class는 모든 것을 알아서 모든 것에 영향을 받는 안티패턴"

> **🎯 왜 배우는가?**
>
> "이 클래스만 수정하면 되는데 왜 테스트가 20개나 깨지지?"라는 경험이 있으신가요?
> 이 섹션에서는 OOP의 캡슐화가 대규모 시스템에서 왜 문제가 되는지 이해하고,
> **God Class 안티패턴을 식별하는 눈**을 기를 수 있습니다.

### 캡슐화의 약속과 현실

우리는 수십 년간 **"데이터와 그 데이터를 조작하는 메서드를 한 클래스에 묶어야 한다(캡슐화)"**고 배웠습니다. 이 이론은 작은 프로그램에서는 완벽하게 작동합니다. 하지만 수백만 라인의 엔터프라이즈 시스템에서는 다음과 같은 문제를 야기합니다.

### ❌ 안티패턴: God Class

**왜 문제인가?**
- **책임 과다**: 하나의 클래스가 주문, 결제, 배송, 취소, 알림 등 모든 책임을 짊어짐
- **변경 영향도 폭발**: 한 기능 수정이 다른 모든 기능에 영향을 줄 수 있음
- **테스트 불가능**: 단일 기능 테스트에 전체 의존성 필요

**Code 1.1**: God Class 안티패턴 - 모든 것을 담은 Order 클래스
```java
// 안티패턴: 모든 것을 담은 God Class
public class Order {
    // 주문 기본 정보
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;

    // 고객 정보
    private Long customerId;
    private String customerName;
    private String customerEmail;

    // 상품 정보
    private List<OrderItem> items;
    private BigDecimal totalAmount;

    // 결제 정보
    private String paymentMethod;
    private boolean isPaid;
    private LocalDateTime paidAt;

    // 배송 정보
    private String shippingAddress;
    private boolean isShipped;
    private String trackingNumber;

    // 취소/환불 정보
    private boolean isCanceled;
    private String cancelReason;
    private BigDecimal refundAmount;

    // 쿠폰 정보
    private String appliedCouponCode;
    private BigDecimal discountAmount;

    // 비즈니스 로직들
    public void applyDiscount(String couponCode) { ... }
    public void processPayment(PaymentInfo info) { ... }
    public void ship(String trackingNumber) { ... }
    public void cancel(String reason) { ... }
    public void refund() { ... }
    public void sendNotification() { ... }
    public void updateInventory() { ... }
    public void calculateTax() { ... }
    // ... 메서드가 50개 이상
}
```

> **Visual Reference - Hexagonal Architecture:**
> ![Hexagonal Architecture](https://8thlight.com/wp-content/uploads/2023/02/ports-adapters-1.png)
> *Source: [8th Light - Ports and Adapters](https://8thlight.com/insights/a-color-coded-guide-to-ports-and-adapters)*

**Figure 1.1**: God Class → DOP 분리 구조도

```
┌───────────────────────────────────────────────────────────────────────┐
│                        [X] God Class (OOP)                            │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                           Order                                 │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌───────────────┐            │  │
│  │  │  Order Data  │ │ Payment Data │ │ Shipping Data │ ...        │  │
│  │  └──────────────┘ └──────────────┘ └───────────────┘            │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌───────────────┐            │  │
│  │  │Discount Logic│ │ Payment Logic│ │ Shipping Logic│ ...        │  │
│  │  └──────────────┘ └──────────────┘ └───────────────┘            │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                      Everything Mixed Together                        │
└───────────────────────────────────────────────────────────────────────┘
                                  ↓
                             Refactoring
                                  ↓
┌───────────────────────────────────────────────────────────────────────┐
│                     [O] DOP Separated Structure                       │
│                                                                       │
│  ┌──────────────────┐       ┌──────────────────────────────────────┐  │
│  │    Data Layer    │       │           Logic Layer                │  │
│  │    (Records)     │       │       (Static Functions)             │  │
│  │  ┌────────────┐  │       │  ┌────────────────────────────────┐  │  │
│  │  │   Order    │  │──────→│  │      OrderCalculations         │  │  │
│  │  └────────────┘  │       │  │  + calculateTotal()            │  │  │
│  │  ┌────────────┐  │       │  │  + applyDiscount()             │  │  │
│  │  │  Payment   │  │──────→│  └────────────────────────────────┘  │  │
│  │  └────────────┘  │       │  ┌────────────────────────────────┐  │  │
│  │  ┌────────────┐  │       │  │      OrderValidations          │  │  │
│  │  │  Shipping  │  │──────→│  │  + validateStock()             │  │  │
│  │  └────────────┘  │       │  └────────────────────────────────┘  │  │
│  └──────────────────┘       └──────────────────────────────────────┘  │
│        Data Only                         Logic Only                   │
└───────────────────────────────────────────────────────────────────────┘
```

### 비유: 레고 vs 점토

> **OOP의 God Class는 점토 덩어리와 같습니다.**
>
> 처음에 점토로 무언가를 만들면 자유롭게 형태를 바꿀 수 있어 좋습니다.
> 하지만 점토 덩어리가 커지면 문제가 시작됩니다:
> - 한 부분을 수정하면 다른 부분이 찌그러집니다
> - 여러 사람이 동시에 작업할 수 없습니다
> - 일부분만 떼어서 재사용할 수 없습니다
>
> **DOP는 레고 블록과 같습니다.**
>
> 레고는 각 블록(데이터)이 독립적이고, 조립 방법(로직)은 별도입니다:
> - 블록 하나를 바꿔도 다른 블록에 영향이 없습니다
> - 여러 사람이 동시에 다른 블록을 조립할 수 있습니다
> - 같은 블록을 여러 구조물에서 재사용할 수 있습니다

### God Class의 실제 피해

**Table 1.1**: God Class가 야기하는 문제 유형과 결과

| 문제 유형 | 증상 | 결과 |
|----------|-----|------|
| 정보의 감옥 | 데이터가 객체 안에 갇힘 | DTO 변환 코드가 전체의 50% |
| 맥락의 혼재 | 하나의 클래스가 여러 역할 수행 | 수정 시 예상치 못한 영역 파괴 |
| 테스트 지옥 | 하나를 테스트하려면 전체 의존성 필요 | Mock 객체 20개 이상 필요 |
| 병합 충돌 | 모든 팀이 같은 파일 수정 | Git 충돌이 일상 |

---

## 1.2 DOP의 4대 원칙 (The 4 Principles of DOP)

> **다른 말로 (In other words):**
> - "데이터는 Record로 투명하게, 로직은 static 함수로 순수하게, 변경은 새 객체로 불변하게"
> - "타입으로 복잡성을 제어하고 컴파일러가 검증하게 하는 원칙"

> **🎯 왜 배우는가?**
>
> "왜 코드를 이렇게 작성해야 하는지" 원칙을 모르면 일관성 없는 코드가 됩니다.
> DOP의 4대 원칙을 이해하면 **팀 전체가 동일한 기준으로 코드를 작성**할 수 있고,
> 코드 리뷰 시 "왜 이렇게 해야 하는지" 명확하게 설명할 수 있습니다.

Chris Kiehl은 이러한 문제를 해결하기 위해 4가지 원칙을 제시합니다.

### 원칙 1: 코드와 데이터를 분리하라 (Separate Code from Data)

#### ❌ 안티패턴: 데이터와 로직 혼합

**왜 문제인가?**
- **재사용 불가**: Order 객체 없이 할인 로직을 사용할 수 없음
- **테스트 어려움**: 할인 계산 테스트에 Order 객체 전체가 필요함
- **의존성 전파**: Order가 변경되면 할인 로직도 영향받음

**Code 1.2**: OOP 방식 - 데이터와 로직이 섞여있음
```java
public class Order {
    private BigDecimal amount;
    private String couponCode;

    public BigDecimal calculateDiscountedPrice() {
        if (couponCode != null) {
            return amount.multiply(new BigDecimal("0.9"));
        }
        return amount;
    }
}
```

#### ✅ 권장패턴: 데이터와 로직 분리

**왜 좋은가?**
- **독립적 테스트**: OrderCalculator를 Order 없이도 단위 테스트 가능
- **재사용성**: 같은 로직을 다른 데이터 타입에도 적용 가능
- **변경 격리**: 데이터 구조 변경과 비즈니스 로직 변경이 독립적

**Code 1.3**: DOP 방식 - 데이터와 로직을 분리
```java
// 데이터: Record
public record Order(BigDecimal amount, String couponCode) {}

// 로직: Static 메서드를 가진 클래스
public class OrderCalculator {
    public static BigDecimal calculateDiscountedPrice(Order order) {
        if (order.couponCode() != null) {
            return order.amount().multiply(new BigDecimal("0.9"));
        }
        return order.amount();
    }
}
```

> **💡 Q&A: OrderCalculator는 어느 패키지에 위치해야 하나요?**
>
> **domain** 패키지가 적합합니다. 이 로직은 비즈니스의 핵심 규칙(할인 계산)을 담고 있기 때문입니다.
> DB 저장이나 알림 발송 같은 로직이라면 `application`이나 `infrastructure`로 가겠지만,
> 순수 비즈니스 계산 로직은 도메인의 영역입니다.
>
> **💡 Q&A: 왜 static 메서드를 쓰나요?**
>
> 이것이 **함수형 프로그래밍 스타일**입니다:
> - **상태 없음(Stateless)**: 함수들은 외부의 어떤 상태도 기억할 필요가 없습니다
> - **네임스페이스(Namespace)**: `OrderCalculator`는 관련된 함수들을 모아놓은 폴더 개념입니다
> - `java.lang.Math.max()`를 쓸 때 `new Math()`를 하지 않는 것과 같은 이치입니다

### 원칙 2: 데이터를 일반적인 형태로 표현하라 (Represent Data with Generic Structures)

> **다른 말로 (In other words):**
> - "Record + 표준 컬렉션(List, Map, Set)을 사용하고, 커스텀 컬렉션 클래스를 만들지 마라"
> - "데이터 구조는 투명하게, 모든 도구와 프레임워크가 자연스럽게 인식할 수 있게"

> **🎯 왜 배우는가?**
> "왜 OrderItemList extends ArrayList<OrderItem>을 만들면 안 되나요?"라는 질문에 답할 수 있습니다.
> 표준 구조를 사용하면 Stream API, Jackson, JPA가 자연스럽게 작동하고,
> 팀원 누구나 코드를 즉시 이해할 수 있습니다.

#### ❌ 안티패턴: 커스텀 컬렉션 클래스

**왜 문제인가?**
- IDE 자동완성이 제대로 작동하지 않음
- Jackson/JPA가 직렬화 방법을 모름 (커스텀 어댑터 필요)
- Stream API를 쓰려면 내부 List를 다시 꺼내야 함
- 새 팀원이 "이 클래스가 뭐지?" 학습 비용 발생

**Code 1.4a**: 커스텀 컬렉션 안티패턴
```java
// Bad: 비즈니스 로직이 섞인 커스텀 컬렉션
public class OrderItems extends ArrayList<OrderItem> {
    public Money calculateTotal() {
        return this.stream()
            .map(item -> item.price().multiply(item.quantity()))
            .reduce(Money.ZERO, Money::add);
    }

    public Map<Category, List<OrderItem>> groupByCategory() { ... }
    public void addWithValidation(OrderItem item) { ... }
}

// 사용할 때
OrderItems items = new OrderItems();
items.addWithValidation(item);
Money total = items.calculateTotal();  // 데이터와 로직이 뒤섞임
```

#### ✅ 권장패턴: 표준 컬렉션 + 별도 함수

**왜 좋은가?**
- Stream API가 자연스럽게 작동
- Jackson이 List<OrderItem>을 자동 직렬화
- 어떤 Java 개발자도 List<T>를 즉시 이해
- 로직 재사용: 다른 List<OrderItem>에도 같은 함수 적용 가능

**Code 1.4b**: DOP 방식 - 표준 컬렉션 사용
```java
// Good: 순수한 데이터 (표준 컬렉션)
public record Order(
    OrderId id,
    List<OrderItem> items,  // 표준 List 사용
    Money totalAmount
) {}

// 로직은 별도 클래스에
public class OrderCalculations {
    public static Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.price().multiply(item.quantity()))
            .reduce(Money.ZERO, Money::add);
    }

    public static Map<Category, List<OrderItem>> groupByCategory(List<OrderItem> items) {
        return items.stream().collect(Collectors.groupingBy(OrderItem::category));
    }
}

// 사용할 때
Order order = new Order(id, List.of(item1, item2), Money.ZERO);
Money total = OrderCalculations.calculateTotal(order.items());
```

> **💡 Q&A: 그럼 Value Object(Money, Email 등)도 만들면 안 되나요?**
>
> Value Object는 만들어도 됩니다. 핵심은 **컬렉션을 래핑하지 마라**입니다.
> - `Money`, `Email`, `OrderId` (O): 의미 있는 단일 값 + 검증
> - `OrderItems extends List` (X): 컬렉션 래핑은 도구 호환성을 깨뜨림

> **💡 Q&A: Map<String, Object>처럼 타입 안전하지 않은 것도 Generic Structure인가요?**
>
> 아닙니다. DOP에서 "Generic"은 `Map<String, Object>` 같은 동적 타입이 아니라,
> **타입 안전한 표준 구조**를 의미합니다:
> - `List<OrderItem>` (O): 타입 안전한 표준 컬렉션
> - `Map<String, Object>` (X): 런타임 캐스팅 필요, 타입 불안전

### 원칙 3: 데이터는 불변이다 (Data is Immutable)

**Code 1.5**: 가변 객체 (안티패턴)
```java
public class MutableOrder {
    private String status;

    public void setStatus(String status) {
        this.status = status;  // 언제 어디서 바뀔지 모름
    }
}
```

**Code 1.6**: 불변 객체 (DOP 권장)
```java
public record Order(OrderId id, Money amount, OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, this.amount, newStatus);  // 새 객체 반환
    }
}
```

### 원칙 4: 스키마와 표현을 분리하라 (Separate Schema from Representation)

> **다른 말로 (In other words):**
> - "스키마(타입 정의)는 '어떤 상태가 존재할 수 있는가'를 정의하고, 표현(인스턴스)은 '지금 실제로 무엇인가'를 담는다"
> - "검증은 시스템 경계에서 한 번, 내부에서는 타입을 신뢰하라"

> **🎯 왜 배우는가?**
> "모든 메서드마다 null 체크와 유효성 검증을 해야 하나요?"라는 고민을 해결합니다.
> 경계에서 검증하고 타입으로 보장하면, 내부 로직은 검증 없이 깔끔하게 작성할 수 있습니다.

#### SQL과 비교하면 이해가 쉽습니다

**Table 1.3**: SQL과 Java DOP의 스키마/표현 대응

| SQL | Java DOP | 역할 |
|-----|----------|------|
| CREATE TABLE (DDL) | sealed interface + record 정의 | 스키마: 가능한 구조 정의 |
| INSERT/SELECT 결과 (DML) | record 인스턴스 | 표현: 실제 데이터 |
| NOT NULL, CHECK 제약 | compact constructor 검증 | 경계에서 유효성 보장 |

#### ❌ 안티패턴: 모든 곳에서 검증

**왜 문제인가?**
- 같은 검증 로직이 10개 메서드에 중복
- 검증 로직 변경 시 모든 곳을 찾아 수정해야 함
- 방어적 코딩이 비즈니스 로직을 가림
- 예외가 어디서든 발생할 수 있어 예측 불가

**Code 1.7a**: 모든 메서드에서 검증하는 안티패턴
```java
public class OrderService {
    public void processOrder(Order order) {
        // 매번 검증해야 함
        if (order == null) throw new IllegalArgumentException();
        if (order.amount() == null) throw new IllegalArgumentException();
        if (order.amount().compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException();
        // ... 드디어 비즈니스 로직
    }

    public void calculateTax(Order order) {
        // 또 검증
        if (order == null) throw new IllegalArgumentException();
        if (order.amount() == null) throw new IllegalArgumentException();
        // ... 또 비즈니스 로직
    }
}
```

#### ✅ 권장패턴: 경계에서 검증, 내부에서 신뢰

**왜 좋은가?**
- 검증 로직이 한 곳에 집중
- 내부 로직은 순수하게 비즈니스만 담당
- 타입 시스템이 유효성을 컴파일 타임에 보장
- 새로운 상태 추가 시 컴파일러가 누락된 처리를 알려줌

**Code 1.7b**: DOP 방식 - 경계에서 검증
```java
// 스키마: 가능한 상태 정의 (sealed interface)
public sealed interface OrderStatus {
    record Pending(Instant createdAt) implements OrderStatus {}
    record Paid(PaymentId paymentId, Instant paidAt) implements OrderStatus {}
    record Shipped(TrackingNumber tracking) implements OrderStatus {}
    record Cancelled(String reason, Instant cancelledAt) implements OrderStatus {}
}

// 표현: 실제 데이터 (record 인스턴스)
public record Order(OrderId id, Money amount, OrderStatus status) {
    // 경계(생성 시점)에서 한 번만 검증
    public Order {
        Objects.requireNonNull(id, "주문 ID는 필수입니다");
        Objects.requireNonNull(amount, "금액은 필수입니다");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다");
        }
    }
}

// 내부 로직: 검증 없이 타입을 신뢰
public class OrderCalculations {
    public static Money calculateTax(Order order) {
        // order가 null이거나 amount가 음수일 가능성 없음
        // 생성 시점에 이미 검증됨
        return order.amount().multiply(TAX_RATE);
    }
}

// API 경계에서 Result로 검증
public class OrderController {
    public Result<Order, ValidationError> createOrder(OrderRequest request) {
        // 시스템 경계: 외부 입력 검증
        return OrderValidator.validate(request)
            .map(validated -> new Order(
                OrderId.generate(),
                validated.amount(),
                new OrderStatus.Pending(Instant.now())
            ));
    }
}
```

#### 상태 전이도 스키마로 제어

**Code 1.7c**: 타입으로 보장되는 상태 전이
```java
public sealed interface OrderStatus {
    // 각 상태에서 가능한 전이만 메서드로 제공
    default Paid pay(PaymentId paymentId) {
        return switch (this) {
            case Pending p -> new Paid(paymentId, Instant.now());
            case Paid p -> throw new IllegalStateException("이미 결제됨");
            case Shipped s -> throw new IllegalStateException("배송 시작됨");
            case Cancelled c -> throw new IllegalStateException("취소됨");
        };
    }

    // 새로운 상태 Refunded를 추가하면?
    // -> 컴파일 에러: "switch가 완전하지 않음, Refunded 케이스 누락"
}
```

> **💡 Q&A: "Parse, Don't Validate"와 무슨 관계인가요?**
>
> 같은 철학입니다:
> - **Validate**: `boolean isValid(String email)` -> 검증 결과만 반환, 이후에도 String
> - **Parse**: `Email parse(String input)` -> 검증 + 타입 변환, 이후는 Email 타입
>
> Parse를 하면 타입 자체가 "검증됨"을 보장하므로, 이후 코드에서 재검증이 불필요합니다.

> **💡 Q&A: Record의 compact constructor에서 검증해도 되나요?**
>
> 네, 이것이 권장 패턴입니다. Record의 compact constructor는 "경계"입니다:
> ```java
> public record Email(String value) {
>     public Email {  // compact constructor = 생성 경계
>         if (!value.contains("@")) throw new IllegalArgumentException();
>     }
> }
> ```
> 단, **비즈니스 규칙 검증**(예: "이 쿠폰이 이 주문에 적용 가능한가")은 별도 Validator 클래스로 분리하세요.

---

## 1.3 DOP vs OOP 비교 (DOP vs OOP Comparison)

> **🎯 왜 배우는가?**
>
> 기존 OOP 프로젝트에서 DOP를 도입할 때, "이게 정말 나은 건가?"라는 의문이 들 수 있습니다.
> 이 비교표를 통해 **각 패러다임의 트레이드오프를 명확히 이해**하고,
> 상황에 맞는 선택을 할 수 있게 됩니다.

**Table 1.2**: DOP와 OOP의 핵심 차이점 비교

| 관점 | OOP | DOP |
|-----|-----|-----|
| 기본 단위 | 객체 (데이터 + 행위) | 데이터(Record) + 함수(Class) |
| 상태 관리 | 가변 (Mutable) | 불변 (Immutable) |
| 캡슐화 | 데이터 은닉 | 데이터 노출 (투명성) |
| 다형성 | 상속, 인터페이스 | Sealed Interface + Pattern Matching |
| 복잡성 관리 | 추상화로 숨김 | 타입으로 제한 |
| 테스트 | Mock 객체 필요 | 순수 함수, Mock 불필요 |
| 재사용 | 상속 | 합성 |

---

## 1.4 이커머스 리팩토링 예시: Before/After (E-commerce Refactoring Example)

> **🎯 왜 배우는가?**
>
> 실제 이커머스 코드에서 DOP를 어떻게 적용하는지 보고 싶으신가요?
> 이 예시를 통해 **God Class를 단계별로 분해하는 실전 기술**을 익히고,
> 내일 당장 레거시 코드 리팩토링에 적용할 수 있습니다.

**Code 1.8**: God Class OrderService (리팩토링 전)
```java
public class OrderService {
    private OrderRepository orderRepository;
    private PaymentGateway paymentGateway;
    private InventoryService inventoryService;
    private NotificationService notificationService;
    private CouponService couponService;

    public Order createOrder(CreateOrderRequest request) {
        // 1. 쿠폰 검증
        Coupon coupon = couponService.validate(request.getCouponCode());

        // 2. 재고 확인
        for (OrderItem item : request.getItems()) {
            if (!inventoryService.hasStock(item.getProductId(), item.getQuantity())) {
                throw new OutOfStockException();
            }
        }

        // 3. 금액 계산 (쿠폰 할인 적용)
        BigDecimal total = calculateTotal(request.getItems());
        BigDecimal discounted = coupon.apply(total);

        // 4. 주문 생성
        Order order = new Order();
        order.setItems(request.getItems());
        order.setTotalAmount(discounted);
        order.setStatus("CREATED");

        // 5. 저장
        orderRepository.save(order);

        // 6. 재고 차감
        for (OrderItem item : request.getItems()) {
            inventoryService.decreaseStock(item.getProductId(), item.getQuantity());
        }

        // 7. 알림 발송
        notificationService.sendOrderCreatedNotification(order);

        return order;
    }
}
```

**Code 1.9**: DOP 스타일 리팩토링 (리팩토링 후)
```java
// 1. 순수한 데이터 정의
public record OrderItem(ProductId productId, Quantity quantity, Money unitPrice) {}

public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderItem> items,
    Money totalAmount,
    OrderStatus status
) {}

public sealed interface OrderStatus {
    record Created(LocalDateTime at) implements OrderStatus {}
    record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {}
    record Shipped(LocalDateTime at, TrackingNumber tracking) implements OrderStatus {}
    record Delivered(LocalDateTime at) implements OrderStatus {}
    record Cancelled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
}

// 2. 순수 함수: 비즈니스 로직
public class OrderCalculations {
    public static Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.unitPrice().multiply(item.quantity().value()))
            .reduce(Money.zero(), Money::add);
    }

    public static Money applyDiscount(Money total, DiscountRate rate) {
        return rate.applyTo(total);
    }
}

// 3. 검증 로직
public class OrderValidations {
    public static Result<List<OrderItem>, StockError> validateStock(
        List<OrderItem> items,
        StockInfo stockInfo
    ) {
        for (OrderItem item : items) {
            if (!stockInfo.hasEnough(item.productId(), item.quantity())) {
                return Result.failure(new StockError(item.productId()));
            }
        }
        return Result.success(items);
    }
}

// 4. 오케스트레이션 (Impure Shell)
public class OrderOrchestrator {
    public Result<Order, OrderError> createOrder(CreateOrderRequest request) {
        // Impure: 데이터 수집
        StockInfo stock = inventoryService.getStockInfo(request.productIds());
        Optional<Coupon> coupon = couponService.find(request.couponCode());

        // Pure: 비즈니스 로직
        return OrderValidations.validateStock(request.items(), stock)
            .map(items -> OrderCalculations.calculateTotal(items))
            .map(total -> coupon.map(c -> c.applyTo(total)).orElse(total))
            .map(finalAmount -> new Order(
                OrderId.generate(),
                request.customerId(),
                request.items(),
                finalAmount,
                new OrderStatus.Created(LocalDateTime.now())
            ));

        // Impure: 부수효과
        // (저장, 알림 등은 Result가 성공일 때만 별도로 처리)
    }
}
```

---

## 퀴즈 Chapter 1 (Quiz Chapter 1)

### Q1.1 [개념 확인] DOP의 4대 원칙
다음 중 DOP의 원칙이 **아닌** 것은?

A. 코드와 데이터를 분리하라<br/>
B. 데이터는 불변이어야 한다<br/>
C. 상속을 통해 재사용성을 높여라<br/>
D. 스키마와 표현을 분리하라

---

### Q1.2 [코드 분석] God Class 식별
다음 코드에서 God Class 안티패턴의 증거가 **아닌** 것은?

```java
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private int stockQuantity;

    public void decreaseStock(int amount) { ... }
    public void updatePrice(BigDecimal newPrice) { ... }
    public void sendLowStockAlert() { ... }
    public void syncToSearchEngine() { ... }
    public void calculateTax() { ... }
    public void generateBarcode() { ... }
}
```

A. 재고 관리, 가격 관리, 알림, 검색 동기화가 한 클래스에 있음<br/>
B. id, name, price 필드가 함께 정의되어 있음<br/>
C. 세금 계산과 바코드 생성이 Product에 있음<br/>
D. 외부 시스템(검색엔진)과의 통신이 도메인 객체에 있음

---

### Q1.3 [설계 문제] 리팩토링 방향
위의 `Product` 클래스를 DOP 스타일로 리팩토링할 때 가장 적절한 접근은?

A. 모든 메서드를 private으로 바꿔서 캡슐화를 강화한다<br/>
B. Product를 Record로 바꾸고, 각 기능을 별도 서비스 클래스로 분리한다<br/>
C. ProductService를 만들어 모든 로직을 옮긴다<br/>
D. 각 메서드마다 별도의 Product 하위 클래스를 만든다

---

### Q1.4 [개념 확인] 불변성의 장점
데이터 불변성이 제공하는 장점으로 **올바르지 않은** 것은?

A. 멀티스레드 환경에서 동기화 없이 안전하게 공유할 수 있다<br/>
B. 객체의 상태가 예측 가능하여 디버깅이 쉽다<br/>
C. 메모리 사용량이 줄어든다<br/>
D. 함수의 부수효과를 줄여 테스트가 쉬워진다

---

### Q1.5 [코드 작성] DOP 스타일 변환
다음 OOP 코드를 DOP 스타일로 변환하세요.

```java
// Before: OOP
public class ShoppingCart {
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        items.add(item);
    }

    public BigDecimal getTotal() {
        return items.stream()
            .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

힌트: Record와 static 메서드를 사용하세요.

---

### Q1.6 [함정 문제] DOP 철학 ⭐
다음 중 DOP(Data-Oriented Programming) 철학에 **가장 어긋나는** 코드는?

**A. 레코드 내에 검증 로직 포함**
```java
public record Email(String value) {
    public Email { if (!value.contains("@")) throw new IllegalArgumentException(); }
}
```

**B. 레코드 내에 파생 데이터 계산 메서드 포함**
```java
public record Rectangle(int width, int height) {
    public int area() { return width * height; }
}
```

**C. 레코드 내에 데이터베이스 저장 로직 포함 (Active Record 패턴)**
```java
public record User(String id, String name) {
    public void saveToDb() {
        Database.getConnection().save(this);
    }
}
```

**D. 다른 레코드를 반환하는 Wither 메서드 포함**
```java
public record Point(int x, int y) {
    public Point withX(int newX) { return new Point(newX, y); }
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.
