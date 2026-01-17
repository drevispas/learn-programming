# Part II: 심화 - 워크플로우와 에러 처리

---

## Chapter 5: 워크플로우를 함수 파이프라인으로

### 학습 목표
1. 비즈니스 프로세스를 **Command → Process → Event** 구조로 이해한다
2. 상태 전이를 타입 변환으로 표현할 수 있다
3. 함수 합성으로 복잡한 워크플로우를 구성할 수 있다
4. 각 단계의 책임을 명확히 분리할 수 있다

---

### 5.1 비즈니스 프로세스 = 함수

#### 비유: 공장 조립 라인

> **비즈니스 프로세스는 공장 조립 라인입니다.**
>
> - **철판** → 프레스 → **차체**
> - **차체** → 도색 → **도색된 차체**
> - **도색된 차체** → 조립 → **완성차**
>
> 소프트웨어도 마찬가지입니다. 우리는 입력을 받아 변환하고, 최종 결과를 만들어냅니다.
> 함수형 아키텍처에서 가장 중요한 패턴은 **Command(명령)** 를 받아 **Event(사건)** 를 발생시키는 것입니다.

```
주문 프로세스:
  PlaceOrderCommand (요청)
      ↓
  [Validate] → ValidatedOrder
      ↓
  [Price] → PricedOrder
      ↓
  [Pay] → PaidOrder
      ↓
  OrderPlacedEvent (결과)
```

---

### 5.2 워크플로우 타입 설계

```java
// 1. 명령 (Command) - 사용자의 의도
public record PlaceOrderCommand(
    String customerId,
    List<UnvalidatedOrderLine> lines,
    String couponCode
) {}

// 2. 검증 후 (도메인 타입) - 유효한 상태
public record ValidatedOrder(
    CustomerId customerId,
    List<ValidatedOrderLine> lines,
    Optional<CouponCode> couponCode
) {}

// 3. 가격 계산 후 - 계산된 상태
public record PricedOrder(
    CustomerId customerId,
    List<PricedOrderLine> lines,
    Money subtotal,
    Money discount,
    Money totalAmount
) {}

// 4. 이벤트 (Event) - 확정된 과거
public record OrderPlacedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    LocalDateTime occurredAt
) {}
```

---

### 5.3 함수 합성: 레고 블록

#### 비유: 레고 블록

> **함수는 레고 블록입니다.**
>
> 레고는 규격화된 연결부가 있어서 조립 가능합니다.
> 함수도 출력 타입 = 다음 입력 타입이면 조립됩니다.
>
> ```
> f: A → B
> g: B → C
> 합성: A → B → C
> ```

```java
public class PlaceOrderWorkflow {
    public Result<OrderPlacedEvent, OrderError> execute(
        PlaceOrderCommand command,
        PaymentMethod paymentMethod
    ) {
        return validateOrder.apply(command)
            .map(priceOrder::apply)
            .flatMap(priced -> processPayment.apply(priced, paymentMethod))
            .map(this::createOrderPlacedEvent);
    }
}
```

---

### 5.4 각 단계 구현

```java
// 검증 단계
public class OrderValidator implements ValidateOrder {
    @Override
    public Result<ValidatedOrder, ValidationError> apply(PlaceOrderCommand command) {
        // 고객 ID 검증
        // 상품 존재 확인
        // 배송지 검증
        return Result.success(new ValidatedOrder(...));
    }
}

// 가격 계산 단계
public class OrderPricer implements PriceOrder {
    @Override
    public PricedOrder apply(ValidatedOrder input) {
        Money subtotal = calculateSubtotal(input.lines());
        Money discount = calculateDiscount(input.couponCode(), subtotal);
        return new PricedOrder(..., subtotal, discount, subtotal.subtract(discount));
    }
}
```

---

### 퀴즈 Chapter 5

#### Q5.1 [개념 확인] 워크플로우 타입
`PlaceOrderCommand`와 `ValidatedOrder`를 분리하는 이유는?

A. 메모리 절약
B. 검증 전후의 데이터가 다른 보장을 가지므로
C. Java 문법 제약
D. 디버깅 용이

---

#### Q5.2 [코드 분석] 파이프라인 실패
`validateOrder`가 실패하면?

```java
validateOrder.apply(input)
    .map(priceOrder::apply)
    .flatMap(processPayment::apply);
```

A. priceOrder 실행 후 실패
B. 즉시 실패, 이후 단계 미실행
C. NullPointerException
D. 모든 단계 실행 후 실패

---

#### Q5.3 [설계 문제] 결제 워크플로우
"쿠폰 → 포인트 → 결제" 순서의 타입 설계로 적절한 것은?

A. 같은 Order 타입 사용
B. OrderWithCoupon → OrderWithPoints → PaidOrder
C. String 상태 플래그
D. Map<String, Object>

---

#### Q5.4 [코드 분석] 단계 분리 이점
검증/가격계산 단계 분리의 이점이 아닌 것은?

A. 독립적 테스트 가능
B. 책임 명확
C. 실행 속도 향상
D. 코드 재사용 용이

---

#### Q5.5 [설계 문제] 이벤트 발행
워크플로우 완료 시 이벤트 발행 이유는?

A. DB 트랜잭션 대체
B. 후속 작업을 느슨하게 결합
C. 코드량 감소
D. 컴파일 시간 단축

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 6: Railway Oriented Programming

### 학습 목표
1. Exception의 문제점을 이해한다
2. Result 타입으로 성공/실패를 명시적으로 표현할 수 있다
3. map, flatMap 연산의 의미와 사용법을 익힌다
4. 에러를 값으로 다루는 함수형 에러 처리를 구현할 수 있다

---

### 6.1 Exception의 문제점

#### 비유: 비상 탈출구 vs 두 갈래 길

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

---

### 6.2 Result 타입: 철도 분기점

#### 비유: 철도 분기점

> **Result 타입은 철도 분기점입니다.**
>
> 기차가 분기점에 도착하면:
> - **성공**: 녹색 선로로 계속 진행
> - **실패**: 빨간 선로로 분기
>
> 한번 빨간 선로에 들어가면 녹색 역들을 모두 건너뜁니다.
> 하지만 선로를 벗어나지 않고 안전하게 종착역에 도착합니다.

#### Result 타입 정의

```java
package com.ecommerce.common;

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

---

### 6.3 map과 flatMap: 역 환승

#### 비유: 역 환승

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

---

### 6.4 ROP 패턴 적용

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
            if (inventoryService.getStock(line.productId()) < line.quantity()) {
                return Result.failure(new OrderError.OutOfStock(line.productId()));
            }
        }
        return Result.success(order);
    }

    private Result<PricedOrder, OrderError> applyCoupon(ValidatedOrder order) {
        return order.couponCode()
            .map(code -> couponService.validate(code))
            .orElse(Result.success(null))
            .map(coupon -> priceOrder(order, coupon));
    }

    private Result<PaidOrder, OrderError> processPayment(PricedOrder order) {
        return paymentGateway.charge(order.totalAmount())
            .mapError(e -> new OrderError.PaymentFailed(e.message()));
    }
}
```

---

### 6.5 에러 타입도 Sum Type으로

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

// 에러 처리
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

A. 함수 시그니처가 실패 가능성을 표현하지 못함
B. 흐름 제어가 GOTO와 유사함
C. 메모리를 많이 사용함
D. 어디서 catch될지 예측하기 어려움

---

#### Q6.2 [코드 분석] map vs flatMap
다음 중 `flatMap`을 사용해야 하는 경우는?

A. `String` → `UppercaseString` 변환
B. `ValidatedOrder` → `Result<PricedOrder, Error>` 변환
C. `Money` → `String` 변환
D. `List<Order>` → `Stream<Order>` 변환

---

#### Q6.3 [코드 분석] ROP 파이프라인
다음 코드에서 `checkInventory`가 실패하면?

```java
validateOrder(input)
    .flatMap(this::checkInventory)
    .flatMap(this::processPayment)
    .map(this::createEvent);
```

A. processPayment가 실행된다
B. createEvent가 실행된다
C. Failure가 반환되고 이후 단계는 실행되지 않는다
D. NullPointerException이 발생한다

---

#### Q6.4 [설계 문제] 에러 타입
주문 에러를 모델링할 때 `sealed interface OrderError`를 사용하는 이유는?

A. 메모리 절약
B. 모든 에러 케이스를 컴파일 타임에 확인 가능
C. 실행 속도 향상
D. JSON 직렬화 용이

---

#### Q6.5 [코드 작성] Result 타입
다음 빈칸에 들어갈 코드는?

```java
Result<ValidatedOrder, Error> result = validateOrder(input);
Result<PricedOrder, Error> priced = result._____(this::calculatePrice);
// calculatePrice의 시그니처: ValidatedOrder -> PricedOrder
```

A. flatMap
B. map
C. filter
D. reduce

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 7: 파이프라인 조립과 검증

### 학습 목표
1. Validation과 Result의 차이를 이해한다
2. Applicative 패턴으로 여러 검증을 병렬로 수행할 수 있다
3. Command와 Event 패턴을 이해한다
4. 실제 이커머스 검증 로직을 구현할 수 있다

---

### 7.1 Validation vs Result

#### 비유: 시험 채점

> **Result는 첫 번째 오답에서 멈추는 채점입니다.**
>
> 10문제 중 3번에서 틀리면 바로 "불합격" 처리.
> 4번~10번이 맞는지 틀린지 알 수 없습니다.
>
> **Validation은 모든 문제를 채점합니다.**
>
> 10문제 모두 채점하고 "3번, 7번, 9번 오답" 리포트 제공.
> 학생이 자신의 모든 실수를 한번에 알 수 있습니다.

```java
// Result: 첫 에러에서 중단
Result<Order, Error> result = validateName(input)
    .flatMap(this::validateEmail)   // 이름 실패하면 여기 안 감
    .flatMap(this::validatePhone);  // 이메일 실패하면 여기 안 감

// Validation: 모든 에러 수집
Validation<Order, List<Error>> result = Validation.combine(
    validateName(input),
    validateEmail(input),
    validatePhone(input)
).map(Order::new);  // 모든 검증 결과를 모아서 처리
```

---

### 7.2 Applicative 패턴: 여러 창구 동시 처리

#### 비유: 은행 여러 창구

> **Applicative는 여러 창구에서 동시에 처리하는 것입니다.**
>
> 대출 신청 시:
> - 1번 창구: 신분증 확인
> - 2번 창구: 재직 증명
> - 3번 창구: 소득 증명
>
> 순차 처리(flatMap): 1번 끝나야 2번 시작
> 병렬 처리(Applicative): 세 창구 동시 처리, 결과 모아서 판단

```java
// Validation 타입 정의
public sealed interface Validation<S, E> permits Valid, Invalid {
    static <S, E> Validation<S, E> valid(S value) {
        return new Valid<>(value);
    }

    static <S, E> Validation<S, List<E>> invalid(E error) {
        return new Invalid<>(List.of(error));
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
}

public record Valid<S, E>(S value) implements Validation<S, E> {}
public record Invalid<S, E>(List<E> errors) implements Validation<S, E> {}
```

---

### 7.3 이커머스 검증 예시

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
            return Validation.invalid(new ValidationError.Required("customerId"));
        }
        try {
            return Validation.valid(new CustomerId(Long.parseLong(input)));
        } catch (NumberFormatException e) {
            return Validation.invalid(new ValidationError.InvalidFormat("customerId", "숫자"));
        }
    }

    private Validation<List<ValidatedOrderLine>, List<ValidationError>> validateOrderLines(
        List<UnvalidatedOrderLine> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            return Validation.invalid(new ValidationError.Required("orderLines"));
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

#### 비유: 주문서와 영수증

> **Command는 주문서입니다.**
>
> "라떼 한 잔 주세요" - 요청하는 것
> 성공할지 실패할지는 아직 모릅니다.
>
> **Event는 영수증입니다.**
>
> "라떼 1잔 결제 완료" - 이미 일어난 사실
> 과거의 불변 기록입니다.

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

---

### 퀴즈 Chapter 7

#### Q7.1 [개념 확인] Validation vs Result
Validation이 Result보다 적합한 경우는?

A. 파이프라인에서 첫 에러에 바로 중단하고 싶을 때
B. 폼 검증에서 모든 에러를 한번에 보여주고 싶을 때
C. 결제 처리처럼 순차적으로 진행해야 할 때
D. 단일 값 검증

---

#### Q7.2 [코드 분석] Applicative
Applicative 패턴의 특징이 아닌 것은?

A. 여러 검증을 독립적으로 수행
B. 모든 에러를 수집
C. 첫 에러에서 중단
D. 검증 결과를 결합

---

#### Q7.3 [설계 문제] Command vs Event
다음 중 Event의 특징은?

A. 미래에 수행할 작업을 나타냄
B. 실패할 수 있음
C. 이미 발생한 불변의 사실
D. 요청을 나타냄

---

#### Q7.4 [코드 분석] 검증 결합
다음 코드의 결과는?

```java
Validation.combine(
    Validation.invalid(new Error("이름 필수")),
    Validation.invalid(new Error("이메일 형식 오류")),
    (name, email) -> new User(name, email)
);
```

A. Valid(User)
B. Invalid([이름 필수])
C. Invalid([이메일 형식 오류])
D. Invalid([이름 필수, 이메일 형식 오류])

---

#### Q7.5 [설계 문제] 이벤트 네이밍
`OrderPlaced` 이벤트의 네이밍이 좋은 이유는?

A. 동사 원형을 사용해서
B. 과거형으로 "이미 일어난 일"을 표현해서
C. 명사를 사용해서
D. 짧아서

---

정답은 Appendix C에서 확인할 수 있습니다.

---

