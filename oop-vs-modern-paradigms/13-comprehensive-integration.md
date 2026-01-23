# 13. Comprehensive Integration (종합 통합)

> **Sources**: DMMF Ch.10 (Comprehensive Project), DOP Ch.9 (JPA/Spring Coexistence)

---

## 1. Full Domain Model with ADT + Result + Pipeline (ADT + Result + 파이프라인 종합)

### 핵심 개념
- **관련 키워드**: ADT, Sum Type, Product Type, Result, Pipeline, State Machine, Domain Model, Bounded Context
- **통찰**: sealed interface(Sum Type) + record(Product Type) + Result + Pipeline을 종합하면, 도메인의 모든 상태를 타입으로 표현하고 워크플로우를 안전하게 구성할 수 있다.
- **설명**: 지금까지 학습한 개별 패턴들 -- Value Object, ADT, 불가능한 상태 제거, Total Function, Result, Pipeline, 대수적 속성, 인터프리터 패턴 -- 은 독립적으로도 유용하지만, 종합 적용했을 때 진정한 가치가 드러난다.

  이커머스 도메인의 전체 워크플로우(회원가입 -> 상품 조회 -> 장바구니 -> 주문 -> 결제 -> 배송)를 타입 안전하게 모델링하면:
  - 각 Bounded Context(회원, 상품, 주문, 결제, 쿠폰)별 독립적인 모델
  - 상태 전이를 sealed interface로 강제
  - 모든 실패 가능성을 Result로 명시
  - 워크플로우를 flatMap 파이프라인으로 구성

```
FULL DOMAIN MODEL ARCHITECTURE
=================================

Bounded Contexts:
+--------+  +--------+  +--------+  +---------+  +--------+
| Member |  |Product |  | Order  |  | Payment |  | Coupon |
|--------|  |--------|  |--------|  |---------|  |--------|
| Grade  |  |Display |  | Status |  | Method  |  | Type   |
| Email  |  |Invent. |  | Lines  |  | Result  |  | Status |
| Points |  |Settle. |  | Error  |  | Error   |  | Error  |
+--------+  +--------+  +--------+  +---------+  +--------+
     |          |            |           |            |
     +----------+-----+------+-----------+------------+
                       |
                       v
              +------------------+
              | Order Workflow   |
              | (Pipeline)       |
              | validate         |
              |   -> price       |
              |   -> coupon      |
              |   -> payment     |
              |   -> confirm     |
              +------------------+
```

### 개념이 아닌 것
- **"모든 것을 한 번에 구현"**: 가장 핵심적인 워크플로우부터 시작하여 점진적으로 확장
- **"Bounded Context = 마이크로서비스"**: 모놀리스 내에서도 패키지로 Context를 분리할 수 있음

### Before: Traditional OOP
```java
// [X] 신(God) 서비스 - 모든 도메인 로직이 한 곳에 결합
public class OrderService {
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 회원 검증
        User user = userRepository.findById(request.getUserId());
        if (user == null) throw new RuntimeException("회원 없음");
        if (!user.isEmailVerified()) throw new RuntimeException("이메일 미인증");

        // 상품 재고 확인
        for (ItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId());
            if (product == null) throw new RuntimeException("상품 없음");
            if (product.getStock() < item.getQuantity())
                throw new RuntimeException("재고 부족");
        }

        // 쿠폰 적용
        BigDecimal total = calculateTotal(request.getItems());
        if (request.getCouponCode() != null) {
            Coupon coupon = couponRepository.findByCode(request.getCouponCode());
            if (coupon == null) throw new RuntimeException("쿠폰 없음");
            if (coupon.isExpired()) throw new RuntimeException("쿠폰 만료");
            total = applyCoupon(total, coupon);
        }

        // 결제
        PaymentResult payment = paymentGateway.charge(user, total);
        if (!payment.isSuccess()) throw new RuntimeException("결제 실패");

        // 주문 생성
        Order order = new Order();
        order.setUserId(user.getId());
        order.setTotal(total);
        order.setStatus("PAID");
        return orderRepository.save(order);
    }
}
```
- **의도 및 코드 설명**: 단일 서비스 메서드에서 회원/상품/쿠폰/결제/주문 모든 도메인 처리. 모든 에러는 RuntimeException
- **뭐가 문제인가**:
  - 단일 메서드 100+ 줄 (읽기/유지보수 곤란)
  - 에러 종류를 호출자가 알 수 없음 (모두 RuntimeException)
  - 도메인 경계 없음 (회원/상품/결제 로직 혼재)
  - 상태 전이 규칙이 문자열("PAID")에 의존
  - 테스트 시 전체 의존성 Mock 필요

### After: Modern Approach
```java
// [O] 종합 통합: ADT + Result + Pipeline
// 1. 각 Bounded Context별 독립 모델
public sealed interface OrderStatus {
    record Unpaid(LocalDateTime deadline) implements OrderStatus {}
    record Paid(LocalDateTime paidAt, TransactionId txId) implements OrderStatus {}
    record Shipping(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
    record Cancelled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
}

public sealed interface OrderError permits
    EmptyOrder, InvalidCustomer, OutOfStock, PaymentFailed, InvalidCoupon {
    record EmptyOrder() implements OrderError {}
    record InvalidCustomer(String reason) implements OrderError {}
    record OutOfStock(ProductId id, int requested, int available) implements OrderError {}
    record PaymentFailed(String reason) implements OrderError {}
    record InvalidCoupon(String code, String reason) implements OrderError {}
}

// 2. 순수 함수 도메인 서비스 (Functional Core)
public class OrderDomainService {
    public static Result<ValidatedOrder, OrderError> validate(
            CreateOrderCommand cmd, Member member, List<InventoryProduct> inventory) {
        if (!member.isEmailVerified())
            return Result.failure(new InvalidCustomer("이메일 미인증"));
        if (cmd.items().isEmpty())
            return Result.failure(new EmptyOrder());
        // 재고 확인
        for (var item : cmd.items()) {
            var product = findProduct(inventory, item.productId());
            if (!product.isAvailable(item.quantity()))
                return Result.failure(new OutOfStock(
                    item.productId(), item.quantity(), product.stock().value()));
        }
        return Result.success(new ValidatedOrder(cmd, member));
    }

    public static Result<PricedOrder, OrderError> applyPricing(
            ValidatedOrder order, Optional<Coupon> coupon) {
        Money subtotal = calculateSubtotal(order.items());
        Money discount = coupon
            .filter(Coupon::isAvailable)
            .map(c -> c.type().calculateDiscount(subtotal))
            .orElse(Money.zero(Currency.KRW));
        return Result.success(new PricedOrder(order, subtotal.subtract(discount)));
    }
}

// 3. 워크플로우 파이프라인 (Imperative Shell)
public class PlaceOrderUseCase {
    public Result<Order, OrderError> execute(CreateOrderCommand cmd) {
        Member member = memberRepository.findById(cmd.memberId()).orElseThrow();
        List<InventoryProduct> inventory = inventoryRepository.findAll(cmd.productIds());
        Optional<Coupon> coupon = cmd.couponCode()
            .flatMap(couponRepository::findByCode);

        return OrderDomainService.validate(cmd, member, inventory)
            .flatMap(validated -> OrderDomainService.applyPricing(validated, coupon))
            .flatMap(priced -> processPayment(priced))
            .map(paid -> {
                Order order = createOrder(paid);
                orderRepository.save(order);
                return order;
            });
    }
}
```
- **의도 및 코드 설명**: 도메인 모델(ADT), 순수 함수(Functional Core), 워크플로우(Pipeline)를 종합하여 타입 안전하고 테스트 용이한 구조
- **무엇이 좋아지나**:
  - 모든 에러가 타입으로 명시 (sealed interface)
  - 상태 전이가 컴파일 타임에 검증
  - 순수 함수는 Mock 없이 테스트
  - 각 단계가 독립적으로 이해/수정 가능
  - flatMap 파이프라인으로 워크플로우 흐름 명확

### 틀리기/놓치기 쉬운 부분
- Imperative Shell(UseCase)에서만 Repository를 호출. Functional Core(DomainService)는 데이터를 파라미터로 받음
- flatMap 체인에서 첫 Failure가 발생하면 이후 단계는 실행되지 않음 (Railway-Oriented)
- Bounded Context 간 통신은 ID 참조 (Customer 객체가 아닌 CustomerId)

### 꼭 기억할 것
- ADT(sealed interface) = 가능한 상태만 표현
- Result = 실패 가능성 명시
- Pipeline(flatMap) = 워크플로우 구성
- Functional Core / Imperative Shell = 관심사 분리

---

## 2. JPA Entity vs Domain Record Mapping (JPA Entity와 도메인 Record 매핑)

### 핵심 개념
- **관련 키워드**: JPA Entity, Domain Record, Mapper, Persistence Layer, Anti-Corruption Layer, Value vs Identity
- **통찰**: JPA Entity(가변, Identity 기반)와 Domain Record(불변, Value 기반)는 본질적으로 다르므로, Mapper가 두 세계를 연결하는 통역사 역할을 한다.
- **설명**: JPA Entity는 영속성 프레임워크의 요구사항(기본 생성자, Setter, 가변 상태, 프록시)을 충족하기 위한 객체이다. 반면 Domain Record는 비즈니스 로직을 위한 불변 값 객체이다. 이 둘을 하나의 클래스로 통합하려는 시도는 양쪽 요구사항이 충돌하여 실패한다.

  해결책은 **Mapper(통역사)**를 두어 Infrastructure Layer(Entity)와 Domain Layer(Record) 사이를 변환하는 것이다. 이는 DDD의 Anti-Corruption Layer(ACL) 개념과 일치한다.

  핵심 원칙: "Entity는 DB와 대화하는 언어, Record는 비즈니스 로직의 언어, Mapper는 통역사"

```
ENTITY vs DOMAIN RECORD
==========================

JPA Entity (Infrastructure):    Domain Record (Core):
+-------------------+           +-------------------+
| @Entity           |           | record Order(     |
| class OrderEntity |           |   OrderId id,     |
| {                 |           |   List<Item> items,|
|   Long id; (mut)  |  Mapper   |   OrderStatus stat|
|   String status;  | <-------> | ) {}              |
|   BigDecimal amt; |           |                   |
|   // setters...   |           | // immutable!     |
| }                 |           | // no setters!    |
+-------------------+           +-------------------+
       |                                |
       v                                v
  Hibernate                      Business Logic
  Dirty Checking                 Pattern Matching
  Lazy Loading                   Pure Functions
```

### 개념이 아닌 것
- **"Entity를 직접 도메인에서 사용"**: Entity의 Lazy Loading, 프록시 등이 도메인 로직을 오염시킴
- **"Record를 JPA Entity로 사용"**: Record는 기본 생성자, Setter가 없어 JPA 요구사항 불충족

### Before: Traditional OOP
```java
// [X] Entity를 도메인 로직에서 직접 사용
@Entity
public class OrderEntity {
    @Id @GeneratedValue private Long id;
    @Enumerated(EnumType.STRING) private OrderStatusEnum status;
    @Column private BigDecimal totalAmount;

    // JPA 필수: 기본 생성자
    protected OrderEntity() {}

    // 비즈니스 로직이 Entity에 혼재 (안티패턴)
    public void markAsPaid(String paymentId) {
        this.status = OrderStatusEnum.PAID;
        this.paymentId = paymentId;
        this.paidAt = LocalDateTime.now();
    }
}

public class OrderService {
    @Transactional
    public void processOrder(Long orderId) {
        OrderEntity entity = orderRepository.findById(orderId).orElseThrow();
        entity.markAsPaid(paymentId); // Entity 직접 조작
        // JPA Dirty Checking이 자동 저장
    }
}
```
- **의도 및 코드 설명**: JPA Entity에 비즈니스 로직을 추가하고, 서비스에서 Entity를 직접 조작
- **뭐가 문제인가**:
  - 비즈니스 로직이 인프라 계층(Entity)에 종속
  - Lazy Loading 예외가 도메인 로직에서 발생 가능
  - 가변 상태로 인한 버그 추적 어려움
  - Entity 변경 시 도메인 로직에 영향
  - 순수 함수 테스트 불가 (JPA 컨텍스트 필요)

### After: Modern Approach
```java
// [O] Mapper로 Entity와 Domain Record 분리
// 1. JPA Entity (Infrastructure 전용)
@Entity @Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue private Long id;
    @Enumerated(EnumType.STRING) private OrderStatusEnum status;
    private BigDecimal totalAmount;
    private LocalDateTime paidAt;
    private String paymentId;
    // JPA 전용: getters, setters, 기본 생성자
}

// 2. Domain Record (비즈니스 로직 전용)
public record Order(OrderId id, List<OrderItem> items, Money total, OrderStatus status) {
    public Order { items = List.copyOf(items); }
}

// 3. Mapper (통역사)
public class OrderMapper {
    public static Order toDomain(OrderEntity entity) {
        OrderStatus status = switch (entity.getStatus()) {
            case PENDING -> new OrderStatus.Unpaid(entity.getCreatedAt());
            case PAID -> new OrderStatus.Paid(entity.getPaidAt(),
                new TransactionId(entity.getPaymentId()));
            case SHIPPED -> new OrderStatus.Shipping(entity.getShippedAt(),
                new TrackingNumber(entity.getTrackingNumber()));
            case DELIVERED -> new OrderStatus.Delivered(entity.getDeliveredAt());
            case CANCELED -> new OrderStatus.Cancelled(entity.getCanceledAt(),
                CancelReason.valueOf(entity.getCancelReason()));
        };
        return new Order(
            new OrderId(entity.getId().toString()),
            mapItems(entity.getItems()),
            Money.krw(entity.getTotalAmount().longValue()),
            status
        );
    }

    public static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        // ... Domain -> Entity 역변환
        return entity;
    }
}

// 4. Repository에서 Mapper 활용
public class JpaOrderRepository implements OrderRepository {
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(Long.parseLong(id.value()))
            .map(OrderMapper::toDomain);
    }
}
```
- **의도 및 코드 설명**: Entity는 JPA 전용, Domain Record는 비즈니스 전용, Mapper가 변환 담당. 도메인은 JPA를 전혀 모름
- **무엇이 좋아지나**:
  - 도메인 로직이 JPA에 독립적 (순수 함수 테스트 가능)
  - Lazy Loading 예외가 도메인에 전파되지 않음
  - Entity 변경이 도메인에 영향 없음 (Mapper만 수정)
  - 상태를 sealed interface로 타입 안전하게 표현
  - 관심사 명확 분리 (Persistence vs Business)

### 틀리기/놓치기 쉬운 부분
- Mapper는 양방향(toDomain, toEntity) 모두 필요. 저장 시에는 toEntity를 사용
- Entity의 enum(OrderStatusEnum)과 Domain의 sealed interface(OrderStatus)는 1:N 관계일 수 있음 (Paid 상태에 paymentId가 포함되는 등)
- 순환 참조 방지: Domain Record에서는 ID만 참조 (Customer 객체가 아닌 CustomerId)

### 꼭 기억할 것
- Entity = Infrastructure 전용 (가변, JPA 요구사항)
- Domain Record = Business 전용 (불변, 타입 안전)
- Mapper = 통역사 (ACL 역할)
- Domain은 Infrastructure를 모른다 (의존성 역전)

---

## 3. Gradual Migration Strategy (점진적 마이그레이션 전략)

### 핵심 개념
- **관련 키워드**: Gradual Migration, Strangler Fig Pattern, Refactoring, Legacy Code, Big Bang Avoidance
- **통찰**: 레거시 OOP 코드를 한 번에 전면 교체하는 것이 아니라, 가장 복잡한 비즈니스 로직부터 순수 함수로 추출하여 점진적으로 전환한다.
- **설명**: 실무에서 "모든 것을 한 번에 현대적 패턴으로 바꾸자"는 접근은 위험하다. 대신 Strangler Fig 패턴처럼 기존 시스템을 유지하면서 새로운 부분을 점진적으로 교체한다.

  단계:
  1. 가장 복잡한 비즈니스 로직 식별 (가격 계산, 할인 규칙 등)
  2. 해당 로직을 순수 함수로 추출 (PriceCalculations 등)
  3. 순수 함수에 대한 테스트 작성 (Mock 불필요)
  4. 기존 Service에서 추출한 순수 함수 호출
  5. 성공적이면 다음 로직으로 확장

  핵심: "작은 승리를 반복하라. 한 번에 큰 변경은 위험하다."

```
GRADUAL MIGRATION STEPS
==========================

Step 1: Identify           Step 2: Extract         Step 3: Test
+-----------------+        +-----------------+     +-----------------+
| OrderService    |        | OrderService    |     | PriceCalcTest   |
| {               |        | {               |     | {               |
|   // 100 lines  |  -->   |   // calls:     |     |   // no mocks!  |
|   // price calc |        |   PriceCalc.f() |     |   assert(f(in)  |
|   // discount   |        | }               |     |     == expected) |
|   // tax        |        |                 |     | }               |
|   // shipping   |        | PriceCalc       |     +-----------------+
| }               |        | { static f() }  |
+-----------------+        +-----------------+

Step 4: Expand gradually
  Service -> [PureFunc1] -> [PureFunc2] -> [PureFunc3]
  (imperative shell wraps functional core)
```

### 개념이 아닌 것
- **"레거시를 한 번에 교체"**: Big Bang 마이그레이션은 위험. 점진적 접근이 안전
- **"JPA를 완전히 제거"**: JPA는 유지하되, 도메인 로직만 분리. Entity는 인프라 계층에 남음

### Before: Traditional OOP
```java
// [X] 모든 로직이 Service에 뭉쳐있는 레거시
public class OrderService {
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 100줄의 복잡한 로직이 한 메서드에...
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : request.getItems()) {
            Product p = productRepository.findById(item.getProductId());
            subtotal = subtotal.add(p.getPrice().multiply(
                BigDecimal.valueOf(item.getQuantity())));
        }

        // 할인 계산 (20줄)
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getCouponCode() != null) {
            // ... 복잡한 할인 로직
        }

        // 세금 계산 (10줄)
        BigDecimal tax = subtotal.subtract(discount)
            .multiply(BigDecimal.valueOf(0.1));

        // 배송비 계산 (15줄)
        BigDecimal shipping = calculateShipping(request.getAddress(), subtotal);

        BigDecimal total = subtotal.subtract(discount).add(tax).add(shipping);
        // ... 주문 생성, 결제, 저장
    }
}
```
- **의도 및 코드 설명**: 가격, 할인, 세금, 배송비 계산이 모두 하나의 트랜잭션 메서드에 결합
- **뭐가 문제인가**:
  - 100줄+ 단일 메서드 (이해 불가)
  - 테스트하려면 Repository, PaymentGateway 등 모두 Mock 필요
  - 가격 계산 로직만 수정하고 싶어도 전체 메서드 이해 필요
  - 버그 발생 시 디버깅 범위가 너무 넓음

### After: Modern Approach
```java
// [O] 점진적 마이그레이션: 순수 함수 추출 -> 테스트 -> 확장
// Step 1: 순수 함수 추출 (Functional Core)
public class PriceCalculations {
    public static Money calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.price().multiply(item.quantity().value()))
            .reduce(Money.zero(Currency.KRW), Money::add);
    }

    public static Money applyDiscount(Money subtotal, Discount discount) {
        return switch (discount) {
            case Discount.NoDiscount() -> subtotal;
            case Discount.Percentage(int rate) ->
                subtotal.multiply(BigDecimal.valueOf(100 - rate))
                    .divide(BigDecimal.valueOf(100));
            case Discount.FixedAmount(Money amount) ->
                subtotal.subtract(amount).max(Money.zero(subtotal.currency()));
        };
    }

    public static Money applyTax(Money amount, TaxRate rate) {
        return amount.add(amount.multiply(rate.value()));
    }

    public static Money calculateTotal(
            List<OrderItem> items, Discount discount,
            TaxRate taxRate, ShippingFee shippingFee) {
        Money subtotal = calculateSubtotal(items);
        Money discounted = applyDiscount(subtotal, discount);
        Money taxed = applyTax(discounted, taxRate);
        return taxed.add(shippingFee.amount());
    }
}

// Step 2: 테스트 (Mock 불필요!)
@Test
void totalWithPercentageDiscount() {
    var items = List.of(new OrderItem(productId, Quantity.of(2), Money.krw(10000)));
    var discount = new Discount.Percentage(10);
    var tax = new TaxRate(BigDecimal.valueOf(0.1));
    var shipping = ShippingFee.free();

    Money result = PriceCalculations.calculateTotal(items, discount, tax, shipping);
    assertEquals(Money.krw(19800), result); // 20000 * 0.9 * 1.1
}

// Step 3: 기존 Service에서 순수 함수 호출 (Imperative Shell)
public class OrderService {
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // I/O: 데이터 로딩
        List<OrderItem> items = loadItems(request);
        Discount discount = loadDiscount(request.couponCode());
        TaxRate taxRate = taxService.getCurrentRate();
        ShippingFee shipping = shippingService.calculate(request.address());

        // 순수 함수 호출 (Functional Core)
        Money total = PriceCalculations.calculateTotal(
            items, discount, taxRate, shipping);

        // I/O: 저장
        return saveOrder(request, items, total);
    }
}
```
- **의도 및 코드 설명**: 복잡한 계산 로직을 순수 함수(PriceCalculations)로 추출하고, Service는 I/O만 담당. 테스트는 Mock 없이 순수 함수만 검증
- **무엇이 좋아지나**:
  - 순수 함수는 Mock 없이 빠르게 테스트
  - 기존 Service 구조를 유지하면서 점진적 개선
  - 가격 계산 로직 변경이 안전 (테스트가 보호)
  - 디버깅 범위 축소 (순수 함수 내부만 확인)
  - 빅뱅 배포 없이 점진적 개선 가능

### 이해를 위한 부가 상세
점진적 마이그레이션의 우선순위:
1. 가장 복잡하고 버그가 많은 비즈니스 로직 (가격 계산, 할인 규칙)
2. 상태 전이가 복잡한 도메인 (주문 상태, 결제 상태)
3. 외부 의존성이 많은 Service (결제 연동, 배송 추적)

### 틀리기/놓치기 쉬운 부분
- 순수 함수 추출 시 Repository 호출이 섞여있으면 파라미터로 데이터를 받도록 변경
- 기존 Service의 @Transactional은 유지. 순수 함수 추출과 트랜잭션 경계는 별개
- 한 번에 모든 로직을 추출하지 말고, 하나씩 확인하며 진행

### 꼭 기억할 것
- 가장 복잡한 로직부터 순수 함수로 추출
- Mock 없는 테스트로 안전성 확보
- 기존 구조 유지하면서 내부를 점진적으로 개선
- Functional Core / Imperative Shell 분리가 핵심

---

## 4. Testing Strategy (테스트 전략) - 순수 함수 우선

### 핵심 개념
- **관련 키워드**: Pure Function Testing, Unit Test, No Mock, Property-Based Testing, Test Pyramid
- **통찰**: 순수 함수는 입력만으로 출력이 결정되므로, Mock 없이 빠르고 안정적인 테스트가 가능하다. 도메인 로직의 테스트 커버리지를 극대화하는 가장 효과적인 방법이다.
- **설명**: 전통적인 테스트는 Repository, Service, Controller 등 외부 의존성을 Mock하여 단위 테스트를 작성했다. 하지만 Mock은 실제 동작과 다를 수 있고, Mock 설정 코드가 테스트보다 길어지는 문제가 있다.

  Functional Core / Imperative Shell 구조에서는:
  - Functional Core (순수 함수): Mock 없이 `assert(f(input) == expected)` 한 줄로 테스트
  - Imperative Shell (UseCase): 통합 테스트로 I/O 흐름 검증
  - 대부분의 비즈니스 로직이 순수 함수이므로, 테스트 피라미드의 하단(단위 테스트)이 두꺼워짐

```
TEST STRATEGY COMPARISON
===========================

Traditional (Mock-heavy):    Modern (Pure Function First):
     /\                           /\
    /IT\                         /IT\  (thin: I/O flow only)
   /----\                       /----\
  / Unit \  <- mock-heavy     / Unit \  <- mock-free!
 /________\                  /________\  (thick: pure functions)

Traditional Unit Test:       Pure Function Test:
  mock(repo)                   var result = calculate(input);
  mock(service)                assertEquals(expected, result);
  mock(gateway)                // done! no mocks!
  when(...).thenReturn(...)
  // test itself is 2 lines
  verify(repo).save(...)
```

### 개념이 아닌 것
- **"Mock이 필요 없다"**: Imperative Shell(I/O 계층)의 통합 테스트에서는 여전히 Mock이나 TestDouble 활용 가능
- **"순수 함수만 테스트하면 충분"**: E2E 테스트, 통합 테스트도 필요. 하지만 비중이 줄어듦

### Before: Traditional OOP
```java
// [X] Mock 의존적인 단위 테스트 - 설정이 테스트보다 길다
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private PaymentGateway paymentGateway;
    @InjectMocks private OrderService orderService;

    @Test
    void createOrder_withCoupon_appliesDiscount() {
        // Given: Mock 설정 (20줄+)
        when(productRepository.findById(any()))
            .thenReturn(new Product(1L, "상품", BigDecimal.valueOf(10000)));
        when(couponRepository.findByCode("SAVE10"))
            .thenReturn(new Coupon("SAVE10", 10, false));
        when(paymentGateway.charge(any(), any()))
            .thenReturn(new PaymentResult(true, "tx-123"));
        when(orderRepository.save(any()))
            .thenAnswer(inv -> inv.getArgument(0));

        // When: 실제 테스트 (2줄)
        Order result = orderService.createOrder(request);

        // Then: 검증 (5줄)
        verify(paymentGateway).charge(any(), eq(BigDecimal.valueOf(9000)));
        verify(orderRepository).save(any());
    }
}
```
- **의도 및 코드 설명**: 4개의 Mock 의존성 설정, 실제 테스트 로직은 2줄, 대부분이 설정과 검증
- **뭐가 문제인가**:
  - Mock 설정이 테스트 로직보다 10배 이상 길다
  - Mock이 실제 동작과 다를 수 있음 (거짓 안전감)
  - Mock 라이브러리 학습 비용
  - 테스트가 구현에 강하게 결합 (리팩토링 시 테스트 깨짐)
  - 느린 테스트 실행 (Mock 프레임워크 초기화)

### After: Modern Approach
```java
// [O] 순수 함수 우선 테스트 - Mock 없이 빠르고 안정적
class PriceCalculationsTest {

    @Test
    void subtotal_withMultipleItems() {
        var items = List.of(
            new OrderItem(productId1, Quantity.of(2), Money.krw(10000)),
            new OrderItem(productId2, Quantity.of(1), Money.krw(5000))
        );
        assertEquals(Money.krw(25000), PriceCalculations.calculateSubtotal(items));
    }

    @Test
    void discount_percentage_appliesCorrectly() {
        var discount = new Discount.Percentage(10);
        assertEquals(Money.krw(9000),
            PriceCalculations.applyDiscount(Money.krw(10000), discount));
    }

    @Test
    void discount_noDiscount_returnsOriginal() {
        assertEquals(Money.krw(10000),
            PriceCalculations.applyDiscount(Money.krw(10000), new Discount.NoDiscount()));
    }

    @Test
    void total_withAllFactors() {
        var items = List.of(new OrderItem(productId, Quantity.of(2), Money.krw(10000)));
        Money result = PriceCalculations.calculateTotal(
            items, new Discount.Percentage(10),
            new TaxRate(BigDecimal.valueOf(0.1)), ShippingFee.free());
        assertEquals(Money.krw(19800), result); // 20000*0.9*1.1
    }
}

// 상태 전이 테스트도 순수 함수!
class OrderTransitionTest {
    @Test
    void unpaidOrder_canBePaid() {
        var order = new Order(orderId, items, total, new OrderStatus.Unpaid(deadline));
        var result = order.pay(transactionId);
        assertTrue(result instanceof Result.Success);
    }

    @Test
    void shippedOrder_cannotBeCancelled() {
        var order = new Order(orderId, items, total,
            new OrderStatus.Shipping(LocalDateTime.now(), tracking));
        var result = order.cancel(CancelReason.CUSTOMER_REQUEST);
        assertTrue(result instanceof Result.Failure);
    }
}
```
- **의도 및 코드 설명**: 순수 함수 테스트는 입력과 예상 출력만으로 완성. Mock 설정 없이 각 함수를 독립적으로 검증
- **무엇이 좋아지나**:
  - 테스트 작성 시간 단축 (Mock 설정 불필요)
  - 테스트가 구현이 아닌 동작을 검증 (리팩토링에 강함)
  - 매우 빠른 실행 (I/O 없음)
  - 테스트 실패 시 원인 명확 (순수 함수 내부만 확인)
  - 높은 커버리지를 낮은 비용으로 달성

### 틀리기/놓치기 쉬운 부분
- 순수 함수 테스트와 통합 테스트는 별개. 순수 함수 테스트가 비즈니스 로직을, 통합 테스트가 I/O 흐름을 검증
- Record의 equals()는 자동 생성되므로, assertEquals에서 값 비교가 자연스럽게 동작
- 테스트에서 LocalDateTime.now() 등 비결정적 값은 파라미터로 주입하여 결정적으로 만들기

### 꼭 기억할 것
- 순수 함수 = assert(f(input) == expected) 한 줄로 테스트
- Mock은 I/O 경계(Imperative Shell)에서만 최소한으로 사용
- 비즈니스 로직의 대부분을 순수 함수로 -> 테스트 피라미드 하단이 두꺼워짐
- 빠르고, 안정적이고, 리팩토링에 강한 테스트

---

## 5. Architecture Decision Record (아키텍처 결정 기록) - when to use which pattern

### 핵심 개념
- **관련 키워드**: ADR, Architecture Decision Record, Pattern Selection, Trade-offs, Context-Dependent
- **통찰**: 어떤 패턴을 언제 사용할지는 맥락에 따라 다르다. ADR로 결정의 근거를 기록하면 팀 전체가 일관된 기준으로 패턴을 선택할 수 있다.
- **설명**: 지금까지 학습한 패턴들은 모든 상황에 항상 적합한 것이 아니다. 프로젝트의 규모, 팀의 숙련도, 도메인 복잡도, 레거시 코드 상황에 따라 적절한 패턴을 선택해야 한다.

  Architecture Decision Record(ADR)는 "왜 이 패턴을 선택했는가?"를 문서화하여, 미래의 팀원이나 자신이 결정의 맥락을 이해할 수 있게 한다.

  패턴 선택 기준:
  - 도메인 복잡도: 단순(CRUD) vs 복잡(상태 전이, 비즈니스 규칙)
  - 팀 숙련도: FP 경험 여부
  - 레거시 상황: 그린필드 vs 기존 코드 존재
  - 변경 빈도: 비즈니스 규칙이 자주 변하는지

```
PATTERN SELECTION DECISION TREE
==================================

  Domain complexity?
  |
  +-- Simple (CRUD) --> Traditional OOP + JPA Entity
  |                      (Record for DTOs is OK)
  |
  +-- Medium ----------> Value Objects + ADT
  |   (some rules)       Result for error handling
  |                      Gradual extraction
  |
  +-- Complex ---------> Full DOP/FP
      (state machines,    Sealed Interface + Record
       business rules,    Pipeline + Result
       distributed)       Rule Engine (if rules change often)

  +---------------------+-------------------+--------------------+
  | Pattern             | Use When          | Avoid When         |
  +---------------------+-------------------+--------------------+
  | Value Object        | Any domain value  | Truly primitive    |
  | ADT (sealed iface)  | Multiple states   | Single state CRUD  |
  | Result              | Fallible ops      | Always-succeed ops |
  | Pipeline (flatMap)  | Multi-step flow   | Single-step ops    |
  | Rule Engine         | Rules change often| Fixed rules        |
  | Wither              | Immutable update  | JPA Entity         |
  +---------------------+-------------------+--------------------+
```

### 개념이 아닌 것
- **"항상 최신 패턴을 써야 한다"**: 단순 CRUD에 full DOP를 적용하면 과잉 설계(overengineering)
- **"패턴은 서로 배타적"**: 하나의 프로젝트에서 상황에 따라 다른 패턴 조합 가능

### Before: Traditional OOP
```java
// [X] 맥락 없는 패턴 적용 - 과잉 설계
// 단순 CRUD인데 Result + Pipeline + ADT를 모두 적용
public sealed interface UserError permits NotFound, AlreadyExists {}
public record User(UserId id, String name) {}

public class CreateUserUseCase {
    public Result<User, UserError> execute(CreateUserCommand cmd) {
        return validateName(cmd.name())
            .flatMap(name -> checkDuplicate(name))
            .flatMap(name -> createUser(name))
            .map(user -> {
                userRepository.save(user);
                return user;
            });
    }
    // 단순 CRUD에 4단계 파이프라인? 과잉!
}
```
- **의도 및 코드 설명**: 간단한 사용자 생성에 Result + Pipeline + sealed interface를 모두 적용하여 과잉 설계
- **뭐가 문제인가**:
  - 단순 CRUD에 과도한 추상화
  - 코드 읽는 비용 > 안전성 이득
  - 팀원 학습 부담 증가
  - YAGNI(You Aren't Gonna Need It) 위반

### After: Modern Approach
```java
// [O] ADR 기반 패턴 선택 - 맥락에 맞는 적절한 수준
// ADR-001: 결제 도메인에 Result + Pipeline 적용
// Context: 결제는 실패 가능성이 높고, 에러 종류가 다양하며, 상태 전이가 복잡
// Decision: Result + sealed interface + Pipeline 적용
// Consequences: 모든 결제 에러가 타입으로 명시, 테스트 용이

public sealed interface PaymentError permits InsufficientFunds, CardExpired, Timeout {}

public Result<Payment, PaymentError> processPayment(PaymentRequest request) {
    return validateCard(request.card())
        .flatMap(card -> checkBalance(card, request.amount()))
        .flatMap(balance -> chargeCard(balance, request.amount()));
}

// ADR-002: 사용자 CRUD에는 단순 접근
// Context: 단순 CRUD, 실패 시 예외로 충분
// Decision: 기본 JPA + Optional 사용
// Consequences: 간결한 코드, 팀 학습 부담 최소

public class UserService {
    public User createUser(String name) {
        if (userRepository.existsByName(name))
            throw new DuplicateException("이미 존재: " + name);
        return userRepository.save(new UserEntity(name));
    }
}

// ADR-003: 할인 규칙에 Rule Engine 적용
// Context: 마케팅 팀이 자주 규칙을 변경, 코드 배포 없이 변경 필요
// Decision: Rule as Data (인터프리터 패턴) 적용
// Consequences: 관리자 UI로 규칙 관리 가능, 규칙 로딩 인프라 필요
```
- **의도 및 코드 설명**: 각 도메인의 복잡도와 요구사항에 맞게 패턴을 선택하고, 그 근거를 ADR로 기록
- **무엇이 좋아지나**:
  - 과잉 설계 방지 (적절한 수준의 추상화)
  - 팀원이 "왜 이 패턴인가"를 이해 가능
  - 일관된 결정 기준으로 코드 스타일 통일
  - 새로운 도메인 추가 시 기존 ADR을 참고하여 빠른 결정

### 이해를 위한 부가 상세
ADR의 기본 형식:
- **Title**: 결정 제목
- **Context**: 왜 결정이 필요한가?
- **Decision**: 무엇을 선택했는가?
- **Consequences**: 선택의 결과와 트레이드오프는?
- **Status**: 제안/수용/대체됨

### 틀리기/놓치기 쉬운 부분
- ADR은 "정답"을 기록하는 것이 아니라 "맥락에서의 최선"을 기록하는 것
- 패턴의 조합도 가능: CRUD 부분은 단순 JPA, 핵심 도메인은 DOP, 규칙은 인터프리터
- 팀의 성장에 따라 ADR을 업데이트. 초기에는 단순하게, 팀이 익숙해지면 점진적으로 확장

### 꼭 기억할 것
- 패턴 선택은 맥락(도메인 복잡도, 팀 숙련도, 변경 빈도)에 따라 결정
- 단순 CRUD에 과도한 패턴 = 과잉 설계
- ADR로 결정의 근거를 기록하면 팀 전체의 일관성 확보
- "모든 곳에 최신 패턴"보다 "적절한 곳에 적절한 패턴"
