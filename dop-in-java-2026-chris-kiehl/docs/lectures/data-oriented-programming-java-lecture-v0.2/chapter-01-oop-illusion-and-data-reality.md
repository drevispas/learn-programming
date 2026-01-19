# Chapter 1: 객체 지향의 환상과 데이터의 실체

## 학습 목표
1. OOP의 캡슐화가 대규모 시스템에서 야기하는 문제를 설명할 수 있다
2. DOP의 4대 원칙을 이해하고 각각의 의미를 설명할 수 있다
3. 데이터와 로직 분리의 장점을 코드로 보여줄 수 있다
4. God Class 안티패턴을 인식하고 리팩토링 방향을 제시할 수 있다
5. DOP와 OOP의 차이점을 표로 정리할 수 있다

---

## 1.1 왜 우리는 고통스러운가?

### 캡슐화의 약속과 현실

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

| 문제 유형 | 증상 | 결과 |
|----------|-----|------|
| 정보의 감옥 | 데이터가 객체 안에 갇힘 | DTO 변환 코드가 전체의 50% |
| 맥락의 혼재 | 하나의 클래스가 여러 역할 수행 | 수정 시 예상치 못한 영역 파괴 |
| 테스트 지옥 | 하나를 테스트하려면 전체 의존성 필요 | Mock 객체 20개 이상 필요 |
| 병합 충돌 | 모든 팀이 같은 파일 수정 | Git 충돌이 일상 |

---

## 1.2 DOP의 4대 원칙

Chris Kiehl은 이러한 문제를 해결하기 위해 4가지 원칙을 제시합니다.

### 원칙 1: 코드와 데이터를 분리하라

#### Before (OOP): 데이터와 로직이 섞여있음

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

#### After (DOP): 데이터와 로직을 분리

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

### 원칙 2: 데이터를 일반적인 형태로 표현하라

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

### 원칙 3: 데이터는 불변(Immutable)이다

#### Before: 가변 객체

```java
public class MutableOrder {
    private String status;

    public void setStatus(String status) {
        this.status = status;  // 언제 어디서 바뀔지 모름
    }
}
```

#### After: 불변 객체

```java
public record Order(OrderId id, Money amount, OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, this.amount, newStatus);  // 새 객체 반환
    }
}
```

### 원칙 4: 스키마와 표현을 분리하라

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

## 1.3 DOP vs OOP 비교

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

## 1.4 이커머스 리팩토링 예시: Before/After

### Before: God Class

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

### After: DOP 스타일

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

## 퀴즈 Chapter 1

### Q1.1 [개념 확인] DOP의 4대 원칙
다음 중 DOP의 원칙이 **아닌** 것은?

A. 코드와 데이터를 분리하라
B. 데이터는 불변이어야 한다
C. 상속을 통해 재사용성을 높여라
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

A. 재고 관리, 가격 관리, 알림, 검색 동기화가 한 클래스에 있음
B. id, name, price 필드가 함께 정의되어 있음
C. 세금 계산과 바코드 생성이 Product에 있음
D. 외부 시스템(검색엔진)과의 통신이 도메인 객체에 있음

---

### Q1.3 [설계 문제] 리팩토링 방향
위의 `Product` 클래스를 DOP 스타일로 리팩토링할 때 가장 적절한 접근은?

A. 모든 메서드를 private으로 바꿔서 캡슐화를 강화한다
B. Product를 Record로 바꾸고, 각 기능을 별도 서비스 클래스로 분리한다
C. ProductService를 만들어 모든 로직을 옮긴다
D. 각 메서드마다 별도의 Product 하위 클래스를 만든다

---

### Q1.4 [개념 확인] 불변성의 장점
데이터 불변성이 제공하는 장점으로 **올바르지 않은** 것은?

A. 멀티스레드 환경에서 동기화 없이 안전하게 공유할 수 있다
B. 객체의 상태가 예측 가능하여 디버깅이 쉽다
C. 메모리 사용량이 줄어든다
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
