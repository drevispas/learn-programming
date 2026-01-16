# Java로 정복하는 데이터 지향 프로그래밍
## Data-Oriented Programming in Java - 이커머스 실전 교재

**대상**: 이커머스 개발자 (회원, 상품, 주문, 결제, 쿠폰 도메인)

**목표**: 복잡성을 수학적으로 제어하고, 컴파일러에게 검증을 위임하는 견고한 시스템 구축

**도구**: Java 17+ (Record, Sealed Interface, Pattern Matching)

**원전**: *Data-Oriented Programming in Java* by Chris Kiehl

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
- **Chapter 8**: 인터프리터 패턴: 로직을 데이터로

### Part V: 레거시 탈출 (Refactoring & Architecture)
- **Chapter 9**: 현실 세계의 DOP: JPA/Spring과의 공존

### Appendix
- **Appendix A**: 흔한 실수와 안티패턴 모음
- **Appendix B**: DOP Java 치트시트
- **Appendix C**: 전체 퀴즈 정답 및 해설

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

```java
// Bad: 데이터와 로직이 섞여있음
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

// Good: 데이터(Record)와 로직(Class)을 분리
public record Order(BigDecimal amount, String couponCode) {}

public class OrderCalculator {
    public static BigDecimal calculateDiscountedPrice(Order order) {
        if (order.couponCode() != null) {
            return order.amount().multiply(new BigDecimal("0.9"));
        }
        return order.amount();
    }
}
```

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

// Good: 공통 구조를 Record로 표현
public record Summary(String id, BigDecimal amount, Map<String, Object> metadata) {}

// 또는 타입 안전성을 위해
public record OrderSummary(OrderId id, Money amount, OrderStatus status) {}
public record PaymentSummary(PaymentId id, Money amount, PaymentMethod method) {}
```

#### 원칙 3: 데이터는 불변(Immutable)이다

```java
// Bad: 가변 객체
public class MutableOrder {
    private String status;

    public void setStatus(String status) {
        this.status = status;  // 언제 어디서 바뀔지 모름
    }
}

// Good: 불변 객체
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

### 1.4 이커머스 리팩토링 예시

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

#### 가장 중요한 개념적 구분

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
>
> 회원(Member)은 여권입니다 - ID가 같으면 같은 회원입니다.
> 금액(Money)은 이름표입니다 - 10,000원은 어디에 있든 10,000원입니다.

#### 비유: 사진과 거울

> **Value는 사진이고, Identity를 가진 객체는 거울에 비친 모습입니다.**
>
> **사진(Value - 불변)**:
> - 사진 속 모습은 절대 변하지 않습니다
> - 같은 순간을 찍은 사진은 모두 동일합니다
> - 사진을 "수정"하려면 새로운 사진을 만들어야 합니다
>
> **거울(Identity - 가변)**:
> - 거울에 비친 모습은 시간에 따라 변합니다
> - 하지만 여전히 "나"라는 사실은 변하지 않습니다
> - 거울 속 나는 계속 같은 사람이지만, 상태가 변합니다

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

// 쿠폰 코드: "SUMMER2024"는 어디에서든 같은 코드
public record CouponCode(String value) {}


// ========== Identity Types (정체성) ==========

// 회원: 이름을 바꿔도 같은 회원
public record Member(MemberId id, String name, EmailAddress email) {}

// 주문: 상태가 바뀌어도 같은 주문
public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {}

// 상품: 가격이 바뀌어도 같은 상품
public record Product(ProductId id, ProductName name, Money price) {}
```

#### Identity 타입의 동등성 비교

```java
// Identity 타입은 ID로 비교해야 함
public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {

    // equals를 ID만으로 재정의 (선택적)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);  // ID만 비교
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

// 사용 예
Order order1 = new Order(new OrderId("ORD-001"), items1, new Created());
Order order2 = new Order(new OrderId("ORD-001"), items2, new Paid());

order1.equals(order2);  // true - 같은 주문 (ID가 같음)
// 하지만 상태는 다름 - 시간에 따른 변화를 나타냄
```

---

### 2.3 Java Record: Value Type의 완벽한 도구

#### Record가 제공하는 것

```java
// 이 한 줄이
public record Money(BigDecimal amount, Currency currency) {}

// 이 모든 것을 제공합니다
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount)
            && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return "Money[amount=" + amount + ", currency=" + currency + "]";
    }
}
```

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
        // this.currency = currency; 자동 실행
    }

    // 팩토리 메서드
    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // 비즈니스 연산 (새 객체 반환)
    public Money add(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("통화가 다릅니다");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
```

---

### 2.4 얕은 불변성 vs 깊은 불변성

#### 얕은 불변성(Shallow Immutability)의 함정

Record는 필드 자체를 `final`로 만들지만, 필드가 참조하는 객체의 내부까지 얼리지는 못합니다.

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

#### 비유: 금고와 열쇠

> **얕은 불변성은 금고 문만 잠그고 열쇠를 남겨둔 것과 같습니다.**
>
> 금고(Record) 자체는 단단히 잠겨있지만,
> 금고 안에 있는 서랍(List)의 열쇠를 외부에 남겨두면
> 누군가 금고를 열지 않고도 서랍을 열어 내용물을 바꿀 수 있습니다.
>
> 진정한 보안(깊은 불변성)을 원한다면,
> 모든 열쇠를 금고 안에 넣고 문을 잠가야 합니다.

#### 깊은 불변성(Deep Immutability) 확보

```java
public record Order(OrderId id, List<OrderItem> items) {
    // Compact Constructor에서 방어적 복사
    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(items);
        // 불변 리스트로 복사 - 외부에서 변경 불가
        items = List.copyOf(items);
    }

    // items()는 이미 불변 리스트를 반환
    // 추가 방어가 필요 없음
}

// 이제 안전!
List<OrderItem> mutableList = new ArrayList<>();
mutableList.add(new OrderItem(productId, quantity));

Order order = new Order(orderId, mutableList);

// 원본 리스트를 수정해도 Order에 영향 없음
mutableList.add(new OrderItem(anotherProduct, anotherQuantity));

// order.items()는 여전히 1개의 아이템만 가짐
```

#### 중첩된 컬렉션의 깊은 불변성

```java
public record Category(
    CategoryId id,
    String name,
    List<Product> products,
    Map<String, String> metadata
) {
    public Category {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        // List 깊은 복사
        products = products == null ? List.of() : List.copyOf(products);
        // Map 깊은 복사
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
```

---

### 2.5 값 변경 패턴: with 메서드

불변 객체에서 값을 "변경"하려면 새 객체를 만들어야 합니다.

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

    // 아이템 추가 (불변 방식)
    public Order addItem(OrderItem item) {
        List<OrderItem> newItems = new ArrayList<>(items);
        newItems.add(item);
        return new Order(id, customerId, newItems, totalAmount, status);
    }
}

// 사용
Order unpaidOrder = new Order(id, customerId, items, total, new Unpaid());
Order paidOrder = unpaidOrder.withStatus(new Paid(LocalDateTime.now(), paymentId));

// unpaidOrder는 여전히 Unpaid 상태 (불변)
// paidOrder는 새로운 Paid 상태의 객체
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

#### Q2.3 [버그 찾기] 깊은 불변성
다음 코드의 문제점을 찾으세요.

```java
public record Order(OrderId id, List<OrderItem> items) {
    public Order(OrderId id, List<OrderItem> items) {
        this.id = id;
        this.items = Collections.unmodifiableList(items);
    }
}

// 사용
List<OrderItem> mutableList = new ArrayList<>();
mutableList.add(item1);
Order order = new Order(orderId, mutableList);
mutableList.add(item2);  // 문제?
```

A. 문제없음 - `unmodifiableList`가 보호함
B. Order 내부의 items가 변경됨
C. 컴파일 에러 발생
D. NullPointerException 발생

---

#### Q2.4 [설계 문제] 적절한 타입 선택
이커머스에서 "배송 주소"를 모델링할 때 적절한 방식은?

A. Entity로 모델링 - 주소도 고유 ID가 있어야 함
B. Value Object로 모델링 - 주소 내용이 같으면 같은 주소
C. String으로 충분 - "서울시 강남구 역삼동"
D. Map<String, String>으로 모델링 - 유연성 확보

---

#### Q2.5 [코드 작성] with 패턴 구현
다음 Record에 `withPrice` 메서드를 추가하세요.

```java
public record Product(
    ProductId id,
    ProductName name,
    Money price,
    StockQuantity stock
) {
    // withPrice 메서드 구현
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.

---

# Part II: 데이터 모델링의 수학 (Algebraic Data Types)

이 파트는 DOP의 핵심입니다. 복잡성을 '감'이 아니라 '수학'으로 제어하는 방법을 배웁니다.

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

#### 시스템 복잡도의 수학적 정의

시스템의 복잡도는 **"가능한 상태의 총 개수(Cardinality)"**와 비례합니다. 상태가 많을수록 테스트해야 할 경우의 수가 늘어나고, 버그가 숨어들 공간이 넓어집니다.

```
복잡도 ∝ 가능한 상태의 수 (Cardinality)
```

| 타입 | 기수(Cardinality) |
|-----|------------------|
| `boolean` | 2 (true, false) |
| `byte` | 256 |
| `int` | 2^32 ≈ 40억 |
| `String` | ∞ (무한) |
| `Optional<T>` | |T| + 1 |
| `enum Status { A, B, C }` | 3 |

#### 비유: 비밀번호 조합

> **기수(Cardinality)는 자물쇠의 비밀번호 조합 수와 같습니다.**
>
> - 3자리 숫자 자물쇠: 10 × 10 × 10 = 1,000가지 조합
> - 4자리 숫자 자물쇠: 10 × 10 × 10 × 10 = 10,000가지 조합
>
> 자릿수(필드)가 하나 늘어날 때마다 복잡도가 10배로 증가합니다.
> 코드에서 필드를 추가하는 것은 자물쇠에 자릿수를 추가하는 것과 같습니다.
>
> 그리고 그 많은 조합 중에서 실제로 "유효한" 비밀번호는 단 하나뿐입니다.
> 나머지 9,999개는 모두 "불가능한 상태"입니다.

---

### 3.2 곱 타입 (Product Type): 상태의 폭발

#### 필드 추가 = 곱셈

클래스에 필드를 추가하는 것은 경우의 수를 **곱하는(Multiply)** 행위입니다.

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
```

#### 상태 폭발의 시각화 (ASCII)

```
isCreated  isPaid  isShipped  isDelivered  isCanceled
    │        │        │           │            │
    ├─T      ├─T      ├─T         ├─T          ├─T
    │        │        │           │            │
    └─F      └─F      └─F         └─F          └─F

총 조합: 2 × 2 × 2 × 2 × 2 = 32가지

하지만 실제로 유효한 주문 상태는?
- Created (생성됨)
- Paid (결제됨)
- Shipped (배송중)
- Delivered (배송완료)
- Canceled (취소됨)

단 5가지뿐! 나머지 27가지는 "불가능한 상태"
```

#### 불가능한 상태의 예

```java
// 논리적으로 불가능하지만, 코드상 표현 가능한 상태들

// 1. 배송됐는데 결제 안 됨?
isShipped = true, isPaid = false

// 2. 취소됐는데 배송 완료?
isCanceled = true, isDelivered = true

// 3. 생성 안 됐는데 결제됨?
isCreated = false, isPaid = true

// 이런 상태가 가능하다는 것 자체가 버그의 원인!
```

---

### 3.3 합 타입 (Sum Type): 상태의 축소

#### OR 관계로 상태 제한

합 타입은 **"이것 아니면 저것(OR)"** 관계입니다. Java에서는 `sealed interface`로 구현합니다.

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
```

**32가지 상태를 5가지로 줄였습니다!** 복잡도가 84% 감소했습니다.

#### 비유: 멀티탭 vs 스위치

> **곱 타입(Product Type)은 멀티탭, 합 타입(Sum Type)은 선택 스위치입니다.**
>
> **멀티탭 (곱 타입)**:
> - 4개의 콘센트가 있으면 각각 ON/OFF = 2^4 = 16가지 조합
> - 모든 조합이 가능하지만, 대부분은 의미 없음
> - "첫 번째만 ON, 나머지 OFF"처럼 원하는 상태를 만들기 복잡
>
> **선택 스위치 (합 타입)**:
> - 4개의 위치가 있는 단일 스위치 = 4가지 상태만 가능
> - 한 번에 하나만 선택됨
> - 명확하고 단순함

#### 계산 비교

```
곱 타입 (AND): A × B × C = 2 × 2 × 2 = 8가지

    ┌─────┬─────┬─────┐
    │  A  │  B  │  C  │
    ├─────┼─────┼─────┤
    │  T  │  T  │  T  │
    │  T  │  T  │  F  │
    │  T  │  F  │  T  │
    │  T  │  F  │  F  │
    │  F  │  T  │  T  │
    │  F  │  T  │  F  │
    │  F  │  F  │  T  │
    │  F  │  F  │  F  │
    └─────┴─────┴─────┘

합 타입 (OR): A + B + C = 1 + 1 + 1 = 3가지

    ┌─────────────┐
    │ A (선택됨)   │
    ├─────────────┤
    │ B           │
    ├─────────────┤
    │ C           │
    └─────────────┘
```

---

### 3.4 실전 예제: 주문 상태 모델링

#### Bad: 곱 타입으로 모델링

```java
public class OrderState {
    private boolean isConfirmed;      // 2
    private boolean isPaid;           // 2
    private boolean isShipping;       // 2
    private boolean isDelivered;      // 2
    private boolean isCanceled;       // 2
    private boolean isRefunded;       // 2
    private boolean isReviewed;       // 2
    private boolean isArchived;       // 2
}
// 총 상태: 2^8 = 256가지
// 유효한 상태: 약 10가지
// 버그 가능성: 246가지 (96%!)
```

#### Good: 합 타입으로 모델링

```java
sealed interface OrderStatus {

    // 주문 진행 상태
    record PendingPayment(LocalDateTime createdAt) implements OrderStatus {}

    record PaymentComplete(
        LocalDateTime paidAt,
        PaymentId paymentId
    ) implements OrderStatus {}

    record Shipping(
        LocalDateTime shippedAt,
        TrackingNumber trackingNumber
    ) implements OrderStatus {}

    record Delivered(
        LocalDateTime deliveredAt,
        String receiverName
    ) implements OrderStatus {}

    // 종료 상태
    record Canceled(
        LocalDateTime canceledAt,
        CancelReason reason
    ) implements OrderStatus {}

    record Refunded(
        LocalDateTime refundedAt,
        RefundId refundId,
        Money refundAmount
    ) implements OrderStatus {}
}
// 총 상태: 6가지
// 유효한 상태: 6가지 (100%!)
// 버그 가능성: 0가지
```

#### 상태별 필수 데이터 강제

합 타입의 또 다른 장점은 **각 상태에 필요한 데이터를 강제**한다는 것입니다.

```java
// Shipping 상태에는 반드시 trackingNumber가 있어야 함
record Shipping(LocalDateTime shippedAt, TrackingNumber trackingNumber)
    implements OrderStatus {}

// 이런 코드는 불가능!
new Shipping(now, null);  // Compile? OK. Runtime? trackingNumber 검증 실패

// Compact Constructor로 강제
record Shipping(LocalDateTime shippedAt, TrackingNumber trackingNumber)
    implements OrderStatus {
    public Shipping {
        Objects.requireNonNull(shippedAt);
        Objects.requireNonNull(trackingNumber);
    }
}
```

---

### 3.5 Deep Dive: Optional은 왜 합 타입인가?

`Optional<T>`는 값이 있거나(`Present`) 없거나(`Empty`) 둘 중 하나입니다.

```java
// Optional의 내부 구조 (개념적)
sealed interface Optional<T> {
    record Present<T>(T value) implements Optional<T> {}
    record Empty<T>() implements Optional<T> {}
}

// 기수 계산
// Cardinality(Optional<T>) = Cardinality(Present) + Cardinality(Empty)
//                          = |T| + 1
```

#### null vs Optional

```java
// null 참조: 암묵적 합 타입
String name = null;  // String | null, 하지만 컴파일러가 모름

// Optional: 명시적 합 타입
Optional<String> name = Optional.empty();  // 컴파일러가 "값이 없을 수 있음"을 앎

// 문제
String name = null;
name.length();  // NullPointerException (런타임)

// 해결
Optional<String> name = Optional.empty();
name.map(String::length);  // 안전하게 처리
```

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

#### Q3.2 [개념 확인] 합 타입의 기수
다음 sealed interface의 기수는?

```java
sealed interface PaymentMethod {
    record Card(String cardNumber) implements PaymentMethod {}
    record BankTransfer(String accountNumber) implements PaymentMethod {}
    record Points(int amount) implements PaymentMethod {}
}
```

A. 3
B. |String| × |String| × |int|
C. |String| + |String| + |int|
D. 무한대

---

#### Q3.3 [코드 분석] 상태 폭발
다음 클래스의 가능한 상태 수와 그 중 유효한 상태 수를 계산하세요.

```java
class Subscription {
    boolean isActive;      // 활성화 여부
    boolean isPaused;      // 일시정지 여부
    boolean isCanceled;    // 취소 여부
    boolean isExpired;     // 만료 여부
}
```

A. 가능: 16, 유효: 4
B. 가능: 16, 유효: 16
C. 가능: 4, 유효: 4
D. 가능: 8, 유효: 4

---

#### Q3.4 [설계 문제] 리팩토링
Q3.3의 `Subscription`을 합 타입으로 리팩토링하면?

A. `sealed interface`로 Active, Paused, Canceled, Expired 정의
B. `enum`으로 4가지 상태 정의
C. A, B 모두 가능
D. 곱 타입이 더 적합하므로 리팩토링 불필요

---

#### Q3.5 [코드 작성] 합 타입 설계
이커머스의 "결제 상태"를 합 타입으로 설계하세요. 요구사항:
- 대기중(Pending)
- 처리중(Processing) - 결제 시도 시각 필요
- 성공(Completed) - 결제 완료 시각, 결제 ID 필요
- 실패(Failed) - 실패 시각, 실패 사유 필요
- 취소(Cancelled) - 취소 시각, 환불 금액 필요

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

#### 전통적인 방식: 런타임 검증

대부분의 개발자는 데이터를 받고 나서 `isValid()`를 호출합니다.

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

#### DOP 방식: 컴파일 타임 강제

DOP 개발자는 **유효하지 않은 데이터는 아예 생성조차 못하게** 타입을 설계합니다.

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

#### Bad: Boolean으로 모델링

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

#### Good: 합 타입으로 모델링

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

#### 비유: 철도 레일

> **Sealed Interface는 철도 분기점(선로 전환기)과 같습니다.**
>
> 철도에서 열차는 반드시 정해진 레일 중 하나로 갑니다:
> - 왼쪽 레일로 가거나
> - 오른쪽 레일로 가거나
> - 절대 레일을 벗어나지 않습니다
>
> 새로운 목적지(새 레일)가 추가되면
> 분기점 관리자는 반드시 새 레일을 설치해야 합니다.
>
> Sealed Interface도 마찬가지입니다.
> 모든 케이스를 처리해야 하고, 새 케이스가 추가되면 컴파일러가 알려줍니다.

#### 컴파일러가 검증하는 코드

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

**유지보수의 안전망**이 획기적으로 튼튼해집니다. 새 상태를 추가하면 컴파일러가 모든 switch문을 찾아 "이곳을 수정하세요"라고 알려줍니다.

---

### 4.4 실전 예제: 결제 상태 모델링

#### 이커머스 결제 상태 (합 타입)

```java
sealed interface PaymentStatus {

    // 대기 중 - 추가 정보 없음
    record Pending() implements PaymentStatus {}

    // 처리 중 - 결제 시도 시각 필요
    record Processing(LocalDateTime startedAt) implements PaymentStatus {}

    // 성공 - 완료 시각, 결제 ID, 금액 필요
    record Completed(
        LocalDateTime completedAt,
        PaymentId paymentId,
        Money amount
    ) implements PaymentStatus {}

    // 실패 - 실패 시각, 사유 필요
    record Failed(
        LocalDateTime failedAt,
        FailureReason reason
    ) implements PaymentStatus {}

    // 환불 - 환불 시각, 원 결제 정보, 환불 금액 필요
    record Refunded(
        LocalDateTime refundedAt,
        PaymentId originalPaymentId,
        Money refundAmount
    ) implements PaymentStatus {}
}

// 실패 사유도 타입으로 명확히
sealed interface FailureReason {
    record InsufficientFunds() implements FailureReason {}
    record CardExpired() implements FailureReason {}
    record NetworkError(String message) implements FailureReason {}
    record FraudSuspected() implements FailureReason {}
}
```

#### 상태별 동작 구현

```java
public class PaymentProcessor {

    public String getPaymentMessage(PaymentStatus status) {
        return switch (status) {
            case Pending() -> "결제 대기 중입니다.";
            case Processing(var startedAt) ->
                "결제 처리 중입니다. (시작: " + startedAt + ")";
            case Completed(var at, var id, var amount) ->
                "결제 완료! " + amount + " (ID: " + id + ")";
            case Failed(var at, var reason) ->
                "결제 실패: " + getFailureMessage(reason);
            case Refunded(var at, var originalId, var amount) ->
                "환불 완료: " + amount + " (원 결제: " + originalId + ")";
        };
    }

    private String getFailureMessage(FailureReason reason) {
        return switch (reason) {
            case InsufficientFunds() -> "잔액이 부족합니다";
            case CardExpired() -> "카드가 만료되었습니다";
            case NetworkError(var msg) -> "네트워크 오류: " + msg;
            case FraudSuspected() -> "사기 의심으로 거부되었습니다";
        };
    }
}
```

---

### 4.5 상태 전이를 타입으로 강제하기

특정 상태에서만 특정 전이가 가능하도록 타입으로 강제할 수 있습니다.

```java
// 상태별 전용 메서드로 전이 강제
public class OrderStateMachine {

    // Unpaid에서만 결제 가능
    public Paid pay(Order order, PaymentId paymentId) {
        return switch (order.status()) {
            case Unpaid() -> new Paid(LocalDateTime.now(), paymentId);
            default -> throw new IllegalStateException(
                "결제 불가 상태: " + order.status()
            );
        };
    }

    // Paid에서만 배송 가능
    public Shipped ship(Order order, TrackingNumber tracking) {
        return switch (order.status()) {
            case Paid(var paidAt, var paymentId) ->
                new Shipped(LocalDateTime.now(), tracking);
            default -> throw new IllegalStateException(
                "배송 불가 상태: " + order.status()
            );
        };
    }

    // Unpaid에서만 취소 가능
    public Canceled cancel(Order order, CancelReason reason) {
        return switch (order.status()) {
            case Unpaid() -> new Canceled(LocalDateTime.now(), reason);
            default -> throw new IllegalStateException(
                "취소 불가 상태: " + order.status()
            );
        };
    }
}
```

#### 더 강력한 버전: 컴파일 타임 강제

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

함수의 시그니처는 "계약"입니다. `User findUser(int id)`는 "id를 주면 User를 반환하겠다"는 약속입니다. 하지만 이 약속은 거짓말일 수 있습니다.

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
> 예상하지 못한 거절을 당하고 다시 대기해야 합니다.
>
> **전체 함수는 친절한 은행 창구입니다.**
>
> 번호표에 미리 안내되어 있습니다:
> "결과: 잔액 / 해지된 계좌 / 권한 없음 / 시스템 오류 중 하나"
> 어떤 경우든 반드시 명확한 결과를 받습니다.

#### 예외의 문제점

```java
// 1. 호출자가 어떤 예외가 발생하는지 알 수 없음
public void processPayment(PaymentInfo info) {
    User user = findUser(info.userId());        // NotFoundException?
    Product product = findProduct(info.productId()); // NotFoundException?
    Money balance = getBalance(user);           // InsufficientFundsException?
    // ...
}

// 2. Checked Exception은 시그니처를 오염시킴
public User findUser(int id) throws NotFoundException,
                                    DatabaseException,
                                    ConnectionTimeoutException {
    // ...
}

// 3. 예외 처리가 흩어짐
try {
    step1();
    step2();
    step3();
} catch (AException e) {
    // A 처리
} catch (BException e) {
    // B 처리
} catch (CException e) {
    // C 처리
}
```

---

### 5.2 전체 함수(Total Function) 만들기

#### 모든 입력에 대해 결과를 반환

전체 함수는 **모든 입력에 대해 반드시 결과를 반환**합니다. 실패도 "결과"입니다.

```java
// 실패를 데이터로 표현
sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}

    // 편의 메서드
    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }
}
```

#### 비유: 택배 배송

> **Result 타입은 택배 배송 결과와 같습니다.**
>
> 택배를 보내면 반드시 결과가 옵니다:
> - **배송 완료**: 수령인 서명, 배송 시각
> - **배송 실패**: 부재중, 주소 불명, 수령 거부
>
> "배송을 시도했는데 아무 연락도 없다"는 없습니다.
> 항상 명확한 결과가 반환됩니다.
>
> 코드에서도 마찬가지입니다:
> - **Success**: 원하는 데이터
> - **Failure**: 실패 사유

#### 전체 함수로 변환

```java
// Before: 부분 함수 (거짓말하는 시그니처)
public User findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        throw new NotFoundException("User not found: " + id);
    }
    return user;
}

// After: 전체 함수 (정직한 시그니처)
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

    // 값 추출 (명시적 처리 강제)
    default S getOrElse(S defaultValue) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> defaultValue;
        };
    }

    default S getOrThrow() {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> throw new RuntimeException(error.toString());
        };
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

    // 에러 변환
    default <E> Result<S, E> mapError(Function<F, E> mapper) {
        return switch (this) {
            case Success(var value) -> Result.success(value);
            case Failure(var error) -> Result.failure(mapper.apply(error));
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

#### 주문 생성 파이프라인

```java
// 에러 타입 정의
sealed interface OrderError {
    record UserNotFound(MemberId id) implements OrderError {}
    record ProductNotFound(ProductId id) implements OrderError {}
    record InsufficientStock(ProductId id, int requested, int available)
        implements OrderError {}
    record InvalidCoupon(CouponCode code, String reason) implements OrderError {}
    record PaymentFailed(String reason) implements OrderError {}
}

// 각 단계를 전체 함수로 구현
public class OrderService {

    public Result<User, OrderError> findUser(MemberId id) {
        return userRepository.find(id)
            .map(Result::<User, OrderError>success)
            .orElse(Result.failure(new OrderError.UserNotFound(id)));
    }

    public Result<Product, OrderError> findProduct(ProductId id) {
        return productRepository.find(id)
            .map(Result::<Product, OrderError>success)
            .orElse(Result.failure(new OrderError.ProductNotFound(id)));
    }

    public Result<Void, OrderError> checkStock(Product product, int quantity) {
        if (product.stock().value() < quantity) {
            return Result.failure(new OrderError.InsufficientStock(
                product.id(),
                quantity,
                product.stock().value()
            ));
        }
        return Result.success(null);
    }

    public Result<Discount, OrderError> validateCoupon(CouponCode code) {
        return couponService.validate(code)
            .mapError(reason -> new OrderError.InvalidCoupon(code, reason));
    }
}
```

#### 파이프라인 조합

```java
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

private Order buildOrder(User user, Product product, int quantity, Discount discount) {
    Money totalPrice = product.price().multiply(quantity);
    Money discountedPrice = discount.applyTo(totalPrice);

    return new Order(
        OrderId.generate(),
        user.id(),
        List.of(new OrderItem(product.id(), Quantity.of(quantity), product.price())),
        discountedPrice,
        new OrderStatus.PendingPayment(LocalDateTime.now())
    );
}
```

#### 에러 처리

```java
public String handleOrderResult(Result<Order, OrderError> result) {
    return result.fold(
        order -> "주문이 생성되었습니다: " + order.id(),
        error -> switch (error) {
            case UserNotFound(var id) ->
                "회원을 찾을 수 없습니다: " + id;
            case ProductNotFound(var id) ->
                "상품을 찾을 수 없습니다: " + id;
            case InsufficientStock(var id, var req, var avail) ->
                "재고 부족: " + id + " (요청: " + req + ", 재고: " + avail + ")";
            case InvalidCoupon(var code, var reason) ->
                "쿠폰 오류 [" + code + "]: " + reason;
            case PaymentFailed(var reason) ->
                "결제 실패: " + reason;
        }
    );
}
```

---

### 5.5 Optional vs Result

| 구분 | Optional<T> | Result<S, F> |
|-----|-------------|--------------|
| 실패 정보 | 없음 (그냥 empty) | 있음 (F 타입) |
| 사용 시점 | 값이 없을 수 있음 | 연산이 실패할 수 있음 |
| 의미 | "값이 있거나 없다" | "성공했거나 실패했다" |
| 예시 | 조회 결과 없음 | 검증 실패, 비즈니스 규칙 위반 |

```java
// Optional: 왜 없는지 알 수 없음
Optional<User> user = repository.find(id);
// user가 empty면... 왜? 삭제됨? 원래 없음? 권한 없음?

// Result: 왜 실패했는지 명확
Result<User, UserError> user = repository.find(id);
// Failure(NotFound), Failure(Suspended), Failure(Deleted) 등 명확
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

// C
Optional<User> findUser(int id) {
    return Optional.ofNullable(db.find(id));
}

// D
int add(int a, int b) {
    return a + b;
}
```

A. A만
B. A와 C
C. A, B, C
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

#### 정의

**결정론적 함수(Deterministic Function)**는:
- 같은 입력에 대해 항상 같은 출력을 반환
- 외부 상태를 읽거나 변경하지 않음
- 부수효과(Side Effect)가 없음

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
>
> DOP는 비즈니스 로직을 "자판기"로 만듭니다.
> 바리스타의 변덕(외부 상태)은 로직 바깥으로 밀어냅니다.

---

### 6.2 순수 함수의 장점

| 장점 | 설명 |
|-----|------|
| 테스트 용이 | Mock 없이 입력만 주면 됨 |
| 캐싱 가능 | 같은 입력 → 같은 출력이므로 결과 저장 가능 |
| 병렬화 안전 | 공유 상태가 없으므로 스레드 안전 |
| 추론 용이 | 코드만 보고 동작 예측 가능 |
| 리팩토링 안전 | 부수효과가 없어 순서 변경 가능 |

#### 테스트 비교

```java
// 비순수 함수 테스트 (Mock 필요)
@Test
void testCalculateDiscount_impure() {
    // Given
    DiscountService discountService = mock(DiscountService.class);
    UserRepository userRepository = mock(UserRepository.class);
    OrderService orderService = new OrderService(discountService, userRepository);

    when(userRepository.find(any())).thenReturn(Optional.of(testUser));
    when(discountService.getRate(any())).thenReturn(0.1);

    // When
    Money result = orderService.calculateDiscount(orderId);

    // Then
    verify(userRepository).find(any());
    verify(discountService).getRate(any());
    assertEquals(Money.krw(9000), result);
}

// 순수 함수 테스트 (Mock 불필요)
@Test
void testCalculateDiscount_pure() {
    // Given
    Money original = Money.krw(10000);
    DiscountRate rate = new DiscountRate(10);

    // When
    Money result = DiscountCalculator.apply(original, rate);

    // Then
    assertEquals(Money.krw(9000), result);
}
```

---

### 6.3 샌드위치 아키텍처

#### 비유: 샌드위치 만들기

> **좋은 코드는 샌드위치 구조입니다.**
>
> ```
> ┌─────────────────────────────────────┐
> │  Top Bun (Impure): 재료 수집        │  ← DB 조회, API 호출
> ├─────────────────────────────────────┤
> │  Meat (Pure): 요리                  │  ← 비즈니스 로직 (순수 함수)
> ├─────────────────────────────────────┤
> │  Bottom Bun (Impure): 서빙          │  ← DB 저장, 알림 발송
> └─────────────────────────────────────┘
> ```
>
> - **Top Bun**: 냉장고에서 재료를 꺼냅니다 (데이터 수집)
> - **Meat**: 재료로 요리합니다 (순수한 비즈니스 로직)
> - **Bottom Bun**: 접시에 담아 서빙합니다 (결과 저장/전달)
>
> 요리(Meat) 과정에서는 절대 냉장고를 열지 않습니다.
> 필요한 재료는 미리 다 꺼내놓아야 합니다.

#### 비유: 공장 생산라인

> **비즈니스 로직은 공장 생산라인과 같습니다.**
>
> **잘못된 생산라인**:
> - 조립 중에 부품이 부족하면 창고에 가서 가져옴
> - 조립 중에 품질 검사 결과를 기다림
> - 조립 중에 주문 정보를 확인함
> → 라인이 자주 멈추고, 예측 불가능함
>
> **좋은 생산라인 (DOP)**:
> - 조립 시작 전에 모든 부품이 준비됨
> - 조립은 오직 부품을 조합하는 것만 함
> - 조립 완료 후에 결과를 검수하고 출하함
> → 라인이 멈추지 않고, 예측 가능함

---

### 6.4 실전 예제: 이커머스 주문 처리

#### Bad: I/O가 로직에 섞여있음

```java
// 안티패턴: 비즈니스 로직 중간에 I/O
public Order processOrder(OrderRequest request) {
    // 1. DB 조회 (Impure)
    User user = userRepository.find(request.userId())
        .orElseThrow();

    // 2. 로직 (Pure?)
    if (user.grade() == Grade.VIP) {
        // 3. 또 DB 조회 (Impure) - 로직 중간에!
        Coupon vipCoupon = couponService.getVipCoupon();
        request = request.withCoupon(vipCoupon);
    }

    // 4. 로직 (Pure?)
    Money total = calculateTotal(request.items());

    // 5. 또 외부 호출 (Impure) - 로직 중간에!
    TaxRate taxRate = taxService.getCurrentRate();
    total = total.applyTax(taxRate);

    // 6. DB 저장 (Impure)
    Order order = new Order(user.id(), request.items(), total);
    return orderRepository.save(order);
}
```

#### Good: 샌드위치 구조

```java
// ========== 순수 함수 (Meat) ==========
public class OrderCalculations {

    // 완전히 순수한 함수들
    public static Money calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.unitPrice().multiply(item.quantity().value()))
            .reduce(Money.zero(), Money::add);
    }

    public static Money applyDiscount(Money subtotal, Optional<Coupon> coupon) {
        return coupon
            .map(c -> c.applyTo(subtotal))
            .orElse(subtotal);
    }

    public static Money applyTax(Money amount, TaxRate taxRate) {
        return amount.multiply(taxRate.multiplier());
    }

    public static Money calculateTotal(
        List<OrderItem> items,
        Optional<Coupon> coupon,
        TaxRate taxRate
    ) {
        Money subtotal = calculateSubtotal(items);
        Money discounted = applyDiscount(subtotal, coupon);
        return applyTax(discounted, taxRate);
    }

    public static Order createOrder(
        CustomerId customerId,
        List<OrderItem> items,
        Money total
    ) {
        return new Order(
            OrderId.generate(),
            customerId,
            List.copyOf(items),
            total,
            new OrderStatus.PendingPayment(LocalDateTime.now())
        );
    }
}

// ========== 오케스트레이션 (Shell) ==========
public class OrderOrchestrator {

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
        Money total = OrderCalculations.calculateTotal(
            request.items(),
            coupon,
            taxRate
        );

        Order order = OrderCalculations.createOrder(
            user.id(),
            request.items(),
            total
        );

        // === Bottom Bun: 부수효과 (Impure) ===
        Order savedOrder = orderRepository.save(order);
        inventoryService.decreaseStock(request.items());
        notificationService.sendOrderConfirmation(savedOrder);

        return Result.success(savedOrder);
    }
}
```

---

### 6.5 테스트 전략

#### 순수 함수 테스트 (단위 테스트)

```java
class OrderCalculationsTest {

    @Test
    void calculateSubtotal_multipleItems() {
        // Given
        List<OrderItem> items = List.of(
            new OrderItem(productId1, Quantity.of(2), Money.krw(10000)),
            new OrderItem(productId2, Quantity.of(1), Money.krw(5000))
        );

        // When
        Money result = OrderCalculations.calculateSubtotal(items);

        // Then
        assertEquals(Money.krw(25000), result);
    }

    @Test
    void applyDiscount_withCoupon() {
        // Given
        Money subtotal = Money.krw(10000);
        Coupon coupon = new PercentageCoupon(10); // 10% 할인

        // When
        Money result = OrderCalculations.applyDiscount(subtotal, Optional.of(coupon));

        // Then
        assertEquals(Money.krw(9000), result);
    }

    @Test
    void calculateTotal_fullPipeline() {
        // Given
        List<OrderItem> items = List.of(
            new OrderItem(productId, Quantity.of(1), Money.krw(10000))
        );
        Optional<Coupon> coupon = Optional.of(new PercentageCoupon(10));
        TaxRate taxRate = new TaxRate(10); // 10% 세금

        // When
        Money result = OrderCalculations.calculateTotal(items, coupon, taxRate);

        // Then
        // 10000 * 0.9 (할인) * 1.1 (세금) = 9900
        assertEquals(Money.krw(9900), result);
    }
}
```

#### 오케스트레이션 테스트 (통합 테스트)

```java
@SpringBootTest
class OrderOrchestratorIntegrationTest {

    @Autowired OrderOrchestrator orchestrator;

    @Test
    void processOrder_success() {
        // Given - 실제 DB에 테스트 데이터 준비
        User user = createTestUser(Grade.VIP);
        Product product = createTestProduct(Money.krw(10000));

        OrderRequest request = new OrderRequest(
            user.id(),
            List.of(new OrderItemRequest(product.id(), 1)),
            null
        );

        // When
        Result<Order, OrderError> result = orchestrator.processOrder(request);

        // Then
        assertTrue(result.isSuccess());
        Order order = result.getOrThrow();
        assertNotNull(order.id());
        assertEquals(user.id(), order.customerId());
    }
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

// D
int addWithTime(int a, int b) {
    return a + b + LocalDateTime.now().getSecond();
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

데이터 모델링에 수학적 속성을 적용하면 분산 시스템에서 강력한 위력을 발휘합니다. 대수적 속성은 "증명된 성질"이므로, 이 성질을 만족하면 특정 동작이 **수학적으로 보장**됩니다.

| 속성 | 정의 | 활용 |
|-----|------|------|
| 결합법칙 | (A ⊕ B) ⊕ C = A ⊕ (B ⊕ C) | 병렬 처리, 분산 계산 |
| 교환법칙 | A ⊕ B = B ⊕ A | 순서 무관한 처리 |
| 멱등성 | f(f(x)) = f(x) | 재시도 안전성 |
| 항등원 | A ⊕ e = A | 빈 값 처리 |

---

### 7.2 결합법칙 (Associativity)

#### 정의

```
(A + B) + C == A + (B + C)
```

순서에 상관없이 묶어서 계산할 수 있다는 속성입니다.

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
> 처리 시간이 획기적으로 줄어듭니다.

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

// 분산 처리 (MapReduce)
// 서버 A: (주문1 + 주문2 + ... + 주문100)
// 서버 B: (주문101 + 주문102 + ... + 주문200)
// ...
// 마지막: 각 서버 결과를 합침
// 결합법칙이 있으므로 결과가 동일함이 보장됨!
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

#### 정의

```
f(f(x)) == f(x)
```

같은 연산을 여러 번 수행해도 결과가 달라지지 않는 성질입니다.

#### 비유: 전등 스위치

> **멱등성은 ON/OFF 스위치가 아닌 "ON 버튼"과 같습니다.**
>
> **토글 스위치 (멱등하지 않음)**:
> - 버튼을 누르면 상태가 바뀜 (ON→OFF, OFF→ON)
> - 두 번 누르면 원래 상태로 돌아감
> - 네트워크 지연으로 "두 번 눌림"이 발생하면 문제!
>
> **ON 버튼 (멱등함)**:
> - 버튼을 누르면 항상 ON 상태
> - 이미 ON이면 아무 변화 없음
> - 몇 번을 눌러도 결과는 항상 ON
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

#### 이커머스 예제: 상태 변경

```java
// 멱등한 주문 상태 변경
sealed interface OrderStatus {
    record Pending() implements OrderStatus {}
    record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {}
    record Shipped(LocalDateTime at, TrackingNumber tracking) implements OrderStatus {}
}

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

#### 정의

```
A ⊕ e = A
e ⊕ A = A
```

연산의 결과가 원래 값 그대로인 특별한 값입니다.

| 연산 | 항등원 |
|-----|-------|
| 덧셈 | 0 |
| 곱셈 | 1 |
| 문자열 연결 | "" (빈 문자열) |
| 리스트 병합 | [] (빈 리스트) |

#### 활용: 빈 값 처리

```java
// Money의 항등원
public record Money(BigDecimal amount, Currency currency) {

    // 항등원: 0원
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

// 항등원이 있으면 reduce가 안전해짐
List<Money> payments = Collections.emptyList();

// 항등원 없이 reduce하면 예외 발생 가능
// payments.stream().reduce(Money::add); // Optional.empty()

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

## Chapter 8: 인터프리터 패턴: 로직을 데이터로

### 학습 목표
1. 인터프리터 패턴의 개념과 필요성을 이해한다
2. 비즈니스 로직을 데이터로 표현하는 방법을 알 수 있다
3. 재귀적 데이터 구조를 설계할 수 있다
4. Rule Engine을 구현할 수 있다
5. 이커머스에서 동적 규칙을 적용하는 실전 사례를 구현할 수 있다

---

### 8.1 왜 로직을 데이터로 만드는가?

비즈니스 규칙이 복잡해질 때, `if-else`를 떡칠하면 여러 문제가 발생합니다:
- 규칙 추가/변경마다 코드 수정 필요
- 규칙 조합이 복잡해지면 이해하기 어려움
- 런타임에 규칙을 변경할 수 없음

**해결책**: 규칙 자체를 **데이터(ADT)**로 표현하고, 별도의 **해석기(Interpreter)**가 실행합니다.

#### 비유: 레시피 북

> **인터프리터 패턴은 레시피 북과 요리사의 관계입니다.**
>
> **if-else 떡칠 (요리사가 모든 레시피를 암기)**:
> - 새 메뉴가 추가되면 요리사 재교육 필요
> - 요리사마다 레시피 해석이 다를 수 있음
> - 레시피를 수정하려면 요리사의 뇌를 수정(...)해야 함
>
> **인터프리터 패턴 (레시피 북 + 요리사)**:
> - 레시피 북(데이터): "토마토 소스 200ml를 넣고 10분간 끓인다"
> - 요리사(해석기): 레시피를 읽고 실행하는 방법을 앎
> - 새 메뉴 추가: 레시피 북에 페이지 추가 (요리사 재교육 불필요)
> - 레시피 수정: 레시피 북만 수정 (요리사는 그대로)

---

### 8.2 Rule Engine 설계

#### 규칙을 데이터로 표현

```java
// 규칙(Rule)을 대수적 데이터 타입으로 표현
sealed interface Rule {
    // 리프 노드 (기본 조건)
    record MinTotal(Money amount) implements Rule {}
    record UserGrade(MemberGrade grade) implements Rule {}
    record ProductCategory(Category category) implements Rule {}
    record DateRange(LocalDate start, LocalDate end) implements Rule {}

    // 복합 노드 (조건 조합)
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule inner) implements Rule {}
}
```

#### 비유: 문법 규칙

> **Rule 타입은 문법 규칙과 같습니다.**
>
> "VIP이면서 주문금액이 5만원 이상이거나, 첫 구매인 경우 무료배송"
>
> 이 규칙을 트리 구조로 표현하면:
> ```
>           Or
>          /  \
>       And    FirstPurchase
>      /   \
>   VIP   MinTotal(50000)
> ```
>
> 각 노드(Rule)는 데이터일 뿐이고,
> 해석기(Interpreter)가 이 트리를 순회하며 평가합니다.

#### 해석기 구현

```java
public class RuleEvaluator {

    public static boolean evaluate(Rule rule, EvaluationContext ctx) {
        return switch (rule) {
            case MinTotal(Money amount) ->
                ctx.orderTotal().isGreaterOrEqual(amount);

            case UserGrade(MemberGrade grade) ->
                ctx.user().grade() == grade;

            case ProductCategory(Category category) ->
                ctx.items().stream()
                    .anyMatch(item -> item.category() == category);

            case DateRange(LocalDate start, LocalDate end) -> {
                LocalDate today = LocalDate.now();
                yield !today.isBefore(start) && !today.isAfter(end);
            }

            // 재귀적 평가
            case And(Rule left, Rule right) ->
                evaluate(left, ctx) && evaluate(right, ctx);

            case Or(Rule left, Rule right) ->
                evaluate(left, ctx) || evaluate(right, ctx);

            case Not(Rule inner) ->
                !evaluate(inner, ctx);
        };
    }
}

// 평가 컨텍스트
public record EvaluationContext(
    User user,
    List<OrderItem> items,
    Money orderTotal
) {}
```

---

### 8.3 이커머스 예제: 프로모션 규칙 엔진

#### 프로모션 규칙 정의

```java
// 프로모션 = 조건(Rule) + 혜택(Benefit)
public record Promotion(
    PromotionId id,
    String name,
    Rule condition,          // 적용 조건
    Benefit benefit,         // 혜택
    int priority             // 우선순위
) {}

// 혜택 타입
sealed interface Benefit {
    record PercentageDiscount(int rate) implements Benefit {}
    record FixedDiscount(Money amount) implements Benefit {}
    record FreeShipping() implements Benefit {}
    record BonusPoints(int points) implements Benefit {}
    record GiftProduct(ProductId productId) implements Benefit {}
}
```

#### 실제 프로모션 예시

```java
public class PromotionFactory {

    // "VIP 회원 전용 10% 할인"
    public static Promotion vipDiscount() {
        return new Promotion(
            PromotionId.of("PROMO-VIP-10"),
            "VIP 전용 10% 할인",
            new Rule.UserGrade(MemberGrade.VIP),
            new Benefit.PercentageDiscount(10),
            1
        );
    }

    // "5만원 이상 구매 시 무료배송"
    public static Promotion freeShippingOver50000() {
        return new Promotion(
            PromotionId.of("PROMO-FREE-SHIP"),
            "5만원 이상 무료배송",
            new Rule.MinTotal(Money.krw(50000)),
            new Benefit.FreeShipping(),
            2
        );
    }

    // "패션 카테고리 VIP 20% 할인 (7월 한정)"
    public static Promotion vipFashionJulyDiscount() {
        Rule condition = new Rule.And(
            new Rule.UserGrade(MemberGrade.VIP),
            new Rule.And(
                new Rule.ProductCategory(Category.FASHION),
                new Rule.DateRange(
                    LocalDate.of(2024, 7, 1),
                    LocalDate.of(2024, 7, 31)
                )
            )
        );

        return new Promotion(
            PromotionId.of("PROMO-VIP-FASHION-JULY"),
            "7월 VIP 패션 20% 할인",
            condition,
            new Benefit.PercentageDiscount(20),
            0  // 가장 높은 우선순위
        );
    }
}
```

#### 프로모션 엔진

```java
public class PromotionEngine {

    private final List<Promotion> promotions;

    public PromotionEngine(List<Promotion> promotions) {
        this.promotions = promotions.stream()
            .sorted(Comparator.comparingInt(Promotion::priority))
            .toList();
    }

    // 적용 가능한 모든 프로모션 찾기
    public List<Promotion> findApplicable(EvaluationContext ctx) {
        return promotions.stream()
            .filter(promo -> RuleEvaluator.evaluate(promo.condition(), ctx))
            .toList();
    }

    // 최우선 프로모션 하나만 적용
    public Optional<Promotion> findBestPromotion(EvaluationContext ctx) {
        return findApplicable(ctx).stream().findFirst();
    }

    // 혜택 적용
    public OrderWithBenefits applyBenefits(
        Order order,
        EvaluationContext ctx
    ) {
        List<Promotion> applicable = findApplicable(ctx);

        Money totalDiscount = Money.zero(order.currency());
        boolean freeShipping = false;
        int bonusPoints = 0;
        List<ProductId> gifts = new ArrayList<>();

        for (Promotion promo : applicable) {
            switch (promo.benefit()) {
                case PercentageDiscount(int rate) -> {
                    Money discount = order.total()
                        .multiply(BigDecimal.valueOf(rate))
                        .divide(BigDecimal.valueOf(100));
                    totalDiscount = totalDiscount.add(discount);
                }
                case FixedDiscount(Money amount) ->
                    totalDiscount = totalDiscount.add(amount);
                case FreeShipping() ->
                    freeShipping = true;
                case BonusPoints(int points) ->
                    bonusPoints += points;
                case GiftProduct(ProductId productId) ->
                    gifts.add(productId);
            }
        }

        return new OrderWithBenefits(
            order,
            totalDiscount,
            freeShipping,
            bonusPoints,
            gifts
        );
    }
}
```

---

### 8.4 동적 규칙 로딩

인터프리터 패턴의 핵심 장점: **규칙을 런타임에 변경 가능**

```java
// DB에서 규칙 로드
public class PromotionRepository {

    public List<Promotion> loadActivePromotions() {
        // DB에서 JSON/YAML 형태로 저장된 규칙을 로드
        List<PromotionEntity> entities = jdbcTemplate.query(
            "SELECT * FROM promotions WHERE is_active = true",
            promotionRowMapper
        );

        return entities.stream()
            .map(this::toPromotion)
            .toList();
    }

    private Promotion toPromotion(PromotionEntity entity) {
        Rule condition = RuleParser.parse(entity.getConditionJson());
        Benefit benefit = BenefitParser.parse(entity.getBenefitJson());

        return new Promotion(
            PromotionId.of(entity.getId()),
            entity.getName(),
            condition,
            benefit,
            entity.getPriority()
        );
    }
}

// JSON → Rule 파싱
public class RuleParser {

    public static Rule parse(String json) {
        JsonNode node = objectMapper.readTree(json);
        return parseNode(node);
    }

    private static Rule parseNode(JsonNode node) {
        String type = node.get("type").asText();

        return switch (type) {
            case "minTotal" -> new Rule.MinTotal(
                Money.krw(node.get("amount").asLong())
            );
            case "userGrade" -> new Rule.UserGrade(
                MemberGrade.valueOf(node.get("grade").asText())
            );
            case "and" -> new Rule.And(
                parseNode(node.get("left")),
                parseNode(node.get("right"))
            );
            case "or" -> new Rule.Or(
                parseNode(node.get("left")),
                parseNode(node.get("right"))
            );
            default -> throw new IllegalArgumentException("Unknown rule: " + type);
        };
    }
}
```

#### 관리자 화면에서 규칙 편집

```json
// DB에 저장된 프로모션 규칙 예시
{
  "id": "PROMO-SUMMER-2024",
  "name": "2024 여름 특가",
  "condition": {
    "type": "and",
    "left": {
      "type": "userGrade",
      "grade": "VIP"
    },
    "right": {
      "type": "dateRange",
      "start": "2024-07-01",
      "end": "2024-08-31"
    }
  },
  "benefit": {
    "type": "percentageDiscount",
    "rate": 15
  },
  "priority": 1
}
```

마케팅 팀이 관리자 화면에서 규칙을 추가/수정하면, **코드 배포 없이** 즉시 적용됩니다.

---

### 퀴즈 Chapter 8

#### Q8.1 [개념 확인] 인터프리터 패턴
인터프리터 패턴의 핵심 아이디어는?

A. 로직을 데이터로 표현하고 별도의 해석기가 실행
B. 모든 로직을 if-else로 구현
C. 상속을 통한 다형성 활용
D. 예외를 통한 흐름 제어

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

우리는 Spring과 Hibernate(JPA)를 사용하는 프로젝트에서 일합니다. JPA Entity는 **Identity(정체성)**이자 **가변 객체**입니다. DOP의 **Value(값)**와는 상극입니다.

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

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItemEntity> items = new ArrayList<>();

    // Setter로 상태 변경 가능
    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }

    // 비즈니스 로직이 Entity에 섞여있음 (안티패턴)
    public void addItem(OrderItemEntity item) {
        items.add(item);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItemEntity::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

#### 비유: 통역사

> **Entity와 Domain Record 사이에는 통역사가 필요합니다.**
>
> **JPA Entity**는 데이터베이스와 대화하는 언어입니다:
> - 가변적 (Hibernate가 상태를 추적해야 함)
> - 지연 로딩, 프록시 등 JPA 전용 기능
> - `@Entity`, `@Column` 등 인프라 관심사가 섞여있음
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

#### 패키지 구조 예시

```
com.ecommerce.order
├── api
│   ├── OrderController.java
│   └── dto
│       ├── CreateOrderRequest.java
│       └── OrderResponse.java
├── application
│   └── OrderOrchestrator.java
├── domain
│   ├── model
│   │   ├── Order.java              (Record)
│   │   ├── OrderItem.java          (Record)
│   │   └── OrderStatus.java        (Sealed Interface)
│   ├── service
│   │   ├── OrderCalculations.java  (순수 함수)
│   │   └── OrderValidations.java   (순수 함수)
│   └── repository
│       └── OrderRepository.java    (Interface)
└── infrastructure
    ├── persistence
    │   ├── OrderEntity.java
    │   ├── OrderItemEntity.java
    │   └── JpaOrderRepository.java (구현체)
    └── mapper
        └── OrderMapper.java
```

---

### 9.3 Entity ↔ Domain Record 변환

#### Mapper 구현

```java
public class OrderMapper {

    // Entity → Domain Record
    public static Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
            .map(OrderMapper::toDomainItem)
            .toList();

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
            items,
            Money.krw(entity.getTotalAmount().longValue()),
            status
        );
    }

    private static OrderItem toDomainItem(OrderItemEntity entity) {
        return new OrderItem(
            new ProductId(entity.getProductId()),
            Quantity.of(entity.getQuantity()),
            Money.krw(entity.getUnitPrice().longValue())
        );
    }

    // Domain Record → Entity (저장용)
    public static void updateEntity(OrderEntity entity, Order domain) {
        // ID는 변경하지 않음 (Identity 유지)
        entity.setTotalAmount(domain.totalAmount().amount());

        switch (domain.status()) {
            case OrderStatus.PendingPayment(var at) -> {
                entity.setStatus(OrderStatusEnum.PENDING);
                entity.setCreatedAt(at);
            }
            case OrderStatus.PaymentComplete(var at, var paymentId) -> {
                entity.setStatus(OrderStatusEnum.PAID);
                entity.setPaidAt(at);
                entity.setPaymentId(paymentId.value());
            }
            case OrderStatus.Shipping(var at, var tracking) -> {
                entity.setStatus(OrderStatusEnum.SHIPPED);
                entity.setShippedAt(at);
                entity.setTrackingNumber(tracking.value());
            }
            case OrderStatus.Delivered(var at) -> {
                entity.setStatus(OrderStatusEnum.DELIVERED);
                entity.setDeliveredAt(at);
            }
            case OrderStatus.Canceled(var at, var reason) -> {
                entity.setStatus(OrderStatusEnum.CANCELED);
                entity.setCanceledAt(at);
                entity.setCancelReason(reason.name());
            }
        }
    }
}
```

#### Repository 구현

```java
// Domain Layer: Interface
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
    List<Order> findByCustomerId(CustomerId customerId);
}

// Infrastructure Layer: JPA 구현체
@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final JpaOrderEntityRepository jpaRepository;

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(Long.parseLong(id.value()))
            .map(OrderMapper::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = jpaRepository.findById(Long.parseLong(order.id().value()))
            .orElseGet(OrderEntity::new);

        OrderMapper.updateEntity(entity, order);

        OrderEntity saved = jpaRepository.save(entity);
        return OrderMapper.toDomain(saved);
    }

    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.value()).stream()
            .map(OrderMapper::toDomain)
            .toList();
    }
}
```

---

### 9.4 Orchestrator 패턴

Application Layer에서 전체 흐름을 조율합니다.

```java
@Service
@RequiredArgsConstructor
public class OrderOrchestrator {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;

    @Transactional
    public Result<Order, OrderError> createOrder(CreateOrderCommand command) {
        // === Top Bun: 데이터 수집 (Impure) ===
        List<Product> products = productRepository.findAllById(command.productIds());

        // === Meat: 비즈니스 로직 (Pure) ===
        Result<List<OrderItem>, OrderError> itemsResult =
            OrderValidations.validateAndCreateItems(command.items(), products);

        if (itemsResult.isFailure()) {
            return Result.failure(itemsResult.getError());
        }

        List<OrderItem> items = itemsResult.getValue();
        Money total = OrderCalculations.calculateTotal(items);

        Order order = new Order(
            OrderId.generate(),
            command.customerId(),
            items,
            total,
            new OrderStatus.PendingPayment(LocalDateTime.now())
        );

        // === Bottom Bun: 부수효과 (Impure) ===
        Order savedOrder = orderRepository.save(order);
        notificationService.sendOrderCreated(savedOrder);

        return Result.success(savedOrder);
    }

    @Transactional
    public Result<Order, OrderError> payOrder(PayOrderCommand command) {
        // Top Bun
        Optional<Order> orderOpt = orderRepository.findById(command.orderId());
        if (orderOpt.isEmpty()) {
            return Result.failure(new OrderError.NotFound(command.orderId()));
        }
        Order order = orderOpt.get();

        // Meat
        Result<Order, OrderError> paymentResult =
            OrderStateMachine.pay(order, command.paymentInfo());

        if (paymentResult.isFailure()) {
            return paymentResult;
        }

        // Bottom Bun
        Order paidOrder = paymentResult.getValue();

        // 실제 결제 처리
        PaymentResult pgResult = paymentGateway.process(command.paymentInfo());
        if (!pgResult.isSuccess()) {
            return Result.failure(new OrderError.PaymentFailed(pgResult.message()));
        }

        Order finalOrder = paidOrder.withStatus(new OrderStatus.PaymentComplete(
            LocalDateTime.now(),
            pgResult.paymentId()
        ));

        Order savedOrder = orderRepository.save(finalOrder);
        notificationService.sendPaymentComplete(savedOrder);

        return Result.success(savedOrder);
    }
}
```

---

### 9.5 점진적 리팩토링 전략

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
    public static Money applyShipping(Money amount, ShippingFee fee) { ... }

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

#### 단계 4: 서비스에서 순수 함수 호출

```java
public class OrderService {
    public Order createOrder(CreateOrderRequest request) {
        // 데이터 수집 (Impure)
        List<Product> products = productRepository.findAll(request.productIds());
        Optional<Discount> discount = discountService.getDiscount(request.couponCode());
        TaxRate taxRate = taxService.getCurrentRate();
        ShippingFee shippingFee = shippingService.calculate(request.address());

        // 비즈니스 로직 (Pure) - 새로 추출한 순수 함수 호출
        List<OrderItem> items = createOrderItems(request.items(), products);
        Money total = PriceCalculations.calculateTotal(
            items, discount, taxRate, shippingFee
        );

        // 저장 (Impure)
        Order order = new Order(..., total, ...);
        return orderRepository.save(order);
    }
}
```

---

### 9.6 주의사항 및 팁

| 상황 | 권장 사항 |
|-----|----------|
| 기존 Entity에 Setter 제거 | 한 번에 다 제거하지 말고, 새 기능부터 Record 사용 |
| 순환 참조 | Domain Record에서는 ID만 참조, 객체 그래프 지양 |
| Lazy Loading | Domain 레이어에서는 이미 로드된 데이터만 사용 |
| 트랜잭션 경계 | Application Layer(Orchestrator)에서 관리 |
| 성능 | 필요한 경우 DTO Projection 사용 |

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

## Appendix A: 흔한 실수와 안티패턴 모음

### A.1 Record에 Setter 추가하기

```java
// 안티패턴: Record에 Setter 메서드 추가
public record Order(OrderId id, Money amount) {
    public void setAmount(Money newAmount) {  // 컴파일 에러!
        this.amount = newAmount;              // final 필드는 변경 불가
    }
}

// 올바른 방법: with 패턴 사용
public record Order(OrderId id, Money amount) {
    public Order withAmount(Money newAmount) {
        return new Order(this.id, newAmount);  // 새 객체 반환
    }
}
```

### A.2 가변 컬렉션 필드

```java
// 안티패턴: List를 그대로 저장
public record Cart(List<CartItem> items) {}

List<CartItem> mutableList = new ArrayList<>();
Cart cart = new Cart(mutableList);
mutableList.add(newItem);  // cart 내부가 변경됨!

// 올바른 방법: 방어적 복사
public record Cart(List<CartItem> items) {
    public Cart {
        items = List.copyOf(items);  // 불변 리스트로 복사
    }
}
```

### A.3 sealed interface에 default 사용

```java
// 안티패턴: sealed interface인데 default 사용
sealed interface Status permits Active, Inactive {}

String getMessage(Status status) {
    return switch (status) {
        case Active a -> "활성";
        default -> "비활성";     // 새 상태 추가 시 누락 감지 안 됨!
    };
}

// 올바른 방법: 모든 case 명시
String getMessage(Status status) {
    return switch (status) {
        case Active a -> "활성";
        case Inactive i -> "비활성";  // 새 상태 추가 시 컴파일 에러
    };
}
```

### A.4 Entity에 비즈니스 로직 추가

```java
// 안티패턴: Entity에 비즈니스 로직
@Entity
public class OrderEntity {
    public void applyDiscount(double rate) {
        this.totalAmount = this.totalAmount.multiply(1 - rate);
    }
}

// 올바른 방법: 순수 함수로 분리
public class OrderCalculations {
    public static Money applyDiscount(Money amount, double rate) {
        return amount.multiply(BigDecimal.valueOf(1 - rate));
    }
}
```

### A.5 Optional을 필드로 사용

```java
// 안티패턴: Record 필드에 Optional 사용
public record Order(
    OrderId id,
    Optional<CouponCode> couponCode  // 직렬화 문제, null 문제
) {}

// 올바른 방법: 합 타입 사용
sealed interface AppliedCoupon {
    record None() implements AppliedCoupon {}
    record Applied(CouponCode code, Money discount) implements AppliedCoupon {}
}

public record Order(OrderId id, AppliedCoupon coupon) {}
```

---

## Appendix B: DOP Java 치트시트

### B.1 Record 기본 문법

```java
// 기본 Record
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

// with 패턴
public record Order(OrderId id, OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, newStatus);
    }
}
```

### B.2 Sealed Interface 문법

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

### B.3 Pattern Matching

```java
// switch expression
String describe(Shape shape) {
    return switch (shape) {
        case Circle(var r) -> "원 (반지름: " + r + ")";
        case Rectangle(var w, var h) -> "사각형 (" + w + "x" + h + ")";
        case Triangle(var b, var h) -> "삼각형 (밑변: " + b + ")";
    };
}

// 가드 조건
String categorize(Shape shape) {
    return switch (shape) {
        case Circle(var r) when r > 10 -> "큰 원";
        case Circle(var r) -> "작은 원";
        case Rectangle r -> "사각형";
        case Triangle t -> "삼각형";
    };
}

// instanceof 패턴
if (status instanceof Completed(var txId)) {
    System.out.println("완료: " + txId);
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

### B.5 불변 컬렉션

```java
// 불변 리스트 생성
List<String> immutable = List.of("a", "b", "c");

// 기존 리스트를 불변으로 복사
List<String> copied = List.copyOf(mutableList);

// 불변 맵
Map<String, Integer> map = Map.of("a", 1, "b", 2);

// 불변 Set
Set<String> set = Set.of("x", "y", "z");
```

---

## Appendix C: 전체 정답

| Ch | Q1 | Q2 | Q3 | Q4 | Q5 |
|----|----|----|----|----|-----|
| 1  | C  | B  | B  | C  | 코드 |
| 2  | B  | C  | B  | B  | 코드 |
| 3  | B  | C  | A  | C  | 코드 |
| 4  | B  | B  | A  | A  | 코드 |
| 5  | B  | A  | A  | B  | 코드 |
| 6  | A  | C  | B  | B  | 코드 |
| 7  | C  | C  | B  | B  | 코드 |
| 8  | A  | A  | A  | B  | 코드 |
| 9  | B  | B  | C  | B  | 코드 |

### Chapter 1 해설

**Q1.1**: C - "상속을 통해 재사용성을 높여라"는 OOP 원칙이지 DOP 원칙이 아닙니다.

**Q1.2**: B - id, name, price 필드가 함께 있는 것은 정상적인 도메인 모델링입니다.

**Q1.3**: B - Product를 Record로 바꾸고 기능별로 서비스 클래스를 분리하는 것이 DOP 방식입니다.

**Q1.4**: C - 불변 객체는 복사 시 메모리를 사용하므로 메모리 사용량이 늘어날 수 있습니다.

**Q1.5 코드**:
```java
public record CartItem(ProductId productId, int quantity, Money price) {}
public record ShoppingCart(List<CartItem> items) {
    public ShoppingCart { items = List.copyOf(items); }
}

public class CartOperations {
    public static ShoppingCart addItem(ShoppingCart cart, CartItem item) {
        List<CartItem> newItems = new ArrayList<>(cart.items());
        newItems.add(item);
        return new ShoppingCart(newItems);
    }
    public static Money getTotal(ShoppingCart cart) {
        return cart.items().stream()
            .map(i -> i.price().multiply(i.quantity()))
            .reduce(Money.zero(), Money::add);
    }
}
```

### Chapter 2 해설

**Q2.1**: B - 주문 금액은 Value Type입니다. 10000원은 어디에서든 10000원입니다.

**Q2.2**: C - Line A는 불변 리스트에 add 시도 (UOE), Line B는 가변 리스트를 넘겨서 외부 변경 가능.

**Q2.3**: B - `unmodifiableList`는 원본 리스트의 뷰를 반환하므로 원본 변경 시 같이 변경됩니다.

**Q2.4**: B - 주소는 내용이 같으면 같은 주소이므로 Value Object입니다.

**Q2.5 코드**:
```java
public record Product(ProductId id, ProductName name, Money price, StockQuantity stock) {
    public Product withPrice(Money newPrice) {
        return new Product(this.id, this.name, newPrice, this.stock);
    }
}
```

### Chapter 3 해설

**Q3.1**: B - Product Type은 곱 타입이므로 4 × 3 = 12가지입니다.

**Q3.2**: C - Sealed interface는 합 타입이므로 |String| + |String| + |int|입니다.

**Q3.3**: A - 2^4 = 16가지 상태가 가능하지만, Active/Paused/Canceled/Expired 4가지만 유효합니다.

**Q3.4**: C - 상태별 추가 데이터가 없으면 enum, 있으면 sealed interface 모두 가능합니다.

**Q3.5 코드**:
```java
sealed interface PaymentStatus {
    record Pending() implements PaymentStatus {}
    record Processing(LocalDateTime startedAt) implements PaymentStatus {}
    record Completed(LocalDateTime at, PaymentId id) implements PaymentStatus {}
    record Failed(LocalDateTime at, String reason) implements PaymentStatus {}
    record Cancelled(LocalDateTime at, Money refundAmount) implements PaymentStatus {}
}
```

### Chapter 4-9 해설

(지면 관계상 핵심 답변만 제공)

**Q4.1**: B - 유효하지 않은 상태를 타입으로 표현할 수 없게 설계하는 것입니다.

**Q5.1**: B - 모든 입력에 대해 정의된 결과를 반환합니다.

**Q6.1**: A - 오직 A만 순수 함수입니다 (부수효과 없음, 외부 상태 참조 없음).

**Q7.2**: C - Math.abs는 멱등합니다. abs(abs(x)) == abs(x)

**Q8.2**: A - "NOT VIP AND MinTotal(30000)"

**Q9.3**: C - 비즈니스 로직은 Domain Layer의 순수 함수에 있어야 합니다.

---

**[강의 마무리]**

DOP는 새로운 언어를 배우는 것이 아닙니다. Java가 가진 강력한 타입 시스템을 최대한 활용하여, **우리의 머리로는 감당할 수 없는 복잡성을 컴파일러에게 외주 주는 기술**입니다.

핵심 정리:
1. **데이터와 로직을 분리**하라 (Record + Static 함수)
2. **불변성**을 유지하라 (with 패턴, 방어적 복사)
3. **합 타입**으로 상태 수를 줄여라 (Sealed Interface)
4. **전체 함수**로 실패를 명시하라 (Result 타입)
5. **샌드위치 아키텍처**로 I/O를 격리하라

이제 여러분은 데이터의 본질을 꿰뚫어 보고, 불가능한 상태가 존재하지 않는 견고한 시스템을 설계할 준비가 되었습니다.
