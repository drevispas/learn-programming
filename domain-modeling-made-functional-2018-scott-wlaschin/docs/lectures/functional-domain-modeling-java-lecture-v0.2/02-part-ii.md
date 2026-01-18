# Part II: 심화 - 워크플로우와 에러 처리

---

## Chapter 5: 워크플로우를 함수 파이프라인으로

#### 🎯 WHY: 비즈니스 프로세스 = 함수

#### 💡 비유: 공장 조립 라인
> **비즈니스 프로세스는 공장 조립 라인입니다.**
> - **철판** → 프레스 → **차체**
> - **차체** → 도색 → **도색된 차체**
> - **도색된 차체** → 조립 → **완성차**

**코드 5.1**: 주문 프로세스 파이프라인
```
주문 프로세스:
  PlaceOrderCommand → [Validate] → ValidatedOrder
                           ↓
                      [Price] → PricedOrder
                           ↓
                      [Pay] → PaidOrder
                           ↓
                   OrderPlaced
```

---

### 5.1 중간 타입으로 데이터 품질 표현

**코드 5.2**: 워크플로우의 중간 타입들
```java
// 1. 명령 (Command) - 사용자의 의도 (검증 전)
public record UnvalidatedOrderLine(String productId, int quantity) {}

public record PlaceOrderCommand(
    String customerId,
    List<UnvalidatedOrderLine> lines,
    String shippingAddress,
    String couponCode
) {}

// 2. 검증 후 - 유효한 상태
public record ValidatedOrderLine(ProductId productId, Quantity quantity, Money unitPrice) {}

public record ValidatedOrder(
    CustomerId customerId,
    List<ValidatedOrderLine> lines,
    ShippingAddress shippingAddress,
    CouponCode couponCode
) {}

// 3. 가격 계산 후
public record PricedOrder(CustomerId customerId, Money totalAmount) {}

// 4. 이벤트 - 확정된 과거
public record OrderPlaced(OrderId orderId, Money totalAmount, LocalDateTime occurredAt) {}
```

> ⚠️ **흔한 실수**: Record 알맹이 꺼내기/포장 미흡
>
> ```java
> // ❌ raw.trim() - raw는 RawText, trim()은 String 메서드
> return raw.trim();
>
> // ✅ new SanitizedText(raw.s().trim())
> return new SanitizedText(raw.s().trim());
> ```

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. 워크플로우를 타입으로 표현하는 것은 다음에서 검증되었습니다:
- Stripe의 결제 파이프라인
- Uber의 주문 처리 시스템
- 대부분의 이벤트 소싱 시스템

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "각 단계의 출력을 다른 타입으로 표현하면 워크플로우가 자기 문서화된다."
- **Eric Evans**: "도메인 이벤트는 비즈니스적으로 의미 있는 상태 변화를 포착한다."

---

### 5.2 함수 합성

#### 💡 비유: 레고 블록
> 함수도 출력 타입 = 다음 입력 타입이면 조립됩니다.
> ```
> f: A → B
> g: B → C
> 합성: A → B → C
> ```

**코드 5.3**: 함수 합성으로 워크플로우 구현
```java
public class PlaceOrderWorkflow {
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder.apply(command)
            .map(priceOrder::apply)
            .flatMap(processPayment::apply)
            .map(this::createOrderPlaced);
    }
}
```

> 💡 전체 워크플로우 구현은 `examples/functional-domain-modeling/` 프로젝트의
> `PlaceOrderUseCase.java`에서 확인할 수 있습니다.

---

### 퀴즈 Chapter 5

#### Q5.1 [개념 확인] `PlaceOrderCommand`와 `ValidatedOrder`를 분리하는 이유는?

**A.** 메모리 절약<br/>
**B.** 검증 전후의 데이터가 다른 보장을 가지므로 *(정답)*<br/>
**C.** Java 문법 제약<br/>
**D.** 디버깅 용이

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 6: Railway Oriented Programming

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

---

## Chapter 7: 파이프라인 조립과 검증

### 학습 목표
1. Validation과 Result의 차이를 이해한다
2. Applicative 패턴으로 여러 검증을 병렬로 수행할 수 있다
3. Command와 Event 패턴을 이해한다
4. 실제 이커머스 검증 로직을 구현할 수 있다

---

### 7.1 Validation vs Result

#### 💡 비유: 시험 채점
> **Result는 첫 번째 오답에서 멈추는 채점입니다.**
>
> 10문제 중 3번에서 틀리면 바로 "불합격" 처리.
> 4번~10번이 맞는지 틀린지 알 수 없습니다.
>
> **Validation은 모든 문제를 채점합니다.**
>
> 10문제 모두 채점하고 "3번, 7번, 9번 오답" 리포트 제공.
> 학생이 자신의 모든 실수를 한번에 알 수 있습니다.

**코드 7.1**: Result vs Validation 비교
```java
// Result: 첫 에러에서 중단
Result<Order, Error> result = validateName(input)
    .flatMap(this::validateEmail)   // 이름 실패하면 여기 안 감
    .flatMap(this::validatePhone);  // 이메일 실패하면 여기 안 감

// Validation: 모든 에러 수집
Validation<Order, List<Error>> result = Validation.combine3(
    validateName(input),
    validateEmail(input),
    validatePhone(input),
    Order::new
);  // 모든 검증 결과를 모아서 처리
```

**표 7.1**: Result vs Validation 비교

| 특성 | Result | Validation |
|------|--------|------------|
| 에러 처리 | 첫 에러에서 중단 | 모든 에러 수집 |
| 적합한 경우 | 파이프라인 (순차 처리) | 폼 검증 (병렬 처리) |
| 에러 타입 | 단일 에러 | 에러 목록 |

---

### 7.2 Applicative 패턴: 여러 창구 동시 처리

#### 💡 비유: 은행 여러 창구
> **Applicative는 여러 창구에서 동시에 처리하는 것입니다.**
>
> 대출 신청 시:
> - 1번 창구: 신분증 확인
> - 2번 창구: 재직 증명
> - 3번 창구: 소득 증명
>
> 순차 처리(flatMap): 1번 끝나야 2번 시작
> 병렬 처리(Applicative): 세 창구 동시 처리, 결과 모아서 판단

**코드 7.2**: Validation 타입 구현
```java
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Validation 타입 - 에러를 수집하는 Applicative 패턴 구현.
 * E 타입 파라미터는 에러 컬렉션 타입 (보통 List<SomeError>)을 나타냅니다.
 */
public sealed interface Validation<S, E> permits Valid, Invalid {
    // 성공
    static <S, E> Validation<S, E> valid(S value) {
        return new Valid<>(value);
    }

    // 실패 (에러 컬렉션 전달)
    static <S, E> Validation<S, E> invalid(E errors) {
        return new Invalid<>(errors);
    }

    // 단일 에러로 Invalid 생성 (편의 메서드)
    static <S, E> Validation<S, List<E>> invalidOne(E error) {
        return new Invalid<>(List.of(error));
    }

    // 성공 값 변환 (실패면 그대로)
    default <NewS> Validation<NewS, E> map(Function<S, NewS> mapper) {
        return switch (this) {
            case Valid<S, E> v -> new Valid<>(mapper.apply(v.value()));
            case Invalid<S, E> i -> new Invalid<>(i.errors());
        };
    }

    // 여러 Validation 결합
    static <A, B, C, E> Validation<C, List<E>> combine(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        BiFunction<A, B, C> combiner
    ) {
        return switch (va) {
            case Valid<A, List<E>> a -> switch (vb) {
                case Valid<B, List<E>> b -> Validation.valid(combiner.apply(a.value(), b.value()));
                case Invalid<B, List<E>> b -> (Validation<C, List<E>>) b;
            };
            case Invalid<A, List<E>> a -> switch (vb) {
                case Valid<B, List<E>> b -> (Validation<C, List<E>>) a;
                case Invalid<B, List<E>> b -> {
                    List<E> errors = new ArrayList<>(a.errors());
                    errors.addAll(b.errors());
                    yield new Invalid<>(errors);
                }
            };
        };
    }

    // 3개 결합
    static <A, B, C, R, E> Validation<R, List<E>> combine3(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        Validation<C, List<E>> vc,
        TriFunction<A, B, C, R> combiner
    ) {
        return combine(
            combine(va, vb, Pair::new),
            vc,
            (ab, c) -> combiner.apply(ab.first(), ab.second(), c)
        );
    }

    // 4개 결합
    static <A, B, C, D, R, E> Validation<R, List<E>> combine4(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        Validation<C, List<E>> vc,
        Validation<D, List<E>> vd,
        QuadFunction<A, B, C, D, R> combiner
    ) {
        return combine(
            combine(va, vb, Pair::new),
            combine(vc, vd, Pair::new),
            (ab, cd) -> combiner.apply(
                ab.first(), ab.second(),
                cd.first(), cd.second()
            )
        );
    }
}

public record Valid<S, E>(S value) implements Validation<S, E> {}
public record Invalid<S, E>(E errors) implements Validation<S, E> {}
public record Pair<A, B>(A first, B second) {}

@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

@FunctionalInterface
public interface QuadFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}
```

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Applicative/Validation 패턴은 다음에서 사용됩니다:
- Vavr의 `Validation<E, T>`
- cats (Scala)의 `Validated`
- Arrow (Kotlin)의 `Validated`

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "Applicative 패턴은 독립적인 검증을 병렬로 수행하여 모든 에러를 수집할 수 있게 한다."

---

### 7.3 이커머스 검증 예시

**코드 7.3**: 이커머스 주문 검증 구현
```java
public class OrderValidationService {

    public Validation<ValidatedOrder, List<ValidationError>> validateOrder(
        PlaceOrderCommand command
    ) {
        // 각 필드 독립적으로 검증
        var customerValidation = validateCustomerId(command.customerId());
        var linesValidation = validateOrderLines(command.lines());
        var addressValidation = validateAddress(command.shippingAddress());
        var couponValidation = validateCoupon(command.couponCode());

        // 모든 검증 결과 결합
        return Validation.combine4(
            customerValidation,
            linesValidation,
            addressValidation,
            couponValidation,
            ValidatedOrder::new
        );
    }

    private Validation<CustomerId, List<ValidationError>> validateCustomerId(String input) {
        if (input == null || input.isBlank()) {
            return Validation.invalidOne(new ValidationError.Required("customerId"));
        }
        try {
            return Validation.valid(new CustomerId(Long.parseLong(input)));
        } catch (NumberFormatException e) {
            return Validation.invalidOne(new ValidationError.InvalidFormat("customerId", "숫자"));
        }
    }

    private Validation<List<ValidatedOrderLine>, List<ValidationError>> validateOrderLines(
        List<UnvalidatedOrderLine> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            return Validation.invalidOne(new ValidationError.Required("orderLines"));
        }

        List<ValidationError> errors = new ArrayList<>();
        List<ValidatedOrderLine> validatedLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var lineValidation = validateOrderLine(line, i);
            switch (lineValidation) {
                case Valid<ValidatedOrderLine, List<ValidationError>> v ->
                    validatedLines.add(v.value());
                case Invalid<ValidatedOrderLine, List<ValidationError>> inv ->
                    errors.addAll(inv.errors());
            }
        }

        return errors.isEmpty()
            ? Validation.valid(validatedLines)
            : new Invalid<>(errors);
    }

    // validateAddress, validateCoupon, validateOrderLine 등은
    // examples/functional-domain-modeling/ 프로젝트에서 확인하세요.
}

// 검증 에러 타입
public sealed interface ValidationError permits
    ValidationError.Required,
    ValidationError.InvalidFormat,
    ValidationError.TooLong,
    ValidationError.OutOfRange,
    ValidationError.NotFound {

    record Required(String field) implements ValidationError {}
    record InvalidFormat(String field, String expectedFormat) implements ValidationError {}
    record TooLong(String field, int maxLength) implements ValidationError {}
    record OutOfRange(String field, int min, int max) implements ValidationError {}
    record NotFound(String field, String value) implements ValidationError {}
}
```

---

### 7.4 Command와 Event

#### 💡 비유: 주문서와 영수증
> **Command는 주문서입니다.**
>
> "라떼 한 잔 주세요" - 요청하는 것
> 성공할지 실패할지는 아직 모릅니다.
>
> **Event는 영수증입니다.**
>
> "라떼 1잔 결제 완료" - 이미 일어난 사실
> 과거의 불변 기록입니다.

**코드 7.4**: Command와 Event 정의
```java
// Command: "이렇게 해주세요" (미래, 실패 가능)
public sealed interface OrderCommand permits
    PlaceOrderCommand, CancelOrderCommand, ShipOrderCommand {

    record PlaceOrderCommand(
        String customerId,
        List<OrderLineCommand> lines,
        String shippingAddress,
        String paymentMethodId
    ) implements OrderCommand {}

    record CancelOrderCommand(
        OrderId orderId,
        String reason
    ) implements OrderCommand {}

    record ShipOrderCommand(
        OrderId orderId,
        String trackingNumber
    ) implements OrderCommand {}
}

// Event: "이렇게 됐습니다" (과거, 불변)
public sealed interface OrderEvent permits
    OrderPlaced, OrderCancelled, OrderShipped, OrderDelivered {

    record OrderPlaced(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderCancelled(
        OrderId orderId,
        String reason,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderShipped(
        OrderId orderId,
        String trackingNumber,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderDelivered(
        OrderId orderId,
        LocalDateTime occurredAt
    ) implements OrderEvent {}
}
```

**표 7.2**: Command vs Event 비교

| 특성 | Command | Event |
|------|---------|-------|
| 시제 | 미래/명령형 | 과거/완료형 |
| 결과 | 성공 또는 실패 가능 | 이미 발생한 사실 |
| 변경 | 변경 요청 | 불변 |
| 예시 | PlaceOrderCommand | OrderPlaced |

---

### 퀴즈 Chapter 7

#### Q7.1 [개념 확인] Validation vs Result

Validation이 Result보다 적합한 경우는?

**A.** 파이프라인에서 첫 에러에 바로 중단하고 싶을 때<br/>
**B.** 폼 검증에서 모든 에러를 한번에 보여주고 싶을 때 *(정답)*<br/>
**C.** 결제 처리처럼 순차적으로 진행해야 할 때<br/>
**D.** 단일 값 검증

---

#### Q7.2 [코드 분석] Applicative

Applicative 패턴의 특징이 아닌 것은?

**A.** 여러 검증을 독립적으로 수행<br/>
**B.** 모든 에러를 수집<br/>
**C.** 첫 에러에서 중단 *(정답)*<br/>
**D.** 검증 결과를 결합

---

#### Q7.3 [설계 문제] Command vs Event

다음 중 Event의 특징은?

**A.** 미래에 수행할 작업을 나타냄<br/>
**B.** 실패할 수 있음<br/>
**C.** 이미 발생한 불변의 사실 *(정답)*<br/>
**D.** 요청을 나타냄

---

#### Q7.4 [코드 분석] 검증 결합

다음 코드의 결과는?

**코드 7.5**: Validation.combine 동작
```java
Validation.combine(
    Validation.invalidOne(new Error("이름 필수")),
    Validation.invalidOne(new Error("이메일 형식 오류")),
    (name, email) -> new User(name, email)
);
```

**A.** Valid(User)<br/>
**B.** Invalid([이름 필수])<br/>
**C.** Invalid([이메일 형식 오류])<br/>
**D.** Invalid([이름 필수, 이메일 형식 오류]) *(정답)*

---

#### Q7.5 [설계 문제] 이벤트 네이밍

`OrderPlaced` 이벤트의 네이밍이 좋은 이유는?

**A.** 동사 원형을 사용해서<br/>
**B.** 과거형으로 "이미 일어난 일"을 표현해서 *(정답)*<br/>
**C.** 명사를 사용해서<br/>
**D.** 짧아서

---

정답은 Appendix D에서 확인할 수 있습니다.
