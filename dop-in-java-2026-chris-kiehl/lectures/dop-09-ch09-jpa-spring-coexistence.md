# Chapter 9: 현실 세계의 DOP - JPA/Spring과의 공존 (Real-World DOP: Coexistence with JPA/Spring)

## 학습 목표 (Learning Objectives)
1. JPA Entity와 DOP Domain Record의 차이를 이해한다
2. Entity에서 Domain Record로의 변환 전략을 적용할 수 있다
3. Spring Boot 프로젝트에서 DOP를 적용하는 구조를 설계할 수 있다
4. 점진적 리팩토링 전략을 수립할 수 있다
5. 실제 프로젝트에서 DOP 도입 시 고려사항을 파악할 수 있다

---

## 9.1 JPA Entity의 한계 (Limitations of JPA Entity)

> **다른 말로 (In other words):**
> - "JPA Entity는 가변(Mutable) + Identity 기반, DOP Record는 불변(Immutable) + Value 기반"
> - "두 세계 사이에는 Mapper(통역사)가 필요"

> **🎯 왜 배우는가?**
>
> "JPA를 쓰면서 DOP를 적용할 수 있나요?"라는 의문이 드시나요?
> JPA Entity의 특성과 한계를 이해하면 **현실적인 공존 전략**을 세울 수 있고,
> 레거시 프로젝트에서도 점진적으로 DOP를 도입할 수 있습니다.

JPA Entity는 **Identity(정체성)**이자 **가변 객체**입니다. DOP의 **Value(값)**와는 상극입니다.

**Code 9.1**: JPA Entity - 가변, Identity 기반
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

### 비유: 통역사

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

## 9.2 레이어 분리 전략 (Layer Separation Strategy)

> **다른 말로 (In other words):**
> - "인프라(Entity) → 도메인(Record) → 표현(DTO) 각 레이어의 책임 분리"
> - "도메인 레이어는 JPA/HTTP를 모른다 - 순수 비즈니스 로직만"

> **🎯 왜 배우는가?**
>
> Entity와 Domain Record를 어느 레이어에서 변환해야 할지 고민되시나요?
> 레이어 분리 전략을 통해 **각 레이어의 책임을 명확히 하고**,
> 인프라 관심사가 도메인을 오염시키지 않는 구조를 설계할 수 있습니다.

### Entity ↔ Domain Record 데이터 흐름

```
┌───────────────────────────────────────────────────────────────────────┐
│                 Entity <-> Domain Record Conversion Flow              │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────┐                                          ┌──────────┐   │
│  │ Database │                                          │   API    │   │
│  └────┬─────┘                                          └────┬─────┘   │
│       │                                                     │         │
│       v (SQL)                                               v (JSON)  │
│  ┌──────────────┐                                  ┌──────────────┐   │
│  │ JPA Entity   │                                  │     DTO      │   │
│  │(mutable,ORM) │                                  │(serializable)│   │
│  └──────┬───────┘                                  └──────┬───────┘   │
│         │                                                 │           │
│         v Mapper.toDomain()                 Mapper.toDto()v           │
│         │                                                 │           │
│  ┌──────┴─────────────────────────────────────────────────┴───────┐   │
│  │                   Domain Record (immutable)                    │   │
│  │                                                                │   │
│  │  ┌──────────────────────────────────────────────────────────┐  │   │
│  │  │                  Pure Business Logic                     │  │   │
│  │  │            (OrderCalculations, Validations)              │  │   │
│  │  └──────────────────────────────────────────────────────────┘  │   │
│  │                                                                │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  Key: Domain layer knows nothing about JPA/HTTP! Pure business only!  │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

**Figure 9.1**: Layer Separation Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    Controller / API Layer                   │
│                (DTO <-> Domain Record conversion)           │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│                (Orchestration, flow control)                │
├─────────────────────────────────────────────────────────────┤
│                    Domain Layer (DOP)                       │
│          (Record, Sealed Interface, pure functions)         │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│             (JPA Entity, Repository impl)                   │
│              (Entity <-> Domain Record conversion)          │
└─────────────────────────────────────────────────────────────┘
```

> **Visual Reference - Layered Architecture:**
> ![Clean Architecture Layers](https://learn.microsoft.com/en-us/azure/architecture/guide/architecture-styles/images/n-tier-logical.svg)
> *Source: [Microsoft Azure - N-tier Architecture](https://learn.microsoft.com/en-us/azure/architecture/guide/architecture-styles/n-tier)*
>
> For hexagonal approach: [8th Light - Ports and Adapters](https://8thlight.com/insights/a-color-coded-guide-to-ports-and-adapters)

---

## 9.3 Entity ↔ Domain Record 변환 (Entity ↔ Domain Record Conversion)

> **🎯 왜 배우는가?**
>
> Entity를 직접 비즈니스 로직에서 사용하다가 Lazy Loading 예외를 만난 적 있으신가요?
> Mapper를 통한 변환 전략을 익히면 **인프라 레이어의 복잡성을 도메인으로부터 격리**하고,
> 순수한 도메인 로직을 유지할 수 있습니다.

### Before: 직접 Entity 사용

**Code 9.2**: Before - 직접 Entity 사용 (안티패턴)
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

### After: Mapper 활용

**Code 9.3**: After - Mapper 활용
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

## 9.4 점진적 리팩토링 전략 (Gradual Refactoring Strategy)

> **🎯 왜 배우는가?**
>
> "레거시 코드를 한 번에 다 바꿀 수 없어요"라는 현실적인 제약이 있으신가요?
> 점진적 리팩토링 전략을 통해 **리스크를 최소화하면서 단계적으로 DOP를 도입**하고,
> 빅뱅 배포 없이 품질을 개선할 수 있습니다.

### 단계 1: 가장 복잡한 로직 선정

**Code 9.4**: Before - 복잡한 로직이 Service에 섞여있음
```java
// Before: 복잡한 가격 계산 로직이 Service에 섞여있음
public class OrderService {
    public Order createOrder(CreateOrderRequest request) {
        // 100줄의 복잡한 로직...
        // 할인, 세금, 배송비, 포인트 적용 등
    }
}
```

### 단계 2: 순수 함수 추출

**Code 9.5**: After - 순수 함수로 추출
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

### 단계 3: 테스트 작성 (Mocking 없이!)

**Code 9.6**: 테스트 작성 - Mocking 없이
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

## 9.5 주의사항 및 팁 (Cautions and Tips)

> **🎯 왜 배우는가?**
>
> DOP 도입 시 흔히 저지르는 실수를 미리 알고 싶으신가요?
> 주의사항과 팁을 숙지하면 **시행착오를 줄이고 효과적으로 DOP를 적용**할 수 있습니다.

**Table 9.1**: JPA/DOP 공존 시 주의사항

| 상황 | 권장 사항 |
|-----|----------|
| 기존 Entity에 Setter 제거 | 한 번에 다 제거하지 말고, 새 기능부터 Record 사용 |
| 순환 참조 | Domain Record에서는 ID만 참조, 객체 그래프 지양 |
| Lazy Loading | Domain 레이어에서는 이미 로드된 데이터만 사용 |
| 트랜잭션 경계 | Application Layer(Orchestrator)에서 관리 |

**Code 9.7**: 권장 - ID 참조 vs 비권장 - 객체 그래프
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

## 퀴즈 Chapter 9 (Quiz Chapter 9)

### Q9.1 [개념 확인] Entity vs Record
JPA Entity와 Domain Record의 차이로 **올바른** 것은?

A. Entity는 불변, Record는 가변<br/>
B. Entity는 가변이고 Identity 기반, Record는 불변이고 Value 기반<br/>
C. 둘 다 불변이지만 용도가 다름<br/>
D. Entity에서는 비즈니스 로직을 넣어야 함

---

### Q9.2 [코드 분석] Mapper의 역할
Mapper 클래스의 역할은?

A. Entity에 비즈니스 로직 추가<br/>
B. Entity와 Domain Record 간의 변환<br/>
C. 데이터베이스 쿼리 최적화<br/>
D. 트랜잭션 관리

---

### Q9.3 [설계 문제] 레이어 배치
"주문 금액 계산" 로직은 어느 레이어에 있어야 하나요?

A. Controller Layer<br/>
B. Infrastructure Layer (Entity)<br/>
C. Domain Layer (순수 함수)<br/>
D. Application Layer

---

### Q9.4 [코드 분석] 점진적 리팩토링
레거시 코드 리팩토링 시 첫 단계로 적절한 것은?

A. 모든 Entity를 Record로 한 번에 변환<br/>
B. 가장 복잡한 비즈니스 로직을 순수 함수로 추출<br/>
C. JPA를 완전히 제거<br/>
D. 새 프로젝트로 처음부터 다시 작성

---

### Q9.5 [코드 작성] Mapper 구현
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
