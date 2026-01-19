# Chapter 2: 데이터란 무엇인가? (Identity vs Value)

## 학습 목표
1. 정체성(Identity)과 값(Value)의 개념적 차이를 명확히 구분할 수 있다
2. Java Record가 Value Type을 표현하는 방식을 이해한다
3. 얕은 불변성과 깊은 불변성의 차이를 설명할 수 있다
4. 방어적 복사를 통해 깊은 불변성을 확보할 수 있다
5. 이커머스 도메인에서 Identity와 Value를 올바르게 식별할 수 있다

---

## 2.1 정체성(Identity)과 값(Value)의 차이

시스템의 모든 데이터는 두 가지 범주 중 하나에 속합니다.

**Table 2.1**: Value와 Identity의 핵심 차이점

| 구분 | Value (값) | Identity (정체성) |
|-----|-----------|------------------|
| 동등성 | 내용이 같으면 같음 | ID가 같으면 같음 |
| 불변성 | 항상 불변 | 상태가 변할 수 있음 |
| 비교 방식 | `equals()` (값 비교) | `==` 또는 ID 비교 |
| 예시 | 금액, 좌표, 날짜 | 회원, 주문, 상품 |

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

## 2.2 이커머스에서의 Identity vs Value

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

## 2.3 Java Record: Value Type의 완벽한 도구

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

---

## 2.4 얕은 불변성 vs 깊은 불변성

### 얕은 불변성의 함정

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

## 2.5 값 변경 패턴: with 메서드

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

---

## 퀴즈 Chapter 2

### Q2.1 [개념 확인] Identity vs Value
다음 중 **Value Type**으로 모델링해야 하는 것은?

A. 고객 (Customer)
B. 주문 금액 (OrderAmount)
C. 상품 (Product)
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

A. Line A만 문제
B. Line B만 문제
C. Line A와 Line B 모두 문제
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

A. `[Alice]`
B. `[Alice, Bob]`
C. 컴파일 에러
D. `NullPointerException`

---

### Q2.4 [설계 문제] 적절한 타입 선택
이커머스에서 "배송 주소"를 모델링할 때 적절한 방식은?

A. Entity로 모델링 - 주소도 고유 ID가 있어야 함
B. Value Object로 모델링 - 주소 내용이 같으면 같은 주소
C. String으로 충분 - "서울시 강남구 역삼동"
D. Map<String, String>으로 모델링 - 유연성 확보

---

정답은 Appendix C에서 확인할 수 있습니다.
