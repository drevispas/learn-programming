# Java로 정복하는 데이터 지향 프로그래밍 v0.2
## 참조: Data-Oriented Programming in Java

**버전**: 0.2 (Enhanced Edition)

**대상**: 백엔드 자바 개발자

**목표**: 복잡성을 수학적으로 제어하고, 컴파일러에게 검증을 위임하는 견고한 시스템 구축

**도구**: Java 25 (Record, Sealed Interface, Pattern Matching, Record Patterns)

**원전**: *Data-Oriented Programming in Java* by Chris Kiehl

---

## Java 17 → Java 25 DOP 관련 변경 사항

| 버전 | 핵심 기능 | JEP | 상태 | DOP 영향 |
|------|----------|-----|------|---------|
| **Java 21** | Record Patterns | 440 | Final | ⭐ 핵심 - 패턴에서 레코드 분해 |
| **Java 21** | Pattern Matching for switch | 441 | Final | ⭐ 핵심 - switch 표현식 완성 |
| **Java 21** | Sequenced Collections | 431 | Final | 유용 - `getFirst()`, `getLast()` |
| **Java 22** | Unnamed Variables & Patterns (`_`) | 456 | Final | 중요 - 사용하지 않는 변수 표시 |
| **Java 25** | Primitive Types in Patterns | 507 | Preview | 보통 - 기본 타입 패턴 매칭 |
| **Java 25** | Scoped Values | 506 | Final | 보통 - 불변 컨텍스트 전달 |

> ⚠️ **중요**: JEP 468 (Derived Record Creation / `with` expression)은 Java 25에 **포함되지 않았습니다**.
> 따라서 Record의 값을 변경한 새 객체를 만들려면 수동 `withXxx()` 메서드가 여전히 필요합니다.

---

## 목차

### Part I: 사고의 전환 (Foundations)
- **Chapter 1**: 객체 지향의 환상과 데이터의 실체
- **Chapter 2**: 데이터란 무엇인가? (Identity vs Value)

### Part II: 데이터 모델링의 수학 (Algebraic Data Types)
- **Chapter 3**: 타입 시스템의 기수(Cardinality) 이론
- **Chapter 4**: 불가능한 상태를 표현 불가능하게 만들기

### Part III: 데이터의 흐름과 제어 (Behavior & Control Flow)
- **Chapter 5**: 전체 함수(Total Functions)와 실패 처리
- **Chapter 6**: 파이프라인과 결정론적 시스템

### Part IV: 고급 기법과 대수적 추론 (Advanced Theory)
- **Chapter 7**: 대수적 속성을 활용한 설계
- **Chapter 8**: 인터프리터 패턴: Rule as Data ⭐ **(강화)**

### Part V: 레거시 탈출 (Refactoring & Architecture)
- **Chapter 9**: 현실 세계의 DOP: JPA/Spring과의 공존

### Appendix
- **Appendix A**: 흔한 실수와 안티패턴 모음 **(강화)**
- **Appendix B**: DOP Java 치트시트
- **Appendix C**: 전체 퀴즈 정답 및 해설 **(강화)**
- **Appendix D**: Final Boss Quiz - DOP 마스터 검증 **(신규)**

---

# Part I: 사고의 전환 (Foundations)

---

## Chapter 1: 객체 지향의 환상과 데이터의 실체

### 학습 목표
1. OOP의 캡슐화가 대규모 시스템에서 야기하는 문제를 설명할 수 있다
2. DOP의 4대 원칙을 이해하고 각각의 의미를 설명할 수 있다
3. 데이터와 로직 분리의 장점을 코드로 보여줄 수 있다
4. God Class 안티패턴을 인식하고 리팩토링 방향을 제시할 수 있다
5. DOP와 OOP의 차이점을 표로 정리할 수 있다

---

### 1.1 왜 우리는 고통스러운가?

#### 캡슐화의 약속과 현실

우리는 수십 년간 **"데이터와 그 데이터를 조작하는 메서드를 한 클래스에 묶어야 한다(캡슐화)"**고 배웠습니다. 이 이론은 작은 프로그램에서는 완벽하게 작동합니다. 하지만 수백만 라인의 엔터프라이즈 시스템에서는 다음과 같은 문제를 야기합니다.

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

#### 비유: 레고 vs 점토

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

#### God Class의 실제 피해

| 문제 유형 | 증상 | 결과 |
|----------|-----|------|
| 정보의 감옥 | 데이터가 객체 안에 갇힘 | DTO 변환 코드가 전체의 50% |
| 맥락의 혼재 | 하나의 클래스가 여러 역할 수행 | 수정 시 예상치 못한 영역 파괴 |
| 테스트 지옥 | 하나를 테스트하려면 전체 의존성 필요 | Mock 객체 20개 이상 필요 |
| 병합 충돌 | 모든 팀이 같은 파일 수정 | Git 충돌이 일상 |

---

### 1.2 DOP의 4대 원칙

Chris Kiehl은 이러한 문제를 해결하기 위해 4가지 원칙을 제시합니다.

#### 원칙 1: 코드와 데이터를 분리하라

##### Before (OOP): 데이터와 로직이 섞여있음

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

##### After (DOP): 데이터와 로직을 분리

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

#### 비유: 도서관과 사서

> **데이터는 도서관의 책이고, 로직은 사서입니다.**
>
> 좋은 도서관에서는:
> - 책(데이터)은 책장에 정돈되어 있습니다
> - 사서(로직)는 책을 찾고, 대출하고, 정리하는 역할을 합니다
> - 책에 "대출 방법"이 적혀있지 않습니다
>
> 나쁜 도서관에서는:
> - 각 책에 "나를 어떻게 대출해야 하는지" 적혀있습니다
> - 대출 정책이 바뀌면 모든 책을 수정해야 합니다
> - 책마다 대출 방식이 달라 혼란스럽습니다

#### 원칙 2: 데이터를 일반적인 형태로 표현하라

```java
// Bad: 비슷한 구조가 여러 클래스로 분산
public class OrderSummary {
    private String id;
    private BigDecimal amount;
    private String status;
}

public class PaymentSummary {
    private String id;
    private BigDecimal amount;
    private String method;
}

// Good: 타입 안전한 Record
public record OrderSummary(OrderId id, Money amount, OrderStatus status) {}
public record PaymentSummary(PaymentId id, Money amount, PaymentMethod method) {}
```

#### 원칙 3: 데이터는 불변(Immutable)이다

##### Before: 가변 객체

```java
public class MutableOrder {
    private String status;

    public void setStatus(String status) {
        this.status = status;  // 언제 어디서 바뀔지 모름
    }
}
```

##### After: 불변 객체

```java
public record Order(OrderId id, Money amount, OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, this.amount, newStatus);  // 새 객체 반환
    }
}
```

#### 원칙 4: 스키마와 표현을 분리하라

```java
// Bad: 생성자에서 모든 검증 수행
public record Order(OrderId id, Money amount) {
    public Order {
        if (amount.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 양수여야 합니다");
        }
        // 수십 가지 검증 로직...
    }
}

// Good: 경계에서 검증, 내부에서는 신뢰
public record Order(OrderId id, Money amount) {}  // 데이터 표현만

public class OrderValidator {
    public static Result<Order, ValidationError> validate(Order order) {
        // 시스템 경계(API, UI)에서만 검증
        if (order.amount().value().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure(new ValidationError("금액은 양수여야 합니다"));
        }
        return Result.success(order);
    }
}
```

---

### 1.3 DOP vs OOP 비교

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

### 1.4 이커머스 리팩토링 예시: Before/After

#### Before: God Class

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

#### After: DOP 스타일

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

### 퀴즈 Chapter 1

#### Q1.1 [개념 확인] DOP의 4대 원칙
다음 중 DOP의 원칙이 **아닌** 것은?

A. 코드와 데이터를 분리하라
B. 데이터는 불변이어야 한다
C. 상속을 통해 재사용성을 높여라
D. 스키마와 표현을 분리하라

---

#### Q1.2 [코드 분석] God Class 식별
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

A. 재고 관리, 가격 관리, 알림, 검색 동기화가 한 클래스에 있음
B. id, name, price 필드가 함께 정의되어 있음
C. 세금 계산과 바코드 생성이 Product에 있음
D. 외부 시스템(검색엔진)과의 통신이 도메인 객체에 있음

---

#### Q1.3 [설계 문제] 리팩토링 방향
위의 `Product` 클래스를 DOP 스타일로 리팩토링할 때 가장 적절한 접근은?

A. 모든 메서드를 private으로 바꿔서 캡슐화를 강화한다
B. Product를 Record로 바꾸고, 각 기능을 별도 서비스 클래스로 분리한다
C. ProductService를 만들어 모든 로직을 옮긴다
D. 각 메서드마다 별도의 Product 하위 클래스를 만든다

---

#### Q1.4 [개념 확인] 불변성의 장점
데이터 불변성이 제공하는 장점으로 **올바르지 않은** 것은?

A. 멀티스레드 환경에서 동기화 없이 안전하게 공유할 수 있다
B. 객체의 상태가 예측 가능하여 디버깅이 쉽다
C. 메모리 사용량이 줄어든다
D. 함수의 부수효과를 줄여 테스트가 쉬워진다

---

#### Q1.5 [코드 작성] DOP 스타일 변환
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

#### Q1.6 [함정 문제] DOP 철학 ⭐
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

---

## Chapter 2: 데이터란 무엇인가? (Identity vs Value)

### 학습 목표
1. 정체성(Identity)과 값(Value)의 개념적 차이를 명확히 구분할 수 있다
2. Java Record가 Value Type을 표현하는 방식을 이해한다
3. 얕은 불변성과 깊은 불변성의 차이를 설명할 수 있다
4. 방어적 복사를 통해 깊은 불변성을 확보할 수 있다
5. 이커머스 도메인에서 Identity와 Value를 올바르게 식별할 수 있다

---

### 2.1 정체성(Identity)과 값(Value)의 차이

시스템의 모든 데이터는 두 가지 범주 중 하나에 속합니다.

| 구분 | Value (값) | Identity (정체성) |
|-----|-----------|------------------|
| 동등성 | 내용이 같으면 같음 | ID가 같으면 같음 |
| 불변성 | 항상 불변 | 상태가 변할 수 있음 |
| 비교 방식 | `equals()` (값 비교) | `==` 또는 ID 비교 |
| 예시 | 금액, 좌표, 날짜 | 회원, 주문, 상품 |

#### 비유: 여권과 이름표

> **Identity는 여권이고, Value는 이름표입니다.**
>
> **여권(Identity)**:
> - 여권 번호가 같으면 같은 사람입니다
> - 이름을 개명해도, 주소를 바꿔도 여전히 같은 사람입니다
> - 여권은 "누구인가"를 식별합니다
>
> **이름표(Value)**:
> - "홍길동"이라고 적힌 이름표 두 개는 같은 이름표입니다
> - 이름표에는 고유 번호가 없습니다
> - 이름표는 "무엇인가"를 나타냅니다

---

### 2.2 이커머스에서의 Identity vs Value

```java
// ========== Value Types (값) ==========

// 금액: 10000원은 어디에서든 10000원
public record Money(BigDecimal amount, Currency currency) {}

// 좌표: (37.5, 127.0)은 어디에서든 같은 위치
public record Coordinate(double latitude, double longitude) {}

// 주소: 내용이 같으면 같은 주소
public record Address(String city, String street, String zipCode) {}


// ========== Identity Types (정체성) ==========

// 회원: 이름을 바꿔도 같은 회원
public record Member(MemberId id, String name, EmailAddress email) {}

// 주문: 상태가 바뀌어도 같은 주문
public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {}

// 상품: 가격이 바뀌어도 같은 상품
public record Product(ProductId id, ProductName name, Money price) {}
```

---

### 2.3 Java Record: Value Type의 완벽한 도구

#### Compact Constructor로 불변식 강제

```java
public record Money(BigDecimal amount, Currency currency) {
    // Compact Constructor - 검증 로직
    public Money {
        Objects.requireNonNull(amount, "금액은 null일 수 없습니다");
        Objects.requireNonNull(currency, "통화는 null일 수 없습니다");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 음수가 될 수 없습니다: " + amount);
        }
        // this.amount = amount; 자동 실행
    }

    // 팩토리 메서드
    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    // 비즈니스 연산 (새 객체 반환)
    public Money add(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("통화가 다릅니다");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

---

### 2.4 얕은 불변성 vs 깊은 불변성

#### 얕은 불변성의 함정

Record는 필드 자체를 `final`로 만들지만, 필드가 참조하는 객체의 내부까지 얼리지는 못합니다.

##### Before: 불변성이 깨지는 코드

```java
// 위험한 코드!
public record Order(OrderId id, List<OrderItem> items) {}

// 문제 발생
List<OrderItem> mutableList = new ArrayList<>();
mutableList.add(new OrderItem(productId, quantity));

Order order = new Order(orderId, mutableList);

// Order 외부에서 내부 데이터 변경 가능!
mutableList.add(new OrderItem(anotherProduct, anotherQuantity));

// order.items()의 크기가 변경됨 - 불변성 파괴!
```

##### After: 방어적 복사로 깊은 불변성 확보

```java
public record Order(OrderId id, List<OrderItem> items) {
    // Compact Constructor에서 방어적 복사
    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(items);
        // 불변 리스트로 복사 - 외부에서 변경 불가
        items = List.copyOf(items);
    }
}

// 이제 안전!
List<OrderItem> mutableList = new ArrayList<>();
mutableList.add(new OrderItem(productId, quantity));

Order order = new Order(orderId, mutableList);

// 원본 리스트를 수정해도 Order에 영향 없음
mutableList.add(new OrderItem(anotherProduct, anotherQuantity));

// order.items()는 여전히 1개의 아이템만 가짐
```

---

### 2.5 값 변경 패턴: with 메서드

불변 객체에서 값을 "변경"하려면 새 객체를 만들어야 합니다.

> 💡 **JEP 468 미포함 안내**: Java 25까지도 `with` expression은 정식 기능으로
> 포함되지 않았습니다. 따라서 수동으로 `withXxx()` 메서드를 작성해야 합니다.

```java
public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderItem> items,
    Money totalAmount,
    OrderStatus status
) {
    public Order {
        items = List.copyOf(items);
    }

    // with 패턴: 하나의 필드만 바꾼 새 객체 반환
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerId, items, totalAmount, newStatus);
    }

    public Order withTotalAmount(Money newAmount) {
        return new Order(id, customerId, items, newAmount, status);
    }
}

// 사용
Order unpaidOrder = new Order(id, customerId, items, total, new Unpaid());
Order paidOrder = unpaidOrder.withStatus(new Paid(LocalDateTime.now(), paymentId));
// unpaidOrder는 여전히 Unpaid 상태 (불변)
```

---

### 퀴즈 Chapter 2

#### Q2.1 [개념 확인] Identity vs Value
다음 중 **Value Type**으로 모델링해야 하는 것은?

A. 고객 (Customer)
B. 주문 금액 (OrderAmount)
C. 상품 (Product)
D. 장바구니 (ShoppingCart)

---

#### Q2.2 [코드 분석] 불변성 위반
다음 코드에서 불변성이 깨지는 지점은?

```java
public record ShoppingCart(CartId id, List<CartItem> items) {}

public class CartService {
    public void addItem(ShoppingCart cart, CartItem item) {
        cart.items().add(item);  // Line A
    }

    public ShoppingCart createCart(CartId id) {
        return new ShoppingCart(id, new ArrayList<>());  // Line B
    }
}
```

A. Line A만 문제
B. Line B만 문제
C. Line A와 Line B 모두 문제
D. 문제없음

---

#### Q2.3 [함정 문제] 불변성의 함정 ⭐
다음 코드를 실행했을 때, 콘솔에 출력되는 결과는?

```java
import java.util.*;

public record Team(String name, List<String> members) {
    // 콤팩트 생성자가 정의되지 않음 (기본 생성자 사용)
}

public class Main {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Alice");

        Team team = new Team("Alpha", list);
        list.add("Bob");

        System.out.println(team.members());
    }
}
```

A. `[Alice]`
B. `[Alice, Bob]`
C. 컴파일 에러
D. `NullPointerException`

---

#### Q2.4 [설계 문제] 적절한 타입 선택
이커머스에서 "배송 주소"를 모델링할 때 적절한 방식은?

A. Entity로 모델링 - 주소도 고유 ID가 있어야 함
B. Value Object로 모델링 - 주소 내용이 같으면 같은 주소
C. String으로 충분 - "서울시 강남구 역삼동"
D. Map<String, String>으로 모델링 - 유연성 확보

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 3: 타입 시스템의 기수(Cardinality) 이론

### 학습 목표
1. 기수(Cardinality)의 개념을 이해하고 타입의 상태 개수를 계산할 수 있다
2. 곱 타입(Product Type)이 상태 폭발을 일으키는 원리를 설명할 수 있다
3. 합 타입(Sum Type)이 상태를 축소하는 원리를 설명할 수 있다
4. Sealed Interface를 사용해 합 타입을 구현할 수 있다
5. 실제 도메인에서 상태 수를 계산하고 최적화할 수 있다

---

### 3.1 기수(Cardinality)란?

시스템의 복잡도는 **"가능한 상태의 총 개수(Cardinality)"**와 비례합니다.

```
복잡도 ∝ 가능한 상태의 수 (Cardinality)
```

| 타입 | 기수(Cardinality) |
|-----|------------------|
| `boolean` | 2 (true, false) |
| `byte` | 256 |
| `int` | 2^32 ≈ 40억 |
| `String` | ∞ (무한) |
| `Optional<T>` | \|T\| + 1 |
| `enum Status { A, B, C }` | 3 |

---

### 3.2 곱 타입 (Product Type): 상태의 폭발

필드를 추가하는 것은 경우의 수를 **곱하는(Multiply)** 행위입니다.

##### Before: 곱 타입으로 인한 상태 폭발

```java
// 각 boolean 필드는 2가지 상태
class Order {
    boolean isCreated;    // 2가지
    boolean isPaid;       // 2가지
    boolean isShipped;    // 2가지
    boolean isDelivered;  // 2가지
    boolean isCanceled;   // 2가지
}
// 총 상태 수 = 2 × 2 × 2 × 2 × 2 = 32가지
// 하지만 유효한 상태는 단 5가지뿐!
// 나머지 27가지는 "불가능한 상태"
```

##### After: 합 타입으로 상태 축소

```java
// 합 타입: 5가지 상태만 가능
sealed interface OrderStatus permits
    Created, Paid, Shipped, Delivered, Canceled {}

record Created(LocalDateTime at) implements OrderStatus {}
record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {}
record Shipped(LocalDateTime at, TrackingNumber tracking) implements OrderStatus {}
record Delivered(LocalDateTime at, ReceiverName receiver) implements OrderStatus {}
record Canceled(LocalDateTime at, CancelReason reason) implements OrderStatus {}

// 총 상태 수 = 1 + 1 + 1 + 1 + 1 = 5가지
// 복잡도가 84% 감소!
```

---

### 3.3 enum vs sealed interface

> **💡 Q&A: enum으로도 충분하지 않나요?**
>
> **결론**: 데이터의 모양(구조)이 다를 수 있느냐가 결정적인 차이입니다.
>
> **Enum의 한계**: 모든 상수가 똑같은 필드 구조를 가져야 합니다.
> ```java
> public enum DeliveryStatus {
>     PREPARING(null, null),     // 불필요한 null
>     SHIPPED("12345", null),    // 배송일시는 null
>     DELIVERED("12345", LocalDateTime.now());
>
>     private final String trackingNumber;  // 모든 필드를 가져야 함
>     private final LocalDateTime deliveredAt;
> }
> ```
>
> **Sealed Interface의 강점**: 각 상태마다 다른 데이터를 가질 수 있습니다.
> ```java
> sealed interface DeliveryStatus {}
> record Preparing() implements DeliveryStatus {}  // 아무것도 없음
> record Shipped(String trackingNumber) implements DeliveryStatus {}  // 송장만
> record Delivered(String trackingNumber, LocalDateTime deliveredAt)
>     implements DeliveryStatus {}  // 송장 + 시간
> ```

---

### 퀴즈 Chapter 3

#### Q3.1 [개념 확인] 기수 계산
다음 타입의 기수(Cardinality)는?

```java
enum Size { S, M, L, XL }
enum Color { RED, BLUE, GREEN }

record Product(Size size, Color color) {}
```

A. 7 (4 + 3)
B. 12 (4 × 3)
C. 16 (4^2)
D. 81 (3^4)

---

#### Q3.2 [함정 문제] 봉인된 운명 ⭐
다음과 같이 완벽한 sealed interface와 switch 문을 작성하여 배포했습니다.

```java
public sealed interface LoginResult permits Success, Failure {}
public record Success() implements LoginResult {}
public record Failure(String reason) implements LoginResult {}

// 로직 코드
public String handleLogin(LoginResult result) {
    return switch (result) {
        case Success s -> "Welcome!";
        case Failure f -> "Error: " + f.reason();
    };
}
```

일주일 뒤, 동료가 `LoginResult`에 `record Timeout() implements LoginResult {}`를 추가하고
`permits` 절에도 `Timeout`을 넣었습니다. 하지만 `handleLogin` 메서드는 수정하지 않았습니다.

이때 컴파일러의 반응은?

A. 에러 없음: 런타임에 Timeout이 들어오면 예외 발생
B. 컴파일 에러: switch 식이 모든 경우를 커버하지 않음
C. 에러 없음: Timeout은 자동으로 무시됨 (null 반환)
D. 워닝(Warning): "모든 케이스를 다루지 않았습니다" 경고만 발생

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 4: 불가능한 상태를 표현 불가능하게 만들기

### 학습 목표
1. "Make Illegal States Unrepresentable" 원칙의 의미를 이해한다
2. 유효성 검증 없이 안전한 코드를 설계할 수 있다
3. Sealed Interface의 망라성(Exhaustiveness)을 활용할 수 있다
4. 타입 시스템으로 비즈니스 규칙을 강제할 수 있다
5. 상태 전이를 타입으로 모델링할 수 있다

---

### 4.1 유효성 검증이 필요 없는 설계

#### Before: 전통적인 방식 - 런타임 검증

```java
// 전통적인 방식
public class OrderService {
    public void processOrder(Order order) {
        // 런타임에 유효성 검증
        if (order.getStatus() == null) {
            throw new IllegalStateException("상태가 없습니다");
        }
        if (order.isPaid() && order.isCanceled()) {
            throw new IllegalStateException("결제됐는데 취소됨?");
        }
        if (order.isShipped() && !order.isPaid()) {
            throw new IllegalStateException("결제 없이 배송됨?");
        }
        // ... 수십 가지 검증 로직

        // 실제 비즈니스 로직
        doProcess(order);
    }
}
```

#### After: DOP 방식 - 컴파일 타임 강제

```java
// DOP 방식: 불가능한 상태가 타입으로 표현 불가능
sealed interface OrderStatus {
    record Unpaid() implements OrderStatus {}
    record Paid(PaymentId paymentId) implements OrderStatus {}
    record Shipped(TrackingNumber tracking) implements OrderStatus {}
    record Canceled(CancelReason reason) implements OrderStatus {}
}

public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {}

// 이제 "결제됐는데 취소됨"은 타입으로 표현 불가능!
// Order는 항상 하나의 명확한 상태만 가짐
```

#### 비유: 자물쇠와 열쇠

> **타입 시스템은 자물쇠와 같습니다.**
>
> **런타임 검증 (경비원)**:
> - 문이 열려있고, 경비원이 지키고 있습니다
> - 경비원이 자리를 비우면 아무나 들어올 수 있습니다
> - 경비원도 실수할 수 있습니다
>
> **컴파일 타임 강제 (자물쇠)**:
> - 문 자체가 잠겨있고, 열쇠가 맞아야만 열립니다
> - 열쇠 없이는 아무도 들어올 수 없습니다
> - 자물쇠는 실수하지 않습니다
>
> DOP는 "경비원을 더 잘 훈련시키자"가 아니라
> "처음부터 자물쇠를 설치하자"입니다.

---

### 4.2 실전 예제: 이메일 인증 상태

#### Before: Boolean으로 모델링

```java
// 불가능한 상태가 가능한 설계
class User {
    String email;
    boolean isEmailVerified;
    LocalDateTime emailVerifiedAt;

    // 문제: 이메일이 null인데 verified가 true?
    // 문제: verified가 false인데 verifiedAt이 있음?
    // 문제: verified가 true인데 verifiedAt이 null?
}
```

#### After: 합 타입으로 모델링

```java
// 불가능한 상태가 불가능한 설계
sealed interface UserEmail {
    record Unverified(String email) implements UserEmail {}
    record Verified(String email, LocalDateTime verifiedAt) implements UserEmail {}
}

public record User(UserId id, String name, UserEmail email) {}

// Verified 상태에는 반드시 verifiedAt이 존재
// Unverified 상태에는 verifiedAt이 없음
// 이메일 없이 인증됨 상태는 타입으로 표현 불가능!
```

---

### 4.3 Switch Expression과 망라성(Exhaustiveness)

Sealed Interface는 컴파일러가 모든 케이스를 처리했는지 검증합니다.

```java
public String getStatusMessage(UserEmail email) {
    return switch (email) {
        case Unverified u -> "이메일 인증이 필요합니다: " + u.email();
        case Verified v -> "인증 완료 (" + v.verifiedAt() + ")";
        // default 불필요! 모든 케이스를 처리했으므로
    };
}
```

#### 새 상태 추가 시 컴파일러의 도움

```java
// 새로운 상태 추가
sealed interface UserEmail {
    record Unverified(String email) implements UserEmail {}
    record Verified(String email, LocalDateTime verifiedAt) implements UserEmail {}
    record Banned(String email, String reason) implements UserEmail {}  // 새로 추가!
}

// 기존 코드
public String getStatusMessage(UserEmail email) {
    return switch (email) {
        case Unverified u -> "이메일 인증이 필요합니다: " + u.email();
        case Verified v -> "인증 완료 (" + v.verifiedAt() + ")";
        // 컴파일 에러! 'Banned'를 처리하지 않았습니다!
    };
}
```

---

### 4.4 상태 전이를 타입으로 강제하기

```java
// 각 상태 타입에서 가능한 전이만 메서드로 정의
sealed interface OrderStatus {

    record Unpaid() implements OrderStatus {
        public Paid pay(PaymentId paymentId) {
            return new Paid(LocalDateTime.now(), paymentId);
        }
        public Canceled cancel(CancelReason reason) {
            return new Canceled(LocalDateTime.now(), reason);
        }
    }

    record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {
        public Shipped ship(TrackingNumber tracking) {
            return new Shipped(LocalDateTime.now(), tracking);
        }
        // cancel은 없음 - Paid 상태에서 취소 불가!
    }

    record Shipped(LocalDateTime at, TrackingNumber tracking)
        implements OrderStatus {
        public Delivered deliver() {
            return new Delivered(LocalDateTime.now());
        }
    }

    record Delivered(LocalDateTime at) implements OrderStatus {}
    record Canceled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
}

// 사용
Unpaid unpaid = new Unpaid();
Paid paid = unpaid.pay(paymentId);     // OK
Shipped shipped = paid.ship(tracking); // OK
// paid.cancel(reason);                // 컴파일 에러! Paid에는 cancel이 없음
```

---

### 퀴즈 Chapter 4

#### Q4.1 [개념 확인] 불가능한 상태
"Make Illegal States Unrepresentable"의 의미는?

A. 모든 상태를 enum으로 정의한다
B. 유효하지 않은 상태를 타입으로 표현할 수 없게 설계한다
C. 런타임에 철저히 검증한다
D. 모든 필드를 private으로 숨긴다

---

#### Q4.2 [코드 분석] 설계 문제점
다음 설계의 문제점은?

```java
record DeliveryInfo(
    boolean isDelivered,
    LocalDateTime deliveredAt,
    String receiverName
) {}
```

A. Record를 사용한 것이 문제
B. isDelivered가 false인데 deliveredAt, receiverName이 있을 수 있음
C. 필드가 너무 적음
D. 문제없음

---

#### Q4.3 [버그 찾기] 망라성 문제
다음 코드의 잠재적 문제점은?

```java
sealed interface OrderStatus permits Pending, Confirmed, Shipped {}

String getMessage(OrderStatus status) {
    return switch (status) {
        case Pending p -> "대기중";
        case Confirmed c -> "확정됨";
        default -> "알 수 없음";
    };
}
```

A. default가 있어서 Shipped가 처리되지 않음
B. sealed interface를 잘못 사용함
C. switch가 아닌 if-else를 써야 함
D. 문제없음

---

#### Q4.4 [설계 문제] 리팩토링
다음을 "불가능한 상태가 불가능한" 설계로 리팩토링하세요.

```java
class Coupon {
    String code;
    boolean isUsed;
    LocalDateTime usedAt;
    String usedByMemberId;
}
```

A. Unused(code)와 Used(code, usedAt, usedByMemberId)로 분리
B. isUsed를 enum으로 변경
C. usedAt과 usedByMemberId를 Optional로 변경
D. 현재 설계가 적절함

---

#### Q4.5 [코드 작성] 배송 상태 설계
이커머스의 "배송 상태"를 불가능한 상태가 없도록 설계하세요.
- 준비중 (Preparing)
- 출고완료 (Dispatched) - 출고시각, 택배사 정보 필요
- 배송중 (InTransit) - 운송장 번호, 현재 위치 필요
- 배송완료 (Delivered) - 수령시각, 수령인 이름 필요
- 반송됨 (Returned) - 반송시각, 반송사유 필요

---

정답은 Appendix C에서 확인할 수 있습니다.

---

# Part III: 데이터의 흐름과 제어 (Behavior & Control Flow)

---

## Chapter 5: 전체 함수(Total Functions)와 실패 처리

### 학습 목표
1. 부분 함수(Partial Function)와 전체 함수(Total Function)의 차이를 설명할 수 있다
2. 예외(Exception)가 왜 "거짓말"인지 이해한다
3. Result 타입을 구현하고 활용할 수 있다
4. "Failure as Data" 패턴을 적용할 수 있다
5. 연쇄적인 실패 처리를 우아하게 구현할 수 있다

---

### 5.1 부분 함수(Partial Function)의 위험

#### 시그니처가 거짓말을 한다

```java
// 시그니처: id를 주면 User를 반환
public User findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        throw new NotFoundException("User not found: " + id);
        // 시그니처에는 이 예외에 대한 언급이 없음!
    }
    return user;
}
```

이것이 **부분 함수(Partial Function)**입니다. 입력 중 일부에 대해서만 결과를 반환하고, 나머지는 예외를 던지거나 null을 반환합니다.

#### 비유: 은행 창구

> **부분 함수는 불친절한 은행 창구와 같습니다.**
>
> "계좌 잔액 조회"라는 창구에 갔습니다.
> 번호표에는 "계좌번호를 주시면 잔액을 알려드립니다"라고 쓰여있습니다.
>
> 그런데 막상 창구에 가니:
> - "그 계좌는 해지됐어요" (예외)
> - "시스템 점검 중이에요" (예외)
> - "조회 권한이 없어요" (예외)
>
> 번호표에는 이런 경우에 대한 안내가 없었습니다.
>
> **전체 함수는 친절한 은행 창구입니다.**
>
> 번호표에 미리 안내되어 있습니다:
> "결과: 잔액 / 해지된 계좌 / 권한 없음 / 시스템 오류 중 하나"
> 어떤 경우든 반드시 명확한 결과를 받습니다.

---

### 5.2 전체 함수(Total Function) 만들기

#### Before: 부분 함수 (거짓말하는 시그니처)

```java
public User findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        throw new NotFoundException("User not found: " + id);
    }
    return user;
}
```

#### After: 전체 함수 (정직한 시그니처)

```java
public Result<User, UserError> findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        return Result.failure(new UserError.NotFound(id));
    }
    return Result.success(user);
}

// 에러 타입도 명확히 정의
sealed interface UserError {
    record NotFound(int id) implements UserError {}
    record Suspended(int id, String reason) implements UserError {}
    record Deleted(int id, LocalDateTime deletedAt) implements UserError {}
}
```

---

### 5.3 Result 타입 완전 구현

```java
public sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}

    // 팩토리 메서드
    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }

    // 변환 (map)
    default <T> Result<T, F> map(Function<S, T> mapper) {
        return switch (this) {
            case Success(var value) -> Result.success(mapper.apply(value));
            case Failure(var error) -> Result.failure(error);
        };
    }

    // 연쇄 (flatMap)
    default <T> Result<T, F> flatMap(Function<S, Result<T, F>> mapper) {
        return switch (this) {
            case Success(var value) -> mapper.apply(value);
            case Failure(var error) -> Result.failure(error);
        };
    }

    // 양쪽 처리
    default <T> T fold(Function<S, T> onSuccess, Function<F, T> onFailure) {
        return switch (this) {
            case Success(var value) -> onSuccess.apply(value);
            case Failure(var error) -> onFailure.apply(error);
        };
    }
}
```

---

### 5.4 이커머스 실전 예제: 주문 처리

```java
// 에러 타입 정의
sealed interface OrderError {
    record UserNotFound(MemberId id) implements OrderError {}
    record ProductNotFound(ProductId id) implements OrderError {}
    record InsufficientStock(ProductId id, int requested, int available)
        implements OrderError {}
    record InvalidCoupon(CouponCode code, String reason) implements OrderError {}
}

// 파이프라인 조합
public Result<Order, OrderError> createOrder(CreateOrderRequest request) {
    return findUser(request.userId())
        .flatMap(user -> findProduct(request.productId())
            .flatMap(product -> checkStock(product, request.quantity())
                .flatMap(__ -> validateCoupon(request.couponCode())
                    .map(discount -> buildOrder(user, product, request.quantity(), discount))
                )
            )
        );
}
```

---

### 퀴즈 Chapter 5

#### Q5.1 [개념 확인] 전체 함수
다음 중 전체 함수(Total Function)의 특징은?

A. 항상 성공을 반환한다
B. 모든 입력에 대해 정의된 결과를 반환한다
C. 예외를 던지지 않으면 전체 함수다
D. void를 반환하면 전체 함수다

---

#### Q5.2 [코드 분석] 부분 함수 식별
다음 중 부분 함수(Partial Function)는?

```java
// A
int divide(int a, int b) {
    return a / b;
}

// B
Result<Integer, String> safeDivide(int a, int b) {
    if (b == 0) return Result.failure("0으로 나눌 수 없습니다");
    return Result.success(a / b);
}
```

A. A만
B. A와 B 모두
C. B만
D. 없음

---

#### Q5.3 [코드 분석] Result 활용
다음 코드의 결과는?

```java
Result<Integer, String> result = Result.success(10)
    .map(x -> x * 2)
    .flatMap(x -> x > 15
        ? Result.success(x)
        : Result.failure("값이 너무 작습니다"));

result.getOrElse(-1);
```

A. 20
B. 10
C. -1
D. "값이 너무 작습니다"

---

#### Q5.4 [설계 문제] 에러 타입 설계
"쿠폰 적용" 기능의 실패 경우를 Result 에러 타입으로 설계하세요.
실패 경우: 쿠폰 없음, 이미 사용됨, 만료됨, 최소 주문금액 미달

A. 모두 String으로 처리
B. sealed interface로 각 경우를 record로 정의
C. enum으로 정의
D. Exception으로 처리

---

#### Q5.5 [코드 작성] 전체 함수 변환
다음 부분 함수를 전체 함수로 변환하세요.

```java
public User withdraw(UserId id, Money amount) {
    User user = userRepository.find(id)
        .orElseThrow(() -> new UserNotFoundException(id));

    if (user.balance().lessThan(amount)) {
        throw new InsufficientBalanceException(user.balance(), amount);
    }

    return user.withdraw(amount);
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 6: 파이프라인과 결정론적 시스템

### 학습 목표
1. 결정론적(Deterministic) 함수의 정의와 장점을 설명할 수 있다
2. 순수 함수(Pure Function)를 작성할 수 있다
3. 샌드위치 아키텍처의 구조를 이해하고 적용할 수 있다
4. 비즈니스 로직에서 I/O를 격리하는 방법을 알 수 있다
5. 테스트 가능한 코드를 설계할 수 있다

---

### 6.1 결정론적 함수란?

```java
// 결정론적 함수 (순수 함수)
public static Money calculateTotal(List<OrderItem> items) {
    return items.stream()
        .map(item -> item.unitPrice().multiply(item.quantity().value()))
        .reduce(Money.zero(), Money::add);
}

// 비결정론적 함수
public Money calculateTotal(List<OrderItem> items) {
    // 외부 상태(taxRate) 참조 - 비결정론적
    TaxRate taxRate = taxService.getCurrentRate();
    return items.stream()
        .map(item -> item.unitPrice().multiply(item.quantity().value()))
        .reduce(Money.zero(), Money::add)
        .applyTax(taxRate);
}
```

#### 비유: 자판기 vs 바리스타

> **결정론적 함수는 자판기와 같습니다.**
>
> **자판기 (결정론적)**:
> - 500원 + 커피 버튼 → 항상 같은 커피
> - 몇 시에 눌러도, 누가 눌러도 같은 결과
> - 예측 가능하고, 테스트하기 쉬움
>
> **바리스타 (비결정론적)**:
> - "커피 주세요" → 바리스타 기분, 재료 상태, 시간에 따라 다른 커피
> - 같은 주문이어도 결과가 다를 수 있음
> - 예측 불가능하고, 테스트하기 어려움

---

### 6.2 샌드위치 아키텍처

```
┌─────────────────────────────────────┐
│  Top Bun (Impure): 재료 수집        │  ← DB 조회, API 호출
├─────────────────────────────────────┤
│  Meat (Pure): 요리                  │  ← 비즈니스 로직 (순수 함수)
├─────────────────────────────────────┤
│  Bottom Bun (Impure): 서빙          │  ← DB 저장, 알림 발송
└─────────────────────────────────────┘
```

#### Before: I/O가 로직에 섞여있음

```java
// 안티패턴: 비즈니스 로직 중간에 I/O
public Order processOrder(OrderRequest request) {
    User user = userRepository.find(request.userId()).orElseThrow();

    if (user.grade() == Grade.VIP) {
        Coupon vipCoupon = couponService.getVipCoupon();  // I/O in middle!
        request = request.withCoupon(vipCoupon);
    }

    Money total = calculateTotal(request.items());
    TaxRate taxRate = taxService.getCurrentRate();  // I/O in middle!
    total = total.applyTax(taxRate);

    Order order = new Order(user.id(), request.items(), total);
    return orderRepository.save(order);
}
```

#### After: 샌드위치 구조

```java
public Result<Order, OrderError> processOrder(OrderRequest request) {
    // === Top Bun: 데이터 수집 (Impure) ===
    Optional<User> userOpt = userRepository.find(request.userId());
    if (userOpt.isEmpty()) {
        return Result.failure(new OrderError.UserNotFound(request.userId()));
    }
    User user = userOpt.get();
    Optional<Coupon> coupon = user.grade() == Grade.VIP
        ? couponService.getVipCoupon()
        : Optional.empty();
    TaxRate taxRate = taxService.getCurrentRate();

    // === Meat: 비즈니스 로직 (Pure) ===
    Money total = OrderCalculations.calculateTotal(request.items(), coupon, taxRate);
    Order order = OrderCalculations.createOrder(user.id(), request.items(), total);

    // === Bottom Bun: 부수효과 (Impure) ===
    Order savedOrder = orderRepository.save(order);
    notificationService.sendOrderConfirmation(savedOrder);

    return Result.success(savedOrder);
}
```

---

### 퀴즈 Chapter 6

#### Q6.1 [개념 확인] 순수 함수
다음 중 순수 함수는?

```java
// A
int add(int a, int b) { return a + b; }

// B
int addWithLog(int a, int b) {
    System.out.println("Adding " + a + " + " + b);
    return a + b;
}

// C
int addWithCounter(int a, int b) {
    counter++;  // 클래스 필드
    return a + b;
}
```

A. A만
B. A와 B
C. A, B, C
D. 모두 순수하지 않음

---

#### Q6.2 [개념 확인] 샌드위치 아키텍처
샌드위치 아키텍처에서 "Meat" 레이어의 특징은?

A. 데이터베이스 접근을 담당한다
B. 알림 발송을 담당한다
C. 순수한 비즈니스 로직만 포함한다
D. 외부 API 호출을 담당한다

---

#### Q6.3 [코드 분석] I/O 격리
다음 코드의 문제점은?

```java
public Money calculatePrice(ProductId id, int quantity) {
    Product product = productRepository.find(id).orElseThrow();
    TaxRate rate = taxService.getCurrentRate();
    return product.price().multiply(quantity).applyTax(rate);
}
```

A. 문제없음
B. 비즈니스 로직 안에 I/O가 섞여있음
C. 예외를 던지는 것이 문제
D. 메서드가 너무 짧음

---

#### Q6.4 [설계 문제] 리팩토링
Q6.3의 코드를 샌드위치 구조로 리팩토링하면?

A. 그대로 둔다
B. Product, TaxRate를 파라미터로 받는 순수 함수로 분리
C. 모든 로직을 Repository로 이동
D. Try-catch로 감싼다

---

#### Q6.5 [코드 작성] 순수 함수 추출
다음 코드에서 순수 함수를 추출하세요.

```java
public Order applyPromotion(OrderId orderId) {
    Order order = orderRepository.find(orderId).orElseThrow();
    Promotion promo = promotionService.getActivePromotion();

    Money discount = order.total().multiply(promo.rate());
    Money newTotal = order.total().subtract(discount);

    Order updatedOrder = order.withTotal(newTotal);
    return orderRepository.save(updatedOrder);
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.

---

> **💡 Q&A: mapToDouble() vs map() - 성능 차이가 있나요?**
>
> **결론**: 숫자를 다룰 때는 `mapToInt/Double`을 쓰는 것이 성능과 편의성 모두 좋습니다.
>
> **이유 1: 박싱/언박싱 패널티**
> - `map()` 사용 시: int → Integer 포장(Boxing) 필요 → 메모리 낭비 & CPU 연산 추가
> - `mapToInt()` 사용 시: IntStream으로 변환되어 포장 없이 int 그대로 처리
>
> **이유 2: 숫자 전용 API 제공**
> - `IntStream`/`DoubleStream`에는 `.sum()`, `.average()`, `.max()`, `.min()` 등 편리한 메서드 제공
> - 일반 `Stream<Integer>`에서는 `.reduce()`를 써야 해서 번거로움
>
> ```java
> // 권장: 숫자 전용 스트림
> int total = items.stream()
>     .mapToInt(Item::quantity)
>     .sum();
>
> // 비권장: 박싱 오버헤드
> Integer total = items.stream()
>     .map(Item::quantity)
>     .reduce(0, Integer::sum);
> ```

---

# Part IV: 고급 기법과 대수적 추론 (Advanced Theory)

---

## Chapter 7: 대수적 속성을 활용한 설계

### 학습 목표
1. 결합법칙(Associativity)의 개념과 분산 처리에서의 활용을 이해한다
2. 멱등성(Idempotence)의 개념과 재시도 안전성을 설명할 수 있다
3. 항등원(Identity Element)을 활용한 설계를 이해한다
4. 대수적 속성을 갖는 데이터 구조를 설계할 수 있다
5. 이커머스에서 대수적 속성을 활용하는 실제 사례를 적용할 수 있다

---

### 7.1 대수적 속성이란?

데이터 모델링에 수학적 속성을 적용하면 분산 시스템에서 강력한 위력을 발휘합니다.

| 속성 | 정의 | 활용 |
|-----|------|------|
| 결합법칙 | (A ⊕ B) ⊕ C = A ⊕ (B ⊕ C) | 병렬 처리, 분산 계산 |
| 교환법칙 | A ⊕ B = B ⊕ A | 순서 무관한 처리 |
| 멱등성 | f(f(x)) = f(x) | 재시도 안전성 |
| 항등원 | A ⊕ e = A | 빈 값 처리 |

---

### 7.2 결합법칙 (Associativity)

#### 비유: 덧셈의 순서

> **결합법칙은 팀원들의 회비 합산과 같습니다.**
>
> 팀원 10명이 각각 회비를 냈습니다.
>
> **방법 1**: (1+2+3+4+5) + (6+7+8+9+10)
> **방법 2**: (1+2+3) + (4+5+6+7) + (8+9+10)
>
> 어떻게 묶어서 더해도 결과는 같습니다 (55원).
>
> 덕분에 10명이 직렬로 줄 서서 합산할 필요 없이,
> 3~4개의 소그룹이 **병렬로** 합산한 뒤 마지막에 합칠 수 있습니다.

#### 분산 처리에서의 활용

```java
// 결합법칙을 만족하는 Money
public record Money(BigDecimal amount, Currency currency) {

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("통화가 다릅니다");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

// 결합법칙 덕분에 병렬 처리 가능
List<Money> orderTotals = getOrderTotals(); // 100만 건

// 직렬 처리: O(n)
Money total1 = orderTotals.stream()
    .reduce(Money.zero(Currency.KRW), Money::add);

// 병렬 처리: O(n/p) - p는 프로세서 수
Money total2 = orderTotals.parallelStream()
    .reduce(Money.zero(Currency.KRW), Money::add);
```

#### 이커머스 예제: 장바구니 병합

```java
// 장바구니 아이템도 결합법칙 적용 가능
public record CartItem(ProductId productId, Quantity quantity, Money price) {}

public record Cart(List<CartItem> items) {

    public static Cart empty() {
        return new Cart(List.of());
    }

    // 결합법칙을 만족하는 병합
    public Cart merge(Cart other) {
        Map<ProductId, CartItem> merged = new HashMap<>();

        // 같은 상품이면 수량 합산
        for (CartItem item : this.items) {
            merged.merge(item.productId(), item,
                (existing, newItem) -> existing.addQuantity(newItem.quantity()));
        }
        for (CartItem item : other.items) {
            merged.merge(item.productId(), item,
                (existing, newItem) -> existing.addQuantity(newItem.quantity()));
        }

        return new Cart(List.copyOf(merged.values()));
    }
}

// (cartA.merge(cartB)).merge(cartC) == cartA.merge(cartB.merge(cartC))
// 순서 상관없이 병합 가능!
```

---

### 7.3 멱등성 (Idempotence)

#### 비유: 전등 스위치

> **멱등성은 ON/OFF 스위치가 아닌 "ON 버튼"과 같습니다.**
>
> **토글 스위치 (멱등하지 않음)**:
> - 버튼을 누르면 상태가 바뀜 (ON→OFF, OFF→ON)
> - 네트워크 지연으로 "두 번 눌림"이 발생하면 문제!
>
> **ON 버튼 (멱등함)**:
> - 버튼을 누르면 항상 ON 상태
> - 이미 ON이면 아무 변화 없음
> - 네트워크 지연으로 "두 번 눌림"이 발생해도 안전!

#### 재시도 안전성

```java
// 멱등하지 않은 결제 (위험!)
public void processPayment(PaymentRequest request) {
    account.withdraw(request.amount());  // 중복 실행되면 두 번 출금!
    merchant.deposit(request.amount());
}

// 멱등한 결제 (안전!)
public void processPayment(PaymentId id, PaymentRequest request) {
    // 이미 처리된 결제인지 확인
    if (paymentRepository.exists(id)) {
        return; // 이미 처리됨, 아무것도 안 함
    }

    account.withdraw(request.amount());
    merchant.deposit(request.amount());

    paymentRepository.save(new Payment(id, request, ProcessedAt.now()));
}

// 네트워크 오류로 재시도해도 안전!
retryOnFailure(() -> processPayment(paymentId, request));
```

#### 이커머스 예제: 멱등한 주문 상태 변경

```java
// 멱등한 주문 상태 변경
public class OrderStateMachine {

    // 멱등한 결제 완료 처리
    public Order markAsPaid(Order order, PaymentId paymentId) {
        return switch (order.status()) {
            // 이미 결제됨 → 그대로 반환 (멱등)
            case Paid p -> order;
            // 이미 배송됨 → 그대로 반환 (멱등)
            case Shipped s -> order;
            // 대기 중 → 결제 완료로 변경
            case Pending() -> order.withStatus(
                new Paid(LocalDateTime.now(), paymentId)
            );
        };
    }

    // markAsPaid(markAsPaid(order, id), id) == markAsPaid(order, id)
    // 두 번 호출해도 결과 동일!
}
```

---

### 7.4 항등원 (Identity Element)

연산의 결과가 원래 값 그대로인 특별한 값입니다.

| 연산 | 항등원 |
|-----|-------|
| 덧셈 | 0 |
| 곱셈 | 1 |
| 문자열 연결 | "" (빈 문자열) |
| 리스트 병합 | [] (빈 리스트) |

```java
// 항등원이 있으면 reduce가 안전해짐
List<Money> payments = Collections.emptyList();

// 항등원과 함께 reduce하면 안전
Money total = payments.stream()
    .reduce(Money.zero(Currency.KRW), Money::add);
// 결과: 0원 (항등원)
```

---

### 7.5 이커머스 실전 예제: 할인 규칙

```java
// 할인 규칙도 대수적 속성을 가질 수 있음
sealed interface Discount {
    record NoDiscount() implements Discount {}  // 항등원
    record Percentage(int rate) implements Discount {}
    record FixedAmount(Money amount) implements Discount {}
}

public class DiscountCalculator {

    // 할인 적용 (결합법칙 만족)
    public static Money apply(Money original, Discount discount) {
        return switch (discount) {
            case NoDiscount() -> original;  // 항등원: 변화 없음
            case Percentage(int rate) -> {
                BigDecimal multiplier = BigDecimal.valueOf(100 - rate)
                    .divide(BigDecimal.valueOf(100));
                yield new Money(
                    original.amount().multiply(multiplier),
                    original.currency()
                );
            }
            case FixedAmount(Money amount) -> {
                BigDecimal newAmount = original.amount().subtract(amount.amount());
                yield new Money(
                    newAmount.max(BigDecimal.ZERO),  // 음수 방지
                    original.currency()
                );
            }
        };
    }

    // 여러 할인을 순차 적용 (결합법칙 덕분에 병렬화 가능)
    public static Money applyAll(Money original, List<Discount> discounts) {
        return discounts.stream()
            .reduce(
                original,
                (money, discount) -> apply(money, discount),
                (m1, m2) -> m1  // 병렬 스트림에서 사용
            );
    }
}
```

---

### 퀴즈 Chapter 7

#### Q7.1 [개념 확인] 결합법칙
다음 중 결합법칙을 만족하는 연산은?

A. 뺄셈 (a - b - c)
B. 나눗셈 (a / b / c)
C. 문자열 연결 (a + b + c)
D. 평균 계산

---

#### Q7.2 [개념 확인] 멱등성
다음 중 멱등한 연산은?

A. counter++
B. list.add(item)
C. Math.abs(x)
D. random.nextInt()

---

#### Q7.3 [코드 분석] 멱등성 문제
다음 코드의 문제점은?

```java
public void applyDiscount(OrderId orderId, DiscountCode code) {
    Order order = orderRepository.find(orderId).orElseThrow();
    Money discounted = order.total().multiply(0.9);
    order.setTotal(discounted);
    orderRepository.save(order);
}
```

A. 문제없음
B. 멱등하지 않아서 중복 호출 시 할인이 중복 적용됨
C. 예외 처리가 없음
D. 트랜잭션이 없음

---

#### Q7.4 [설계 문제] 멱등성 확보
Q7.3의 코드를 멱등하게 만드는 방법은?

A. synchronized 추가
B. 이미 할인이 적용됐는지 상태를 저장하고 확인
C. 트랜잭션 추가
D. 예외를 던짐

---

#### Q7.5 [코드 작성] 항등원 설계
이커머스의 "배송료" 타입을 설계하세요. 요구사항:
- 배송료는 0원 이상
- 여러 배송료를 합산할 수 있음 (결합법칙)
- 무료 배송은 항등원 역할

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 8: 인터프리터 패턴 - Rule as Data ⭐ (강화)

### 학습 목표
1. 인터프리터 패턴의 개념과 필요성을 이해한다
2. 비즈니스 로직을 데이터로 표현하는 방법을 알 수 있다
3. 재귀적 데이터 구조를 설계할 수 있다
4. Rule Engine을 단계별로 구현할 수 있다
5. 흔한 실수를 피하고 동적 규칙 로딩을 구현할 수 있다

---

### 8.1 왜 로직을 데이터로 만드는가?

비즈니스 규칙이 복잡해질 때, `if-else`를 떡칠하면 여러 문제가 발생합니다:
- 규칙 추가/변경마다 코드 수정 필요
- 규칙 조합이 복잡해지면 이해하기 어려움
- 런타임에 규칙을 변경할 수 없음

**해결책**: 규칙 자체를 **데이터(ADT)**로 표현하고, 별도의 **해석기(Interpreter)**가 실행합니다.

---

### 8.2 Rule Engine 단계별 구현 ⭐

#### Step 1: 기본 Equals 조건

```java
public record Customer(String country, String type, int totalSpend) {}

public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
}

public class RuleEngine {
    public static boolean evaluate(Rule rule, Customer customer) {
        return switch (rule) {
            case Rule.Equals(var attr, var value) -> switch (attr) {
                case "country" -> customer.country().equals(value);
                case "type" -> customer.type().equals(value);
                default -> false;
            };
        };
    }
}
```

#### Step 2: And/Or/Not 논리 연산 추가

```java
public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule rule) implements Rule {}
}
```

#### Step 3: GTE 조건 추가 - 흔한 실수 사례 ⭐

##### [Trap] "데이터"와 "값"의 시점 차이

**잘못된 설계** (흔한 실수):
```java
// ❌ 값(int)을 직접 받음 - 규칙 생성 시점에 결과 확정!
record GTE(int left, int right) implements Rule {}
```

**올바른 설계**:
```java
// ✅ 속성 이름(String)을 저장 - 평가 시점에 데이터 참조
record GTE(String attribute, int threshold) implements Rule {}

case Rule.GTE(var attr, var threshold) -> switch (attr) {
    case "totalSpend" -> customer.totalSpend() >= threshold;
    default -> false;
};
```

---

### 8.3 복잡한 할인 규칙 예제

"한국(KR)에 살면서, (구매액 100만원 이상이거나 VIP)"

```java
Rule krDiscountRule = new Rule.And(
    new Rule.Equals("country", "KR"),
    new Rule.Or(
        new Rule.Equals("type", "VIP"),
        new Rule.GTE("totalSpend", 1_000_000)
    )
);

Customer krCustomer = new Customer("KR", "GOLD", 1_500_000);
boolean discountable = RuleEngine.evaluate(krDiscountRule, krCustomer); // true
```

---

### 8.4 동적 규칙 로딩 (DB/JSON)

```json
{
  "type": "and",
  "left": { "type": "equals", "attribute": "country", "value": "KR" },
  "right": { "type": "gte", "attribute": "totalSpend", "threshold": 500000 }
}
```

마케팅 팀이 관리자 화면에서 규칙을 수정하면, **코드 배포 없이** 즉시 적용됩니다.

---

### 8.5 Before/After: if-else vs Rule Engine

##### Before: 하드코딩
```java
if (customer.country().equals("KR") && customer.type().equals("VIP")) {
    return true;
}
// 새 규칙마다 코드 수정 필요...
```

##### After: 데이터 주도
```java
List<Rule> rules = loadFromDatabase();
return rules.stream().anyMatch(r -> RuleEngine.evaluate(r, customer));
// 새 규칙은 데이터 추가만!
```

---

### 퀴즈 Chapter 8

#### Q8.1 [함정 문제] GTE 규칙 설계 실수 ⭐
다음 GTE 규칙 설계의 문제점은?
```java
record GTE(int left, int right) implements Rule {}
Rule rule = new Rule.GTE(customer.totalSpend(), 1_000_000);
```

A. 문제없음
B. 규칙 생성 시점에 이미 결과가 결정되어, 다른 고객에게 재사용 불가
C. 컴파일 에러
D. 성능 문제

---

#### Q8.2 [코드 분석] 규칙 표현
다음 비즈니스 규칙을 표현한 Rule은?
"VIP 등급이 아니면서 주문금액이 3만원 이상인 경우"

A. `new And(new Not(new UserGrade(VIP)), new MinTotal(30000))`
B. `new Or(new Not(new UserGrade(VIP)), new MinTotal(30000))`
C. `new Not(new And(new UserGrade(VIP), new MinTotal(30000)))`
D. `new And(new UserGrade(VIP), new MinTotal(30000))`

---

#### Q8.3 [코드 분석] 재귀 구조
다음 Rule의 평가 순서는?

```java
Rule rule = new Or(
    new And(new UserGrade(VIP), new MinTotal(50000)),
    new ProductCategory(FASHION)
);
```

A. VIP → MinTotal → And → FASHION → Or
B. Or → And → VIP → MinTotal → FASHION
C. FASHION → MinTotal → VIP → And → Or
D. Or → FASHION → And → VIP → MinTotal

---

#### Q8.4 [설계 문제] 새 규칙 추가
"첫 구매 고객" 조건을 추가하려면?

A. if-else 문 추가
B. Rule sealed interface에 새 record 추가 + 해석기에 case 추가
C. 새로운 클래스 상속
D. 기존 Rule 수정

---

#### Q8.5 [코드 작성] 규칙 설계
다음 프로모션 규칙을 Rule 타입으로 표현하세요.
"신규 회원(가입 30일 이내)이거나, 주문금액이 10만원 이상이면서 전자제품 카테고리 상품을 포함한 경우"

---

정답은 Appendix C에서 확인할 수 있습니다.

---

# Part V: 레거시 탈출 (Refactoring & Architecture)

---

## Chapter 9: 현실 세계의 DOP: JPA/Spring과의 공존

### 학습 목표
1. JPA Entity와 DOP Domain Record의 차이를 이해한다
2. Entity에서 Domain Record로의 변환 전략을 적용할 수 있다
3. Spring Boot 프로젝트에서 DOP를 적용하는 구조를 설계할 수 있다
4. 점진적 리팩토링 전략을 수립할 수 있다
5. 실제 프로젝트에서 DOP 도입 시 고려사항을 파악할 수 있다

---

### 9.1 JPA Entity의 한계

JPA Entity는 **Identity(정체성)**이자 **가변 객체**입니다. DOP의 **Value(값)**와는 상극입니다.

```java
// JPA Entity: 가변, Identity 기반
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status;

    // Setter로 상태 변경 가능
    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }

    // 비즈니스 로직이 Entity에 섞여있음 (안티패턴)
    public void addItem(OrderItemEntity item) {
        items.add(item);
        recalculateTotal();
    }
}
```

#### 비유: 통역사

> **Entity와 Domain Record 사이에는 통역사가 필요합니다.**
>
> **JPA Entity**는 데이터베이스와 대화하는 언어입니다:
> - 가변적 (Hibernate가 상태를 추적해야 함)
> - 지연 로딩, 프록시 등 JPA 전용 기능
>
> **Domain Record**는 비즈니스 로직이 사용하는 언어입니다:
> - 불변적 (값이 변하지 않음)
> - 순수한 데이터 (인프라 관심사 없음)
> - 타입 안전성 (sealed interface 등)
>
> 두 언어 사이에서 **Mapper(통역사)**가 변환을 담당합니다.

---

### 9.2 레이어 분리 전략

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller / API Layer                    │
│                  (DTO ↔ Domain Record 변환)                  │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                         │
│              (Orchestration, 순서 제어)                      │
├─────────────────────────────────────────────────────────────┤
│                    Domain Layer (DOP)                        │
│           (Record, Sealed Interface, 순수 함수)              │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                       │
│           (JPA Entity, Repository 구현체)                    │
│                (Entity ↔ Domain Record 변환)                 │
└─────────────────────────────────────────────────────────────┘
```

---

### 9.3 Entity ↔ Domain Record 변환

#### Before: 직접 Entity 사용

```java
public class OrderService {
    public OrderEntity createOrder(CreateOrderRequest request) {
        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(request.customerId());
        entity.setStatus(OrderStatusEnum.PENDING);
        // Entity를 직접 조작 - 도메인 로직 없음
        return orderRepository.save(entity);
    }
}
```

#### After: Mapper 활용

```java
public class OrderMapper {
    // Entity → Domain Record
    public static Order toDomain(OrderEntity entity) {
        OrderStatus status = switch (entity.getStatus()) {
            case PENDING -> new OrderStatus.PendingPayment(entity.getCreatedAt());
            case PAID -> new OrderStatus.PaymentComplete(
                entity.getPaidAt(),
                new PaymentId(entity.getPaymentId())
            );
            case SHIPPED -> new OrderStatus.Shipping(
                entity.getShippedAt(),
                new TrackingNumber(entity.getTrackingNumber())
            );
            case DELIVERED -> new OrderStatus.Delivered(entity.getDeliveredAt());
            case CANCELED -> new OrderStatus.Canceled(
                entity.getCanceledAt(),
                CancelReason.valueOf(entity.getCancelReason())
            );
        };

        return new Order(
            new OrderId(entity.getId().toString()),
            new CustomerId(entity.getCustomerId()),
            mapItems(entity.getItems()),
            Money.krw(entity.getTotalAmount().longValue()),
            status
        );
    }
}
```

---

### 9.4 점진적 리팩토링 전략

#### 단계 1: 가장 복잡한 로직 선정

```java
// Before: 복잡한 가격 계산 로직이 Service에 섞여있음
public class OrderService {
    public Order createOrder(CreateOrderRequest request) {
        // 100줄의 복잡한 로직...
        // 할인, 세금, 배송비, 포인트 적용 등
    }
}
```

#### 단계 2: 순수 함수 추출

```java
// After: 순수 함수로 추출
public class PriceCalculations {
    public static Money calculateSubtotal(List<OrderItem> items) { ... }
    public static Money applyDiscount(Money subtotal, Discount discount) { ... }
    public static Money applyTax(Money amount, TaxRate taxRate) { ... }

    // 통합
    public static Money calculateTotal(
        List<OrderItem> items,
        Optional<Discount> discount,
        TaxRate taxRate,
        ShippingFee shippingFee
    ) {
        Money subtotal = calculateSubtotal(items);
        Money discounted = discount
            .map(d -> applyDiscount(subtotal, d))
            .orElse(subtotal);
        Money taxed = applyTax(discounted, taxRate);
        return applyShipping(taxed, shippingFee);
    }
}
```

#### 단계 3: 테스트 작성 (Mocking 없이!)

```java
class PriceCalculationsTest {

    @Test
    void calculateTotal_withAllDiscounts() {
        // Given
        List<OrderItem> items = List.of(
            new OrderItem(productId, Quantity.of(2), Money.krw(10000))
        );
        Optional<Discount> discount = Optional.of(new Discount.Percentage(10));
        TaxRate taxRate = new TaxRate(10);
        ShippingFee shippingFee = ShippingFee.free();

        // When
        Money result = PriceCalculations.calculateTotal(
            items, discount, taxRate, shippingFee
        );

        // Then
        // 20000 * 0.9 * 1.1 = 19800
        assertEquals(Money.krw(19800), result);
    }
}
```

---

### 9.5 주의사항 및 팁

| 상황 | 권장 사항 |
|-----|----------|
| 기존 Entity에 Setter 제거 | 한 번에 다 제거하지 말고, 새 기능부터 Record 사용 |
| 순환 참조 | Domain Record에서는 ID만 참조, 객체 그래프 지양 |
| Lazy Loading | Domain 레이어에서는 이미 로드된 데이터만 사용 |
| 트랜잭션 경계 | Application Layer(Orchestrator)에서 관리 |

```java
// 권장: ID 참조
public record Order(
    OrderId id,
    CustomerId customerId,  // Customer 객체가 아닌 ID만!
    List<OrderItem> items,
    Money totalAmount,
    OrderStatus status
) {}

// 비권장: 객체 그래프
public record Order(
    OrderId id,
    Customer customer,      // 전체 객체 참조 - 비권장
    List<OrderItem> items,
    ...
) {}
```

---

### 퀴즈 Chapter 9

#### Q9.1 [개념 확인] Entity vs Record
JPA Entity와 Domain Record의 차이로 **올바른** 것은?

A. Entity는 불변, Record는 가변
B. Entity는 가변이고 Identity 기반, Record는 불변이고 Value 기반
C. 둘 다 불변이지만 용도가 다름
D. Entity에서는 비즈니스 로직을 넣어야 함

---

#### Q9.2 [코드 분석] Mapper의 역할
Mapper 클래스의 역할은?

A. Entity에 비즈니스 로직 추가
B. Entity와 Domain Record 간의 변환
C. 데이터베이스 쿼리 최적화
D. 트랜잭션 관리

---

#### Q9.3 [설계 문제] 레이어 배치
"주문 금액 계산" 로직은 어느 레이어에 있어야 하나요?

A. Controller Layer
B. Infrastructure Layer (Entity)
C. Domain Layer (순수 함수)
D. Application Layer

---

#### Q9.4 [코드 분석] 점진적 리팩토링
레거시 코드 리팩토링 시 첫 단계로 적절한 것은?

A. 모든 Entity를 Record로 한 번에 변환
B. 가장 복잡한 비즈니스 로직을 순수 함수로 추출
C. JPA를 완전히 제거
D. 새 프로젝트로 처음부터 다시 작성

---

#### Q9.5 [코드 작성] Mapper 구현
다음 Entity를 Domain Record로 변환하는 Mapper를 작성하세요.

```java
@Entity
public class ProductEntity {
    @Id private Long id;
    private String name;
    private BigDecimal price;
    @Enumerated(EnumType.STRING) private ProductStatusEnum status;
}

// Domain Record
public record Product(
    ProductId id,
    ProductName name,
    Money price,
    ProductStatus status
) {}

sealed interface ProductStatus {
    record Available() implements ProductStatus {}
    record OutOfStock() implements ProductStatus {}
    record Discontinued() implements ProductStatus {}
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.

---

# Appendix

---

## Appendix A: 흔한 실수와 안티패턴 모음 (강화)

### A.1 Record에 가변 컬렉션 저장 (방어적 복사 누락)

```java
// ❌ 안티패턴
public record Cart(List<CartItem> items) {}
List<CartItem> mutableList = new ArrayList<>();
Cart cart = new Cart(mutableList);
mutableList.add(newItem);  // cart 내부가 변경됨!

// ✅ 올바른 방법
public record Cart(List<CartItem> items) {
    public Cart { items = List.copyOf(items); }
}
```

### A.2 sealed interface에 default 사용

```java
// ❌ 새 상태 추가 시 누락 감지 안 됨
return switch (status) {
    case Active a -> "활성";
    default -> "비활성";
};

// ✅ 모든 case 명시
return switch (status) {
    case Active a -> "활성";
    case Inactive i -> "비활성";
};
```

### A.3 Entity에 비즈니스 로직 추가 (Active Record)

```java
// ❌ DOP 철학 위반
public record User(String id, String name) {
    public void saveToDb() { Database.save(this); }
}

// ✅ 데이터와 로직 분리
public record User(String id, String name) {}
public class UserRepository { void save(User u) {...} }
```

### A.4 Rule Engine에서 값과 속성 혼동

```java
// ❌ 규칙 생성 시점에 결과 확정
record GTE(int left, int right) implements Rule {}

// ✅ 평가 시점에 데이터 참조
record GTE(String attribute, int threshold) implements Rule {}
```

### A.5 Optional을 필드로 사용

```java
// ❌ 안티패턴: Record 필드에 Optional 사용
public record Order(
    OrderId id,
    Optional<CouponCode> couponCode  // 직렬화 문제, null 문제
) {}

// ✅ 올바른 방법: 합 타입 사용
sealed interface AppliedCoupon {
    record None() implements AppliedCoupon {}
    record Applied(CouponCode code, Money discount) implements AppliedCoupon {}
}

public record Order(OrderId id, AppliedCoupon coupon) {}
```

---

## Appendix B: DOP Java 치트시트

### B.1 Record 기본 문법 (Java 16+)

```java
// 기본 Record (Java 16+)
public record Point(int x, int y) {}

// Compact Constructor (검증)
public record Money(BigDecimal amount) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("음수 금액");
        }
    }
}

// 팩토리 메서드
public record UserId(long value) {
    public static UserId of(long value) {
        return new UserId(value);
    }
}

// with 패턴 (수동 구현 - JEP 468 미포함으로 필수)
public record Order(OrderId id, OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, newStatus);
    }
}
```

### B.2 Sealed Interface 문법 (Java 17+)

```java
// 기본 Sealed Interface
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height) implements Shape {}

// 중첩 정의
sealed interface PaymentStatus {
    record Pending() implements PaymentStatus {}
    record Completed(String transactionId) implements PaymentStatus {}
    record Failed(String reason) implements PaymentStatus {}
}
```

### B.3 Pattern Matching (Java 21+)

```java
// Record Patterns in switch (Java 21+ JEP 440, 441)
String describe(Shape shape) {
    return switch (shape) {
        case Circle(var r) -> "원 (반지름: " + r + ")";
        case Rectangle(var w, var h) -> "사각형 (" + w + "x" + h + ")";
        case Triangle(var b, var h) -> "삼각형 (밑변: " + b + ")";
    };
}

// 가드 조건 (Java 21+)
String categorize(Shape shape) {
    return switch (shape) {
        case Circle(var r) when r > 10 -> "큰 원";
        case Circle(var r) -> "작은 원";
        case Rectangle r -> "사각형";
        case Triangle t -> "삼각형";
    };
}

// Unnamed Variables (Java 22+ JEP 456) - 사용하지 않는 변수에 _ 사용
String getPaymentId(OrderStatus status) {
    return switch (status) {
        case Paid(_, var paymentId) -> paymentId;  // 첫 번째 필드는 무시
        case Unpaid _ -> "N/A";  // 전체 변수를 무시
        case Shipping _, Delivered _, Cancelled _ -> "N/A";
    };
}
```

### B.4 Result 타입 활용

```java
// Result 정의
sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}
}

// 사용
Result<User, String> result = findUser(id);

// 처리
String message = switch (result) {
    case Success(var user) -> "찾음: " + user.name();
    case Failure(var error) -> "에러: " + error;
};

// map/flatMap
result
    .map(user -> user.email())
    .flatMap(email -> sendEmail(email));
```

### B.5 불변 컬렉션 (Java 9+)

```java
// 불변 리스트 생성 (Java 9+)
List<String> immutable = List.of("a", "b", "c");

// 기존 리스트를 불변으로 복사 (Java 10+)
List<String> copied = List.copyOf(mutableList);

// 불변 맵 (Java 9+)
Map<String, Integer> map = Map.of("a", 1, "b", 2);

// Sequenced Collections (Java 21+ JEP 431)
List<String> list = List.of("a", "b", "c");
String first = list.getFirst();  // "a"
String last = list.getLast();    // "c"
List<String> reversed = list.reversed();  // ["c", "b", "a"]
```

### B.6 방어적 복사 패턴

```java
// Record에서 불변성 보장
public record Order(OrderId id, List<OrderItem> items) {
    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(items);
        items = List.copyOf(items);  // 방어적 복사
    }
}
```

### B.7 Java 25 DOP 기능

```java
// Primitive Types in Patterns (Java 25 Preview - JEP 507)
// --enable-preview 플래그 필요
int categorize(Object obj) {
    return switch (obj) {
        case Integer i when i > 0 -> 1;
        case Integer i when i < 0 -> -1;
        case Integer _ -> 0;
        default -> throw new IllegalArgumentException();
    };
}

// Scoped Values (Java 25+ JEP 506) - 불변 컨텍스트 전달
// ThreadLocal 대신 불변 값 전달에 적합
static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

void processRequest(User user) {
    ScopedValue.runWhere(CURRENT_USER, user, () -> {
        // 이 스코프 내에서 CURRENT_USER.get()으로 접근 가능
        handleRequest();
    });
}
```

> ⚠️ **JEP 468 (Derived Record Creation) 미포함 안내**:
> `with` expression (`record with { field = value; }`)은 Java 25에 **포함되지 않았습니다**.
> Record 필드 변경 시 수동 `withXxx()` 메서드 또는 생성자를 사용하세요.

---

## Appendix C: 전체 정답 및 해설 (강화)

### 정답표

| Ch | Q1 | Q2 | Q3 | Q4 | Q5 |
|----|----|----|----|----|-----|
| 1  | C  | B  | B  | C  | 코드 |
| 2  | B  | C  | B  | B  | 코드 |
| 3  | B  | C  | A  | C  | 코드 |
| 4  | B  | B  | A  | A  | 코드 |
| 5  | B  | A  | A  | B  | 코드 |
| 6  | A  | C  | B  | B  | 코드 |
| 7  | C  | C  | B  | B  | 코드 |
| 8  | B  | A  | A  | B  | 코드 |
| 9  | B  | B  | C  | B  | 코드 |

### 주요 해설

**Ch 4 (불가능한 상태)**
- Q4.1: B - 유효하지 않은 상태를 타입으로 표현할 수 없게 설계
- Q4.2: B - isDelivered가 false인데 다른 필드가 있을 수 있는 불일치 상태
- Q4.3: A - default가 Shipped 처리를 가로채서 망라성 검증 무효화
- Q4.4: A - Unused와 Used로 분리하면 불일치 상태 불가능
- Q4.5: 코드 - sealed interface로 각 상태별 필요한 데이터만 포함

**Ch 5 (전체 함수)**
- Q5.1: B - 전체 함수는 모든 입력에 대해 정의된 결과를 반환
- Q5.2: A - `divide`만 부분 함수 (0으로 나눌 때 예외)
- Q5.3: A - 10 * 2 = 20, 20 > 15이므로 Success(20), getOrElse 결과는 20
- Q5.4: B - sealed interface로 NotFound, AlreadyUsed, Expired, MinOrderAmountNotMet 정의
- Q5.5: 코드 - Result<User, WithdrawError>를 반환하도록 변환

**Ch 6 (파이프라인)**
- Q6.1: A - 오직 A만 순수 함수 (부수효과 없음)
- Q6.2: C - Meat 레이어는 순수한 비즈니스 로직만 포함
- Q6.3: B - productRepository.find()와 taxService.getCurrentRate()가 I/O
- Q6.4: B - Product, TaxRate를 파라미터로 받는 순수 함수로 분리
- Q6.5: 코드 - applyPromotionDiscount(Order, Promotion) 순수 함수 추출

**Ch 7 (대수적 속성)**
- Q7.1: C - 문자열 연결은 결합법칙 만족 (a + (b + c) == (a + b) + c)
- Q7.2: C - Math.abs는 멱등 (abs(abs(x)) == abs(x))
- Q7.3: B - 중복 호출 시 할인이 계속 적용됨 (90% → 81% → ...)
- Q7.4: B - 할인 적용 여부를 상태로 저장하고 확인
- Q7.5: 코드 - Money와 유사하게 zero() 항등원 구현

**Ch 8 (인터프리터)**
- Q8.1: B - 값 대신 속성 이름을 저장해야 다른 데이터에 재사용 가능
- Q8.2: A - And(Not(UserGrade(VIP)), MinTotal(30000))
- Q8.3: A - 왼쪽부터 깊이 우선 탐색 (VIP → MinTotal → And → FASHION → Or)
- Q8.4: B - Rule sealed interface에 새 record 추가 + 해석기에 case 추가
- Q8.5: 코드 - Or(NewMember, And(MinTotal(100000), ProductCategory(ELECTRONICS)))

**Ch 9 (JPA/Spring)**
- Q9.1: B - Entity는 가변/Identity, Record는 불변/Value
- Q9.2: B - Mapper는 Entity ↔ Domain Record 변환 담당
- Q9.3: C - 주문 금액 계산은 Domain Layer의 순수 함수
- Q9.4: B - 가장 복잡한 비즈니스 로직을 순수 함수로 추출하여 시작
- Q9.5: 코드 - switch문으로 ProductStatusEnum → ProductStatus 변환

### 함정 문제 해설

**Q1.6 (DOP 철학)**: **C** - Active Record 패턴은 데이터에 DB 의존성과 부수 효과를 섞어 DOP 철학을 정면 위반

**Q2.3 (불변성의 함정)**: **B** `[Alice, Bob]` - Record 필드가 가변 List를 참조하면 외부에서 변경 가능. 방어적 복사 필요

**Q3.2 (봉인된 운명)**: **B** 컴파일 에러 - Sealed Interface는 모든 케이스 처리를 강제

**Q8.1 (GTE 설계 실수)**: **B** - 값 대신 속성 이름을 저장해야 다른 데이터에 재사용 가능

---

## Appendix D: Final Boss Quiz - DOP 마스터 검증 (신규)

### 문제 1: 불변성의 함정

```java
public record Team(String name, List<String> members) {}

List<String> list = new ArrayList<>();
list.add("Alice");
Team team = new Team("Alpha", list);
list.add("Bob");
System.out.println(team.members());
```

**정답**: `[Alice, Bob]` - 방어적 복사 누락으로 불변성 파괴

---

### 문제 2: 봉인된 운명

sealed interface에 새 케이스 추가 후 switch 미수정 시?

**정답**: B (컴파일 에러) - Sealed Interface의 핵심 장점

---

### 문제 3: DOP 아키텍처

DOP 철학에 가장 어긋나는 코드?

**정답**: C (Active Record 패턴) - 데이터에 부수 효과를 섞으면 안 됨

---

**[강의 마무리]**

DOP 핵심 정리:
1. **데이터와 로직을 분리** (Record + Static 함수)
2. **불변성 유지** (방어적 복사, with 패턴)
3. **합 타입으로 상태 축소** (Sealed Interface)
4. **전체 함수로 실패 명시** (Result 타입)
5. **Rule as Data로 유연성 확보** (인터프리터 패턴)
