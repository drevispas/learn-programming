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

**[그림 13.1]** Full Domain Model with ADT + Result + Pipeline (ADT + Result + 파이프라인 종합)
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

**[코드 13.1]** Traditional OOP: 신(God) 서비스 - 모든 도메인 로직이 한 곳에 결합
```java
 1| // package: com.ecommerce.order
 2| // [X] 신(God) 서비스 - 모든 도메인 로직이 한 곳에 결합
 3| public class OrderService {
 4|   public OrderResponse createOrder(CreateOrderRequest request) {
 5|     // 회원 검증
 6|     User user = userRepository.findById(request.getUserId());
 7|     if (user == null) throw new RuntimeException("회원 없음");
 8|     if (!user.isEmailVerified()) throw new RuntimeException("이메일 미인증");
 9| 
10|     // 상품 재고 확인
11|     for (ItemRequest item : request.getItems()) {
12|       Product product = productRepository.findById(item.getProductId());
13|       if (product == null) throw new RuntimeException("상품 없음");
14|       if (product.getStock() < item.getQuantity())
15|         throw new RuntimeException("재고 부족");
16|     }
17| 
18|     // 쿠폰 적용
19|     BigDecimal total = calculateTotal(request.getItems());
20|     if (request.getCouponCode() != null) {
21|       Coupon coupon = couponRepository.findByCode(request.getCouponCode());
22|       if (coupon == null) throw new RuntimeException("쿠폰 없음");
23|       if (coupon.isExpired()) throw new RuntimeException("쿠폰 만료");
24|       total = applyCoupon(total, coupon);
25|     }
26| 
27|     // 결제
28|     PaymentResult payment = paymentGateway.charge(user, total);
29|     if (!payment.isSuccess()) throw new RuntimeException("결제 실패");
30| 
31|     // 주문 생성
32|     Order order = new Order();
33|     order.setUserId(user.getId());
34|     order.setTotal(total);
35|     order.setStatus("PAID");
36|     return orderRepository.save(order);
37|   }
38| }
```
- **의도 및 코드 설명**: 단일 서비스 메서드에서 회원/상품/쿠폰/결제/주문 모든 도메인 처리. 모든 에러는 RuntimeException
- **뭐가 문제인가**:
  - 단일 메서드 100+ 줄 (읽기/유지보수 곤란)
  - 에러 종류를 호출자가 알 수 없음 (모두 RuntimeException)
  - 도메인 경계 없음 (회원/상품/결제 로직 혼재)
  - 상태 전이 규칙이 문자열("PAID")에 의존
  - 테스트 시 전체 의존성 Mock 필요

### After: Modern Approach

**[코드 13.2]** Modern: 종합 통합: ADT + Result + Pipeline
```java
 1| // package: com.ecommerce.order
 2| // [O] 종합 통합: ADT + Result + Pipeline
 3| // 1. 각 Bounded Context별 독립 모델
 4| public sealed interface OrderStatus {
 5|   record Unpaid(LocalDateTime deadline) implements OrderStatus {}
 6|   record Paid(LocalDateTime paidAt, TransactionId txId) implements OrderStatus {}
 7|   record Shipping(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
 8|   record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
 9|   record Cancelled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
10| }
11| 
12| public sealed interface OrderError permits
13|   EmptyOrder, InvalidCustomer, OutOfStock, PaymentFailed, InvalidCoupon {
14|   record EmptyOrder() implements OrderError {}
15|   record InvalidCustomer(String reason) implements OrderError {}
16|   record OutOfStock(ProductId id, int requested, int available) implements OrderError {}
17|   record PaymentFailed(String reason) implements OrderError {}
18|   record InvalidCoupon(String code, String reason) implements OrderError {}
19| }
20| 
21| // 2. 순수 함수 도메인 서비스 (Functional Core)
22| public class OrderDomainService {
23|   public static Result<ValidatedOrder, OrderError> validate(
24|       CreateOrderCommand cmd, Member member, List<InventoryProduct> inventory) {
25|     if (!member.isEmailVerified())
26|       return Result.failure(new InvalidCustomer("이메일 미인증"));
27|     if (cmd.items().isEmpty())
28|       return Result.failure(new EmptyOrder());
29|     // 재고 확인
30|     for (var item : cmd.items()) {
31|       var product = findProduct(inventory, item.productId());
32|       if (!product.isAvailable(item.quantity()))
33|         return Result.failure(new OutOfStock(
34|           item.productId(), item.quantity(), product.stock().value()));
35|     }
36|     return Result.success(new ValidatedOrder(cmd, member));
37|   }
38| 
39|   public static Result<PricedOrder, OrderError> applyPricing(
40|       ValidatedOrder order, Optional<Coupon> coupon) {
41|     Money subtotal = calculateSubtotal(order.items());
42|     Money discount = coupon
43|       .filter(Coupon::isAvailable)
44|       .map(c -> c.type().calculateDiscount(subtotal))
45|       .orElse(Money.zero(Currency.KRW));
46|     return Result.success(new PricedOrder(order, subtotal.subtract(discount)));
47|   }
48| }
49| 
50| // 3. 워크플로우 파이프라인 (Imperative Shell)
51| public class PlaceOrderUseCase {
52|   public Result<Order, OrderError> execute(CreateOrderCommand cmd) {
53|     Member member = memberRepository.findById(cmd.memberId()).orElseThrow();
54|     List<InventoryProduct> inventory = inventoryRepository.findAll(cmd.productIds());
55|     Optional<Coupon> coupon = cmd.couponCode()
56|       .flatMap(couponRepository::findByCode);
57| 
58|     return OrderDomainService.validate(cmd, member, inventory)
59|       .flatMap(validated -> OrderDomainService.applyPricing(validated, coupon))
60|       .flatMap(priced -> processPayment(priced))
61|       .map(paid -> {
62|         Order order = createOrder(paid);
63|         orderRepository.save(order);
64|         return order;
65|       });
66|   }
67| }
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

**[그림 13.2]** JPA Entity vs Domain Record Mapping (JPA Entity와 도메인 Record 매핑)
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

**[코드 13.3]** Traditional OOP: Entity를 도메인 로직에서 직접 사용
```java
 1| // package: com.ecommerce.order
 2| // [X] Entity를 도메인 로직에서 직접 사용
 3| @Entity
 4| public class OrderEntity {
 5|   @Id @GeneratedValue private Long id;
 6|   @Enumerated(EnumType.STRING) private OrderStatusEnum status;
 7|   @Column private BigDecimal totalAmount;
 8| 
 9|   // JPA 필수: 기본 생성자
10|   protected OrderEntity() {}
11| 
12|   // 비즈니스 로직이 Entity에 혼재 (안티패턴)
13|   public void markAsPaid(String paymentId) {
14|     this.status = OrderStatusEnum.PAID;
15|     this.paymentId = paymentId;
16|     this.paidAt = LocalDateTime.now();
17|   }
18| }
19| 
20| public class OrderService {
21|   @Transactional
22|   public void processOrder(Long orderId) {
23|     OrderEntity entity = orderRepository.findById(orderId).orElseThrow();
24|     entity.markAsPaid(paymentId); // Entity 직접 조작
25|     // JPA Dirty Checking이 자동 저장
26|   }
27| }
```
- **의도 및 코드 설명**: JPA Entity에 비즈니스 로직을 추가하고, 서비스에서 Entity를 직접 조작
- **뭐가 문제인가**:
  - 비즈니스 로직이 인프라 계층(Entity)에 종속
  - Lazy Loading 예외가 도메인 로직에서 발생 가능
  - 가변 상태로 인한 버그 추적 어려움
  - Entity 변경 시 도메인 로직에 영향
  - 순수 함수 테스트 불가 (JPA 컨텍스트 필요)

### After: Modern Approach

**[코드 13.4]** Modern: Mapper로 Entity와 Domain Record 분리
```java
 1| // package: com.ecommerce.order
 2| // [O] Mapper로 Entity와 Domain Record 분리
 3| // 1. JPA Entity (Infrastructure 전용)
 4| @Entity @Table(name = "orders")
 5| public class OrderEntity {
 6|   @Id @GeneratedValue private Long id;
 7|   @Enumerated(EnumType.STRING) private OrderStatusEnum status;
 8|   private BigDecimal totalAmount;
 9|   private LocalDateTime paidAt;
10|   private String paymentId;
11|   // JPA 전용: getters, setters, 기본 생성자
12| }
13| 
14| // 2. Domain Record (비즈니스 로직 전용)
15| public record Order(OrderId id, List<OrderItem> items, Money total, OrderStatus status) {
16|   public Order { items = List.copyOf(items); }
17| }
18| 
19| // 3. Mapper (통역사)
20| public class OrderMapper {
21|   public static Order toDomain(OrderEntity entity) {
22|     OrderStatus status = switch (entity.getStatus()) {
23|       case PENDING -> new OrderStatus.Unpaid(entity.getCreatedAt());
24|       case PAID -> new OrderStatus.Paid(entity.getPaidAt(),
25|         new TransactionId(entity.getPaymentId()));
26|       case SHIPPED -> new OrderStatus.Shipping(entity.getShippedAt(),
27|         new TrackingNumber(entity.getTrackingNumber()));
28|       case DELIVERED -> new OrderStatus.Delivered(entity.getDeliveredAt());
29|       case CANCELED -> new OrderStatus.Cancelled(entity.getCanceledAt(),
30|         CancelReason.valueOf(entity.getCancelReason()));
31|     };
32|     return new Order(
33|       new OrderId(entity.getId().toString()),
34|       mapItems(entity.getItems()),
35|       Money.krw(entity.getTotalAmount().longValue()),
36|       status
37|     );
38|   }
39| 
40|   public static OrderEntity toEntity(Order order) {
41|     OrderEntity entity = new OrderEntity();
42|     // ... Domain -> Entity 역변환
43|     return entity;
44|   }
45| }
46| 
47| // 4. Repository에서 Mapper 활용
48| public class JpaOrderRepository implements OrderRepository {
49|   public Optional<Order> findById(OrderId id) {
50|     return jpaRepository.findById(Long.parseLong(id.value()))
51|       .map(OrderMapper::toDomain);
52|   }
53| }
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

**[그림 13.3]** Gradual Migration Strategy (점진적 마이그레이션 전략)
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

**[코드 13.5]** Traditional OOP: 모든 로직이 Service에 뭉쳐있는 레거시
```java
 1| // package: com.ecommerce.order
 2| // [X] 모든 로직이 Service에 뭉쳐있는 레거시
 3| public class OrderService {
 4|   @Transactional
 5|   public Order createOrder(CreateOrderRequest request) {
 6|     // 100줄의 복잡한 로직이 한 메서드에...
 7|     BigDecimal subtotal = BigDecimal.ZERO;
 8|     for (ItemRequest item : request.getItems()) {
 9|       Product p = productRepository.findById(item.getProductId());
10|       subtotal = subtotal.add(p.getPrice().multiply(
11|         BigDecimal.valueOf(item.getQuantity())));
12|     }
13| 
14|     // 할인 계산 (20줄)
15|     BigDecimal discount = BigDecimal.ZERO;
16|     if (request.getCouponCode() != null) {
17|       // ... 복잡한 할인 로직
18|     }
19| 
20|     // 세금 계산 (10줄)
21|     BigDecimal tax = subtotal.subtract(discount)
22|       .multiply(BigDecimal.valueOf(0.1));
23| 
24|     // 배송비 계산 (15줄)
25|     BigDecimal shipping = calculateShipping(request.getAddress(), subtotal);
26| 
27|     BigDecimal total = subtotal.subtract(discount).add(tax).add(shipping);
28|     // ... 주문 생성, 결제, 저장
29|   }
30| }
```
- **의도 및 코드 설명**: 가격, 할인, 세금, 배송비 계산이 모두 하나의 트랜잭션 메서드에 결합
- **뭐가 문제인가**:
  - 100줄+ 단일 메서드 (이해 불가)
  - 테스트하려면 Repository, PaymentGateway 등 모두 Mock 필요
  - 가격 계산 로직만 수정하고 싶어도 전체 메서드 이해 필요
  - 버그 발생 시 디버깅 범위가 너무 넓음

### After: Modern Approach

**[코드 13.6]** Modern: 점진적 마이그레이션: 순수 함수 추출 -> 테스트 -> 확장
```java
 1| // package: com.ecommerce.shared
 2| // [O] 점진적 마이그레이션: 순수 함수 추출 -> 테스트 -> 확장
 3| // Step 1: 순수 함수 추출 (Functional Core)
 4| public class PriceCalculations {
 5|   public static Money calculateSubtotal(List<OrderItem> items) {
 6|     return items.stream()
 7|       .map(item -> item.price().multiply(item.quantity().value()))
 8|       .reduce(Money.zero(Currency.KRW), Money::add);
 9|   }
10| 
11|   public static Money applyDiscount(Money subtotal, Discount discount) {
12|     return switch (discount) {
13|       case Discount.NoDiscount() -> subtotal;
14|       case Discount.Percentage(int rate) ->
15|         subtotal.multiply(BigDecimal.valueOf(100 - rate))
16|           .divide(BigDecimal.valueOf(100));
17|       case Discount.FixedAmount(Money amount) ->
18|         subtotal.subtract(amount).max(Money.zero(subtotal.currency()));
19|     };
20|   }
21| 
22|   public static Money applyTax(Money amount, TaxRate rate) {
23|     return amount.add(amount.multiply(rate.value()));
24|   }
25| 
26|   public static Money calculateTotal(
27|       List<OrderItem> items, Discount discount,
28|       TaxRate taxRate, ShippingFee shippingFee) {
29|     Money subtotal = calculateSubtotal(items);
30|     Money discounted = applyDiscount(subtotal, discount);
31|     Money taxed = applyTax(discounted, taxRate);
32|     return taxed.add(shippingFee.amount());
33|   }
34| }
35| 
36| // Step 2: 테스트 (Mock 불필요!)
37| @Test
38| void totalWithPercentageDiscount() {
39|   var items = List.of(new OrderItem(productId, Quantity.of(2), Money.krw(10000)));
40|   var discount = new Discount.Percentage(10);
41|   var tax = new TaxRate(BigDecimal.valueOf(0.1));
42|   var shipping = ShippingFee.free();
43| 
44|   Money result = PriceCalculations.calculateTotal(items, discount, tax, shipping);
45|   assertEquals(Money.krw(19800), result); // 20000 * 0.9 * 1.1
46| }
47| 
48| // Step 3: 기존 Service에서 순수 함수 호출 (Imperative Shell)
49| public class OrderService {
50|   @Transactional
51|   public Order createOrder(CreateOrderRequest request) {
52|     // I/O: 데이터 로딩
53|     List<OrderItem> items = loadItems(request);
54|     Discount discount = loadDiscount(request.couponCode());
55|     TaxRate taxRate = taxService.getCurrentRate();
56|     ShippingFee shipping = shippingService.calculate(request.address());
57| 
58|     // 순수 함수 호출 (Functional Core)
59|     Money total = PriceCalculations.calculateTotal(
60|       items, discount, taxRate, shipping);
61| 
62|     // I/O: 저장
63|     return saveOrder(request, items, total);
64|   }
65| }
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

**[그림 13.4]** Testing Strategy (테스트 전략) - 순수 함수 우선
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

**[코드 13.7]** Traditional OOP: Mock 의존적인 단위 테스트 - 설정이 테스트보다 길다
```java
 1| // package: com.ecommerce.order
 2| // [X] Mock 의존적인 단위 테스트 - 설정이 테스트보다 길다
 3| @ExtendWith(MockitoExtension.class)
 4| class OrderServiceTest {
 5|   @Mock private OrderRepository orderRepository;
 6|   @Mock private ProductRepository productRepository;
 7|   @Mock private CouponRepository couponRepository;
 8|   @Mock private PaymentGateway paymentGateway;
 9|   @InjectMocks private OrderService orderService;
10| 
11|   @Test
12|   void createOrder_withCoupon_appliesDiscount() {
13|     // Given: Mock 설정 (20줄+)
14|     when(productRepository.findById(any()))
15|       .thenReturn(new Product(1L, "상품", BigDecimal.valueOf(10000)));
16|     when(couponRepository.findByCode("SAVE10"))
17|       .thenReturn(new Coupon("SAVE10", 10, false));
18|     when(paymentGateway.charge(any(), any()))
19|       .thenReturn(new PaymentResult(true, "tx-123"));
20|     when(orderRepository.save(any()))
21|       .thenAnswer(inv -> inv.getArgument(0));
22| 
23|     // When: 실제 테스트 (2줄)
24|     Order result = orderService.createOrder(request);
25| 
26|     // Then: 검증 (5줄)
27|     verify(paymentGateway).charge(any(), eq(BigDecimal.valueOf(9000)));
28|     verify(orderRepository).save(any());
29|   }
30| }
```
- **의도 및 코드 설명**: 4개의 Mock 의존성 설정, 실제 테스트 로직은 2줄, 대부분이 설정과 검증
- **뭐가 문제인가**:
  - Mock 설정이 테스트 로직보다 10배 이상 길다
  - Mock이 실제 동작과 다를 수 있음 (거짓 안전감)
  - Mock 라이브러리 학습 비용
  - 테스트가 구현에 강하게 결합 (리팩토링 시 테스트 깨짐)
  - 느린 테스트 실행 (Mock 프레임워크 초기화)

### After: Modern Approach

**[코드 13.8]** Modern: 순수 함수 우선 테스트 - Mock 없이 빠르고 안정적
```java
 1| // package: com.ecommerce.shared
 2| // [O] 순수 함수 우선 테스트 - Mock 없이 빠르고 안정적
 3| class PriceCalculationsTest {
 4| 
 5|   @Test
 6|   void subtotal_withMultipleItems() {
 7|     var items = List.of(
 8|       new OrderItem(productId1, Quantity.of(2), Money.krw(10000)),
 9|       new OrderItem(productId2, Quantity.of(1), Money.krw(5000))
10|     );
11|     assertEquals(Money.krw(25000), PriceCalculations.calculateSubtotal(items));
12|   }
13| 
14|   @Test
15|   void discount_percentage_appliesCorrectly() {
16|     var discount = new Discount.Percentage(10);
17|     assertEquals(Money.krw(9000),
18|       PriceCalculations.applyDiscount(Money.krw(10000), discount));
19|   }
20| 
21|   @Test
22|   void discount_noDiscount_returnsOriginal() {
23|     assertEquals(Money.krw(10000),
24|       PriceCalculations.applyDiscount(Money.krw(10000), new Discount.NoDiscount()));
25|   }
26| 
27|   @Test
28|   void total_withAllFactors() {
29|     var items = List.of(new OrderItem(productId, Quantity.of(2), Money.krw(10000)));
30|     Money result = PriceCalculations.calculateTotal(
31|       items, new Discount.Percentage(10),
32|       new TaxRate(BigDecimal.valueOf(0.1)), ShippingFee.free());
33|     assertEquals(Money.krw(19800), result); // 20000*0.9*1.1
34|   }
35| }
36| 
37| // 상태 전이 테스트도 순수 함수!
38| class OrderTransitionTest {
39|   @Test
40|   void unpaidOrder_canBePaid() {
41|     var order = new Order(orderId, items, total, new OrderStatus.Unpaid(deadline));
42|     var result = order.pay(transactionId);
43|     assertTrue(result instanceof Result.Success);
44|   }
45| 
46|   @Test
47|   void shippedOrder_cannotBeCancelled() {
48|     var order = new Order(orderId, items, total,
49|       new OrderStatus.Shipping(LocalDateTime.now(), tracking));
50|     var result = order.cancel(CancelReason.CUSTOMER_REQUEST);
51|     assertTrue(result instanceof Result.Failure);
52|   }
53| }
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

**[그림 13.5]** Architecture Decision Record (아키텍처 결정 기록) - when to use which pattern
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

**[코드 13.9]** Traditional OOP: 맥락 없는 패턴 적용 - 과잉 설계
```java
 1| // package: com.ecommerce.auth
 2| // [X] 맥락 없는 패턴 적용 - 과잉 설계
 3| // 단순 CRUD인데 Result + Pipeline + ADT를 모두 적용
 4| public sealed interface UserError permits NotFound, AlreadyExists {}
 5| public record User(UserId id, String name) {}
 6| 
 7| public class CreateUserUseCase {
 8|   public Result<User, UserError> execute(CreateUserCommand cmd) {
 9|     return validateName(cmd.name())
10|       .flatMap(name -> checkDuplicate(name))
11|       .flatMap(name -> createUser(name))
12|       .map(user -> {
13|         userRepository.save(user);
14|         return user;
15|       });
16|   }
17|   // 단순 CRUD에 4단계 파이프라인? 과잉!
18| }
```
- **의도 및 코드 설명**: 간단한 사용자 생성에 Result + Pipeline + sealed interface를 모두 적용하여 과잉 설계
- **뭐가 문제인가**:
  - 단순 CRUD에 과도한 추상화
  - 코드 읽는 비용 > 안전성 이득
  - 팀원 학습 부담 증가
  - YAGNI(You Aren't Gonna Need It) 위반

### After: Modern Approach

**[코드 13.10]** Modern: ADR 기반 패턴 선택 - 맥락에 맞는 적절한 수준
```java
 1| // package: com.ecommerce.payment
 2| // [O] ADR 기반 패턴 선택 - 맥락에 맞는 적절한 수준
 3| // ADR-001: 결제 도메인에 Result + Pipeline 적용
 4| // Context: 결제는 실패 가능성이 높고, 에러 종류가 다양하며, 상태 전이가 복잡
 5| // Decision: Result + sealed interface + Pipeline 적용
 6| // Consequences: 모든 결제 에러가 타입으로 명시, 테스트 용이
 7| 
 8| public sealed interface PaymentError permits InsufficientFunds, CardExpired, Timeout {}
 9| 
10| public Result<Payment, PaymentError> processPayment(PaymentRequest request) {
11|   return validateCard(request.card())
12|     .flatMap(card -> checkBalance(card, request.amount()))
13|     .flatMap(balance -> chargeCard(balance, request.amount()));
14| }
15| 
16| // ADR-002: 사용자 CRUD에는 단순 접근
17| // Context: 단순 CRUD, 실패 시 예외로 충분
18| // Decision: 기본 JPA + Optional 사용
19| // Consequences: 간결한 코드, 팀 학습 부담 최소
20| 
21| public class UserService {
22|   public User createUser(String name) {
23|     if (userRepository.existsByName(name))
24|       throw new DuplicateException("이미 존재: " + name);
25|     return userRepository.save(new UserEntity(name));
26|   }
27| }
28| 
29| // ADR-003: 할인 규칙에 Rule Engine 적용
30| // Context: 마케팅 팀이 자주 규칙을 변경, 코드 배포 없이 변경 필요
31| // Decision: Rule as Data (인터프리터 패턴) 적용
32| // Consequences: 관리자 UI로 규칙 관리 가능, 규칙 로딩 인프라 필요
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
