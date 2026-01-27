# 02. Foundations & Paradigm Shift (기초와 패러다임 전환)

> **Sources**: DMMF Ch.1 (DDD 기초), DOP Intro (DOP 4원칙), DOP Ch.1 (OOP 환상과 데이터 실체)

---

## 1. God Class와 캡슐화의 한계 (God Class and the Limits of Encapsulation)

### 핵심 개념
- **관련 키워드**: God Class, Encapsulation, Single Responsibility, Coupling
- **통찰**: 캡슐화는 "데이터와 행위를 한 곳에"라고 가르치지만, 규모가 커지면 "모든 것을 한 곳에"로 전락한다.
- **설명**: OOP의 캡슐화 원칙은 소규모에서 잘 작동합니다. 하지만 시스템이 성장하면 하나의 클래스가 점점 많은 책임을 흡수하여 God Class가 됩니다. 주문, 결제, 배송, 알림 로직이 하나의 클래스에 모이면, 한 기능 수정이 다른 모든 기능의 테스트를 깨뜨립니다.

  God Class의 판별 기준은 명확합니다: 500줄 이상의 LOC, 10개 이상의 의존성 주입, 여러 도메인 관심사 처리, 혼합된 추상화 수준. 이 중 2개 이상 해당되면 God Class를 의심해야 합니다.

**[그림 02.1]** God Class와 캡슐화의 한계 (God Class and the Limits of Encapsulation)
```
+---------------------------------------------+
|              GOD CLASS (Order)              |
|---------------------------------------------|
| - id, status, items, payment, shipping,     |
|   coupon, customer, notification...         |
|---------------------------------------------|
| + createOrder()    + processPayment()       |
| + ship()           + cancel()               |
| + refund()         + sendNotification()     |
| + updateInventory()+ calculateTax()         |
+---------------------------------------------+
         | changes propagate everywhere
         v
  [All tests break when one thing changes]


          DOP / DMMF Separation
          ~~~~~~~~~~~~~~~~~~~~~~

+--------+   +--------+   +--------+   +--------+
| Order  |   |Payment |   |Shipping|   |  Tax   |
| Record |   | Calc.  |   | Calc.  |   | Calc.  |
+--------+   +--------+   +--------+   +--------+
  (data)      (logic)       (logic)      (logic)
```

### 개념이 아닌 것
- **"클래스가 크면 무조건 God Class"**: 크기보다 책임의 수와 응집도가 핵심 기준
- **"God Class = 모든 OOP"**: OOP도 SRP를 강조하지만, 실무에서 캡슐화가 God Class로 퇴화하는 경향이 문제

### Before: Traditional OOP

**[코드 02.1]** Traditional OOP: God Class: 모든 것을 담은 OrderService
```java
 1| // package: com.ecommerce.order
 2| // [X] God Class: 모든 것을 담은 OrderService
 3| @RequiredArgsConstructor
 4| public class OrderService {
 5|   private final UserRepository userRepo;
 6|   private final ProductRepository productRepo;
 7|   private final PaymentGateway paymentGateway;
 8|   private final ShippingService shippingService;
 9|   private final EmailService emailService;
10|   private final InventoryService inventoryService;
11|   private final CouponService couponService;
12|   private final TaxCalculator taxCalculator;
13|
14|   public Order createOrder(CreateOrderRequest request) {
15|     // 200줄의 혼합된 로직: 검증 + 계산 + 저장 + 알림
16|     Coupon coupon = couponService.validate(request.getCouponCode());
17|     for (OrderItem item : request.getItems()) {
18|       if (!inventoryService.hasStock(item.getProductId(), item.getQuantity()))
19|         throw new OutOfStockException();
20|     }
21|     BigDecimal total = calculateTotal(request.getItems());
22|     BigDecimal discounted = coupon.apply(total);
23|     Order order = new Order();
24|     order.setItems(request.getItems());
25|     order.setTotalAmount(discounted);
26|     order.setStatus("CREATED");
27|     orderRepository.save(order);
28|     inventoryService.decreaseStock(/*...*/);
29|     emailService.sendOrderCreatedNotification(order);
30|     return order;
31|   }
32|
33|   public void processPayment(Order order, PaymentInfo info) { /* 150줄 */ }
34|   public void arrangeShipping(Order order) { /* 100줄 */ }
35|   // ... 메서드가 계속 증가
36| }
```
- **의도 및 코드 설명**: 하나의 서비스 클래스가 주문 생성, 결제, 배송, 알림까지 모두 담당
- **뭐가 문제인가**:
  - 8개 이상의 의존성 주입 (낮은 응집도)
  - 한 기능 수정 시 무관한 테스트 실패
  - 단위 테스트에 Mock 8개 이상 필요
  - 여러 팀이 같은 파일 수정 시 Git 충돌

### After: DMMF 관점 (DDD + FP)

**[코드 02.2]** DMMF: Bounded Context별 분리 + 순수 함수 + Orchestration Shell
```java
 1| // package: com.ecommerce.order (primary), com.ecommerce.auth
 2| // [O] Bounded Context별 분리 + 순수 함수 + Orchestration Shell
 3|
 4| // --- Domain Types (Bounded Context별) ---
 5| package com.ecommerce.order;
 6|
 7| public record Customer(
 8|   CustomerId id,
 9|   ShippingAddress address,
10|   ContactInfo contact
11| ) {}
12|
13| package com.ecommerce.auth;
14|
15| public record AppUser(
16|   long id,
17|   String username,
18|   String passwordHash,
19|   Set<Role> roles
20| ) {}
21|
22| // --- Pure Domain Logic ---
23| public class OrderDomainService {
24|   public static Money calculateTotal(List<OrderItem> items) {
25|     return items.stream()
26|       .map(item -> item.unitPrice().multiply(item.quantity().value()))
27|       .reduce(Money.zero(), Money::add);
28|   }
29|
30|   public static Money applyDiscount(Money total, DiscountRate rate) {
31|     return rate.applyTo(total);
32|   }
33|
34|   public static Result<Coupon, OrderError> validateCoupon(CouponCode code, Coupon coupon) {
35|     return coupon.isValid()
36|       ? Result.success(coupon)
37|       : Result.failure(new OrderError.InvalidCoupon(code));
38|   }
39|
40|   public static Result<Void, OrderError> validateStock(List<OrderItem> items, StockInfo stock) {
41|     return items.stream().allMatch(item -> stock.hasEnough(item.productId(), item.quantity()))
42|       ? Result.success(null)
43|       : Result.failure(new OrderError.OutOfStock());
44|   }
45| }
46|
47| // --- Orchestration Shell (Impure) ---
48| @RequiredArgsConstructor
49| public class PlaceOrderUseCase {
50|   private final OrderRepository orderRepository;
51|   private final InventoryService inventoryService;
52|   private final CouponRepository couponRepository;
53|   private final EmailService emailService;
54|
55|   public Result<Order, OrderError> execute(CreateOrderCommand cmd) {
56|     Coupon coupon = couponRepository.findByCode(cmd.couponCode());
57|     StockInfo stock = inventoryService.getStockInfo(cmd.productIds());
58|
59|     return OrderDomainService.validateCoupon(cmd.couponCode(), coupon)
60|       .flatMap(_ -> OrderDomainService.validateStock(cmd.items(), stock))
61|       .map(_ -> OrderDomainService.calculateTotal(cmd.items()))
62|       .map(total -> OrderDomainService.applyDiscount(total, coupon.discountRate()))
63|       .map(discounted -> new Order(
64|         OrderId.generate(), cmd.customerId(), cmd.items(), discounted, OrderStatus.CREATED))
65|       .peek(order -> orderRepository.save(order))
66|       .peek(order -> inventoryService.decreaseStock(cmd.items()))
67|       .peek(order -> emailService.sendOrderCreatedNotification(order));
68|   }
69| }
```

### After: DOP 관점

**[코드 02.3]** DOP: 데이터(Record) + 계산(Calculations) + 조율(Orchestrator)
```java
 1| // package: com.ecommerce.order
 2| // [O] 데이터(Record) + 계산(Calculations) + 조율(Orchestrator)
 3|
 4| // --- Data ---
 5| public record Order(
 6|   OrderId id,
 7|   CustomerId customerId,
 8|   List<OrderItem> items,
 9|   Money totalAmount,
10|   OrderStatus status
11| ) {}
12|
13| // --- Calculations (Pure, Static) ---
14| public class OrderCalculations {
15|   public static Money calculateTotal(List<OrderItem> items) {
16|     return items.stream()
17|       .map(item -> item.unitPrice().multiply(item.quantity().value()))
18|       .reduce(Money.zero(), Money::add);
19|   }
20|
21|   public static Money applyDiscount(Money total, DiscountRate rate) {
22|     return rate.applyTo(total);
23|   }
24| }
25|
26| public class OrderValidations {
27|   public static Result<List<OrderItem>, OrderError> validateStock(
28|     List<OrderItem> items, StockInfo stock
29|   ) {
30|     return items.stream().allMatch(item -> stock.hasEnough(item.productId(), item.quantity()))
31|       ? Result.success(items)
32|       : Result.failure(new OrderError.OutOfStock());
33|   }
34|
35|   public static Result<Coupon, OrderError> validateCoupon(CouponCode code, Coupon coupon) {
36|     return coupon.isValid()
37|       ? Result.success(coupon)
38|       : Result.failure(new OrderError.InvalidCoupon(code));
39|   }
40| }
41|
42| // --- Orchestrator (Impure Shell) ---
43| @RequiredArgsConstructor
44| public class OrderOrchestrator {
45|   private final InventoryService inventoryService;
46|   private final CouponRepository couponRepository;
47|   private final OrderRepository orderRepository;
48|   private final EmailService emailService;
49|
50|   public Result<Order, OrderError> createOrder(CreateOrderRequest request) {
51|     StockInfo stock = inventoryService.getStockInfo(request.productIds());
52|     Coupon coupon = couponRepository.findByCode(request.couponCode());
53|
54|     return OrderValidations.validateStock(request.items(), stock)
55|       .flatMap(_ -> OrderValidations.validateCoupon(request.couponCode(), coupon))
56|       .map(_ -> OrderCalculations.calculateTotal(request.items()))
57|       .map(total -> OrderCalculations.applyDiscount(total, coupon.discountRate()))
58|       .map(discounted -> new Order(
59|         OrderId.generate(), request.customerId(), request.items(), discounted, OrderStatus.CREATED))
60|       .peek(order -> orderRepository.save(order))
61|       .peek(_ -> inventoryService.decreaseStock(request.items()))
62|       .peek(order -> emailService.sendOrderCreatedNotification(order));
63|   }
64| }
```

### 관점 차이 분석
- **공통점**: 둘 다 God Class를 분해하고, 순수 로직과 부수효과를 분리
- **DMMF 주장**: Bounded Context로 맥락별 분리가 핵심. 같은 "사용자"도 맥락마다 다른 타입(`AppUser`, `Customer`, `Payee`)
- **DOP 주장**: 4원칙 기반 분리(데이터/코드, 표준 구조, 불변, 스키마/표현)가 핵심. `*Calculations` 클래스로 로직 분리
- **실무 권장**: 대규모 팀이면 DMMF의 BC 분리 우선, 단일 팀이면 DOP의 Record+Calculations 패턴으로 시작

### 이해를 위한 부가 상세

**DMMF의 Bounded Context**: 같은 단어가 맥락에 따라 다른 의미를 가집니다. "User"가 인증에서는 AppUser(로그인 주체), 주문에서는 Customer(배송 대상), 정산에서는 Payee(수금 대상)입니다. 이를 억지로 합치면 God Class가 됩니다.

**DOP의 4대 원칙**:
1. 코드와 데이터 분리 (Record + static methods)
2. 일반적 형태로 데이터 표현 (표준 컬렉션, 커스텀 래퍼 금지)
3. 불변성 (새 객체 생성으로 변경)
4. 스키마와 표현 분리 (sealed interface = 스키마, record instance = 표현)

### 틀리기/놓치기 쉬운 부분
- **"Spring @Service에 로직 두는 것도 DOP 아닌가?"**: 맞습니다. 이미 Service에 로직, DTO에 데이터를 두는 패턴은 DOP와 유사합니다. DOP는 이를 도메인 레이어까지 확장합니다.
- **"Calculations 클래스는 Util 클래스 아닌가?"**: 차이점은 Util은 범용 유틸리티, Calculations는 특정 도메인 데이터에 대한 순수 함수 모음입니다.
- **"OOP에서도 SRP 지키면 되지 않나?"**: 이론적으로 맞지만, 캡슐화 원칙("데이터와 행위를 한 곳에")이 실무에서 SRP 위반으로 이어지는 경향이 있습니다.

### 꼭 기억할 것
1. **God Class 판별**: 의존성 10개 이상, LOC 500줄 이상, 다중 책임 → 2개 이상이면 분해 대상
2. **분리 원칙**: 데이터는 Record로, 계산은 static 메서드로, 부수효과는 Orchestrator(Shell)로
3. **BC vs Calculations**: 팀/도메인 경계가 중요하면 BC, 코드 수준 분리가 급하면 Calculations 패턴
4. **Pure Function 기준**: 같은 입력 → 같은 출력, 부수효과 없음, 외부 상태 참조 없음

---

## 2. 불변성의 원칙 (Immutability Principle)

### 핵심 개념
- **관련 키워드**: Immutability, Record, Wither Pattern, Defensive Copy
- **통찰**: 가변 객체는 "누가 언제 바꿨는지" 추적이 불가능하다. 불변 객체는 생성 이후 영원히 동일하다.
- **설명**: 가변 상태는 디버깅, 동시성, 캐싱 등 모든 측면에서 복잡성을 가중시킵니다. Java Record는 불변 데이터 객체를 간결하게 표현하며, Wither 패턴으로 상태 변경을 "새 객체 생성"으로 대체합니다.

  함수형 DDD에서는 Entity조차 불변으로 다룹니다. Entity의 상태 변경은 "같은 ID를 가진 새로운 객체 생성"입니다.

### 개념이 아닌 것
- **"불변 = 값을 못 바꿈"**: 바꿀 수 있습니다. 다만 "기존 객체를 수정"이 아니라 "새 객체를 생성"하는 것
- **"불변 = 성능 저하"**: 현대 JVM의 Eden GC는 단명 객체를 효율적으로 처리합니다

### Before: Traditional OOP

**[코드 02.4]** Traditional OOP: 가변 객체: 언제 어디서 상태가 바뀌는지 추적 불가
```java
 1| // package: com.ecommerce.shared
 2| // [X] 가변 객체: 언제 어디서 상태가 바뀌는지 추적 불가
 3| public class MutableOrder {
 4|   private String status;
 5|   private int amount;
 6| 
 7|   public void setStatus(String status) { this.status = status; }
 8|   public void setAmount(int amount) { this.amount = amount; }
 9| }
10| 
11| // 문제 상황
12| MutableOrder order = new MutableOrder();
13| order.setStatus("PAID");
14| order.setAmount(10000);
15| // ... 100줄의 코드 ...
16| processOrder(order);  // 이 시점에 order의 상태가 뭐지?
17|            // 중간에 누가 바꿨을 수도 있음!
18| 
19| // 멀티스레드 환경에서는 더 심각
20| // Thread A: order.setStatus("CANCELLED");
21| // Thread B: order.setAmount(20000);
22| // 결과: 취소된 주문인데 금액이 변경됨 (일관성 파괴)
```
- **의도 및 코드 설명**: 전통적 setter 기반 가변 객체
- **뭐가 문제인가**:
  - 어디서든 `setXxx()`로 상태를 바꿀 수 있어 추적 불가
  - 멀티스레드에서 레이스 컨디션 발생
  - 캐시에 넣어도 외부에서 변경 가능
  - 디버깅 시 "누가 이 값을 바꿨지?" 추적 지옥

### After: Modern Approach

**[코드 02.5]** Modern: Java Record + Wither 패턴: 불변 객체
```java
 1| // package: com.ecommerce.order
 2| // [O] Java Record + Wither 패턴: 불변 객체
 3| public record Order(OrderId id, Money amount, OrderStatus status) {
 4| 
 5|   // Wither: 하나의 필드만 바꾼 새 객체 반환
 6|   public Order withStatus(OrderStatus newStatus) {
 7|     return new Order(id, amount, newStatus);
 8|   }
 9| 
10|   public Order withAmount(Money newAmount) {
11|     return new Order(id, newAmount, status);
12|   }
13| }
14| 
15| // 사용: 원본은 절대 변하지 않음
16| Order paidOrder = new Order(id, Money.krw(10000), new Unpaid());
17| Order shippedOrder = paidOrder.withStatus(new Paid(LocalDateTime.now(), paymentId));
18| 
19| // paidOrder는 여전히 Unpaid
20| // shippedOrder는 Paid
21| // 두 객체가 공존 → 히스토리 추적 가능 (Git 커밋처럼)
```
- **의도 및 코드 설명**: Record는 모든 필드가 `private final`이며 setter가 없음. Wither로 새 객체 생성
- **무엇이 좋아지나**:
  - 생성 이후 상태가 절대 변하지 않아 추론이 쉬움
  - 멀티스레드에서 동기화 없이 안전하게 공유
  - 이전 상태 객체를 보관하면 자동으로 히스토리 형성
  - `equals`/`hashCode`/`toString` 자동 생성

### 이해를 위한 부가 상세

**얕은 불변성 주의**: Record의 필드가 `List`일 때, `List` 참조는 final이지만 내부 원소는 변경 가능합니다. `List.copyOf()`로 깊은 불변성을 확보해야 합니다.

**[코드 02.6]** Order record
```java
1| // package: com.ecommerce.order
2| public record Order(List<OrderItem> items) {
3|   public Order {
4|     items = List.copyOf(items);  // 방어적 복사로 깊은 불변성 확보
5|   }
6| }
```

### 틀리기/놓치기 쉬운 부분
- **"Entity는 상태가 변하니까 mutable이어야 하지 않나?"**: 아닙니다. 상태 변경 = 같은 ID를 가진 새 객체 생성
- **`List.of()` vs `List.copyOf()`**: 이미 불변인 리스트에 `copyOf()`를 쓰면 복사 없이 원본 반환 (효율적)
- **JEP 468**: Java 25에도 `with` expression은 미포함. 수동 Wither 작성 필요

### 꼭 기억할 것
1. **Record = 불변 데이터의 최적 도구**: 모든 필드 `private final`, setter 없음
2. **Wither 패턴**: 값을 "바꾸려면" `withXxx()` 메서드로 새 객체 반환
3. **깊은 불변성**: 컬렉션 필드는 반드시 `List.copyOf()`로 방어적 복사
4. **불변 = 안전**: 스레드 안전, 캐시 안전, 디버깅 용이

---

## 3. 순수 함수와 참조 투명성 (Pure Functions and Referential Transparency)

### 핵심 개념
- **관련 키워드**: Pure Function, Side Effect, Referential Transparency, Determinism
- **통찰**: 순수 함수는 "입력이 같으면 결과가 항상 같고, 세상에 아무 흔적도 남기지 않는" 함수이다.
- **설명**: 순수 함수의 핵심은 참조 투명성입니다. `add(2, 3)`을 숫자 `5`로 대체해도 프로그램의 동작이 바뀌지 않습니다. 이는 테스트(Mock 불필요), 동시성(Lock 불필요), 추론(함수 안만 보면 됨)에서 강력한 이점을 제공합니다.

**[그림 02.2]** 순수 함수와 참조 투명성 (Pure Functions and Referential Transparency)
```
+------------------+         +------------------+
|   Impure Func    |         |   Pure Func      |
|------------------|         |------------------|
| input -> output  |         | input -> output  |
| + DB write       |         | (nothing else)   |
| + console log    |         |                  |
| + global state   |         |                  |
+------------------+         +------------------+
  Side effects make            Same input =
  testing hard                 same output always
```

### 개념이 아닌 것
- **"순수 함수 = static 메서드"**: static이라도 전역 변수를 읽으면 비순수
- **"순수 함수 = 유틸 함수"**: 비즈니스 로직도 순수 함수로 작성 가능

### Before: Traditional OOP

**[코드 02.7]** Traditional OOP: 비순수 함수: 외부 상태에 의존하고, 부수효과 발생
```java
 1| // package: com.ecommerce.order
 2| // [X] 비순수 함수: 외부 상태에 의존하고, 부수효과 발생
 3| public class OrderProcessor {
 4|   private int processedCount = 0;  // 외부 상태
 5| 
 6|   public BigDecimal calculateTotal(Order order) {
 7|     processedCount++;  // 부수효과: 외부 상태 변경
 8|     System.out.println("Processing: " + order.getId());  // 부수효과: I/O
 9|     BigDecimal total = order.getItems().stream()
10|       .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQty())))
11|       .reduce(BigDecimal.ZERO, BigDecimal::add);
12|     auditLog.write("Calculated: " + total);  // 부수효과: I/O
13|     return total;
14|   }
15| }
```
- **의도 및 코드 설명**: 총액 계산이라는 순수한 목적에 카운팅, 로깅 등 부수효과가 섞여 있음
- **뭐가 문제인가**:
  - 같은 Order를 넣어도 processedCount가 달라져 결과 예측 어려움
  - 테스트 시 auditLog Mock 필요
  - 멀티스레드에서 processedCount 레이스 컨디션

### After: Modern Approach

**[코드 02.8]** Modern: 순수 함수: 입력만으로 결과를 만듦, 부수효과 없음
```java
 1| // package: com.ecommerce.order
 2| // [O] 순수 함수: 입력만으로 결과를 만듦, 부수효과 없음
 3| public class OrderCalculations {
 4|   public static Money calculateTotal(List<OrderItem> items) {
 5|     return items.stream()
 6|       .map(item -> item.unitPrice().multiply(item.quantity().value()))
 7|       .reduce(Money.zero(), Money::add);
 8|   }
 9| }
10| 
11| // 부수효과는 Imperative Shell에서 별도 처리
12| public class OrderOrchestrator {
13|   public void processOrder(Order order) {
14|     // Pure: 계산
15|     Money total = OrderCalculations.calculateTotal(order.items());
16|     // Impure: 부수효과 (명시적으로 분리)
17|     auditLog.write("Calculated: " + total);
18|     orderRepository.save(order.withAmount(total));
19|   }
20| }
```
- **의도 및 코드 설명**: 순수 계산과 부수효과를 명시적으로 분리
- **무엇이 좋아지나**:
  - `calculateTotal`은 Mock 없이 `assert(f(input) == expected)` 한 줄로 테스트
  - 멀티스레드에서 안전 (상태 변경 없음)
  - 함수 시그니처만 보면 동작 범위 예측 가능

### 틀리기/놓치기 쉬운 부분
- **`System.out.println()`도 부수효과**: 콘솔(외부 세계)의 상태를 변경하기 때문
- **`LocalDateTime.now()`는 비순수**: 호출 시점에 따라 결과가 달라짐. 테스트 시 시간을 파라미터로 주입해야 함
- **`Random`도 비순수**: 매번 다른 결과를 반환

### 꼭 기억할 것
1. **순수 함수 = 같은 입력 → 같은 출력 + 부수효과 없음**
2. **참조 투명성**: `f(x)`를 결과값으로 대체해도 프로그램 동작이 동일
3. **분리 원칙**: 계산(Pure)은 Calculations 클래스에, 부수효과(Impure)는 Orchestrator/Shell에

---

## 4. Ubiquitous Language와 자기 문서화 코드 (Ubiquitous Language and Self-Documenting Code)

### 핵심 개념
- **관련 키워드**: Ubiquitous Language, Domain Expert, Self-Documenting Code
- **통찰**: 코드가 곧 문서여야 한다. 기획자가 코드를 읽고 "이게 내가 말한 거야"라고 확인할 수 있어야 한다.
- **설명**: DDD의 핵심 중 하나는 기획자(Domain Expert)와 개발자가 같은 용어를 사용하는 것입니다. 코드에서 `if (user.level == 3)` 대신 `if (member.grade() == MemberGrade.VIP)`라고 쓰면, 기획서와 코드 사이의 번역 비용이 사라집니다.

### 개념이 아닌 것
- **"변수명을 길게 쓰는 것"**: 길이보다 도메인 용어와의 일치가 중요
- **"주석을 많이 다는 것"**: 주석이 필요한 코드는 이름이 잘못된 코드

### Before: Traditional OOP

**[코드 02.9]** Traditional OOP: 기획서와 코드의 용어가 다름
```java
 1| // package: com.ecommerce.shared
 2| // [X] 기획서와 코드의 용어가 다름
 3| public class Util {
 4|   public static double calc(double a, String c) {
 5|     if (c != null && !c.isEmpty()) {
 6|       return a * 0.9;
 7|     }
 8|     return a;
 9|   }
10| }
11| 
12| // 호출: 무엇을 하는지 알 수 없음
13| double result = Util.calc(order.getAmt(), req.getCode());
```
- **의도 및 코드 설명**: 할인 적용 로직이지만 이름에서 의도를 파악할 수 없음
- **뭐가 문제인가**:
  - `a`, `c`가 무엇인지 코드만 보고 알 수 없음
  - 기획서: "쿠폰을 적용하면 할인된 금액이 계산됩니다" → 코드에서 어디인지 찾을 수 없음
  - 새 개발자가 합류하면 코드를 이해하는 데 시간 소요

### After: Modern Approach

**[코드 02.10]** Modern: 기획서의 용어가 그대로 코드에 등장
```java
 1| // package: com.ecommerce.coupon
 2| // [O] 기획서의 용어가 그대로 코드에 등장
 3| public class CouponCalculations {
 4|   public static DiscountedPrice applyCoupon(OriginalPrice price, Coupon coupon) {
 5|     Money discounted = coupon.discountRate().applyTo(price.value());
 6|     return new DiscountedPrice(discounted);
 7|   }
 8| }
 9| 
10| // 호출: 기획서를 읽은 사람이면 바로 이해 가능
11| DiscountedPrice finalPrice = CouponCalculations.applyCoupon(originalPrice, coupon);
```
- **의도 및 코드 설명**: 도메인 용어(`applyCoupon`, `OriginalPrice`, `DiscountedPrice`)가 코드에 직접 등장
- **무엇이 좋아지나**:
  - 기획자가 코드 리뷰에 참여 가능
  - 새 개발자의 온보딩 시간 단축
  - 도메인 지식이 코드에 캡처되어 문서화 자동 달성

### 틀리기/놓치기 쉬운 부분
- **"영어로 코딩해야 하는데 한국어 도메인 용어는?"**: 팀 합의로 용어 사전(Glossary)을 만들고, 코드에서는 영어 번역을 일관되게 사용
- **"타입을 너무 많이 만들면 오버엔지니어링?"**: 도메인 전문가가 구분하는 개념은 타입으로 분리할 가치가 있음

### 꼭 기억할 것
1. **용어 사전**: 기획 용어와 코드 타입의 1:1 매핑 테이블을 유지
2. **자기 문서화**: 코드의 이름만 읽어도 비즈니스 의도가 드러나야 함
3. **번역 비용 제거**: `user.level == 3` → `member.grade() == MemberGrade.VIP`

---

## 5. OOP에 대한 태도 차이 (Attitudes Toward OOP)

### 핵심 개념
- **관련 키워드**: OOP vs FP, Paradigm Shift, Encapsulation vs Transparency
- **통찰**: DMMF는 "OOP를 FP로 강화"하고, DOP는 "OOP 캡슐화를 근본적으로 대체"한다.
- **설명**: 두 책 모두 OOP의 문제를 지적하지만, 해법의 강도가 다릅니다. DMMF는 OOP의 뼈대(클래스, 인터페이스)를 유지하면서 FP 원칙(불변성, 순수함수)을 주입합니다. DOP는 OOP의 핵심 전제("데이터와 행위를 함께 캡슐화")를 정면으로 부정하고, 데이터와 코드를 완전히 분리합니다.

### After: DMMF 관점 (DDD + FP)

**[코드 02.11]** DMMF: OOP 구조를 유지하면서 FP 원칙 적용
```java
 1| // package: com.ecommerce.shared
 2| // DMMF: OOP 구조를 유지하면서 FP 원칙 적용
 3| // Record에 비즈니스 메서드를 포함할 수 있음 (Value Object)
 4| public record Money(BigDecimal amount, Currency currency) {
 5|   public Money add(Money other) {
 6|     if (this.currency != other.currency)
 7|       throw new IllegalArgumentException("통화 불일치");
 8|     return new Money(this.amount.add(other.amount), this.currency);
 9|   }
10| 
11|   public Money multiply(int factor) {
12|     return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
13|   }
14| }
```

### After: DOP 관점

**[코드 02.12]** DOP: 데이터와 코드를 완전 분리
```java
 1| // package: com.ecommerce.shared
 2| // DOP: 데이터와 코드를 완전 분리
 3| // Record에는 데이터만, 로직은 Calculations 클래스로
 4| public record Money(BigDecimal amount, Currency currency) {}
 5| 
 6| public class MoneyCalculations {
 7|   public static Money add(Money a, Money b) {
 8|     if (a.currency() != b.currency())
 9|       throw new IllegalArgumentException("통화 불일치");
10|     return new Money(a.amount().add(b.amount()), a.currency());
11|   }
12| 
13|   public static Money multiply(Money money, int factor) {
14|     return new Money(money.amount().multiply(BigDecimal.valueOf(factor)), money.currency());
15|   }
16| }
```

### 관점 차이 분석
- **공통점**: 둘 다 불변성, 순수 함수, 타입 안전성을 추구
- **DMMF 주장**: 도메인 개념에 밀접한 연산은 타입에 포함되어야 자연스러움. `money.add(other)`가 `MoneyCalculations.add(a, b)`보다 직관적
- **DOP 주장**: 로직이 데이터에 섞이면 재사용성 저하. `MoneyCalculations`는 다양한 컨텍스트에서 독립적으로 테스트/재사용 가능
- **실무 권장**: 단순 파생값/연산(add, multiply)은 Record에 포함해도 괜찮지만, 외부 의존성이 필요하거나 복잡한 로직은 외부 함수로 분리

### 틀리기/놓치기 쉬운 부분
- **DOP도 Record에 메서드를 허용하는 경우**: compact constructor 검증, wither 메서드, 단순 파생값(`area()` 등)
- **DMMF의 한계**: Spring DI 패턴과 스타일이 충돌할 수 있음 (의존성을 함수 파라미터로 전달)

### 꼭 기억할 것
1. **DMMF**: "OOP + FP". Record에 도메인 연산 포함 가능
2. **DOP**: "OOP 대체". Record = 순수 데이터, 로직 = Calculations
3. **경계선**: 외부 의존성 필요 시 → 외부 함수로 분리 (양측 동의)
