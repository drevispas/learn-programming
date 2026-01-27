# Appendix B: Pattern Quick Reference (패턴 요약표)

> 전체 문서에서 다룬 모든 패턴의 빠른 참조표. 각 패턴의 이름, 한 줄 설명, Java 25 미니 코드 예제, 관련 토픽을 포함한다.

---

## 핵심 패턴 요약

**[표 B.1]** 핵심 패턴 요약
| # | Pattern | One-line Description | Topic |
|---|---------|---------------------|-------|
| 1 | Value Object | 원시 타입 대신 도메인 의미를 가진 불변 래퍼 타입 | 03 |
| 2 | Compact Constructor | Record 생성 시 불변 조건을 검증하는 축약 생성자 | 03 |
| 3 | Wither Pattern | 불변 객체의 일부 필드만 변경한 새 인스턴스 반환 | 03 |
| 4 | Sum Type (ADT) | sealed interface로 가능한 상태를 OR로 열거 | 04 |
| 5 | Product Type | record로 필수 필드를 AND로 결합 | 04 |
| 6 | Exhaustive Pattern Matching | sealed interface의 모든 case를 switch로 망라 처리 | 04 |
| 7 | Record Pattern | switch에서 Record 필드를 분해하여 바인딩 | 04 |
| 8 | State Machine | sealed interface로 상태 전이를 타입으로 강제 | 05 |
| 9 | Phantom Type | 런타임 데이터 없이 컴파일 타임 상태를 추적하는 타입 | 05 |
| 10 | Total Function | 모든 입력에 대해 유효한 결과를 반환 (예외 대신 Result) | 06 |
| 11 | Result Type | Success/Failure로 연산 결과를 명시적 표현 | 06 |
| 12 | Railway-Oriented Programming | flatMap 체인으로 성공/실패 경로 분리 | 06, 10 |
| 13 | Pipeline Pattern | 순수 함수들을 flatMap/map으로 순차 연결 | 07 |
| 14 | Functional Core / Imperative Shell | 순수 로직(Core)과 I/O(Shell)를 분리 | 08, 14 |
| 15 | Validation (Applicative) | 모든 에러를 수집하는 병렬 검증 패턴 | 08, 10 |
| 16 | Monoid (Assoc + Identity) | 결합법칙 + 항등원으로 안전한 reduce 보장 | 12 |
| 17 | Idempotent Operation | 멱등성 키로 중복 실행 방지 | 12 |
| 18 | Rule as Data | 비즈니스 규칙을 sealed interface(ADT)로 데이터화 | 13 |
| 19 | Rule Engine | 규칙 데이터를 패턴 매칭으로 평가하는 순수 함수 | 13 |
| 20 | Interpreter Separation | 규칙 정의(data)와 실행(interpreter)을 분리 | 13 |
| 21 | Entity-Record Mapper | JPA Entity와 Domain Record 간 변환 통역사 | 14 |
| 22 | Gradual Migration | 복잡한 로직부터 순수 함수로 점진 추출 | 14 |
| 23 | Bounded Context Model | 같은 도메인을 컨텍스트별 독립 모델로 분리 | 01, 14 |
| 24 | Defensive Copy | Record의 컬렉션 필드를 List.copyOf로 보호 | 03 |
| 25 | Null Object via ADT | null 대신 NoDiscount(), Empty() 등 ADT case 사용 | 04, 12 |
| 26 | Event Store | 상태 변화를 이벤트로 저장하여 완전한 이력 보존 | 15 |
| 27 | Aggregate Reconstitution | fold(initialState, events)로 현재 상태 재구성 | 15 |
| 28 | Decider Pattern | (State, Command) -> Result<Events, Error> 순수 함수 | 15 |
| 29 | Projection (CQRS) | 이벤트 스트림을 읽기 전용 모델로 변환 | 15 |
| 30 | Aggregate Root Pattern | 외부에서는 Root만 참조, 내부 Entity 보호 | 01 |
| 31 | Repository Pattern (DDD) | Aggregate 단위 저장/조회 추상화 (DAO와 구분) | 01 |
| 32 | Domain Service Pattern | 여러 Aggregate 협력이 필요한 상태 없는 순수 함수 | 01 |

---

## 미니 코드 예제

### 1. Value Object

**[코드 B.1]** Money record
```java
1| // package: com.ecommerce.shared
2| public record Money(BigDecimal amount, Currency currency) {
3|   public Money { if (amount.signum() < 0) throw new IllegalArgumentException(); }
4|   public Money add(Money o) { return new Money(amount.add(o.amount), currency); }
5| }
```

### 2. Compact Constructor

**[코드 B.2]** Email record
```java
1| // package: com.ecommerce.shared
2| public record Email(String value) {
3|   public Email { if (!value.contains("@")) throw new IllegalArgumentException("유효하지 않은 이메일"); }
4| }
```

### 3. Wither Pattern

**[코드 B.3]** Order record
```java
1| // package: com.ecommerce.order
2| public record Order(OrderId id, OrderStatus status, Money total) {
3|   public Order withStatus(OrderStatus s) { return new Order(id, s, total); }
4| }
```

### 4. Sum Type (ADT)

**[코드 B.4]** OrderStatus interface
```java
1| // package: com.ecommerce.order
2| public sealed interface OrderStatus permits Unpaid, Paid, Shipped, Cancelled {}
3| public record Unpaid(LocalDateTime deadline) implements OrderStatus {}
4| public record Paid(LocalDateTime paidAt, TransactionId txId) implements OrderStatus {}
```

### 5. Product Type

**[코드 B.5]** OrderLine record
```java
1| // package: com.ecommerce.shared
2| public record OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
3|   public Money lineTotal() { return unitPrice.multiply(quantity.value()); }
4| }
```

### 6. Exhaustive Pattern Matching

**[코드 B.6]** Exhaustive Pattern Matching
```java
1| // package: com.ecommerce.shared
2| String label = switch (status) {
3|   case Unpaid u -> "결제 대기";
4|   case Paid p -> "결제 완료";
5|   case Shipped s -> "배송 중";
6|   case Cancelled c -> "취소됨";
7| };
```

### 7. Record Pattern

**[코드 B.7]** Record Pattern
```java
1| // package: com.ecommerce.shared
2| return switch (status) {
3|   case Paid(var at, var txId) -> "결제일: " + at + ", TX: " + txId;
4|   case Unpaid _ -> "미결제";
5|   case Shipped _, Cancelled _ -> "기타";
6| };
```

### 8. State Machine

**[코드 B.8]** State Machine
```java
1| // package: com.ecommerce.shared
2| public Result<Order, OrderError> ship(TrackingNumber tracking) {
3|   return switch (status) {
4|     case Paid p -> Result.success(withStatus(new Shipped(LocalDateTime.now(), tracking)));
5|     case Unpaid _, Shipped _, Cancelled _ -> Result.failure(new CannotShip(id));
6|   };
7| }
```

### 9. Phantom Type

**[코드 B.9]** Email record
```java
1| // package: com.ecommerce.shared
2| public record Email<S extends EmailState>(String value) {}
3| sealed interface EmailState permits Unverified, Verified {}
4| Email<Verified> verify(Email<Unverified> email, String code) { /*...*/ }
```

### 10. Total Function

**[코드 B.10]** Total Function
```java
1| // package: com.ecommerce.shared
2| public static Result<Money, DivisionError> safeDivide(Money amount, int divisor) {
3|   if (divisor == 0) return Result.failure(new DivisionByZero());
4|   return Result.success(amount.divide(divisor));
5| }
```

### 11. Result Type

**[코드 B.11]** Result interface
```java
1| // package: com.ecommerce.shared
2| public sealed interface Result<S, F> {
3|   record Success<S, F>(S value) implements Result<S, F> {}
4|   record Failure<S, F>(F error) implements Result<S, F> {}
5| }
```

### 12. Railway-Oriented Programming

**[코드 B.12]** Railway-Oriented Programming
```java
1| // package: com.ecommerce.order
2| Result<Order, OrderError> result = validate(cmd)
3|   .flatMap(v -> applyPricing(v))
4|   .flatMap(p -> processPayment(p))
5|   .map(paid -> createOrder(paid));
```

### 13. Pipeline Pattern

**[코드 B.13]** Pipeline Pattern
```java
1| // package: com.ecommerce.order
2| public Result<Order, OrderError> execute(CreateOrderCommand cmd) {
3|   return OrderDomainService.validate(cmd, member, inventory)
4|     .flatMap(validated -> applyPricing(validated, coupon))
5|     .flatMap(priced -> charge(priced))
6|     .map(paid -> save(paid));
7| }
```

### 14. Functional Core / Imperative Shell

**[코드 B.14]** Core (pure): static Money calculateTotal(List<Item> items, Discount d) { ... }
```java
1| // package: com.ecommerce.coupon
2| // Core (pure): static Money calculateTotal(List<Item> items, Discount d) { ... }
3| // Shell (I/O): items = repo.findAll(); total = calculateTotal(items, d); repo.save(order);
```

### 15. Validation (Applicative)

**[코드 B.15]** Validation (Applicative)
```java
1| // package: com.ecommerce.auth
2| Validation<User, List<Error>> user = Validation.combine3(
3|   validateName(name), validateEmail(email), validatePhone(phone), User::new
4| ); // 모든 에러 수집
```

### 16. Monoid (Assoc + Identity)

**[코드 B.16]** Monoid (Assoc + Identity)
```java
1| // package: com.ecommerce.shared
2| Money total = orderTotals.parallelStream()
3|   .reduce(Money.zero(Currency.KRW), Money::add);
4| // 항등원 + 결합법칙 -> 병렬 안전
```

### 17. Idempotent Operation

**[코드 B.17]** Idempotent Operation
```java
1| // package: com.ecommerce.payment
2| public Result<Payment, Error> process(PaymentId id, PaymentRequest req) {
3|   if (paymentRepo.exists(id)) return Result.success(paymentRepo.findById(id).get());
4|   return doProcess(req).map(p -> { paymentRepo.save(id, p); return p; });
5| }
```

### 18. Rule as Data

**[코드 B.18]** Rule interface
```java
1| // package: com.ecommerce.rule
2| public sealed interface Rule {
3|   record Equals(String attr, String value) implements Rule {}
4|   record GTE(String attr, int threshold) implements Rule {}
5|   record And(Rule left, Rule right) implements Rule {}
6| }
```

### 19. Rule Engine

**[코드 B.19]** Rule Engine
```java
1| // package: com.ecommerce.rule
2| public static boolean evaluate(Rule rule, Customer c) {
3|   return switch (rule) {
4|     case Equals(var a, var v) -> getAttr(c, a).equals(v);
5|     case GTE(var a, var t) -> getInt(c, a) >= t;
6|     case And(var l, var r) -> evaluate(l, c) && evaluate(r, c);
7|   };
8| }
```

### 20. Interpreter Separation

**[코드 B.20]** Rule = WHAT (data), Interpreter = HOW (function)
```java
1| // package: com.ecommerce.rule
2| // Rule = WHAT (data), Interpreter = HOW (function)
3| boolean result = RuleEvaluator.evaluate(rule, customer);   // 평가
4| String desc = RuleExplainer.explain(rule);                 // 설명
5| String sql = RuleToSql.toWhere(rule);                      // SQL 변환
```

### 21. Entity-Record Mapper

**[코드 B.21]** Entity-Record Mapper
```java
1| // package: com.ecommerce.order
2| public static Order toDomain(OrderEntity e) {
3|   OrderStatus status = switch (e.getStatus()) {
4|     case PAID -> new Paid(e.getPaidAt(), new TransactionId(e.getPaymentId()));
5|     case PENDING -> new Unpaid(e.getCreatedAt());
6|   };
7|   return new Order(new OrderId(e.getId().toString()), mapItems(e.getItems()), status);
8| }
```

### 22. Gradual Migration

**[코드 B.22]** Step 1: 순수 함수 추출
```java
1| // package: com.ecommerce.shared
2| // Step 1: 순수 함수 추출
3| public class PriceCalc { static Money total(List<Item> items, Discount d) { ... } }
4| // Step 2: Service에서 호출
5| Money total = PriceCalc.total(items, discount); // 기존 Service 구조 유지!
```

### 23. Bounded Context Model

**[코드 B.23]** DisplayProduct record
```java
1| // package: com.ecommerce.product
2| public record DisplayProduct(ProductId id, String name, Money price) {}     // 전시용
3| public record InventoryProduct(ProductId id, int stock) {}                  // 재고용
4| public record SettlementProduct(ProductId id, Money supplyPrice) {}         // 정산용
```

### 24. Defensive Copy

**[코드 B.24]** Cart record
```java
1| // package: com.ecommerce.shared
2| public record Cart(List<CartItem> items) {
3|   public Cart { items = List.copyOf(items); } // 외부 변경 차단
4| }
```

### 25. Null Object via ADT

**[코드 B.25]** Discount interface
```java
1| // package: com.ecommerce.coupon
2| public sealed interface Discount {
3|   record NoDiscount() implements Discount {}    // null 대신 항등원
4|   record Percentage(int rate) implements Discount {}
5|   record FixedAmount(Money amt) implements Discount {}
6| }
```

### 26. Event Store

**[코드 B.26]** OrderEvent interface
```java
1| // package: com.ecommerce.order
2| public sealed interface OrderEvent {
3|   record OrderPlaced(OrderId id, List<OrderItem> items, LocalDateTime at) implements OrderEvent {}
4|   record OrderPaid(OrderId id, PaymentId paymentId, LocalDateTime at) implements OrderEvent {}
5|   record OrderShipped(OrderId id, TrackingNumber tracking, LocalDateTime at) implements OrderEvent {}
6| }
```

### 27. Aggregate Reconstitution

**[코드 B.27]** Aggregate Reconstitution
```java
1| // package: com.ecommerce.order
2| public static OrderState reconstitute(List<OrderEvent> events) {
3|   return events.stream()
4|     .reduce(OrderState.initial(), OrderAggregate::apply, (s1, s2) -> s2);
5| }
```

### 28. Decider Pattern

**[코드 B.28]** Decider Pattern
```java
1| // package: com.ecommerce.order
2| public static Result<List<OrderEvent>, OrderError> decide(OrderState state, OrderCommand cmd) {
3|   return switch (cmd) {
4|     case PlaceOrder c -> handlePlace(state, c);
5|     case PayOrder c -> handlePay(state, c);
6|   };
7| }
```

### 29. Projection (CQRS)

**[코드 B.29]** Projection (CQRS)
```java
1| // package: com.ecommerce.order
2| public void on(OrderEvent event) {
3|   switch (event) {
4|     case OrderPlaced e -> summaryRepo.save(new OrderSummary(e.id(), "PLACED", e.at()));
5|     case OrderPaid e -> summaryRepo.updateStatus(e.id(), "PAID", e.at());
6|   }
7| }
```

### 30. Aggregate Root Pattern

**[코드 B.30]** Aggregate Root Pattern
```java
1| // package: com.ecommerce.order
2| public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {
3|   public Order addItem(ProductId p, Quantity q, Money price) {
4|     var newItem = new OrderItem(ItemId.generate(), p, q, price);
5|     return new Order(id, List.copyOf(append(items, newItem)), status);
6|   }
7| }
```

### 31. Repository Pattern (DDD)

**[코드 B.31]** Repository Pattern (DDD)
```java
1| // package: com.ecommerce.order
2| public interface OrderRepository {
3|   Optional<Order> findById(OrderId id);  // Domain type 반환
4|   void save(Order order);                 // Aggregate 단위 저장
5| }
```

### 32. Domain Service Pattern

**[코드 B.32]** Domain Service Pattern
```java
1| // package: com.ecommerce.order
2| public class OrderDomainService {
3|   public static Result<Order, OrderError> validateAndCreate(
4|     CreateOrderCommand cmd, Customer customer, StockInfo stock) {
5|     return validateStock(cmd.items(), stock).map(items -> new Order(...));
6|   }
7| }
```

---

## 패턴 조합 가이드

**[표 B.2]** 패턴 조합 가이드
| 상황 | 추천 패턴 조합 |
|------|---------------|
| 단순 CRUD | Value Object + Compact Constructor |
| 다중 상태 도메인 | Sum Type + State Machine + Exhaustive Matching |
| 실패 가능 연산 | Result + flatMap Pipeline |
| 폼 검증 | Validation (Applicative) |
| 복잡한 워크플로우 | Pipeline + Functional Core / Imperative Shell |
| 자주 변경되는 규칙 | Rule Engine + Dynamic Loading |
| JPA 기존 코드 | Entity-Record Mapper + Gradual Migration |
| 분산/병렬 처리 | Monoid + Idempotent Operation |
| 도메인 모델링 | Bounded Context + ADT + Value Object |
| 이력 추적 필수 도메인 | Event Store + Aggregate Reconstitution + Decider |
| 복잡한 조회 요구 | Event Sourcing + Projection (CQRS) |
