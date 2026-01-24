# 05. Error Handling & ROP (에러 처리와 Railway-Oriented Programming)

> Sources: DMMF Ch.6 (Railway-Oriented Programming), DOP Ch.5 (Total Functions)

---

## 1. Total Functions vs Partial Functions (전체 함수 vs 부분 함수)

### 핵심 개념
- **관련 키워드**: Total Function, Partial Function, honest signature, completeness
- **통찰**: 전체 함수는 모든 입력에 대해 정의된 출력을 보장하여 시그니처가 거짓말하지 않는다.
- **설명**:

부분 함수(Partial Function)는 특정 입력에 대해 예외를 던지거나 정의되지 않은 동작을 하는 함수다. 예를 들어 `int divide(int a, int b)`는 `b=0`일 때 ArithmeticException을 던지지만 시그니처에는 그 가능성이 표현되지 않는다. 호출자는 소스 코드를 들여다보지 않으면 어떤 입력이 실패를 유발하는지 알 수 없다.

전체 함수(Total Function)는 모든 가능한 입력에 대해 유효한 출력을 반환하는 함수다. 실패 가능성이 있으면 `Result<S, F>`처럼 반환 타입에 명시한다. 이렇게 하면 시그니처만 보고도 실패 가능성을 파악할 수 있으며, 컴파일러가 에러 처리를 강제할 수 있다.

DOP에서는 "시그니처가 거짓말하면 호출자가 실패 케이스를 놓친다"고 표현하며, DMMF에서는 "Exception은 함수형 합성을 깨뜨린다"고 설명한다. 두 관점 모두 부분 함수를 전체 함수로 바꾸는 것을 핵심 원칙으로 삼는다.

**[그림 05.1]** Total Functions vs Partial Functions (전체 함수 vs 부분 함수)
```
+------------------------------------------------------------+
|               Partial Function                             |
|   input --> [function] --+--> output (some inputs)         |
|                          +--> EXCEPTION (other inputs)     |
|                          +--> null (edge cases)            |
|   Signature LIES: "always returns Output"                  |
+------------------------------------------------------------+

+------------------------------------------------------------+
|               Total Function                               |
|   input --> [function] --+--> Result.Success(output)       |
|                          +--> Result.Failure(error)        |
|   Signature is HONEST: "returns Success OR Failure"        |
+------------------------------------------------------------+
```

### 개념이 아닌 것
- **"예외를 안 쓰면 전체 함수"**: null을 반환해도 부분 함수일 수 있다. 핵심은 모든 입력에 대해 정의된 출력이 있어야 한다는 점이다.
- **"모든 함수가 Result를 반환해야 한다"**: 실패할 수 없는 순수 연산(예: `int add(int a, int b)`)은 이미 전체 함수이므로 Result가 불필요하다.

### Before: Traditional OOP

**[코드 05.1]** Traditional OOP: Partial Function - 시그니처가 거짓말하는 안티패턴
```java
 1| // package: com.ecommerce.order
 2| // [X] Partial Function - 시그니처가 거짓말하는 안티패턴
 3| public class UserService {
 4|   public User findUser(int id) {
 5|     User user = database.find(id);
 6|     if (user == null) {
 7|       throw new NotFoundException("User not found: " + id);
 8|       // 시그니처에는 이 예외에 대한 언급이 없음
 9|     }
10|     return user;
11|   }
12| 
13|   public Order createOrder(OrderRequest request) {
14|     // 시그니처: "항상 Order를 반환합니다"
15|     // 현실: 재고 부족, 잘못된 쿠폰 등으로 예외 발생 가능
16|     if (outOfStock) throw new OutOfStockException();
17|     if (invalidCoupon) throw new InvalidCouponException();
18|     return new Order(...);
19|   }
20| }
```
- **의도 및 코드 설명**: `findUser`와 `createOrder`는 정상 경로만 시그니처에 표현하고, 실패 경로는 Exception으로 숨긴다.
- **뭐가 문제인가**:
  - 시그니처 거짓말: 메서드 시그니처가 실패 가능성을 숨김
  - 비지역적 점프: 코드가 GOTO처럼 예측 불가능하게 점프
  - 에러 처리 강제 불가: try-catch 없어도 컴파일 성공
  - 호출자가 소스를 읽지 않으면 어떤 예외가 발생하는지 알 수 없음

### After: Modern Approach

**[코드 05.2]** Modern: Total Function - 정직한 시그니처
```java
 1| // package: com.ecommerce.auth
 2| // [O] Total Function - 정직한 시그니처
 3| public sealed interface UserError {
 4|   record NotFound(int id) implements UserError {}
 5|   record Suspended(int id, String reason) implements UserError {}
 6|   record Deleted(int id, LocalDateTime deletedAt) implements UserError {}
 7| }
 8| 
 9| public class UserService {
10|   public Result<User, UserError> findUser(int id) {
11|     User user = database.find(id);
12|     if (user == null) {
13|       return Result.failure(new UserError.NotFound(id));
14|     }
15|     return Result.success(user);
16|   }
17| }
```
- **의도 및 코드 설명**: 반환 타입 `Result<User, UserError>`에 실패 가능성이 명시된다. 에러 타입도 sealed interface로 정의하여 모든 실패 케이스가 열거된다.
- **무엇이 좋아지나**:
  - 시그니처가 정직: 호출자가 실패 가능성을 즉시 인지
  - 컴파일러 강제: switch 패턴 매칭에서 모든 에러 케이스 처리 강제
  - 함수 합성 가능: Result를 map/flatMap으로 연쇄 가능
  - 테스트 용이: 예외를 catch할 필요 없이 반환값 비교

### 이해를 위한 부가 상세
- Java의 Checked Exception은 throws 절로 명시하지만, 결국 RuntimeException으로 감싸거나 무시하는 경우가 많다. 또한 Checked Exception은 함수형 인터페이스(Function, Consumer 등)와 호환되지 않아 람다 합성이 어렵다.
- Rust의 `Result<T, E>`, Kotlin의 `Result<T>`, Vavr의 `Either<L, R>` 등 현대 언어에서는 이 패턴이 표준이다.

### 틀리기/놓치기 쉬운 부분
- Infrastructure 경계(DB 연결 실패, 네트워크 타임아웃)에서는 여전히 Exception이 적합할 수 있다. Result는 비즈니스 로직의 **예상된 실패**에 적합하다.
- `Result.failure()`를 반환하면서도 내부에서 catch한 예외를 그대로 넘기면 구체적인 실패 정보가 사라질 수 있다.

### 꼭 기억할 것
- 전체 함수의 핵심: **시그니처가 모든 가능한 결과를 표현**한다.
- Exception은 "예외적 상황"(프로그래밍 오류, 인프라 장애)에, Result는 "예상된 실패"(검증 실패, 비즈니스 규칙 위반)에 사용한다.

---

## 2. Result Type Pattern (Result<Success, Failure>)

### 핵심 개념
- **관련 키워드**: Result, Either, Success, Failure, sealed interface, pattern matching
- **통찰**: Result 타입은 성공과 실패를 하나의 합 타입(Sum Type)으로 표현하여 에러를 일급 값으로 다룬다.
- **설명**:

Result 타입은 `Success<S, F>`와 `Failure<S, F>` 두 가지 상태만 가능한 sealed interface이다. 이 패턴은 "에러를 값으로 다루기(Failure as Data)"를 실현한다. 예외와 달리 에러가 데이터로 존재하므로 변환, 결합, 전달이 자유롭다.

DMMF에서는 이를 "철도 분기점" 비유로 설명한다. 기차가 분기점에 도착하면 성공(녹색 선로) 또는 실패(빨간 선로)로 갈라진다. 한번 실패 선로에 들어가면 이후 역들을 모두 건너뛰지만, 선로를 벗어나지 않고 안전하게 종착역에 도착한다.

DOP에서는 에러 타입을 sealed interface로 정의하여 패턴 매칭으로 모든 케이스를 강제 처리하는 것을 강조한다. 이를 통해 에러 처리 누락을 컴파일 타임에 잡을 수 있다.

**[그림 05.2]** Result Type Pattern (Result<Success, Failure>)
```
+-----------------------------------------------+
|        Result<S, F> (Sealed Interface)         |
+-----------------------------------------------+
|                    |                           |
|   +------------+   |   +----------------+     |
|   | Success<S> |   |   | Failure<F>     |     |
|   | value: S   |   |   | error: F       |     |
|   +------------+   |   +----------------+     |
|                    |                           |
|   Operations:      |                           |
|   - map(f)         : Success(A) -> Success(B) |
|   - flatMap(f)     : Success(A) -> Result<B>  |
|   - mapError(f)    : Failure(E) -> Failure(E2)|
|   - fold(onS, onF) : Result -> T              |
+-----------------------------------------------+
```

### 개념이 아닌 것
- **"Optional의 확장판"**: Optional은 값의 유무만 표현하지만, Result는 왜 없는지(에러 정보)까지 담는다.
- **"try-catch의 대체품"**: Result는 비즈니스 로직의 예상된 실패를 다루며, 예외적 오류(OOM, StackOverflow)는 여전히 Exception이 적합하다.

### Before: Traditional OOP

**[코드 05.3]** Traditional OOP: Exception으로 에러 처리 - 흐름 제어가 GOTO와 유사
```java
 1| // package: com.ecommerce.order
 2| // [X] Exception으로 에러 처리 - 흐름 제어가 GOTO와 유사
 3| public class OrderProcessor {
 4|   public OrderResult processOrder(OrderRequest request) {
 5|     try {
 6|       Order order = createOrder(request);
 7|       Payment payment = processPayment(order);
 8|       sendNotification(order);
 9|       return new OrderResult(order, payment);
10|     } catch (OutOfStockException e) {
11|       log.error("재고 부족", e);
12|       return null;  // 또는 기본값 반환
13|     } catch (PaymentException e) {
14|       log.error("결제 실패", e);
15|       return null;
16|     } catch (Exception e) {
17|       // 어떤 단계에서 실패했는지 알기 어려움
18|       throw new RuntimeException("주문 처리 실패", e);
19|     }
20|   }
21| }
```
- **의도 및 코드 설명**: try-catch로 여러 단계의 예외를 한곳에서 처리하려 한다.
- **뭐가 문제인가**:
  - 어떤 단계에서 실패했는지 catch 블록에서 구분 어려움
  - null 반환으로 NPE 위험 전파
  - 새로운 예외 추가 시 catch 블록 수정 필요 (OCP 위반)
  - 함수형 합성 불가: 각 단계를 체이닝할 수 없음

### After: Modern Approach

**[코드 05.4]** Modern: Result 타입으로 에러를 일급 값으로 처리
```java
 1| // package: com.ecommerce.shared
 2| // [O] Result 타입으로 에러를 일급 값으로 처리
 3| public sealed interface Result<S, F> {
 4|   record Success<S, F>(S value) implements Result<S, F> {}
 5|   record Failure<S, F>(F error) implements Result<S, F> {}
 6| 
 7|   static <S, F> Result<S, F> success(S value) {
 8|     return new Success<>(value);
 9|   }
10| 
11|   static <S, F> Result<S, F> failure(F error) {
12|     return new Failure<>(error);
13|   }
14| 
15|   default <T> Result<T, F> map(Function<S, T> mapper) {
16|     return switch (this) {
17|       case Success(var value) -> Result.success(mapper.apply(value));
18|       case Failure(var error) -> Result.failure(error);
19|     };
20|   }
21| 
22|   default <T> Result<T, F> flatMap(Function<S, Result<T, F>> mapper) {
23|     return switch (this) {
24|       case Success(var value) -> mapper.apply(value);
25|       case Failure(var error) -> Result.failure(error);
26|     };
27|   }
28| 
29|   default <T> T fold(Function<S, T> onSuccess, Function<F, T> onFailure) {
30|     return switch (this) {
31|       case Success(var value) -> onSuccess.apply(value);
32|       case Failure(var error) -> onFailure.apply(error);
33|     };
34|   }
35| }
36| 
37| // 도메인 에러를 sealed interface로 정의
38| public sealed interface OrderError {
39|   record EmptyOrder() implements OrderError {}
40|   record OutOfStock(ProductId productId) implements OrderError {}
41|   record InvalidCoupon(CouponCode code, String reason) implements OrderError {}
42|   record PaymentFailed(String reason) implements OrderError {}
43| }
```
- **의도 및 코드 설명**: Result는 sealed interface로 구현하며, Success와 Failure를 record로 정의한다. map/flatMap/fold 연산으로 값을 변환하거나 결과를 추출한다.
- **무엇이 좋아지나**:
  - 에러가 값(데이터): 변환, 결합, 직렬화 가능
  - 합성 가능: map/flatMap으로 체이닝
  - 타입 안전: sealed interface로 모든 에러 케이스 열거
  - 패턴 매칭: switch로 각 에러를 처리하며 누락 시 컴파일 에러

### 이해를 위한 부가 상세
- `fold` 메서드는 Result를 최종적으로 "풀어서" 하나의 값으로 만들 때 사용한다. 예: HTTP 응답 변환.
- `mapError`는 에러 타입을 변환할 때 사용한다. 하위 도메인 에러를 상위 도메인 에러로 변환할 때 유용하다.

### 틀리기/놓치기 쉬운 부분
- `Success`와 `Failure`는 모두 2개의 타입 파라미터를 가진다. `Success<S, F>`에서 F는 사용하지 않지만 타입 시스템 통일을 위해 필요하다.
- `value()`나 `error()` 직접 호출은 위험하다. 항상 `fold`, `map`, `flatMap` 또는 패턴 매칭을 사용하라.

### 꼭 기억할 것
- Result의 핵심 가치: **에러를 예외적 흐름이 아닌 정상적인 데이터 흐름으로 표현**한다.
- Java에서는 sealed interface + record로 구현하며, Rust의 Result, Scala의 Either와 동일한 개념이다.

---

## 3. Railway-Oriented Programming (두 개의 레일 비유)

### 핵심 개념
- **관련 키워드**: Railway, two-track, switch function, bind, composition
- **통찰**: 모든 함수를 "성공 트랙"과 "실패 트랙" 두 레일 위에서 동작하도록 만들면, 파이프라인 전체가 자동으로 에러를 전파한다.
- **설명**:

Railway-Oriented Programming(ROP)은 Scott Wlaschin이 제안한 에러 처리 패턴이다. 핵심 아이디어는 모든 워크플로우를 두 줄의 철도 선로(성공/실패)로 시각화하는 것이다. 각 단계(함수)는 "switch" 역할을 하며, 입력이 성공 트랙에 있으면 처리하고, 실패 트랙에 있으면 그대로 통과시킨다.

이 패턴을 사용하면 에러 처리 코드가 비즈니스 로직에 섞이지 않는다. 각 단계는 자신의 역할(검증, 가격 계산, 결제 등)에만 집중하고, "이전 단계가 실패했으면 나를 건너뛰어라"는 로직은 flatMap이 자동으로 처리한다.

ROP의 핵심 이점은 **선형적 코드 흐름**이다. try-catch의 비지역적 점프나 중첩된 if-else 없이, 깔끔한 파이프라인으로 복잡한 워크플로우를 표현할 수 있다.

**[그림 05.3]** Railway-Oriented Programming (두 개의 레일 비유)
```
=== Railway-Oriented Programming: Two Tracks ===

 Success Track: ----[Validate]----[Price]----[Pay]----[Event]--->
                       |             |          |
                       v             v          v
 Failure Track: -------+-------------+----------+--------------->
                  (EmptyOrder)  (OutOfStock) (PaymentFailed)

 Once on Failure Track, all subsequent steps are SKIPPED.
 The error propagates automatically to the end.
```

### 개념이 아닌 것
- **"모든 함수가 두 개의 출력을 가져야 한다"**: 실패할 수 없는 순수 변환(map 대상)은 단일 트랙 함수이며, flatMap으로 감쌀 필요 없다.
- **"try-catch를 Result로 바꾸기만 하면 된다"**: ROP의 진짜 가치는 함수 합성에 있다. 단순 교체가 아닌 파이프라인 설계가 핵심이다.

### Before: Traditional OOP

**[코드 05.5]** Traditional OOP: 중첩된 try-catch와 조건문으로 워크플로우 구현
```java
 1| // package: com.ecommerce.order
 2| // [X] 중첩된 try-catch와 조건문으로 워크플로우 구현
 3| public class PlaceOrderService {
 4|   public OrderPlaced execute(PlaceOrderCommand command) {
 5|     // 검증
 6|     if (command.lines().isEmpty()) {
 7|       throw new ValidationException("주문 항목 없음");
 8|     }
 9| 
10|     ValidatedOrder validated;
11|     try {
12|       validated = validateOrder(command);
13|     } catch (ValidationException e) {
14|       throw e;
15|     }
16| 
17|     // 재고 확인
18|     try {
19|       checkInventory(validated);
20|     } catch (OutOfStockException e) {
21|       throw new OrderException("재고 부족", e);
22|     }
23| 
24|     // 결제
25|     PaidOrder paid;
26|     try {
27|       paid = processPayment(validated);
28|     } catch (PaymentException e) {
29|       throw new OrderException("결제 실패", e);
30|     }
31| 
32|     return createEvent(paid);
33|   }
34| }
```
- **의도 및 코드 설명**: 각 단계를 try-catch로 감싸 에러를 처리한다. 각 catch에서 새로운 예외로 감싸 throw한다.
- **뭐가 문제인가**:
  - 비즈니스 로직(validate, check, pay)이 에러 처리 코드에 묻힘
  - 새 단계 추가 시 또 다른 try-catch 블록 필요
  - 예외 감싸기(wrapping)로 원본 정보 손실 가능
  - 코드의 해피 패스를 읽기 어려움

### After: Modern Approach

**[코드 05.6]** Modern: Railway-Oriented Programming - flatMap 체이닝
```java
 1| // package: com.ecommerce.order
 2| // [O] Railway-Oriented Programming - flatMap 체이닝
 3| public class PlaceOrderWorkflow {
 4|   public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
 5|     return validateOrder(command)
 6|       .flatMap(this::checkInventory)
 7|       .flatMap(this::applyCoupon)
 8|       .flatMap(this::processPayment)
 9|       .map(this::createEvent);
10|   }
11| 
12|   private Result<ValidatedOrder, OrderError> validateOrder(PlaceOrderCommand command) {
13|     if (command.lines().isEmpty()) {
14|       return Result.failure(new OrderError.EmptyOrder());
15|     }
16|     return Result.success(new ValidatedOrder(command));
17|   }
18| 
19|   private Result<ValidatedOrder, OrderError> checkInventory(ValidatedOrder order) {
20|     for (var line : order.lines()) {
21|       if (inventoryService.getStock(line.productId()) < line.quantity().value()) {
22|         return Result.failure(new OrderError.OutOfStock(line.productId()));
23|       }
24|     }
25|     return Result.success(order);
26|   }
27| 
28|   private Result<PricedOrder, OrderError> applyCoupon(ValidatedOrder order) {
29|     return couponService.validate(order.couponCode())
30|       .map(coupon -> priceOrder(order, coupon))
31|       .mapError(e -> new OrderError.InvalidCoupon(order.couponCode(), e.message()));
32|   }
33| 
34|   private Result<PaidOrder, OrderError> processPayment(PricedOrder order) {
35|     return paymentGateway.charge(order.totalAmount())
36|       .mapError(e -> new OrderError.PaymentFailed(e.message()));
37|   }
38| 
39|   private OrderPlaced createEvent(PaidOrder order) {
40|     return new OrderPlaced(order.id(), order.totalAmount(), LocalDateTime.now());
41|   }
42| }
```
- **의도 및 코드 설명**: 각 단계는 `Result`를 반환하는 독립 함수이며, `flatMap`으로 연결된다. 마지막 `createEvent`는 실패할 수 없으므로 `map`을 사용한다.
- **무엇이 좋아지나**:
  - 해피 패스가 선형으로 읽힘: validate -> check -> coupon -> pay -> event
  - 에러 전파 자동: flatMap이 Failure를 만나면 이후 단계 스킵
  - 각 단계 독립 테스트 가능: 개별 함수를 단위 테스트
  - 새 단계 추가가 한 줄: `.flatMap(this::newStep)`

### 이해를 위한 부가 상세
- `map`과 `flatMap`의 선택 기준: 변환 함수가 `A -> B`이면 `map`, `A -> Result<B, E>`이면 `flatMap`이다.
- `mapError`는 하위 도메인의 에러 타입을 상위 도메인 에러로 변환할 때 필수적이다. 예를 들어 PaymentGateway의 에러를 OrderError로 변환한다.

### 틀리기/놓치기 쉬운 부분
- 마지막 단계에서 `map`이 아닌 `flatMap`을 사용하면 불필요한 중첩이 생길 수 있다. 변환이 절대 실패하지 않으면 `map`을 사용하라.
- 에러 타입이 다른 함수를 연결할 때 `mapError`로 통일하지 않으면 타입 불일치 컴파일 에러가 발생한다.

### 꼭 기억할 것
- ROP의 핵심: **한번 Failure 트랙에 들어가면 이후 단계는 모두 건너뛰고, Failure가 끝까지 전파**된다.
- `flatMap`은 "환승"(다른 Result로 갈아탐), `map`은 "같은 노선 내 이동"(값만 변환)이다.

---

## 4. flatMap/map Chaining for Error Composition (에러 합성을 위한 체이닝)

### 핵심 개념
- **관련 키워드**: map, flatMap, bind, compose, chaining, functor, monad
- **통찰**: map은 컨테이너 안의 값을 변환하고, flatMap은 컨테이너를 반환하는 함수를 중첩 없이 연결한다.
- **설명**:

`map`과 `flatMap`은 Result 타입의 핵심 연산이다. `map(f)`은 Success 안의 값에 함수 `f: A -> B`를 적용하여 `Result<B, E>`를 만든다. Failure면 함수를 실행하지 않고 그대로 Failure를 반환한다. 이는 "컨테이너 구조는 유지하되, 안의 값만 변환"하는 Functor 패턴이다.

`flatMap(g)`은 `g: A -> Result<B, E>` 형태의 함수를 적용한다. `map`으로 이런 함수를 적용하면 `Result<Result<B, E>, E>`로 이중 포장이 되는데, `flatMap`은 이를 "평평하게(flat)" 만들어 `Result<B, E>`로 반환한다. 이것이 Monad 패턴의 핵심이다.

체이닝을 통해 여러 단계의 연산을 선형적으로 합성할 수 있다. 각 단계가 독립적으로 실패할 수 있더라도, 파이프라인 전체가 하나의 `Result`로 수렴한다.

**[그림 05.4]** flatMap/map Chaining for Error Composition (에러 합성을 위한 체이닝)
```
=== map vs flatMap ===

map:     Result<A, E> --f: A->B----------> Result<B, E>
         (container preserved, value transformed)

flatMap: Result<A, E> --g: A->Result<B,E>-> Result<B, E>
         (container flattened, no nesting)

=== Why flatMap? Avoiding Double Wrapping ===

map + (A -> Result<B>):
  Success(A) --map--> Success(Result<B, E>)  -- NESTED!

flatMap + (A -> Result<B>):
  Success(A) --flatMap--> Result<B, E>       -- FLAT!
```

### 개념이 아닌 것
- **"flatMap은 map보다 상위 개념"**: 둘은 용도가 다르다. 변환 함수의 반환 타입에 따라 선택한다.
- **"체이닝이 길면 성능이 나쁘다"**: 각 단계는 단순 함수 호출이며, 스택 깊이가 문제가 될 정도가 아니다.

### Before: Traditional OOP

**[코드 05.7]** Traditional OOP: map 없이 매번 switch로 분기
```java
 1| // package: com.ecommerce.order
 2| // [X] map 없이 매번 switch로 분기
 3| public class OrderTransformer {
 4|   Result<OrderDto, OrderError> convertToDto(Result<Order, OrderError> result) {
 5|     return switch (result) {
 6|       case Success<Order, OrderError> s -> Result.success(toDto(s.value()));
 7|       case Failure<Order, OrderError> f -> Result.failure(f.error());
 8|     };
 9|   }
10| 
11|   Result<String, OrderError> getOrderId(Result<Order, OrderError> result) {
12|     return switch (result) {
13|       case Success<Order, OrderError> s -> Result.success(s.value().id());
14|       case Failure<Order, OrderError> f -> Result.failure(f.error());
15|     };
16|   }
17| 
18|   // 패턴 반복: Success면 꺼내서 변환하고 다시 감싸고, Failure면 그대로...
19| }
```
- **의도 및 코드 설명**: Result 안의 값을 변환할 때마다 switch로 Success/Failure를 분기해야 한다.
- **뭐가 문제인가**:
  - 동일한 boilerplate 반복 (Success 분기 + Failure 전달)
  - 새 변환마다 같은 패턴의 코드 작성 필요
  - 실수로 Failure 케이스에서 타입 캐스팅 에러 가능
  - 함수 합성이 아닌 절차적 코드

### After: Modern Approach

**[코드 05.8]** Modern: map/flatMap으로 깔끔한 체이닝
```java
 1| // package: com.ecommerce.order
 2| // [O] map/flatMap으로 깔끔한 체이닝
 3| public class OrderPipeline {
 4|   public Result<OrderConfirmation, OrderError> process(PlaceOrderCommand cmd) {
 5|     return validateOrder(cmd)                    // Result<ValidatedOrder, OrderError>
 6|       .flatMap(this::checkInventory)           // Result<ValidatedOrder, OrderError>
 7|       .map(this::calculatePrice)               // Result<PricedOrder, OrderError>
 8|       .flatMap(this::processPayment)           // Result<PaidOrder, OrderError>
 9|       .map(this::toConfirmation);              // Result<OrderConfirmation, OrderError>
10|   }
11| 
12|   // 절대 실패하지 않는 변환 -> map
13|   private PricedOrder calculatePrice(ValidatedOrder order) {
14|     Money total = order.lines().stream()
15|       .map(line -> line.unitPrice().multiply(line.quantity()))
16|       .reduce(Money.zero(), Money::add);
17|     return new PricedOrder(order.customerId(), order.lines(), total);
18|   }
19| 
20|   // 실패할 수 있는 연산 -> flatMap
21|   private Result<PaidOrder, OrderError> processPayment(PricedOrder order) {
22|     return paymentGateway.charge(order.totalAmount())
23|       .map(txId -> new PaidOrder(order, txId))
24|       .mapError(e -> new OrderError.PaymentFailed(e.message()));
25|   }
26| 
27|   // 절대 실패하지 않는 변환 -> map
28|   private OrderConfirmation toConfirmation(PaidOrder order) {
29|     return new OrderConfirmation(order.id(), order.totalAmount(), LocalDateTime.now());
30|   }
31| }
```
- **의도 및 코드 설명**: `calculatePrice`와 `toConfirmation`은 실패하지 않으므로 `map`, `checkInventory`와 `processPayment`는 실패할 수 있으므로 `flatMap`을 사용한다.
- **무엇이 좋아지나**:
  - boilerplate 제거: switch 분기 없이 한 줄로 변환
  - 의도 명확: map은 "안전한 변환", flatMap은 "실패 가능한 단계"
  - 타입 추론: 컴파일러가 각 단계의 입출력 타입 검증
  - 합성 용이: 새 단계를 한 줄로 추가/제거 가능

### 이해를 위한 부가 상세
- `map`은 Optional.map(), Stream.map()과 동일한 패턴이다. "컨테이너 안의 값을 변환하되, 컨테이너 구조는 유지"하는 Functor 법칙을 따른다.
- `flatMap`은 Optional.flatMap(), Stream.flatMap()과 동일하다. "컨테이너를 반환하는 함수를 적용하되, 이중 포장을 방지"한다.

### 틀리기/놓치기 쉬운 부분
- 에러 타입이 단계마다 다를 경우(예: PaymentError, CouponError), `mapError`로 공통 에러 타입(OrderError)으로 변환해야 체이닝이 가능하다.
- `map` 안에서 예외를 던지면 ROP 파이프라인이 깨진다. map에 전달하는 함수는 반드시 순수해야 한다.

### 꼭 기억할 것
- 선택 기준: 함수가 `Result`를 반환하면 **flatMap**, 단순 값을 반환하면 **map**이다.
- 체이닝의 핵심 가치: **해피 패스를 선형으로 읽을 수 있고, 에러 전파는 자동**이다.

---

## 5. Validation (Applicative) vs Result (Monadic) - 모든 에러 수집 vs 빠른 실패

### 핵심 개념
- **관련 키워드**: Validation, Applicative, error accumulation, fail-fast, combine, parallel validation
- **통찰**: Result.flatMap은 첫 에러에서 중단하고(fail-fast), Validation.combine은 모든 에러를 수집한다(error accumulation).
- **설명**:

Result의 flatMap은 Monadic 합성이다. 이전 단계의 결과에 다음 단계가 의존하므로, 첫 실패 시 나머지를 건너뛴다. 결제 파이프라인처럼 "앞 단계 성공이 다음 단계의 전제조건"인 경우에 적합하다.

Validation은 Applicative 합성이다. 각 검증이 서로 독립적이므로, 모든 검증을 수행한 후 에러를 목록으로 수집한다. 폼 검증처럼 "모든 오류를 한 번에 사용자에게 보여줘야 하는" 경우에 적합하다.

DMMF에서는 이 둘의 차이를 "시험 채점"으로 비유한다. Result는 "첫 오답에서 불합격 처리", Validation은 "모든 문제를 채점하고 종합 리포트 제공"이다.

**[그림 05.5]** Validation (Applicative) vs Result (Monadic) - 모든 에러 수집 vs 빠른 실패
```
=== Result.flatMap: Fail-Fast (Sequential) ===

  [validateName] --FAIL--> stop! Error: "Name required"
       |                   (email, password NOT checked)
       v (success)
  [validateEmail] --FAIL--> stop! Error: "Invalid email"
       |
       v (success)
  [validatePassword] --> Success(User)


=== Validation.combine: Error Accumulation (Parallel) ===

  [validateName]     ---+
                        |
  [validateEmail]    ---+--> combine --> Invalid([
                        |                  "Name required",
  [validatePassword] ---+                  "Invalid email",
                                           "Password too short"
                                         ])
                              OR
                              Valid(User)
```

### 개념이 아닌 것
- **"Validation은 항상 Result보다 좋다"**: 단계 간 의존성이 있으면(예: 1단계 결과를 2단계 입력으로) Validation을 쓸 수 없다.
- **"Applicative는 병렬 실행을 의미한다"**: 여기서 "병렬"은 논리적 독립성을 뜻하며, 반드시 멀티스레드 실행은 아니다.

### Before: Traditional OOP

**[코드 05.9]** Traditional OOP: 첫 에러에서 예외를 던져 나머지 검증 불가
```java
 1| // package: com.ecommerce.auth
 2| // [X] 첫 에러에서 예외를 던져 나머지 검증 불가
 3| public class UserRegistrationService {
 4|   public User register(RegistrationForm form) {
 5|     // 첫 에러에서 throw - 나머지 필드 검증 안됨
 6|     if (form.name() == null || form.name().isBlank()) {
 7|       throw new ValidationException("이름은 필수입니다");
 8|     }
 9|     if (!form.email().contains("@")) {
10|       throw new ValidationException("이메일 형식이 올바르지 않습니다");
11|     }
12|     if (form.password().length() < 8) {
13|       throw new ValidationException("비밀번호는 8자 이상이어야 합니다");
14|     }
15|     return new User(form.name(), form.email(), form.password());
16|   }
17|   // 사용자는 에러를 하나씩 고쳐야 함 - 나쁜 UX
18| }
```
- **의도 및 코드 설명**: 순차적으로 검증하다 첫 오류에서 예외를 던진다.
- **뭐가 문제인가**:
  - 사용자가 3개 필드를 모두 잘못 입력해도 에러 메시지는 1개만 표시
  - 수정 -> 재시도 -> 다음 에러 -> 수정 -> 재시도 (최악 3번 왕복)
  - 나쁜 UX: 사용자가 자신의 모든 실수를 한 번에 알 수 없음

### After: Modern Approach

**[코드 05.10]** Modern: Validation.combine으로 모든 에러 수집
```java
 1| // package: com.ecommerce.auth
 2| // [O] Validation.combine으로 모든 에러 수집
 3| public sealed interface Validation<S, E> {
 4|   record Valid<S, E>(S value) implements Validation<S, E> {}
 5|   record Invalid<S, E>(E errors) implements Validation<S, E> {}
 6| 
 7|   static <A, B, C, R, E> Validation<R, List<E>> combine3(
 8|     Validation<A, List<E>> va,
 9|     Validation<B, List<E>> vb,
10|     Validation<C, List<E>> vc,
11|     TriFunction<A, B, C, R> combiner
12|   ) {
13|     // 모든 Valid이면 combiner 적용, 하나라도 Invalid이면 에러 수집
14|     List<E> errors = new ArrayList<>();
15|     if (va instanceof Invalid<A, List<E>> a) errors.addAll(a.errors());
16|     if (vb instanceof Invalid<B, List<E>> b) errors.addAll(b.errors());
17|     if (vc instanceof Invalid<C, List<E>> c) errors.addAll(c.errors());
18| 
19|     if (!errors.isEmpty()) return new Invalid<>(errors);
20| 
21|     return new Valid<>(combiner.apply(
22|       ((Valid<A, List<E>>) va).value(),
23|       ((Valid<B, List<E>>) vb).value(),
24|       ((Valid<C, List<E>>) vc).value()
25|     ));
26|   }
27| }
28| 
29| // 사용 예: 폼 검증
30| public class UserRegistrationService {
31|   public Validation<User, List<ValidationError>> register(RegistrationForm form) {
32|     return Validation.combine3(
33|       validateName(form.name()),
34|       validateEmail(form.email()),
35|       validatePassword(form.password()),
36|       User::new
37|     );
38|     // 결과: Invalid([이름 필수, 이메일 형식 오류, 비밀번호 길이 부족])
39|     // 모든 에러를 한 번에 보여줌 - 좋은 UX!
40|   }
41| 
42|   private Validation<String, List<ValidationError>> validateName(String name) {
43|     if (name == null || name.isBlank()) {
44|       return new Invalid<>(List.of(new ValidationError.Required("name")));
45|     }
46|     return new Valid<>(name);
47|   }
48| 
49|   private Validation<String, List<ValidationError>> validateEmail(String email) {
50|     if (!email.contains("@")) {
51|       return new Invalid<>(List.of(new ValidationError.InvalidFormat("email", "user@domain")));
52|     }
53|     return new Valid<>(email);
54|   }
55| 
56|   private Validation<String, List<ValidationError>> validatePassword(String password) {
57|     if (password.length() < 8) {
58|       return new Invalid<>(List.of(new ValidationError.TooShort("password", 8)));
59|     }
60|     return new Valid<>(password);
61|   }
62| }
```
- **의도 및 코드 설명**: `Validation.combine3`은 세 검증을 모두 수행하고, 하나라도 Invalid이면 에러를 모아 반환한다. 모두 Valid이면 combiner 함수로 결과를 합성한다.
- **무엇이 좋아지나**:
  - 좋은 UX: 모든 에러를 한 번에 사용자에게 표시
  - 독립적 검증: 각 필드 검증이 서로 영향 없이 수행
  - 타입 안전: ValidationError sealed interface로 에러 종류 열거
  - 합성 가능: combine3, combine4 등으로 확장

### 이해를 위한 부가 상세
- **선택 가이드**:
  - 폼 검증(회원가입, 주문 입력) -> Validation (모든 에러 한 번에)
  - 결제 파이프라인(순차적 비즈니스 로직) -> Result.flatMap (빠른 실패)
  - 검증 간 의존성이 있으면(예: "이메일 형식 맞으면 중복 검사") -> Result.flatMap
  - 검증 간 독립적이면(이름, 이메일, 비밀번호 각각) -> Validation.combine

### 틀리기/놓치기 쉬운 부분
- Validation의 에러 타입은 보통 `List<E>`이다. 단일 에러를 List로 감싸는 것을 잊지 마라.
- Validation과 Result를 혼합하여 사용할 수 있다: 먼저 Validation으로 입력을 검증하고, 검증 통과 후 Result.flatMap으로 비즈니스 파이프라인을 진행한다.

### 꼭 기억할 것
- **Result.flatMap = Fail-Fast = 순차적 의존 = 결제/파이프라인**
- **Validation.combine = Error Accumulation = 독립적 검증 = 폼 검증**
- 실무에서는 두 가지를 조합하여 사용한다: 입력 검증(Validation) -> 비즈니스 파이프라인(Result.flatMap)
