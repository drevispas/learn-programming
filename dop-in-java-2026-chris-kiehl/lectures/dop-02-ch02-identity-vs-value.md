# Chapter 2: 데이터란 무엇인가? - 정체성과 값 (What is Data? - Identity vs Value)

## 학습 목표 (Learning Objectives)
1. 정체성(Identity)과 값(Value)의 개념적 차이를 명확히 구분할 수 있다
2. Java Record가 Value Type을 표현하는 방식을 이해한다
3. 얕은 불변성과 깊은 불변성의 차이를 설명할 수 있다
4. 방어적 복사를 통해 깊은 불변성을 확보할 수 있다
5. 이커머스 도메인에서 Identity와 Value를 올바르게 식별할 수 있다

---

## 2.1 정체성과 값의 차이 (The Difference Between Identity and Value)

> **다른 말로 (In other words):**
> - "Identity는 '누구인가'를 ID로 식별, Value는 '무엇인가'를 내용으로 비교"
> - "Identity는 가변 허용(이름 바꿔도 같은 사람), Value는 불변 필수(10원은 항상 10원)"

> **🎯 왜 배우는가?**
>
> "이 객체를 equals로 비교해야 할까, ==로 비교해야 할까?" 고민해본 적 있으신가요?
> Identity와 Value를 구분하면 **동등성 비교 로직에서 발생하는 버그를 원천 차단**하고,
> 도메인 모델링 시 올바른 타입 선택을 할 수 있습니다.

시스템의 모든 데이터는 두 가지 범주 중 하나에 속합니다.

### Value vs Identity 비교 다이어그램

```
┌───────────────────────────────────────────────────────────────────────┐
│                        Value vs Identity                              │
├───────────────────────────────────┬───────────────────────────────────┤
│            [$] Value              │           [#] Identity            │
├───────────────────────────────────┼───────────────────────────────────┤
│                                   │                                   │
│   10,000 KRW  ═══  10,000 KRW     │   Member #123  ≠  Member #456     │
│        ↓                          │         ↓                         │
│   Same if content matches         │   Same if ID matches              │
│                                   │                                   │
│  ┌───────────┐ ┌───────────┐      │  ┌─────────────────────────────┐  │
│  │  Money    │ │  Money    │      │  │ Member                      │  │
│  │  10,000   │=│  10,000   │      │  │ id: 123                     │  │
│  │  KRW      │ │  KRW      │      │  │ name: "Hong" → "Kim"        │  │
│  └───────────┘ └───────────┘      │  │ (Same member if name changes│  │
│         Equal!                    │  └─────────────────────────────┘  │
│                                   │                                   │
│  Examples: Money, Coordinates,    │  Examples: Member, Order,         │
│            Date                   │            Product                │
│  Immutable, equals() comparison   │  Mutable allowed, ID comparison   │
└───────────────────────────────────┴───────────────────────────────────┘
```

**Table 2.1**: Value와 Identity의 핵심 차이점

| 구분      | Value (값)           | Identity (정체성)   |
| --------- | -------------------- | ------------------- |
| 동등성    | 내용이 같으면 같음   | ID가 같으면 같음    |
| 불변성    | 항상 불변            | 상태가 변할 수 있음 |
| 비교 방식 | `equals()` (값 비교) | `==` 또는 ID 비교   |
| 예시      | 금액, 좌표, 날짜     | 회원, 주문, 상품    |

### 비유: 여권과 이름표

> **Identity는 여권이고, Value는 이름표입니다.**
>
> **여권(Identity)**:
> - 여권 번호가 같으면 같은 사람입니다
> - 이름을 개명해도, 주소를 바꿔도 여전히 같은 사람입니다
> - 여권은 "누구인가"를 식별합니다
>
> **이름표(Value)**:
> - "홍길동"이라고 적힌 이름표 두 개는 같은 이름표입니다
> - 이름표에는 고유 번호가 없습니다
> - 이름표는 "무엇인가"를 나타냅니다

---

## 2.2 이커머스에서의 Identity vs Value (Identity vs Value in E-commerce)

> **🎯 왜 배우는가?**
>
> 실무에서 "회원", "주문", "금액"을 어떻게 모델링할지 매번 고민되시나요?
> 이커머스 도메인 예시를 통해 **Identity와 Value를 직관적으로 구분하는 기준**을 익히고,
> 도메인 설계 시 자신감을 가질 수 있습니다.

**Code 2.1**: Value Types - 값으로 동등성 판단
```java
// ========== Value Types (값) ==========

// 금액: 10000원은 어디에서든 10000원
public record Money(BigDecimal amount, Currency currency) {}

// 좌표: (37.5, 127.0)은 어디에서든 같은 위치
public record Coordinate(double latitude, double longitude) {}

// 주소: 내용이 같으면 같은 주소
public record Address(String city, String street, String zipCode) {}
```

**Code 2.2**: Identity Types - ID로 동등성 판단
```java
// ========== Identity Types (정체성) ==========

// 회원: 이름을 바꿔도 같은 회원
public record Member(MemberId id, String name, EmailAddress email) {}

// 주문: 상태가 바뀌어도 같은 주문
public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {}

// 상품: 가격이 바뀌어도 같은 상품
public record Product(ProductId id, ProductName name, Money price) {}
```

---

## 2.3 Java Record: Value Type의 완벽한 도구 (Java Record: The Perfect Tool for Value Types)

> **다른 말로 (In other words):**
> - "Record는 equals/hashCode/toString을 자동 생성하는 불변 데이터 캐리어"
> - "Compact Constructor로 불변식(Invariant)을 강제하는 방법"

> **🎯 왜 배우는가?**
>
> Value Object를 매번 equals/hashCode 오버라이드하며 만드느라 지치셨나요?
> Java Record를 활용하면 **보일러플레이트 코드 없이 완벽한 Value Type**을 만들 수 있고,
> 컴파일러가 불변성을 보장해줍니다.

### Compact Constructor로 불변식 강제

**Code 2.3**: Money Record - Compact Constructor와 비즈니스 연산
```java
public record Money(BigDecimal amount, Currency currency) {
    // Compact Constructor - 검증 로직
    public Money {
        Objects.requireNonNull(amount, "금액은 null일 수 없습니다");
        Objects.requireNonNull(currency, "통화는 null일 수 없습니다");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 음수가 될 수 없습니다: " + amount);
        }
        // this.amount = amount; 자동 실행
    }

    // 팩토리 메서드
    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    // 비즈니스 연산 (새 객체 반환)
    public Money add(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("통화가 다릅니다");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

> **💡 Q&A: 검증은 compact constructor에? 아니면 별도 Validator에?**
>
> **핵심 구분: 불변식 vs 비즈니스 규칙**
>
> | 구분 | 불변식(Invariant) | 비즈니스 규칙(Business Rule) |
> |-----|------------------|---------------------------|
> | 정의 | 객체가 "존재"하기 위한 필수 조건 | 특정 컨텍스트에서 "유효"하기 위한 조건 |
> | 위치 | Compact Constructor | 별도 Validator 클래스 |
> | 예시 | null 불가, 음수 불가 | 최소 금액 10,000원, VIP 할인 적용 |
> | 실패 시 | `IllegalArgumentException` | `Result.failure(ValidationError)` |
>
> ```java
> // [O] Compact Constructor: 불변식 (객체 존재 조건)
> public record Money(BigDecimal amount, Currency currency) {
>     public Money {
>         Objects.requireNonNull(amount);   // null이면 Money 자체가 성립 안 함
>         Objects.requireNonNull(currency);
>         if (amount.compareTo(BigDecimal.ZERO) < 0) {
>             throw new IllegalArgumentException("음수 금액 불가");
>         }
>     }
> }
>
> // [O] Validator 클래스: 비즈니스 규칙 (컨텍스트 의존)
> public class OrderValidator {
>     public static Result<Order, ValidationError> validate(Order order) {
>         // "주문 금액은 10,000원 이상" - 이건 비즈니스 정책
>         if (order.totalAmount().isLessThan(Money.krw(10_000))) {
>             return Result.failure(new MinimumOrderAmountError());
>         }
>         return Result.success(order);
>     }
> }
> ```
>
> **리트머스 테스트:**
> - "이 검증이 실패하면 객체 자체가 의미 없는가?" → Yes → Compact Constructor
> - "이 검증은 상황에 따라 달라지는가?" → Yes → Validator
>
> **dop-01과의 관계:**
> - dop-01 Code 1.7의 "Bad"는 **수십 가지 비즈니스 규칙**을 compact constructor에 넣은 경우
> - 여기 Code 2.3은 **기본 불변식**만 compact constructor에 있으므로 올바름

---

## 2.4 얕은 불변성 vs 깊은 불변성 (Shallow Immutability vs Deep Immutability)

> **다른 말로 (In other words):**
> - "얕은 불변성: 필드 참조는 바꿀 수 없지만, 참조하는 객체 내부는 변경 가능"
> - "깊은 불변성: 객체 전체와 그 내부까지 완전히 변경 불가능"

> **🎯 왜 배우는가?**
>
> "Record를 썼는데 왜 데이터가 바뀌지?"라는 당황스러운 경험이 있으신가요?
> 얕은 불변성과 깊은 불변성의 차이를 이해하면 **멀티스레드 환경에서도 안전한 불변 객체**를 만들 수 있고,
> 방어적 복사의 필요성을 명확히 알 수 있습니다.

### ❌ 안티패턴: 얕은 불변성의 함정

**왜 문제인가?**
- **외부 변경 가능**: Record 외부에서 내부 컬렉션을 수정할 수 있음
- **예측 불가능**: 언제 어디서 데이터가 바뀌는지 추적 불가
- **멀티스레드 위험**: 동시 수정으로 인한 경쟁 상태(Race Condition) 발생

Record는 필드 자체를 `final`로 만들지만, 필드가 참조하는 객체의 내부까지 얼리지는 못합니다.

**Code 2.4**: 불변성이 깨지는 코드 (안티패턴)
```java
// 위험한 코드!
public record Order(OrderId id, List<OrderItem> items) {}

// 문제 발생
List<OrderItem> mutableList = new ArrayList<>();
mutableList.add(new OrderItem(productId, quantity));

Order order = new Order(orderId, mutableList);

// Order 외부에서 내부 데이터 변경 가능!
mutableList.add(new OrderItem(anotherProduct, anotherQuantity));

// order.items()의 크기가 변경됨 - 불변성 파괴!
```

### ✅ 권장패턴: 방어적 복사로 깊은 불변성 확보

**왜 좋은가?**
- **완전한 불변성**: 외부에서 내부 상태 변경 불가능
- **스레드 안전**: 동기화 없이 여러 스레드에서 안전하게 공유
- **디버깅 용이**: 생성 시점 이후 상태가 절대 변하지 않음

**Code 2.5**: 방어적 복사로 깊은 불변성 확보
```java
public record Order(OrderId id, List<OrderItem> items) {
    // Compact Constructor에서 방어적 복사
    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(items);
        // 불변 리스트로 복사 - 외부에서 변경 불가
        items = List.copyOf(items);
    }
}

// 이제 안전!
List<OrderItem> mutableList = new ArrayList<>();
mutableList.add(new OrderItem(productId, quantity));

Order order = new Order(orderId, mutableList);

// 원본 리스트를 수정해도 Order에 영향 없음
mutableList.add(new OrderItem(anotherProduct, anotherQuantity));

// order.items()는 여전히 1개의 아이템만 가짐
```

---

## 2.5 값 변경 패턴: with 메서드 (Value Update Pattern: Wither Methods)

> **다른 말로 (In other words):**
> - "불변 객체에서 값을 '변경'하려면 새 객체를 생성하고 반환"
> - "withXxx() 메서드로 하나의 필드만 바꾼 새 인스턴스를 생성"

> **🎯 왜 배우는가?**
>
> 불변 객체인데 값을 어떻게 "변경"하지?라는 의문이 드시나요?
> with 패턴을 익히면 **불변성을 유지하면서도 직관적으로 상태를 업데이트**할 수 있고,
> 함수형 프로그래밍의 핵심 기법을 자연스럽게 적용할 수 있습니다.

불변 객체에서 값을 "변경"하려면 새 객체를 만들어야 합니다.

> 💡 **JEP 468 미포함 안내**: Java 25까지도 `with` expression은 정식 기능으로
> 포함되지 않았습니다. 따라서 수동으로 `withXxx()` 메서드를 작성해야 합니다.

**Code 2.6**: Wither 패턴 - 불변 객체의 값 변경
```java
public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderItem> items,
    Money totalAmount,
    OrderStatus status
) {
    public Order {
        items = List.copyOf(items);
    }

    // with 패턴: 하나의 필드만 바꾼 새 객체 반환
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerId, items, totalAmount, newStatus);
    }

    public Order withTotalAmount(Money newAmount) {
        return new Order(id, customerId, items, newAmount, status);
    }
}

// 사용
Order unpaidOrder = new Order(id, customerId, items, total, new Unpaid());
Order paidOrder = unpaidOrder.withStatus(new Paid(LocalDateTime.now(), paymentId));
// unpaidOrder는 여전히 Unpaid 상태 (불변)
```

> **💡 스타일 가이드: Record에서 `this.` 접두사**
>
> ```java
> // 스타일 A: this. 생략 (권장)
> public Order withStatus(OrderStatus newStatus) {
>     return new Order(id, customerId, items, totalAmount, newStatus);
> }
>
> // 스타일 B: this. 명시
> public Order withStatus(OrderStatus newStatus) {
>     return new Order(this.id, this.customerId, this.items, this.totalAmount, newStatus);
> }
> ```
>
> **권장: 스타일 A (this. 생략)**
>
> | 이유 | 설명 |
> |-----|------|
> | **Record 관용적 스타일** | Record는 투명한 데이터 캐리어, 간결함이 미덕 |
> | **이름 충돌 없음** | 파라미터가 `newStatus`이므로 `status`와 충돌 없음 |
> | **가독성** | 5개 필드를 한 줄에 쓸 때 `this.`가 없으면 더 읽기 쉬움 |
>
> **단, 이름이 충돌할 때는 `this.` 필수:**
> ```java
> // 파라미터명이 필드명과 같을 때
> public Order withStatus(OrderStatus status) {
>     return new Order(this.id, this.customerId, this.items, this.totalAmount, status);
>     //                ^^^^^^^ this. 필수 (status와 구분)
> }
> ```
>
> **팀 규칙이 있다면 팀 규칙을 따르세요.** 일관성이 스타일보다 중요합니다.

---

## 퀴즈 Chapter 2 (Quiz Chapter 2)

### Q2.1 [개념 확인] Identity vs Value
다음 중 **Value Type**으로 모델링해야 하는 것은?

A. 고객 (Customer)<br/>
B. 주문 금액 (OrderAmount)<br/>
C. 상품 (Product)<br/>
D. 장바구니 (ShoppingCart)

---

### Q2.2 [코드 분석] 불변성 위반
다음 코드에서 불변성이 깨지는 지점은?

```java
public record ShoppingCart(CartId id, List<CartItem> items) {}

public class CartService {
    public void addItem(ShoppingCart cart, CartItem item) {
        cart.items().add(item);  // Line A
    }

    public ShoppingCart createCart(CartId id) {
        return new ShoppingCart(id, new ArrayList<>());  // Line B
    }
}
```

A. Line A만 문제<br/>
B. Line B만 문제<br/>
C. Line A와 Line B 모두 문제<br/>
D. 문제없음

---

### Q2.3 [함정 문제] 불변성의 함정 ⭐
다음 코드를 실행했을 때, 콘솔에 출력되는 결과는?

```java
import java.util.*;

public record Team(String name, List<String> members) {
    // 콤팩트 생성자가 정의되지 않음 (기본 생성자 사용)
}

public class Main {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("Alice");

        Team team = new Team("Alpha", list);
        list.add("Bob");

        System.out.println(team.members());
    }
}
```

A. `[Alice]`<br/>
B. `[Alice, Bob]`<br/>
C. 컴파일 에러<br/>
D. `NullPointerException`

---

### Q2.4 [설계 문제] 적절한 타입 선택
이커머스에서 "배송 주소"를 모델링할 때 적절한 방식은?

A. Entity로 모델링 - 주소도 고유 ID가 있어야 함<br/>
B. Value Object로 모델링 - 주소 내용이 같으면 같은 주소<br/>
C. String으로 충분 - "서울시 강남구 역삼동"<br/>
D. Map<String, String>으로 모델링 - 유연성 확보

---

정답은 Appendix C에서 확인할 수 있습니다.
