# 04. Algebraic Data Types (대수적 데이터 타입)

> **Sources**: DMMF Ch.3 (Composite Types - AND/OR), DOP Ch.3 (Cardinality Theory)

---

## 1. Product Type과 Sum Type (곱 타입과 합 타입)

### 핵심 개념
- **관련 키워드**: Product Type, Sum Type, Algebraic Data Types, Record, Sealed Interface
- **통찰**: 모든 데이터 구조는 AND(곱)와 OR(합) 두 가지 결합 방식의 조합이다.
- **설명**: Product Type(AND)은 "A 그리고 B 그리고 C"로, 모든 필드가 동시에 존재합니다. Java Record가 이를 표현합니다. Sum Type(OR)은 "A 또는 B 또는 C"로, 하나만 선택됩니다. Java Sealed Interface가 이를 표현합니다.

  이 두 가지 결합을 올바르게 활용하면 도메인의 "가능한 상태"를 정확히 표현할 수 있고, 불가능한 상태는 타입 시스템이 차단합니다.

**[그림 04.1]** Product Type과 Sum Type (곱 타입과 합 타입)
```
ALGEBRAIC DATA TYPES (ADT)
===========================

Product Type (AND)              Sum Type (OR)
+-----------------+             +------------------+
| record Order(   |             | sealed interface |
|   OrderId id,   |             |   PaymentMethod  |
|   Money amount, |             +--------+---------+
|   Status status |                      |
| )               |              +-------+-------+
+-----------------+              |       |       |
                               Card  Transfer Points
id AND amount AND status       (pick exactly one)

State space:                   State space:
  |id| x |amount| x |status|    |Card| + |Transfer| + |Points|
  = N1 * N2 * N3                = N1 + N2 + N3
```

### 개념이 아닌 것
- **"Product Type = 클래스"**: 클래스는 가변 상태와 행위를 포함하지만, Product Type은 순수 데이터 조합
- **"Sum Type = 상속"**: 상속은 개방적(Open)이지만, Sum Type(sealed)은 폐쇄적(Closed)이고 망라적

### Before: Traditional OOP

**[코드 04.1]** Traditional OOP: String/enum으로 상태 표현: 상태별 데이터를 담을 수 없음
```java
 1| // package: com.ecommerce.order
 2| // [X] String/enum으로 상태 표현: 상태별 데이터를 담을 수 없음
 3| public class Order {
 4|   private String paymentMethod;  // "CARD", "TRANSFER", "POINTS"
 5|   // 카드일 때만 필요한 데이터
 6|   private String cardNumber;
 7|   private YearMonth cardExpiry;
 8|   // 계좌이체일 때만 필요한 데이터
 9|   private String bankAccount;
10|   private String bankCode;
11|   // 포인트일 때만 필요한 데이터
12|   private int pointsUsed;
13| 
14|   // 문제: 카드 결제인데 bankAccount에 값이 있으면?
15|   // 문제: 포인트 결제인데 cardNumber에 값이 있으면?
16|   // → 불가능한 상태가 표현 가능!
17| }
```
- **의도 및 코드 설명**: 결제 수단에 따라 필요한 데이터가 다르지만, 모든 필드가 하나의 클래스에
- **뭐가 문제인가**:
  - 불필요한 필드에 값이 들어갈 수 있는 불가능한 상태 허용
  - null 체크가 곳곳에 필요
  - 새 결제수단 추가 시 기존 필드와의 관계를 모두 검증해야 함
  - 상태별 로직에 if/else 분기 필연적

### After: Modern Approach

**[코드 04.2]** Modern: Sum Type: 각 상태가 필요한 데이터만 보유
```java
 1| // package: com.ecommerce.payment
 2| // [O] Sum Type: 각 상태가 필요한 데이터만 보유
 3| public sealed interface PaymentMethod {
 4|   record CreditCard(String cardNumber, YearMonth expiry, String cvc) implements PaymentMethod {}
 5|   record BankTransfer(String accountNumber, String bankCode) implements PaymentMethod {}
 6|   record Points(int amount) implements PaymentMethod {}
 7| }
 8| 
 9| // Product Type: Order는 모든 필드를 AND로 결합
10| public record Order(
11|   OrderId id,
12|   Customer customer,
13|   List<OrderLine> lines,
14|   PaymentMethod payment  // Sum Type을 필드로 가짐
15| ) {
16|   public Order {
17|     lines = List.copyOf(lines);
18|   }
19| }
20| 
21| // Pattern Matching으로 안전한 분기
22| public class PaymentProcessor {
23|   public static PaymentReceipt process(PaymentMethod method, Money amount) {
24|     return switch (method) {
25|       case CreditCard card -> chargeCard(card, amount);
26|       case BankTransfer transfer -> initiateTransfer(transfer, amount);
27|       case Points points -> deductPoints(points, amount);
28|     };
29|     // 새 결제수단 추가 시 → 컴파일 에러로 누락 방지
30|   }
31| }
```
- **의도 및 코드 설명**: 각 결제수단 variant가 자신에게 필요한 데이터만 보유. Pattern Matching으로 망라적 처리
- **무엇이 좋아지나**:
  - 불가능한 상태 표현 자체가 불가능 (카드 결제에 bankCode 없음)
  - null 체크 불필요 (각 variant의 필드는 항상 존재)
  - 새 variant 추가 시 컴파일러가 모든 switch를 찾아줌
  - 코드가 도메인을 정확히 반영

### 이해를 위한 부가 상세

**Record 위치 (sealed interface의 내부 vs 외부)**:
- 내부 정의: `OrderStatus.Created` → 부모 없이 의미 없는 타입에 적합
- 외부 정의: `Created` → 독립적으로 재사용되는 타입에 적합
- 실무 가이드: *variant가 5개 이하면 내부 정의, 많으면 외부 분리*

### 틀리기/놓치기 쉬운 부분
- **`permits` 절 생략**: 내부 정의 시 생략 가능, 외부 정의 시 필수
- **default 절 금지**: sealed interface의 switch에서 default를 쓰면 새 variant 추가 시 컴파일 경고를 놓침
- **Non-sealed 허용**: sealed의 하위가 non-sealed이면 외부 확장 가능 → ADT의 폐쇄성을 깨뜨림

### 꼭 기억할 것
1. **Product Type = Record**: 모든 필드가 AND로 결합
2. **Sum Type = Sealed Interface**: variant 중 하나만 선택 (OR)
3. **Pattern Matching**: sealed interface + switch = 망라적 분기
4. **default 금지**: 새 variant 추가 시 컴파일 에러를 받기 위해

---

## 2. 기수 이론: 상태 공간 계산 (Cardinality Theory: State Space Calculation)

### 핵심 개념
- **관련 키워드**: Cardinality, State Space, Complexity, Boolean Explosion
- **통찰**: 복잡도는 "가능한 상태의 총 개수"와 비례한다. 곱 타입은 상태를 곱하고, 합 타입은 상태를 더한다.
- **설명**: `boolean` 필드 5개가 있으면 상태 공간은 2^5 = 32입니다. 하지만 유효한 상태가 5개뿐이라면, 32 - 5 = 27가지의 불가능한 상태가 존재합니다. Sealed Interface로 5가지 variant를 정의하면 상태 공간이 정확히 5로 줄어들어 복잡도가 84% 감소합니다.

**[그림 04.2]** 기수 이론: 상태 공간 계산 (Cardinality Theory: State Space Calculation)
```
BOOLEAN FLAGS (Product Type)       SEALED INTERFACE (Sum Type)
============================       ============================

isCreated:   T/F                    sealed interface OrderStatus {
isPaid:      T/F     2^5 = 32        Created    |
isShipped:   T/F  possible states    Paid       | = 5 states
isDelivered: T/F                     Shipped    |
isCanceled:  T/F                     Delivered  |
                                     Canceled   |
Valid: 5                           }
Invalid: 27 (!)
                                   Complexity reduced by 84%!
```

### 개념이 아닌 것
- **"기수가 작으면 항상 좋다"**: 도메인의 실제 복잡도를 반영해야 함. 필요한 상태를 줄이면 표현력 부족
- **"상태가 많으면 곱 타입 자체가 나쁘다"**: 곱 타입은 "모든 필드가 독립적으로 의미 있을 때" 적절

### Before: Traditional OOP

**[코드 04.3]** Traditional OOP: Boolean 필드로 상태 표현: 2^5 = 32가지 상태 (유효한 건 5개)
```java
 1| // package: com.ecommerce.order
 2| // [X] Boolean 필드로 상태 표현: 2^5 = 32가지 상태 (유효한 건 5개)
 3| public class Order {
 4|   boolean isCreated;
 5|   boolean isPaid;
 6|   boolean isShipped;
 7|   boolean isDelivered;
 8|   boolean isCanceled;
 9| 
10|   public void ship() {
11|     // 매번 방어적 검증 필요
12|     if (!isPaid) throw new IllegalStateException("결제 안 됨");
13|     if (isCanceled) throw new IllegalStateException("취소됨");
14|     if (isShipped) throw new IllegalStateException("이미 배송됨");
15|     this.isShipped = true;
16|   }
17| }
18| 
19| // 모순 상태 가능:
20| Order broken = new Order();
21| broken.isPaid = true;
22| broken.isCanceled = true;  // 결제됐는데 취소? 컴파일 OK!
```
- **의도 및 코드 설명**: boolean 플래그로 주문 상태를 표현
- **뭐가 문제인가**:
  - 32가지 중 27가지가 불가능한 상태 → 런타임 검증 필수
  - 모순 상태(`isPaid && isCanceled`)가 컴파일 가능
  - 상태 추가 시 조합 폭발 (boolean 하나 추가 → 상태 2배)
  - 모든 비즈니스 로직에 방어 코드 필요

### After: Modern Approach

**[코드 04.4]** Modern: Sealed Interface: 정확히 5가지 유효한 상태만 표현
```java
 1| // package: com.ecommerce.order
 2| // [O] Sealed Interface: 정확히 5가지 유효한 상태만 표현
 3| public sealed interface OrderStatus {
 4|   record Created(LocalDateTime at) implements OrderStatus {}
 5|   record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {}
 6|   record Shipped(LocalDateTime at, TrackingNumber tracking) implements OrderStatus {}
 7|   record Delivered(LocalDateTime at, ReceiverName receiver) implements OrderStatus {}
 8|   record Canceled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
 9| }
10| 
11| public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {
12|   public Order { items = List.copyOf(items); }
13| }
14| 
15| // 상태별 데이터가 정확히 맞음:
16| // - Shipped는 TrackingNumber를 가짐
17| // - Canceled는 CancelReason을 가짐
18| // - Created는 둘 다 없음
19| // 모순 상태는 타입으로 표현 불가능!
```
- **의도 및 코드 설명**: 각 상태가 자신에게 필요한 데이터만 보유하는 Sum Type
- **무엇이 좋아지나**:
  - 상태 공간 = 5 (84% 감소)
  - 모순 상태 컴파일 불가능
  - 상태별 데이터가 타입에 강제됨
  - 새 상태 추가 시 모든 switch에서 컴파일 에러

### 이해를 위한 부가 상세

**기수 계산 공식**:
- Product Type: `|A × B| = |A| × |B|` (곱)
- Sum Type: `|A + B| = |A| + |B|` (합)
- Optional: `|Optional<T>| = |T| + 1` (값 또는 없음)

**실전 예시**: 결제 방법(3가지) + 배송 방법(2가지)
- 곱 타입: 3 × 2 = 6가지 조합 (전부 유효하면 OK)
- *불필요한 조합이 있으면 합 타입으로 분리*

### 틀리기/놓치기 쉬운 부분
- **enum vs sealed interface**: 상태에 데이터가 필요 없으면 enum으로 충분. 데이터가 필요하면 sealed interface
- **boolean 1개는 괜찮은가?**: 독립적인 속성(isArchived 등)은 boolean OK. 상호 배타적 상태는 Sum Type
- **상태 공간이 큰 게 항상 나쁜가?**: 모든 조합이 유효하면 문제 없음. 무효한 조합이 많을 때 Sum Type 도입

### 꼭 기억할 것
1. **곱 타입 = 상태 곱셈**: boolean N개 → 2^N 가지 상태
2. **합 타입 = 상태 덧셈**: variant N개 → N가지 상태
3. **유효하지 않은 조합이 많으면**: 곱 타입 → 합 타입으로 리팩토링
4. **상태별 데이터**: 각 variant가 자신에게 필요한 데이터만 보유

---

## 3. Pattern Matching과 Exhaustiveness (패턴 매칭과 망라성)

### 핵심 개념
- **관련 키워드**: Pattern Matching, Exhaustive Switch, Record Patterns, Deconstruction
- **통찰**: Sealed Interface + Switch Expression = 컴파일러가 모든 케이스를 처리했는지 보장한다.
- **설명**: Java 21+의 Pattern Matching은 sealed interface의 모든 variant를 switch에서 처리하도록 강제합니다. 하나라도 빠뜨리면 컴파일 에러가 발생합니다. 이를 "exhaustiveness check"라고 합니다.

### 개념이 아닌 것
- **"switch = if/else의 대체"**: 패턴 매칭은 타입 분해(deconstruction)까지 포함
- **"default를 넣으면 안전"**: default는 새 variant 추가 시 컴파일 경고를 숨김 → 위험

### Before: Traditional OOP

**[코드 04.5]** Traditional OOP: if/else 체인 + instanceof 캐스팅
```java
 1| // package: com.ecommerce.shared
 2| // [X] if/else 체인 + instanceof 캐스팅
 3| public String describePayment(Object payment) {
 4|   if (payment instanceof CreditCard) {
 5|     CreditCard card = (CreditCard) payment;
 6|     return "Card: " + card.getNumber();
 7|   } else if (payment instanceof BankTransfer) {
 8|     BankTransfer transfer = (BankTransfer) payment;
 9|     return "Bank: " + transfer.getAccount();
10|   } else {
11|     return "Unknown";  // 새 타입 추가 시 여기로 빠짐 → 런타임 버그
12|   }
13| }
```
- **의도 및 코드 설명**: instanceof + 캐스팅으로 타입 분기
- **뭐가 문제인가**:
  - `else` 절이 새 타입을 삼켜서 런타임에 "Unknown" 반환
  - 캐스팅이 장황하고 타입 안전하지 않음
  - 새 결제수단 추가 시 컴파일러가 경고하지 않음

### After: Modern Approach

**[코드 04.6]** Modern: Sealed Interface + Pattern Matching = 망라적 분기
```java
 1| // package: com.ecommerce.shared
 2| // [O] Sealed Interface + Pattern Matching = 망라적 분기
 3| public String describePayment(PaymentMethod method) {
 4|   return switch (method) {
 5|     case CreditCard card -> "Card: " + card.cardNumber();
 6|     case BankTransfer transfer -> "Bank: " + transfer.accountNumber();
 7|     case Points points -> "Points: " + points.amount();
 8|     // 새 variant 추가 시 → 컴파일 에러! (switch not exhaustive)
 9|   };
10| }
11| 
12| // Record Patterns로 필드 직접 분해
13| public Money calculateFee(PaymentMethod method, Money amount) {
14|   return switch (method) {
15|     case CreditCard(var num, var exp, var cvc) -> amount.multiply(0.03);
16|     case BankTransfer(var acc, var bank) -> Money.krw(500);
17|     case Points(var pts) -> Money.zero();
18|   };
19| }
```
- **의도 및 코드 설명**: switch expression이 모든 variant를 처리하도록 강제
- **무엇이 좋아지나**:
  - 새 variant 추가 시 모든 switch에서 컴파일 에러
  - 캐스팅 불필요 (패턴 변수로 자동 바인딩)
  - Record Patterns로 필드 직접 분해 가능
  - 반환값이 보장됨 (switch expression은 값을 반환해야 함)

### 틀리기/놓치기 쉬운 부분
- **`case null` 처리**: sealed interface 변수가 null일 수 있다면 `case null ->` 추가
- **Guard 패턴**: `case CreditCard card when card.isExpired() ->` 로 조건 추가 가능
- **Unnamed Patterns**: `case CreditCard(_, var exp, _) ->` 불필요한 필드 무시 (Java 22+)

### 꼭 기억할 것
1. **default 금지**: *sealed interface switch에서 default 사용하지 말 것* 
2. **Record Patterns**: `case Type(var a, var b) ->` 로 필드 직접 분해
3. **Switch Expression**: 값을 반환하는 형태 사용 (statement 아닌 expression)
4. **Exhaustiveness**: 컴파일러가 모든 케이스 처리를 강제
