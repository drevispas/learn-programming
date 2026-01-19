# Chapter 6: Railway Oriented Programming

> Part II: 심화 - 워크플로우와 에러 처리

---

### 학습 목표
1. Exception의 문제점을 이해한다
2. Result 타입으로 성공/실패를 명시적으로 표현할 수 있다
3. map, flatMap 연산의 의미와 사용법을 익힌다
4. 에러를 값으로 다루는 함수형 에러 처리를 구현할 수 있다

---

### 6.1 Exception의 문제점

#### 💡 비유: 비상 탈출구 vs 두 갈래 길
> **Exception은 비상 탈출구입니다.**
>
> 비상 탈출구는 건물 어디서든 뛰어내릴 수 있습니다.
> 하지만 어디로 떨어질지, 누가 받아줄지 알 수 없습니다.
>
> Exception도 마찬가지입니다:
> - 어디서든 throw 가능
> - 어디서 catch될지 예측 불가
> - 중간 코드들을 모두 건너뜀
>
> **Result 타입은 두 갈래 길입니다.**
>
> 기차역에서 "성공행"과 "실패행" 두 플랫폼이 있습니다.
> 어떤 열차를 타든 목적지가 명확합니다.
> 중간에 건너뛰거나 예상치 못한 곳으로 가지 않습니다.

#### Exception의 구체적 문제점

**코드 6.1**: Exception의 문제점들
```java
// 문제 1: 시그니처가 거짓말을 함
public Order createOrder(OrderRequest request) {
    // 시그니처: "항상 Order를 반환합니다"
    // 현실: 재고 부족, 잘못된 쿠폰 등으로 예외 발생 가능
    if (outOfStock) throw new OutOfStockException();
    if (invalidCoupon) throw new InvalidCouponException();
    return new Order(...);
}

// 문제 2: 어디서 catch될지 모름
try {
    Order order = createOrder(request);
    Payment payment = processPayment(order);  // 여기서도 예외 가능
    sendNotification(order);  // 여기서도 예외 가능
} catch (Exception e) {
    // 어떤 단계에서 실패했는지 알기 어려움
}

// 문제 3: 흐름 제어가 GOTO와 유사
void methodA() { methodB(); }
void methodB() { methodC(); }
void methodC() { throw new SomeException(); }  // 바로 catch로 점프!
```

#### ❌ Anti-pattern: Exception for Control Flow

**왜 나쁜가?**
1. **시그니처 거짓말**: 메서드 시그니처가 실패 가능성을 숨김
2. **비지역적 점프**: 코드가 GOTO처럼 예측 불가능하게 점프
3. **에러 처리 강제 불가**: try-catch 없어도 컴파일 성공

**반박 예상 질문:**
> "Java의 Checked Exception은 이 문제를 해결하지 않나요?"

**답변:** Checked Exception은 throws 절로 명시하지만, 결국 RuntimeException으로 감싸거나 무시하는 경우가 많습니다. 또한 함수형 합성이 어려워집니다.

---

### 6.2 Result 타입: 철도 분기점

#### 💡 비유: 철도 분기점
> **Result 타입은 철도 분기점입니다.**
>
> 기차가 분기점에 도착하면:
> - **성공**: 녹색 선로로 계속 진행
> - **실패**: 빨간 선로로 분기
>
> 한번 빨간 선로에 들어가면 녹색 역들을 모두 건너뜁니다.
> 하지만 선로를 벗어나지 않고 안전하게 종착역에 도착합니다.

**그림 6.1**: Railway-Oriented Programming - 성공/실패 트랙

![Railway Switch](https://fsharpforfunandprofit.com/posts/recipe-part2/Recipe_RailwaySwitch.png)

*출처: [Railway Oriented Programming - F# for Fun and Profit](https://fsharpforfunandprofit.com/posts/recipe-part2/) - Scott Wlaschin*

**그림 6.2**: Railway-Oriented Programming - 함수들의 연결

![Railway Compose](https://fsharpforfunandprofit.com/posts/recipe-part2/Recipe_Railway_Compose2.png)

*출처: [Railway Oriented Programming - F# for Fun and Profit](https://fsharpforfunandprofit.com/posts/recipe-part2/) - Scott Wlaschin*

#### Result 타입 정의

**코드 6.2**: Result 타입 구현
```java
package com.ecommerce.common;

import java.util.function.Function;

// 성공(S) 또는 실패(F)를 담는 컨테이너
public sealed interface Result<S, F> permits Success, Failure {

    // 성공 여부 확인
    boolean isSuccess();
    boolean isFailure();

    // 값 추출 (사용 주의)
    S value();
    F error();

    // 팩토리 메서드
    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }

    // 실패 값을 변환 (성공이면 그대로)
    default <NewF> Result<S, NewF> mapError(Function<F, NewF> mapper) {
        return switch (this) {
            case Success<S, F> s -> (Result<S, NewF>) s;
            case Failure<S, F> f -> Result.failure(mapper.apply(f.error()));
        };
    }
}

public record Success<S, F>(S value) implements Result<S, F> {
    @Override public boolean isSuccess() { return true; }
    @Override public boolean isFailure() { return false; }
    @Override public F error() { throw new IllegalStateException("Success has no error"); }
}

public record Failure<S, F>(F error) implements Result<S, F> {
    @Override public boolean isSuccess() { return false; }
    @Override public boolean isFailure() { return true; }
    @Override public S value() { throw new IllegalStateException("Failure has no value"); }
}
```

> ⚠️ **흔한 실수**: `new` 키워드 누락
>
> ```java
> // ❌ return Success<>(new User(...));
> // ✅ return new Success<>(new User(...));
> ```

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Result/Either 패턴은 다음에서 표준으로 사용됩니다:
- Rust의 `Result<T, E>`
- Kotlin의 `Result<T>` (stdlib)
- Vavr (Java)의 `Either<L, R>`, `Try<T>`
- Arrow (Kotlin)의 `Either<A, B>`

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "Railway Oriented Programming은 에러 처리를 일급 시민으로 만든다."
- **Martin Odersky** (Scala 창시자): "Either 타입은 예외의 대안으로, 함수형 합성을 가능하게 한다."

**참고 자료:**
- [Railway Oriented Programming](https://fsharpforfunandprofit.com/rop/) - Scott Wlaschin

---

### 6.2.1 컨테이너와 변환 - 왜 map이 필요한가?

#### 💡 비유: 선물 포장

> **map은 선물 상자 안의 내용물만 바꾸는 것입니다.**
>
> 상자(Result)를 열어서, 안에 든 물건(Order)을 다른 물건(OrderDto)으로 교체하고,
> 다시 같은 종류의 상자에 넣어 돌려줍니다.
>
> 상자가 빈 상태(Failure)라면? 상자를 열어봐도 바꿀 게 없으니
> 그냥 빈 상자를 그대로 돌려줍니다.

#### 문제: map 없이 값을 변환하려면?

Result 안의 값을 변환하려면 매번 switch로 분기해야 합니다:

**코드 6.2.1a**: map 없이 Result 안의 값 변환
```java
// 매번 이렇게 해야 하나요?
Result<OrderDto, OrderError> convertToDto(Result<Order, OrderError> result) {
    return switch (result) {
        case Success<Order, OrderError> s -> Result.success(toDto(s.value()));
        case Failure<Order, OrderError> f -> (Result<OrderDto, OrderError>) f;
    };
}

// 이것도요?
Result<String, OrderError> getOrderId(Result<Order, OrderError> result) {
    return switch (result) {
        case Success<Order, OrderError> s -> Result.success(s.value().id());
        case Failure<Order, OrderError> f -> (Result<String, OrderError>) f;
    };
}
```

이 패턴이 반복됩니다:
1. Success면 값을 꺼내서 변환하고 다시 Success로 감싼다
2. Failure면 그대로 반환한다

**map은 이 반복 패턴을 추상화한 것입니다.**

#### 다이어그램: map의 동작

```
=== map: 변환 함수가 순수 값을 반환할 때 ===

   f: A -> B (절대 실패 안 함)

   ┌──────────┐     map(f)      ┌──────────┐
   │ Success  │ ───────────────→│ Success  │
   │    A     │       f(A)=B    │    B     │
   └──────────┘                 └──────────┘

   ┌──────────┐     map(f)      ┌──────────┐
   │ Failure  │ ───────────────→│ Failure  │
   │   err    │   (f 실행 안됨) │   err    │
   └──────────┘                 └──────────┘
```

#### Before/After 비교

**코드 6.2.1b**: map 사용 전후 비교
```java
// ❌ Before: 반복적인 switch 문
Result<OrderDto, OrderError> convertToDto(Result<Order, OrderError> result) {
    return switch (result) {
        case Success<Order, OrderError> s -> Result.success(toDto(s.value()));
        case Failure<Order, OrderError> f -> (Result<OrderDto, OrderError>) f;
    };
}

// ✅ After: map으로 간결하게
Result<OrderDto, OrderError> convertToDto(Result<Order, OrderError> result) {
    return result.map(this::toDto);
}
```

#### 핵심 포인트

> 📌 **map은 "컨테이너 안의 값을 변환하되, 컨테이너 구조는 유지"하는 패턴입니다.**
>
> 이 패턴은 Result뿐 아니라 Optional, List, Stream에도 동일하게 적용됩니다:
> - `Optional.map()` - Optional 안의 값 변환
> - `Stream.map()` - Stream 각 요소 변환
> - `Result.map()` - Result 안의 성공 값 변환

**퀴즈 Q6.X1**: map과 flatMap 중 어떤 것을 사용해야 할까요?
> 변환 함수의 반환 타입이 `A -> B`이면 **map**, `A -> Result<B, E>`이면 **flatMap**

---

### 6.3 map과 flatMap: 역 환승

#### 💡 비유: 역 환승
> **map은 같은 노선 내 이동입니다.**
>
> 성공 선로에서 값을 변환하지만 선로는 그대로입니다.
> `A → B` 형태의 순수 함수를 적용합니다.
>
> **flatMap은 환승입니다.**
>
> 다른 노선(다른 Result)으로 갈아탈 수 있습니다.
> `A → Result<B, E>` 형태의 함수를 적용합니다.

#### map 연산

**코드 6.3**: map 연산 구현
```java
// map: 성공 값 변환 (실패면 그대로 통과)
public <NewS> Result<NewS, F> map(Function<S, NewS> mapper) {
    return switch (this) {
        case Success<S, F> s -> Result.success(mapper.apply(s.value()));
        case Failure<S, F> f -> (Result<NewS, F>) f;  // 실패는 그대로
    };
}

// 사용 예
Result<ValidatedOrder, OrderError> validated = validateOrder(input);
Result<PricedOrder, OrderError> priced = validated.map(order -> priceOrder(order));
// validateOrder가 실패하면 priceOrder는 실행되지 않음
```

#### flatMap 연산

**코드 6.4**: flatMap 연산 구현
```java
// flatMap: 결과가 Result인 함수 적용
public <NewS> Result<NewS, F> flatMap(Function<S, Result<NewS, F>> mapper) {
    return switch (this) {
        case Success<S, F> s -> mapper.apply(s.value());
        case Failure<S, F> f -> (Result<NewS, F>) f;
    };
}

// 사용 예: 각 단계가 실패할 수 있을 때
Result<PaidOrder, OrderError> result = validateOrder(input)
    .flatMap(this::checkInventory)      // 재고 확인 (실패 가능)
    .flatMap(this::processPayment);     // 결제 (실패 가능)
```

**표 6.1**: map vs flatMap 비교

| 연산 | 변환 함수 시그니처 | 사용 시점 |
|-----|-------------------|----------|
| `map` | `A → B` | 변환이 절대 실패하지 않을 때 |
| `flatMap` | `A → Result<B, E>` | 변환이 실패할 수 있을 때 |

> ⚠️ **흔한 실수**: 타입 불일치
>
> ```java
> String r = switch (result) {
>     case Success<Integer> s -> s.value();  // ❌ Integer인데 String 기대
>     case Failure<String> f -> f.error();
> };
>
> // ✅ String.valueOf(s.value())로 변환 필요
> ```

---

### 6.3.1 왜 flatMap이 필요한가?

#### 💡 비유: 택배 재포장

> **map으로 "Result를 반환하는 함수"를 적용하면 상자 안에 또 상자가 들어갑니다.**
>
> 예를 들어, `Result<Order, Error>`에 `Order -> Result<PricedOrder, Error>` 함수를 map으로 적용하면
> `Result<Result<PricedOrder, Error>, Error>` - 이중 포장이 됩니다!
>
> **flatMap은 "상자를 열어서, 안에 든 상자의 내용물만 꺼내는" 것입니다.**
> 이중 포장을 단일 포장으로 "평평하게(flat)" 만들어줍니다.

#### 문제: 실패할 수 있는 연산을 연쇄하려면?

주문 처리에서 각 단계가 실패할 수 있습니다:

**코드 6.3.1a**: map을 잘못 사용한 경우
```java
// 문제: map을 사용하면 Result가 중첩됨
Result<Order, Error> validated = validateOrder(input);

// checkInventory는 Result를 반환하는 함수
// Result<Result<Order, Error>, Error> - 이중 포장!
var nested = validated.map(this::checkInventory);  // ❌

// 계속하면 더 심해짐
// Result<Result<Result<Order, Error>, Error>, Error>
var moreNested = nested.map(???);  // ❌❌
```

**flatMap은 이 중첩을 방지합니다:**

**코드 6.3.1b**: flatMap으로 올바르게 연쇄
```java
// flatMap은 Result를 반환하는 함수를 받아서
// 중첩 없이 단일 Result로 반환
Result<PaidOrder, Error> result = validateOrder(input)
    .flatMap(this::checkInventory)   // Result<Order, Error>
    .flatMap(this::processPayment);  // Result<PaidOrder, Error>
    // 중첩 없이 깔끔!
```

#### 다이어그램: flatMap의 동작

```
=== flatMap: 변환 함수가 Result를 반환할 때 ===

   g: A -> Result<B, E> (실패할 수 있음)

   ┌──────────┐    flatMap(g)   ┌──────────┐
   │ Success  │ ───────────────→│ Success  │ (g가 성공하면)
   │    A     │     g(A)        │    B     │
   └──────────┘                 └──────────┘
                                    또는
                                ┌──────────┐
                                │ Failure  │ (g가 실패하면)
                                │  err2    │
                                └──────────┘

   ┌──────────┐    flatMap(g)   ┌──────────┐
   │ Failure  │ ───────────────→│ Failure  │
   │   err    │   (g 실행 안됨) │   err    │
   └──────────┘                 └──────────┘
```

#### map과 flatMap의 핵심 차이

```
=== map vs flatMap 비교 ===

map:     Success(A)  ---f--->  Success(f(A))     f: A -> B
flatMap: Success(A)  ---g--->  g(A)              g: A -> Result<B, E>
                               (Success 또는 Failure)
```

#### 퀴즈: 언제 무엇을 사용할까?

**표 6.2**: map vs flatMap 선택 가이드

| 변환 함수 | 예시 | 사용할 연산 |
|----------|-----|-----------|
| `Order -> OrderDto` | DTO 변환 | **map** |
| `Order -> String` | ID 추출 | **map** |
| `Order -> Result<PricedOrder, E>` | 가격 계산 (실패 가능) | **flatMap** |
| `Order -> Result<Order, E>` | 재고 확인 (실패 가능) | **flatMap** |

> 💡 **기억하기**: 함수가 `Result`를 반환하면 **flatMap**, 아니면 **map**

---

### 6.4 ROP 패턴 적용

**코드 6.5**: Railway Oriented Programming 전체 예시
```java
public class PlaceOrderWorkflow {

    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder(command)
            .flatMap(this::checkInventory)
            .flatMap(this::applyCoupon)
            .flatMap(this::processPayment)
            .map(this::createEvent);  // 최종 변환은 map
    }

    // 각 단계가 Result 반환
    private Result<ValidatedOrder, OrderError> validateOrder(PlaceOrderCommand command) {
        if (command.lines().isEmpty()) {
            return Result.failure(new OrderError.EmptyOrder());
        }
        // ... 검증 로직
        return Result.success(new ValidatedOrder(...));
    }

    private Result<ValidatedOrder, OrderError> checkInventory(ValidatedOrder order) {
        for (var line : order.lines()) {
            if (inventoryService.getStock(line.productId()) < line.quantity().value()) {
                return Result.failure(new OrderError.OutOfStock(line.productId()));
            }
        }
        return Result.success(order);
    }

    private Result<PricedOrder, OrderError> applyCoupon(ValidatedOrder order) {
        return couponService.validate(order.couponCode())
            .map(coupon -> priceOrder(order, coupon));
    }

    private Result<PaidOrder, OrderError> processPayment(PricedOrder order) {
        return paymentGateway.charge(order.totalAmount())
            .mapError(e -> new OrderError.PaymentFailed(e.message()));
    }
}
```

> 💡 `inventoryService.getStock()`, `couponService.validate()`, `paymentGateway.charge()`의
> 전체 구현은 `examples/functional-domain-modeling/` 프로젝트에서 확인할 수 있습니다.

---

### 6.5 에러 타입도 Sum Type으로

**코드 6.6**: 도메인 에러를 Sum Type으로 정의
```java
// 도메인 에러를 sealed interface로 정의
public sealed interface OrderError permits
    OrderError.EmptyOrder,
    OrderError.InvalidCustomer,
    OrderError.OutOfStock,
    OrderError.InvalidCoupon,
    OrderError.PaymentFailed {

    record EmptyOrder() implements OrderError {}
    record InvalidCustomer(String customerId) implements OrderError {}
    record OutOfStock(ProductId productId) implements OrderError {}
    record InvalidCoupon(CouponCode code, String reason) implements OrderError {}
    record PaymentFailed(String reason) implements OrderError {}
}

// 에러 처리 - 모든 케이스 처리 강제
String handleError(OrderError error) {
    return switch (error) {
        case OrderError.EmptyOrder e -> "주문 상품이 없습니다";
        case OrderError.InvalidCustomer e -> "존재하지 않는 고객: " + e.customerId();
        case OrderError.OutOfStock e -> "품절된 상품: " + e.productId();
        case OrderError.InvalidCoupon e -> "쿠폰 오류: " + e.reason();
        case OrderError.PaymentFailed e -> "결제 실패: " + e.reason();
    };
}
```

---

### 퀴즈 Chapter 6

#### Q6.1 [개념 확인] Exception 문제

Exception의 문제점이 아닌 것은?

**A.** 함수 시그니처가 실패 가능성을 표현하지 못함<br/>
**B.** 흐름 제어가 GOTO와 유사함<br/>
**C.** 메모리를 많이 사용함 *(정답)*<br/>
**D.** 어디서 catch될지 예측하기 어려움

---

#### Q6.2 [코드 분석] map vs flatMap

다음 중 `flatMap`을 사용해야 하는 경우는?

**A.** `String` → `UppercaseString` 변환<br/>
**B.** `ValidatedOrder` → `Result<PricedOrder, Error>` 변환 *(정답)*<br/>
**C.** `Money` → `String` 변환<br/>
**D.** `List<Order>` → `Stream<Order>` 변환

---

#### Q6.3 [코드 분석] ROP 파이프라인

다음 코드에서 `checkInventory`가 실패하면?

**코드 6.7**: ROP 파이프라인 동작 분석
```java
validateOrder(input)
    .flatMap(this::checkInventory)
    .flatMap(this::processPayment)
    .map(this::createEvent);
```

**A.** processPayment가 실행된다<br/>
**B.** createEvent가 실행된다<br/>
**C.** Failure가 반환되고 이후 단계는 실행되지 않는다 *(정답)*<br/>
**D.** NullPointerException이 발생한다

---

정답은 Appendix D에서 확인할 수 있습니다.
