# Chapter 3: 타입 시스템의 기수(Cardinality) 이론

## 학습 목표
1. 기수(Cardinality)의 개념을 이해하고 타입의 상태 개수를 계산할 수 있다
2. 곱 타입(Product Type)이 상태 폭발을 일으키는 원리를 설명할 수 있다
3. 합 타입(Sum Type)이 상태를 축소하는 원리를 설명할 수 있다
4. Sealed Interface를 사용해 합 타입을 구현할 수 있다
5. 실제 도메인에서 상태 수를 계산하고 최적화할 수 있다

---

## 3.1 기수(Cardinality)란?

시스템의 복잡도는 **"가능한 상태의 총 개수(Cardinality)"**와 비례합니다.

```
복잡도 ∝ 가능한 상태의 수 (Cardinality)
```

| 타입 | 기수(Cardinality) |
|-----|------------------|
| `boolean` | 2 (true, false) |
| `byte` | 256 |
| `int` | 2^32 ≈ 40억 |
| `String` | ∞ (무한) |
| `Optional<T>` | \|T\| + 1 |
| `enum Status { A, B, C }` | 3 |

---

## 3.2 곱 타입 (Product Type): 상태의 폭발

필드를 추가하는 것은 경우의 수를 **곱하는(Multiply)** 행위입니다.

### Before: 곱 타입으로 인한 상태 폭발

```java
// 각 boolean 필드는 2가지 상태
class Order {
    boolean isCreated;    // 2가지
    boolean isPaid;       // 2가지
    boolean isShipped;    // 2가지
    boolean isDelivered;  // 2가지
    boolean isCanceled;   // 2가지
}
// 총 상태 수 = 2 × 2 × 2 × 2 × 2 = 32가지
// 하지만 유효한 상태는 단 5가지뿐!
// 나머지 27가지는 "불가능한 상태"
```

### After: 합 타입으로 상태 축소

```java
// 합 타입: 5가지 상태만 가능
sealed interface OrderStatus permits
    Created, Paid, Shipped, Delivered, Canceled {}

record Created(LocalDateTime at) implements OrderStatus {}
record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {}
record Shipped(LocalDateTime at, TrackingNumber tracking) implements OrderStatus {}
record Delivered(LocalDateTime at, ReceiverName receiver) implements OrderStatus {}
record Canceled(LocalDateTime at, CancelReason reason) implements OrderStatus {}

// 총 상태 수 = 1 + 1 + 1 + 1 + 1 = 5가지
// 복잡도가 84% 감소!
```

---

## 3.3 enum vs sealed interface

> **💡 Q&A: enum으로도 충분하지 않나요?**
>
> **결론**: 데이터의 모양(구조)이 다를 수 있느냐가 결정적인 차이입니다.
>
> **Enum의 한계**: 모든 상수가 똑같은 필드 구조를 가져야 합니다.
> ```java
> public enum DeliveryStatus {
>     PREPARING(null, null),     // 불필요한 null
>     SHIPPED("12345", null),    // 배송일시는 null
>     DELIVERED("12345", LocalDateTime.now());
>
>     private final String trackingNumber;  // 모든 필드를 가져야 함
>     private final LocalDateTime deliveredAt;
> }
> ```
>
> **Sealed Interface의 강점**: 각 상태마다 다른 데이터를 가질 수 있습니다.
> ```java
> sealed interface DeliveryStatus {}
> record Preparing() implements DeliveryStatus {}  // 아무것도 없음
> record Shipped(String trackingNumber) implements DeliveryStatus {}  // 송장만
> record Delivered(String trackingNumber, LocalDateTime deliveredAt)
>     implements DeliveryStatus {}  // 송장 + 시간
> ```

---

## 퀴즈 Chapter 3

### Q3.1 [개념 확인] 기수 계산
다음 타입의 기수(Cardinality)는?

```java
enum Size { S, M, L, XL }
enum Color { RED, BLUE, GREEN }

record Product(Size size, Color color) {}
```

A. 7 (4 + 3)
B. 12 (4 × 3)
C. 16 (4^2)
D. 81 (3^4)

---

### Q3.2 [함정 문제] 봉인된 운명 ⭐
다음과 같이 완벽한 sealed interface와 switch 문을 작성하여 배포했습니다.

```java
public sealed interface LoginResult permits Success, Failure {}
public record Success() implements LoginResult {}
public record Failure(String reason) implements LoginResult {}

// 로직 코드
public String handleLogin(LoginResult result) {
    return switch (result) {
        case Success s -> "Welcome!";
        case Failure f -> "Error: " + f.reason();
    };
}
```

일주일 뒤, 동료가 `LoginResult`에 `record Timeout() implements LoginResult {}`를 추가하고
`permits` 절에도 `Timeout`을 넣었습니다. 하지만 `handleLogin` 메서드는 수정하지 않았습니다.

이때 컴파일러의 반응은?

A. 에러 없음: 런타임에 Timeout이 들어오면 예외 발생
B. 컴파일 에러: switch 식이 모든 경우를 커버하지 않음
C. 에러 없음: Timeout은 자동으로 무시됨 (null 반환)
D. 워닝(Warning): "모든 케이스를 다루지 않았습니다" 경고만 발생

---

정답은 Appendix C에서 확인할 수 있습니다.
