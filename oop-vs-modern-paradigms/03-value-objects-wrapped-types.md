# 03. Value Objects / Wrapped Types (값 객체와 래핑 타입)

> **Sources**: DMMF Ch.2 (Wrapped Objects), DOP Ch.2 (Identity vs Value)

---

## 1. Primitive Obsession 해결 (Solving Primitive Obsession)

### 핵심 개념
- **관련 키워드**: Primitive Obsession, Type Safety, Value Object, Compile-time Verification
- **통찰**: `String`과 `int`는 "라벨 없는 약병"이다. 컴파일러가 구분하지 못하면 인간도 실수한다.
- **설명**: Primitive Obsession은 도메인 개념(이메일, 금액, ID)을 원시 타입(`String`, `double`, `long`)으로 표현하는 안티패턴입니다. 파라미터 순서를 바꿔도 컴파일이 성공하고, 음수 금액이나 빈 이메일도 시스템에 들어올 수 있습니다.

  Java Record를 사용하면 도메인 타입을 한 줄로 정의할 수 있어, "타입이 너무 많아진다"는 우려가 크게 줄어듭니다.

**[그림 03.1]** Primitive Obsession 해결 (Solving Primitive Obsession)
```
Primitive Obsession:

  processPayment(String, String, String, double, String)
                   ^        ^       ^       ^       ^
                   |        |       |       |       |
              customerId  email  productId amount couponCode

  All are String/double -> compiler cannot catch swap errors


Type-Safe Design:

  processPayment(CustomerId, Email, ProductId, Money, CouponCode)

  Swap any two -> COMPILE ERROR
```

### 개념이 아닌 것
- **"모든 필드를 래핑해야 한다"**: 도메인에서 구분이 필요한 개념만 래핑. `firstName`과 `lastName`은 같은 `String`이어도 혼동 위험이 낮으면 그대로 사용 가능
- **"Value Object = getter만 있는 클래스"**: Record의 자동 생성 accessor, equals, hashCode가 핵심

### Before: Traditional OOP

**[코드 03.1]** Traditional OOP: Primitive Obsession: 모든 것이 String과 double
```java
 1| // package: com.ecommerce.payment
 2| // [X] Primitive Obsession: 모든 것이 String과 double
 3| public class PaymentService {
 4|   public void processPayment(
 5|     String customerId,
 6|     String email,
 7|     String productId,
 8|     double amount,
 9|     String couponCode
10|   ) {
11|     // 검증이 메서드 곳곳에 흩어짐
12|     if (email == null || !email.contains("@"))
13|       throw new IllegalArgumentException("Invalid email");
14|     if (amount <= 0)
15|       throw new IllegalArgumentException("Invalid amount");
16|     // ... 비즈니스 로직
17|   }
18| }
19| 
20| // 호출 시 순서를 바꿔도 컴파일 성공!
21| paymentService.processPayment(
22|   "user@email.com",   // customerId 자리에 email!
23|   "COUPON-2024",      // email 자리에 couponCode!
24|   "12345",            // productId? customerId?
25|   -500.0,             // 음수 금액도 OK
26|   null                // null도 OK
27| );
```
- **의도 및 코드 설명**: 모든 파라미터가 원시 타입이라 컴파일러가 실수를 잡지 못함
- **뭐가 문제인가**:
  - 파라미터 순서 바꿔도 컴파일 성공 (런타임 버그)
  - 검증 로직이 분산되어 유지보수 어려움
  - 코드에서 `String`이 무엇을 의미하는지 파악 불가
  - 음수 금액, 빈 문자열 등 무효한 값이 시스템에 침투

### After: DMMF 관점 (DDD + FP)

**[코드 03.2]** DMMF: 도메인 전용 Value Object + 비즈니스 연산 포함
```java
 1| // package: com.ecommerce.shared
 2| // [O] DMMF: 도메인 전용 Value Object + 비즈니스 연산 포함
 3| public record CustomerId(String value) {
 4|   public CustomerId {
 5|     Objects.requireNonNull(value, "고객 ID는 필수입니다");
 6|   }
 7| }
 8| 
 9| public record EmailAddress(String value) {
10|   private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
11|   public EmailAddress {
12|     if (value == null || !PATTERN.matcher(value).matches())
13|       throw new IllegalArgumentException("유효하지 않은 이메일: " + value);
14|   }
15| }
16| 
17| public record Money(BigDecimal amount, Currency currency) {
18|   public Money {
19|     Objects.requireNonNull(amount);
20|     Objects.requireNonNull(currency);
21|     if (amount.compareTo(BigDecimal.ZERO) < 0)
22|       throw new IllegalArgumentException("금액은 음수 불가: " + amount);
23|   }
24| 
25|   // DMMF: 도메인 연산을 타입에 포함
26|   public Money add(Money other) {
27|     requireSameCurrency(other);
28|     return new Money(amount.add(other.amount), currency);
29|   }
30| 
31|   public Money multiply(int factor) {
32|     return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
33|   }
34| 
35|   private void requireSameCurrency(Money other) {
36|     if (this.currency != other.currency)
37|       throw new IllegalArgumentException("통화 불일치");
38|   }
39| }
40| 
41| // 타입 안전한 메서드 시그니처
42| public Result<PaymentReceipt, PaymentError> processPayment(
43|   CustomerId customerId,
44|   EmailAddress email,
45|   ProductId productId,
46|   Money amount,
47|   CouponCode couponCode
48| ) { /* ... */ }
```

### After: DOP 관점

**[코드 03.3]** DOP: Record는 순수 데이터, 로직은 Calculations 클래스
```java
 1| // package: com.ecommerce.shared
 2| // [O] DOP: Record는 순수 데이터, 로직은 Calculations 클래스
 3| public record CustomerId(String value) {
 4|   public CustomerId { Objects.requireNonNull(value); }
 5| }
 6| 
 7| public record EmailAddress(String value) {
 8|   private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
 9|   public EmailAddress {
10|     if (value == null || !PATTERN.matcher(value).matches())
11|       throw new IllegalArgumentException("유효하지 않은 이메일");
12|   }
13| }
14| 
15| // DOP: Money에는 데이터만
16| public record Money(BigDecimal amount, Currency currency) {
17|   public Money {
18|     Objects.requireNonNull(amount);
19|     Objects.requireNonNull(currency);
20|     if (amount.compareTo(BigDecimal.ZERO) < 0)
21|       throw new IllegalArgumentException("금액은 음수 불가");
22|   }
23| }
24| 
25| // DOP: 연산은 별도 Calculations
26| public class MoneyCalculations {
27|   public static Money add(Money a, Money b) {
28|     if (a.currency() != b.currency())
29|       throw new IllegalArgumentException("통화 불일치");
30|     return new Money(a.amount().add(b.amount()), a.currency());
31|   }
32| 
33|   public static Money multiply(Money money, int factor) {
34|     return new Money(
35|       money.amount().multiply(BigDecimal.valueOf(factor)),
36|       money.currency()
37|     );
38|   }
39| }
```

### 관점 차이 분석
- **공통점**: 둘 다 Primitive Obsession을 Record로 해결하고, compact constructor에서 기본 검증
- **DMMF 주장**: `money.add(other)`가 도메인 전문가에게 더 자연스러운 표현. Money는 "더할 수 있는 값"이라는 도메인 개념
- **DOP 주장**: `MoneyCalculations.add(a, b)`가 테스트/재사용에 유리. Money 외에 다른 타입에도 같은 패턴 적용 가능
- **실무 권장**: 단순 산술(add, subtract, multiply)은 Record에 포함해도 무방. 복잡한 비즈니스 규칙(할인 정책, 세금 계산)은 외부 Calculations로

### 이해를 위한 부가 상세

**Constrained Type 패턴**: 범위나 형식을 가진 도메인 값을 타입으로 표현합니다.

**[코드 03.4]** OrderQuantity record
```java
 1| // package: com.ecommerce.order
 2| public record OrderQuantity(int value) {
 3|   public OrderQuantity {
 4|     if (value < 1 || value > 99)
 5|       throw new IllegalArgumentException("수량은 1-99 사이여야 합니다");
 6|   }
 7| }
 8| 
 9| public record Percentage(int value) {
10|   public Percentage {
11|     if (value < 0 || value > 100)
12|       throw new IllegalArgumentException("퍼센트는 0-100 사이");
13|   }
14| }
```

### 틀리기/놓치기 쉬운 부분
- **`double`로 금액 표현**: 부동소수점 오차 발생. 반드시 `BigDecimal` 사용
- **`if (quantity < 0)`로 검증**: 수량은 1 이상이어야 하므로 `< 1`로 검증
- **null 검증 누락**: compact constructor에서 `Objects.requireNonNull()` 습관화

### 꼭 기억할 것
1. **Primitive Obsession 해결**: 도메인 개념마다 Record 타입 생성
2. **Compact Constructor**: 객체 존재 조건(불변식)을 여기서 검증
3. **DMMF vs DOP**: 단순 연산은 Record에 포함 가능(DMMF), 복잡한 로직은 외부로(DOP)
4. **타입이 문서**: `processPayment(CustomerId, EmailAddress, Money)` → 시그니처가 곧 사양서

---

## 2. Value Object vs Entity (값 객체와 엔티티)

### 핵심 개념
- **관련 키워드**: Value Object, Entity, Identity, Equality, Lifecycle
- **통찰**: Value는 "내용이 같으면 같은 것"이고, Entity는 "ID가 같으면 같은 것"이다.
- **설명**: 만원짜리 두 장은 "같은" 만원입니다(Value). 하지만 같은 100만원이 들어있어도 "내 계좌"와 "네 계좌"는 다릅니다(Entity). 이 구분은 `equals()`를 어떻게 구현할지, 불변으로 만들지 여부를 결정합니다.

**[그림 03.2]** Value Object vs Entity (값 객체와 엔티티)
```
VALUE OBJECT                    ENTITY
+----------+                    +----------+
| Money    |                    | Order    |
|----------|                    |----------|
| amount   |                    | id (PK)  |
| currency |                    | status   |
+----------+                    | items    |
                                +----------+
Equality: all fields            Equality: id only
Immutable: always               Mutable: state changes
                                (but we model as immutable + wither)
```

### 개념이 아닌 것
- **"Value Object = 작은 객체"**: 크기가 아니라 동등성 비교 방식이 기준
- **"Entity = 변경 가능해야 한다"**: 함수형에서 Entity도 불변으로 모델링 (wither 패턴)

### Before: Traditional OOP

**[코드 03.5]** Value와 Entity의 구분이 모호한 OOP 코드
```java
 1| // package: com.ecommerce.order
 2| // [X] Value와 Entity의 구분이 모호한 OOP 코드
 3| public class Order {
 4|   private Long id;
 5|   private BigDecimal amount;
 6|   private String status;
 7| 
 8|   // equals를 id로만? 아니면 전체 필드로?
 9|   // → 개발자마다 다르게 구현 → 버그
10|   @Override
11|   public boolean equals(Object o) {
12|     if (this == o) return true;
13|     if (!(o instanceof Order)) return false;
14|     Order other = (Order) o;
15|     return Objects.equals(id, other.id);  // id만? amount도?
16|   }
17| 
18|   // setter로 상태 변경 → 언제 바뀌는지 추적 불가
19|   public void setStatus(String status) { this.status = status; }
20|   public void setAmount(BigDecimal amount) { this.amount = amount; }
21| }
```
- **의도 및 코드 설명**: equals 구현 기준이 모호하고, setter로 상태가 자유롭게 변경됨
- **뭐가 문제인가**:
  - `equals`가 id 기반인지 값 기반인지 코드를 읽어야 앎
  - setter로 아무 시점에나 상태 변경 가능
  - HashMap의 키로 사용 시 hashCode 변경으로 인한 유실 가능

### After: Modern Approach

**[코드 03.6]** Modern: Value Object: 값이 같으면 동일 (Record가 자동 처리)
```java
 1| // package: com.ecommerce.order
 2| // [O] Value Object: 값이 같으면 동일 (Record가 자동 처리)
 3| public record Money(BigDecimal amount, Currency currency) {}
 4| 
 5| Money m1 = Money.krw(10000);
 6| Money m2 = Money.krw(10000);
 7| m1.equals(m2);  // true - 값이 같으니 같은 Money
 8| 
 9| // [O] Entity: ID가 같으면 동일 (커스텀 equals 또는 ID 비교)
10| public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {
11|   public Order {
12|     items = List.copyOf(items);
13|   }
14| 
15|   // Wither로 상태 변경
16|   public Order withStatus(OrderStatus newStatus) {
17|     return new Order(id, items, newStatus);
18|   }
19| 
20|   // Entity의 동등성: ID만으로 판단
21|   @Override
22|   public boolean equals(Object o) {
23|     return o instanceof Order other && this.id.equals(other.id);
24|   }
25| 
26|   @Override
27|   public int hashCode() {
28|     return id.hashCode();
29|   }
30| }
31| 
32| // 사용: 상태가 달라도 같은 주문
33| Order unpaid = new Order(orderId, items, new Unpaid());
34| Order paid = unpaid.withStatus(new Paid(now, paymentId));
35| unpaid.equals(paid);  // true - 같은 ID
```
- **의도 및 코드 설명**: Value는 Record 기본 equals 사용, Entity는 ID 기반 equals 오버라이드
- **무엇이 좋아지나**:
  - 코드에서 Value/Entity 구분이 명확
  - Record의 자동 equals/hashCode로 보일러플레이트 제거 (Value)
  - Entity의 equals는 ID 기반으로 명시적 선언

### 틀리기/놓치기 쉬운 부분
- **"Record는 모든 필드로 equals하니까 Entity에 부적합?"**: equals/hashCode를 오버라이드하면 됨
- **"Entity를 Record로 만들면 JPA와 충돌?"**: JPA Entity는 `@Entity` 클래스로, 도메인 모델은 Record로 분리

### 꼭 기억할 것
1. **Value Object**: 값이 같으면 동일, 항상 불변, Record 기본 사용
2. **Entity**: ID가 같으면 동일, 상태 변경은 Wither로 새 객체 생성
3. **구분 기준**: "이것의 내용을 바꿔도 같은 것인가?" → Yes면 Entity, No면 Value

---

## 3. Compact Constructor와 검증 전략 (Compact Constructor and Validation Strategy)

### 핵심 개념
- **관련 키워드**: Compact Constructor, Invariant, Validation, Parse Don't Validate
- **통찰**: "생성된 객체는 반드시 유효하다"는 보장이 있으면, 이후 코드에서 재검증이 불필요하다.
- **설명**: Compact Constructor는 "공항 입국 심사"와 같습니다. 심사를 통과하지 못한 데이터는 시스템에 들어올 수 없고, 일단 통과하면 "유효하다"고 확신할 수 있습니다. 이것이 "Parse, Don't Validate" 원칙입니다.

**[그림 03.3]** Compact Constructor와 검증 전략 (Compact Constructor and Validation Strategy)
```
+-------------------+        +-------------------+
| External World    |        | Domain (trusted)  |
|                   | parse  |                   |
| raw: "abc@x.com" | =====> | Email("abc@x.com")|
| raw: "invalid"   | ====X  | (never exists)    |
+-------------------+        +-------------------+
                      ^
                      |
              Compact Constructor
              (validation gate)
```

### 개념이 아닌 것
- **"모든 검증을 compact constructor에"**: 비즈니스 규칙은 Validator로 분리
- **"Parse, Don't Validate = 예외만 던지면 됨"**: 핵심은 "검증 통과 = 타입 변환"이라는 점

### Before: Traditional OOP

**[코드 03.7]** Traditional OOP: 방어적 코딩: 모든 메서드에서 매번 검증
```java
 1| // package: com.ecommerce.order
 2| // [X] 방어적 코딩: 모든 메서드에서 매번 검증
 3| public class OrderService {
 4|   public void processOrder(Order order) {
 5|     if (order == null) throw new IllegalArgumentException();
 6|     if (order.getAmount() == null) throw new IllegalArgumentException();
 7|     if (order.getAmount().compareTo(BigDecimal.ZERO) <= 0)
 8|       throw new IllegalArgumentException();
 9|     // ... 드디어 비즈니스 로직 시작
10|   }
11| 
12|   public void calculateTax(Order order) {
13|     if (order == null) throw new IllegalArgumentException();
14|     if (order.getAmount() == null) throw new IllegalArgumentException();
15|     // ... 동일한 검증 반복
16|   }
17| }
```
- **의도 및 코드 설명**: 매 메서드마다 동일한 null/범위 체크 반복
- **뭐가 문제인가**:
  - 동일한 검증 코드가 10개 메서드에 중복
  - 검증 규칙 변경 시 모든 곳을 찾아 수정
  - 비즈니스 로직이 방어 코드에 파묻힘

### After: DMMF 관점 (DDD + FP)

**[코드 03.8]** DMMF: 모든 검증을 Compact Constructor에 집중
```java
 1| // package: com.ecommerce.shared
 2| // [O] DMMF: 모든 검증을 Compact Constructor에 집중
 3| public record Money(BigDecimal amount, Currency currency) {
 4|   public Money {
 5|     Objects.requireNonNull(amount, "금액은 null 불가");
 6|     Objects.requireNonNull(currency, "통화는 null 불가");
 7|     if (amount.compareTo(BigDecimal.ZERO) < 0)
 8|       throw new IllegalArgumentException("금액은 음수 불가: " + amount);
 9|     // 비즈니스 규칙도 여기서 검증 (DMMF 스타일)
10|     if (amount.scale() > 2)
11|       throw new IllegalArgumentException("소수점 2자리까지만 허용");
12|   }
13| }
14| 
15| // 이후 코드: 재검증 불필요
16| public class TaxCalculations {
17|   public static Money calculateTax(Money orderAmount, TaxRate rate) {
18|     // Money는 이미 유효함이 보장됨 → null 체크 불필요
19|     return orderAmount.multiply(rate.value());
20|   }
21| }
```

### After: DOP 관점

**[코드 03.9]** DOP: 불변식만 Compact Constructor, 비즈니스 규칙은 Validator
```java
 1| // package: com.ecommerce.shared
 2| // [O] DOP: 불변식만 Compact Constructor, 비즈니스 규칙은 Validator
 3| public record Money(BigDecimal amount, Currency currency) {
 4|   public Money {
 5|     // 불변식: 객체가 존재하기 위한 최소 조건만
 6|     Objects.requireNonNull(amount);
 7|     Objects.requireNonNull(currency);
 8|     if (amount.compareTo(BigDecimal.ZERO) < 0)
 9|       throw new IllegalArgumentException("음수 금액 불가");
10|   }
11| }
12| 
13| // 비즈니스 규칙: 별도 Validator (컨텍스트에 따라 다를 수 있음)
14| public class OrderValidator {
15|   public static Result<Order, ValidationError> validate(Order order) {
16|     if (order.totalAmount().amount().compareTo(BigDecimal.valueOf(10_000)) < 0)
17|       return Result.failure(new MinimumOrderAmountError());
18|     if (order.items().isEmpty())
19|       return Result.failure(new EmptyOrderError());
20|     return Result.success(order);
21|   }
22| }
```

### 관점 차이 분석
- **공통점**: 둘 다 "생성된 객체는 유효하다"는 원칙 공유, compact constructor에서 null/범위 검증
- **DMMF 주장**: 가능한 모든 검증을 constructor에 넣어서, 타입 자체가 "검증됨"을 보장. "소수점 2자리" 같은 규칙도 Money의 불변식
- **DOP 주장**: constructor에는 "객체 존재 조건"만, "최소 주문금액 10,000원" 같은 비즈니스 규칙은 컨텍스트에 따라 다르므로 Validator로 분리
- **실무 권장**: 구분 기준은 "이 규칙이 바뀌면 타입 자체를 바꿔야 하는가?". Yes면 constructor, No면 Validator

### 틀리기/놓치기 쉬운 부분
- **"Result 반환 vs Exception throw"**: 불변식 위반은 Exception(프로그래밍 에러), 비즈니스 규칙 위반은 Result(예상된 실패)
- **"Parse, Don't Validate" 핵심**: `boolean isValid(String)` → 검증만 하고 여전히 String. `Email parse(String)` → 검증 + 타입 변환

### 꼭 기억할 것
1. **Compact Constructor = 입국 심사**: 통과하지 못하면 시스템에 들어올 수 없음
2. **불변식 vs 비즈니스 규칙**: 객체 존재 조건은 constructor, 상황별 규칙은 Validator
3. **Parse, Don't Validate**: 검증 결과를 타입으로 캡처하면 재검증 불필요
4. **Exception vs Result**: 불변식 → throw, 비즈니스 → Result.failure()

---

## 4. 깊은 불변성 확보 (Ensuring Deep Immutability)

### 핵심 개념
- **관련 키워드**: Shallow Immutability, Deep Immutability, Defensive Copy, List.copyOf
- **통찰**: Record는 필드 참조의 재할당은 막지만, 참조하는 컬렉션의 내부 수정은 막지 못한다.
- **설명**: `public record Order(List<OrderItem> items) {}`에서 `items` 필드는 final이지만, `order.items().add(newItem)`은 성공합니다. 외부에서 전달받은 가변 리스트의 참조를 그대로 보관하기 때문입니다.

### 개념이 아닌 것
- **"Record를 쓰면 자동으로 불변"**: 참조 타입 필드는 여전히 내부 수정 가능
- **"List.of()만 쓰면 됨"**: 외부에서 가변 리스트를 전달하면 여전히 문제

### Before: Traditional OOP

**[코드 03.10]** Traditional OOP: 얕은 불변성의 함정
```java
 1| // package: com.ecommerce.order
 2| // [X] 얕은 불변성의 함정
 3| public record Order(OrderId id, List<OrderItem> items) {}
 4| 
 5| List<OrderItem> mutableList = new ArrayList<>();
 6| mutableList.add(new OrderItem(productId, quantity));
 7| 
 8| Order order = new Order(orderId, mutableList);
 9| 
10| // Order 외부에서 내부 데이터 변경 가능!
11| mutableList.add(new OrderItem(anotherProduct, anotherQty));
12| // order.items().size()가 2로 변함 → 불변성 파괴!
13| 
14| // accessor를 통한 변경도 가능
15| order.items().add(new OrderItem(thirdProduct, thirdQty));
16| // order.items().size()가 3으로 변함!
```
- **의도 및 코드 설명**: Record인데도 외부에서 내부 리스트를 변경할 수 있음
- **뭐가 문제인가**:
  - 외부 리스트 참조로 Record 내부 상태 변경 가능
  - accessor로 얻은 리스트에 직접 add 가능
  - 멀티스레드에서 예측 불가능한 동작

### After: Modern Approach

**[코드 03.11]** Modern: Compact Constructor에서 방어적 복사
```java
 1| // package: com.ecommerce.order
 2| // [O] Compact Constructor에서 방어적 복사
 3| public record Order(OrderId id, List<OrderItem> items) {
 4|   public Order {
 5|     Objects.requireNonNull(id);
 6|     Objects.requireNonNull(items);
 7|     items = List.copyOf(items);  // 불변 리스트로 복사
 8|   }
 9| }
10| 
11| // 이제 안전!
12| List<OrderItem> mutableList = new ArrayList<>();
13| mutableList.add(new OrderItem(productId, quantity));
14| 
15| Order order = new Order(orderId, mutableList);
16| 
17| mutableList.add(new OrderItem(anotherProduct, anotherQty));
18| // order.items()는 여전히 1개 (방어적 복사로 독립)
19| 
20| order.items().add(new OrderItem(thirdProduct, thirdQty));
21| // UnsupportedOperationException! (불변 리스트)
```
- **의도 및 코드 설명**: `List.copyOf()`로 외부 참조와 단절하고 불변 리스트로 변환
- **무엇이 좋아지나**:
  - 외부에서 내부 상태 변경 불가능
  - accessor로 얻은 리스트도 수정 불가
  - 멀티스레드에서 동기화 없이 안전
  - 이미 불변인 리스트에 `List.copyOf()`를 쓰면 복사 없이 원본 반환 (성능 이슈 없음)

### 틀리기/놓치기 쉬운 부분
- **`List.of()` vs `List.copyOf()`**: `List.of()`는 새 리스트 생성, `copyOf()`는 이미 불변이면 복사 안 함
- **Map/Set도 동일**: `Map.copyOf()`, `Set.copyOf()` 사용
- **중첩 컬렉션**: `List<List<Item>>`은 내부 리스트도 불변화 필요

### 꼭 기억할 것
1. **Record + 컬렉션 = 방어적 복사 필수**: `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()`
2. **Compact Constructor에서 처리**: 모든 컬렉션 필드를 불변으로 변환
3. **성능 걱정 불필요**: 이미 불변인 경우 `copyOf()`는 복사하지 않음
