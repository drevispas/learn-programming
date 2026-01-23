# Appendix B: Pattern Quick Reference (패턴 요약표)

> 전체 문서에서 다룬 모든 패턴의 빠른 참조표. 각 패턴의 이름, 한 줄 설명, Java 25 미니 코드 예제, 관련 토픽을 포함한다.

---

## 핵심 패턴 요약

| # | Pattern | One-line Description | Topic |
|---|---------|---------------------|-------|
| 1 | Value Object | 원시 타입 대신 도메인 의미를 가진 불변 래퍼 타입 | 02 |
| 2 | Compact Constructor | Record 생성 시 불변 조건을 검증하는 축약 생성자 | 02 |
| 3 | Wither Pattern | 불변 객체의 일부 필드만 변경한 새 인스턴스 반환 | 02 |
| 4 | Sum Type (ADT) | sealed interface로 가능한 상태를 OR로 열거 | 03 |
| 5 | Product Type | record로 필수 필드를 AND로 결합 | 03 |
| 6 | Exhaustive Pattern Matching | sealed interface의 모든 case를 switch로 망라 처리 | 03 |
| 7 | Record Pattern | switch에서 Record 필드를 분해하여 바인딩 | 03 |
| 8 | State Machine | sealed interface로 상태 전이를 타입으로 강제 | 04 |
| 9 | Phantom Type | 런타임 데이터 없이 컴파일 타임 상태를 추적하는 타입 | 04 |
| 10 | Total Function | 모든 입력에 대해 유효한 결과를 반환 (예외 대신 Result) | 05 |
| 11 | Result Type | Success/Failure로 연산 결과를 명시적 표현 | 05, 06 |
| 12 | Railway-Oriented Programming | flatMap 체인으로 성공/실패 경로 분리 | 06, 08 |
| 13 | Pipeline Pattern | 순수 함수들을 flatMap/map으로 순차 연결 | 06 |
| 14 | Functional Core / Imperative Shell | 순수 로직(Core)과 I/O(Shell)를 분리 | 06, 13 |
| 15 | Validation (Applicative) | 모든 에러를 수집하는 병렬 검증 패턴 | 07, 08 |
| 16 | Monoid (Assoc + Identity) | 결합법칙 + 항등원으로 안전한 reduce 보장 | 11 |
| 17 | Idempotent Operation | 멱등성 키로 중복 실행 방지 | 11 |
| 18 | Rule as Data | 비즈니스 규칙을 sealed interface(ADT)로 데이터화 | 12 |
| 19 | Rule Engine | 규칙 데이터를 패턴 매칭으로 평가하는 순수 함수 | 12 |
| 20 | Interpreter Separation | 규칙 정의(data)와 실행(interpreter)을 분리 | 12 |
| 21 | Entity-Record Mapper | JPA Entity와 Domain Record 간 변환 통역사 | 13 |
| 22 | Gradual Migration | 복잡한 로직부터 순수 함수로 점진 추출 | 13 |
| 23 | Bounded Context Model | 같은 도메인을 컨텍스트별 독립 모델로 분리 | 01, 13 |
| 24 | Defensive Copy | Record의 컬렉션 필드를 List.copyOf로 보호 | 02 |
| 25 | Null Object via ADT | null 대신 NoDiscount(), Empty() 등 ADT case 사용 | 03, 11 |

---

## 미니 코드 예제

### 1. Value Object
```java
public record Money(BigDecimal amount, Currency currency) {
    public Money { if (amount.signum() < 0) throw new IllegalArgumentException(); }
    public Money add(Money o) { return new Money(amount.add(o.amount), currency); }
}
```

### 2. Compact Constructor
```java
public record Email(String value) {
    public Email { if (!value.contains("@")) throw new IllegalArgumentException("유효하지 않은 이메일"); }
}
```

### 3. Wither Pattern
```java
public record Order(OrderId id, OrderStatus status, Money total) {
    public Order withStatus(OrderStatus s) { return new Order(id, s, total); }
}
```

### 4. Sum Type (ADT)
```java
public sealed interface OrderStatus permits Unpaid, Paid, Shipped, Cancelled {}
public record Unpaid(LocalDateTime deadline) implements OrderStatus {}
public record Paid(LocalDateTime paidAt, TransactionId txId) implements OrderStatus {}
```

### 5. Product Type
```java
public record OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
    public Money lineTotal() { return unitPrice.multiply(quantity.value()); }
}
```

### 6. Exhaustive Pattern Matching
```java
String label = switch (status) {
    case Unpaid u -> "결제 대기";
    case Paid p -> "결제 완료";
    case Shipped s -> "배송 중";
    case Cancelled c -> "취소됨";
};
```

### 7. Record Pattern
```java
return switch (status) {
    case Paid(var at, var txId) -> "결제일: " + at + ", TX: " + txId;
    case Unpaid _ -> "미결제";
    case Shipped _, Cancelled _ -> "기타";
};
```

### 8. State Machine
```java
public Result<Order, OrderError> ship(TrackingNumber tracking) {
    return switch (status) {
        case Paid p -> Result.success(withStatus(new Shipped(LocalDateTime.now(), tracking)));
        case Unpaid _, Shipped _, Cancelled _ -> Result.failure(new CannotShip(id));
    };
}
```

### 9. Phantom Type
```java
public record Email<S extends EmailState>(String value) {}
sealed interface EmailState permits Unverified, Verified {}
Email<Verified> verify(Email<Unverified> email, String code) { /*...*/ }
```

### 10. Total Function
```java
public static Result<Money, DivisionError> safeDivide(Money amount, int divisor) {
    if (divisor == 0) return Result.failure(new DivisionByZero());
    return Result.success(amount.divide(divisor));
}
```

### 11. Result Type
```java
public sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}
}
```

### 12. Railway-Oriented Programming
```java
Result<Order, OrderError> result = validate(cmd)
    .flatMap(v -> applyPricing(v))
    .flatMap(p -> processPayment(p))
    .map(paid -> createOrder(paid));
```

### 13. Pipeline Pattern
```java
public Result<Order, OrderError> execute(CreateOrderCommand cmd) {
    return OrderDomainService.validate(cmd, member, inventory)
        .flatMap(validated -> applyPricing(validated, coupon))
        .flatMap(priced -> charge(priced))
        .map(paid -> save(paid));
}
```

### 14. Functional Core / Imperative Shell
```java
// Core (pure): static Money calculateTotal(List<Item> items, Discount d) { ... }
// Shell (I/O): items = repo.findAll(); total = calculateTotal(items, d); repo.save(order);
```

### 15. Validation (Applicative)
```java
Validation<User, List<Error>> user = Validation.combine3(
    validateName(name), validateEmail(email), validatePhone(phone), User::new
); // 모든 에러 수집
```

### 16. Monoid (Assoc + Identity)
```java
Money total = orderTotals.parallelStream()
    .reduce(Money.zero(Currency.KRW), Money::add);
// 항등원 + 결합법칙 -> 병렬 안전
```

### 17. Idempotent Operation
```java
public Result<Payment, Error> process(PaymentId id, PaymentRequest req) {
    if (paymentRepo.exists(id)) return Result.success(paymentRepo.findById(id).get());
    return doProcess(req).map(p -> { paymentRepo.save(id, p); return p; });
}
```

### 18. Rule as Data
```java
public sealed interface Rule {
    record Equals(String attr, String value) implements Rule {}
    record GTE(String attr, int threshold) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
}
```

### 19. Rule Engine
```java
public static boolean evaluate(Rule rule, Customer c) {
    return switch (rule) {
        case Equals(var a, var v) -> getAttr(c, a).equals(v);
        case GTE(var a, var t) -> getInt(c, a) >= t;
        case And(var l, var r) -> evaluate(l, c) && evaluate(r, c);
    };
}
```

### 20. Interpreter Separation
```java
// Rule = WHAT (data), Interpreter = HOW (function)
boolean result = RuleEvaluator.evaluate(rule, customer);   // 평가
String desc = RuleExplainer.explain(rule);                 // 설명
String sql = RuleToSql.toWhere(rule);                      // SQL 변환
```

### 21. Entity-Record Mapper
```java
public static Order toDomain(OrderEntity e) {
    OrderStatus status = switch (e.getStatus()) {
        case PAID -> new Paid(e.getPaidAt(), new TransactionId(e.getPaymentId()));
        case PENDING -> new Unpaid(e.getCreatedAt());
    };
    return new Order(new OrderId(e.getId().toString()), mapItems(e.getItems()), status);
}
```

### 22. Gradual Migration
```java
// Step 1: 순수 함수 추출
public class PriceCalc { static Money total(List<Item> items, Discount d) { ... } }
// Step 2: Service에서 호출
Money total = PriceCalc.total(items, discount); // 기존 Service 구조 유지!
```

### 23. Bounded Context Model
```java
public record DisplayProduct(ProductId id, String name, Money price) {}     // 전시용
public record InventoryProduct(ProductId id, int stock) {}                  // 재고용
public record SettlementProduct(ProductId id, Money supplyPrice) {}         // 정산용
```

### 24. Defensive Copy
```java
public record Cart(List<CartItem> items) {
    public Cart { items = List.copyOf(items); } // 외부 변경 차단
}
```

### 25. Null Object via ADT
```java
public sealed interface Discount {
    record NoDiscount() implements Discount {}    // null 대신 항등원
    record Percentage(int rate) implements Discount {}
    record FixedAmount(Money amt) implements Discount {}
}
```

---

## 패턴 조합 가이드

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
