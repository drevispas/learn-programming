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

데이터 모델링에 수학적 속성을 적용하면 분산 시스템에서 강력한 위력을 발휘합니다.

| 속성 | 정의 | 활용 |
|-----|------|------|
| 결합법칙 | (A ⊕ B) ⊕ C = A ⊕ (B ⊕ C) | 병렬 처리, 분산 계산 |
| 교환법칙 | A ⊕ B = B ⊕ A | 순서 무관한 처리 |
| 멱등성 | f(f(x)) = f(x) | 재시도 안전성 |
| 항등원 | A ⊕ e = A | 빈 값 처리 |

---

### 7.2 결합법칙 (Associativity)

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

#### 비유: 전등 스위치

> **멱등성은 ON/OFF 스위치가 아닌 "ON 버튼"과 같습니다.**
>
> **토글 스위치 (멱등하지 않음)**:
> - 버튼을 누르면 상태가 바뀜 (ON→OFF, OFF→ON)
> - 네트워크 지연으로 "두 번 눌림"이 발생하면 문제!
>
> **ON 버튼 (멱등함)**:
> - 버튼을 누르면 항상 ON 상태
> - 이미 ON이면 아무 변화 없음
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

#### 이커머스 예제: 멱등한 주문 상태 변경

```java
// 멱등한 주문 상태 변경
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

연산의 결과가 원래 값 그대로인 특별한 값입니다.

| 연산 | 항등원 |
|-----|-------|
| 덧셈 | 0 |
| 곱셈 | 1 |
| 문자열 연결 | "" (빈 문자열) |
| 리스트 병합 | [] (빈 리스트) |

```java
// 항등원이 있으면 reduce가 안전해짐
List<Money> payments = Collections.emptyList();

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

## Chapter 8: 인터프리터 패턴 - Rule as Data ⭐ (강화)

### 학습 목표
1. 인터프리터 패턴의 개념과 필요성을 이해한다
2. 비즈니스 로직을 데이터로 표현하는 방법을 알 수 있다
3. 재귀적 데이터 구조를 설계할 수 있다
4. Rule Engine을 단계별로 구현할 수 있다
5. 흔한 실수를 피하고 동적 규칙 로딩을 구현할 수 있다

---

### 8.1 왜 로직을 데이터로 만드는가?

비즈니스 규칙이 복잡해질 때, `if-else`를 떡칠하면 여러 문제가 발생합니다:
- 규칙 추가/변경마다 코드 수정 필요
- 규칙 조합이 복잡해지면 이해하기 어려움
- 런타임에 규칙을 변경할 수 없음

**해결책**: 규칙 자체를 **데이터(ADT)**로 표현하고, 별도의 **해석기(Interpreter)**가 실행합니다.

---

### 8.2 Rule Engine 단계별 구현 ⭐

#### Step 1: 기본 Equals 조건

```java
public record Customer(String country, String type, int totalSpend) {}

public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
}

public class RuleEngine {
    public static boolean evaluate(Rule rule, Customer customer) {
        return switch (rule) {
            case Rule.Equals(var attr, var value) -> switch (attr) {
                case "country" -> customer.country().equals(value);
                case "type" -> customer.type().equals(value);
                default -> false;
            };
        };
    }
}
```

#### Step 2: And/Or/Not 논리 연산 추가

```java
public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule rule) implements Rule {}
}
```

#### Step 3: GTE 조건 추가 - 흔한 실수 사례 ⭐

##### [Trap] "데이터"와 "값"의 시점 차이

**잘못된 설계** (흔한 실수):
```java
// ❌ 값(int)을 직접 받음 - 규칙 생성 시점에 결과 확정!
record GTE(int left, int right) implements Rule {}
```

**올바른 설계**:
```java
// ✅ 속성 이름(String)을 저장 - 평가 시점에 데이터 참조
record GTE(String attribute, int threshold) implements Rule {}

case Rule.GTE(var attr, var threshold) -> switch (attr) {
    case "totalSpend" -> customer.totalSpend() >= threshold;
    default -> false;
};
```

---

### 8.3 복잡한 할인 규칙 예제

"한국(KR)에 살면서, (구매액 100만원 이상이거나 VIP)"

```java
Rule krDiscountRule = new Rule.And(
    new Rule.Equals("country", "KR"),
    new Rule.Or(
        new Rule.Equals("type", "VIP"),
        new Rule.GTE("totalSpend", 1_000_000)
    )
);

Customer krCustomer = new Customer("KR", "GOLD", 1_500_000);
boolean discountable = RuleEngine.evaluate(krDiscountRule, krCustomer); // true
```

---

### 8.4 동적 규칙 로딩 (DB/JSON)

```json
{
  "type": "and",
  "left": { "type": "equals", "attribute": "country", "value": "KR" },
  "right": { "type": "gte", "attribute": "totalSpend", "threshold": 500000 }
}
```

마케팅 팀이 관리자 화면에서 규칙을 수정하면, **코드 배포 없이** 즉시 적용됩니다.

---

### 8.5 Before/After: if-else vs Rule Engine

##### Before: 하드코딩
```java
if (customer.country().equals("KR") && customer.type().equals("VIP")) {
    return true;
}
// 새 규칙마다 코드 수정 필요...
```

##### After: 데이터 주도
```java
List<Rule> rules = loadFromDatabase();
return rules.stream().anyMatch(r -> RuleEngine.evaluate(r, customer));
// 새 규칙은 데이터 추가만!
```

---

### 퀴즈 Chapter 8

#### Q8.1 [함정 문제] GTE 규칙 설계 실수 ⭐
다음 GTE 규칙 설계의 문제점은?
```java
record GTE(int left, int right) implements Rule {}
Rule rule = new Rule.GTE(customer.totalSpend(), 1_000_000);
```

A. 문제없음
B. 규칙 생성 시점에 이미 결과가 결정되어, 다른 고객에게 재사용 불가
C. 컴파일 에러
D. 성능 문제

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
