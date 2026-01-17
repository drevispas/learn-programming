# Part III: 데이터의 흐름과 제어 (Behavior & Control Flow)

---

## Chapter 5: 전체 함수(Total Functions)와 실패 처리

### 학습 목표
1. 부분 함수(Partial Function)와 전체 함수(Total Function)의 차이를 설명할 수 있다
2. 예외(Exception)가 왜 "거짓말"인지 이해한다
3. Result 타입을 구현하고 활용할 수 있다
4. "Failure as Data" 패턴을 적용할 수 있다
5. 연쇄적인 실패 처리를 우아하게 구현할 수 있다

---

### 5.1 부분 함수(Partial Function)의 위험

#### 시그니처가 거짓말을 한다

```java
// 시그니처: id를 주면 User를 반환
public User findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        throw new NotFoundException("User not found: " + id);
        // 시그니처에는 이 예외에 대한 언급이 없음!
    }
    return user;
}
```

이것이 **부분 함수(Partial Function)**입니다. 입력 중 일부에 대해서만 결과를 반환하고, 나머지는 예외를 던지거나 null을 반환합니다.

#### 비유: 은행 창구

> **부분 함수는 불친절한 은행 창구와 같습니다.**
>
> "계좌 잔액 조회"라는 창구에 갔습니다.
> 번호표에는 "계좌번호를 주시면 잔액을 알려드립니다"라고 쓰여있습니다.
>
> 그런데 막상 창구에 가니:
> - "그 계좌는 해지됐어요" (예외)
> - "시스템 점검 중이에요" (예외)
> - "조회 권한이 없어요" (예외)
>
> 번호표에는 이런 경우에 대한 안내가 없었습니다.
>
> **전체 함수는 친절한 은행 창구입니다.**
>
> 번호표에 미리 안내되어 있습니다:
> "결과: 잔액 / 해지된 계좌 / 권한 없음 / 시스템 오류 중 하나"
> 어떤 경우든 반드시 명확한 결과를 받습니다.

---

### 5.2 전체 함수(Total Function) 만들기

#### Before: 부분 함수 (거짓말하는 시그니처)

```java
public User findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        throw new NotFoundException("User not found: " + id);
    }
    return user;
}
```

#### After: 전체 함수 (정직한 시그니처)

```java
public Result<User, UserError> findUser(int id) {
    User user = database.find(id);
    if (user == null) {
        return Result.failure(new UserError.NotFound(id));
    }
    return Result.success(user);
}

// 에러 타입도 명확히 정의
sealed interface UserError {
    record NotFound(int id) implements UserError {}
    record Suspended(int id, String reason) implements UserError {}
    record Deleted(int id, LocalDateTime deletedAt) implements UserError {}
}
```

---

### 5.3 Result 타입 완전 구현

```java
public sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}

    // 팩토리 메서드
    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }

    // 변환 (map)
    default <T> Result<T, F> map(Function<S, T> mapper) {
        return switch (this) {
            case Success(var value) -> Result.success(mapper.apply(value));
            case Failure(var error) -> Result.failure(error);
        };
    }

    // 연쇄 (flatMap)
    default <T> Result<T, F> flatMap(Function<S, Result<T, F>> mapper) {
        return switch (this) {
            case Success(var value) -> mapper.apply(value);
            case Failure(var error) -> Result.failure(error);
        };
    }

    // 양쪽 처리
    default <T> T fold(Function<S, T> onSuccess, Function<F, T> onFailure) {
        return switch (this) {
            case Success(var value) -> onSuccess.apply(value);
            case Failure(var error) -> onFailure.apply(error);
        };
    }
}
```

---

### 5.4 이커머스 실전 예제: 주문 처리

```java
// 에러 타입 정의
sealed interface OrderError {
    record UserNotFound(MemberId id) implements OrderError {}
    record ProductNotFound(ProductId id) implements OrderError {}
    record InsufficientStock(ProductId id, int requested, int available)
        implements OrderError {}
    record InvalidCoupon(CouponCode code, String reason) implements OrderError {}
}

// 파이프라인 조합
public Result<Order, OrderError> createOrder(CreateOrderRequest request) {
    return findUser(request.userId())
        .flatMap(user -> findProduct(request.productId())
            .flatMap(product -> checkStock(product, request.quantity())
                .flatMap(__ -> validateCoupon(request.couponCode())
                    .map(discount -> buildOrder(user, product, request.quantity(), discount))
                )
            )
        );
}
```

---

### 퀴즈 Chapter 5

#### Q5.1 [개념 확인] 전체 함수
다음 중 전체 함수(Total Function)의 특징은?

A. 항상 성공을 반환한다
B. 모든 입력에 대해 정의된 결과를 반환한다
C. 예외를 던지지 않으면 전체 함수다
D. void를 반환하면 전체 함수다

---

#### Q5.2 [코드 분석] 부분 함수 식별
다음 중 부분 함수(Partial Function)는?

```java
// A
int divide(int a, int b) {
    return a / b;
}

// B
Result<Integer, String> safeDivide(int a, int b) {
    if (b == 0) return Result.failure("0으로 나눌 수 없습니다");
    return Result.success(a / b);
}
```

A. A만
B. A와 B 모두
C. B만
D. 없음

---

#### Q5.3 [코드 분석] Result 활용
다음 코드의 결과는?

```java
Result<Integer, String> result = Result.success(10)
    .map(x -> x * 2)
    .flatMap(x -> x > 15
        ? Result.success(x)
        : Result.failure("값이 너무 작습니다"));

result.getOrElse(-1);
```

A. 20
B. 10
C. -1
D. "값이 너무 작습니다"

---

#### Q5.4 [설계 문제] 에러 타입 설계
"쿠폰 적용" 기능의 실패 경우를 Result 에러 타입으로 설계하세요.
실패 경우: 쿠폰 없음, 이미 사용됨, 만료됨, 최소 주문금액 미달

A. 모두 String으로 처리
B. sealed interface로 각 경우를 record로 정의
C. enum으로 정의
D. Exception으로 처리

---

#### Q5.5 [코드 작성] 전체 함수 변환
다음 부분 함수를 전체 함수로 변환하세요.

```java
public User withdraw(UserId id, Money amount) {
    User user = userRepository.find(id)
        .orElseThrow(() -> new UserNotFoundException(id));

    if (user.balance().lessThan(amount)) {
        throw new InsufficientBalanceException(user.balance(), amount);
    }

    return user.withdraw(amount);
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 6: 파이프라인과 결정론적 시스템

### 학습 목표
1. 결정론적(Deterministic) 함수의 정의와 장점을 설명할 수 있다
2. 순수 함수(Pure Function)를 작성할 수 있다
3. 샌드위치 아키텍처의 구조를 이해하고 적용할 수 있다
4. 비즈니스 로직에서 I/O를 격리하는 방법을 알 수 있다
5. 테스트 가능한 코드를 설계할 수 있다

---

### 6.1 결정론적 함수란?

```java
// 결정론적 함수 (순수 함수)
public static Money calculateTotal(List<OrderItem> items) {
    return items.stream()
        .map(item -> item.unitPrice().multiply(item.quantity().value()))
        .reduce(Money.zero(), Money::add);
}

// 비결정론적 함수
public Money calculateTotal(List<OrderItem> items) {
    // 외부 상태(taxRate) 참조 - 비결정론적
    TaxRate taxRate = taxService.getCurrentRate();
    return items.stream()
        .map(item -> item.unitPrice().multiply(item.quantity().value()))
        .reduce(Money.zero(), Money::add)
        .applyTax(taxRate);
}
```

#### 비유: 자판기 vs 바리스타

> **결정론적 함수는 자판기와 같습니다.**
>
> **자판기 (결정론적)**:
> - 500원 + 커피 버튼 → 항상 같은 커피
> - 몇 시에 눌러도, 누가 눌러도 같은 결과
> - 예측 가능하고, 테스트하기 쉬움
>
> **바리스타 (비결정론적)**:
> - "커피 주세요" → 바리스타 기분, 재료 상태, 시간에 따라 다른 커피
> - 같은 주문이어도 결과가 다를 수 있음
> - 예측 불가능하고, 테스트하기 어려움

---

### 6.2 샌드위치 아키텍처

```
┌─────────────────────────────────────┐
│  Top Bun (Impure): 재료 수집        │  ← DB 조회, API 호출
├─────────────────────────────────────┤
│  Meat (Pure): 요리                  │  ← 비즈니스 로직 (순수 함수)
├─────────────────────────────────────┤
│  Bottom Bun (Impure): 서빙          │  ← DB 저장, 알림 발송
└─────────────────────────────────────┘
```

#### Before: I/O가 로직에 섞여있음

```java
// 안티패턴: 비즈니스 로직 중간에 I/O
public Order processOrder(OrderRequest request) {
    User user = userRepository.find(request.userId()).orElseThrow();

    if (user.grade() == Grade.VIP) {
        Coupon vipCoupon = couponService.getVipCoupon();  // I/O in middle!
        request = request.withCoupon(vipCoupon);
    }

    Money total = calculateTotal(request.items());
    TaxRate taxRate = taxService.getCurrentRate();  // I/O in middle!
    total = total.applyTax(taxRate);

    Order order = new Order(user.id(), request.items(), total);
    return orderRepository.save(order);
}
```

#### After: 샌드위치 구조

```java
public Result<Order, OrderError> processOrder(OrderRequest request) {
    // === Top Bun: 데이터 수집 (Impure) ===
    Optional<User> userOpt = userRepository.find(request.userId());
    if (userOpt.isEmpty()) {
        return Result.failure(new OrderError.UserNotFound(request.userId()));
    }
    User user = userOpt.get();
    Optional<Coupon> coupon = user.grade() == Grade.VIP
        ? couponService.getVipCoupon()
        : Optional.empty();
    TaxRate taxRate = taxService.getCurrentRate();

    // === Meat: 비즈니스 로직 (Pure) ===
    Money total = OrderCalculations.calculateTotal(request.items(), coupon, taxRate);
    Order order = OrderCalculations.createOrder(user.id(), request.items(), total);

    // === Bottom Bun: 부수효과 (Impure) ===
    Order savedOrder = orderRepository.save(order);
    notificationService.sendOrderConfirmation(savedOrder);

    return Result.success(savedOrder);
}
```

---

### 퀴즈 Chapter 6

#### Q6.1 [개념 확인] 순수 함수
다음 중 순수 함수는?

```java
// A
int add(int a, int b) { return a + b; }

// B
int addWithLog(int a, int b) {
    System.out.println("Adding " + a + " + " + b);
    return a + b;
}

// C
int addWithCounter(int a, int b) {
    counter++;  // 클래스 필드
    return a + b;
}
```

A. A만
B. A와 B
C. A, B, C
D. 모두 순수하지 않음

---

#### Q6.2 [개념 확인] 샌드위치 아키텍처
샌드위치 아키텍처에서 "Meat" 레이어의 특징은?

A. 데이터베이스 접근을 담당한다
B. 알림 발송을 담당한다
C. 순수한 비즈니스 로직만 포함한다
D. 외부 API 호출을 담당한다

---

#### Q6.3 [코드 분석] I/O 격리
다음 코드의 문제점은?

```java
public Money calculatePrice(ProductId id, int quantity) {
    Product product = productRepository.find(id).orElseThrow();
    TaxRate rate = taxService.getCurrentRate();
    return product.price().multiply(quantity).applyTax(rate);
}
```

A. 문제없음
B. 비즈니스 로직 안에 I/O가 섞여있음
C. 예외를 던지는 것이 문제
D. 메서드가 너무 짧음

---

#### Q6.4 [설계 문제] 리팩토링
Q6.3의 코드를 샌드위치 구조로 리팩토링하면?

A. 그대로 둔다
B. Product, TaxRate를 파라미터로 받는 순수 함수로 분리
C. 모든 로직을 Repository로 이동
D. Try-catch로 감싼다

---

#### Q6.5 [코드 작성] 순수 함수 추출
다음 코드에서 순수 함수를 추출하세요.

```java
public Order applyPromotion(OrderId orderId) {
    Order order = orderRepository.find(orderId).orElseThrow();
    Promotion promo = promotionService.getActivePromotion();

    Money discount = order.total().multiply(promo.rate());
    Money newTotal = order.total().subtract(discount);

    Order updatedOrder = order.withTotal(newTotal);
    return orderRepository.save(updatedOrder);
}
```

---

정답은 Appendix C에서 확인할 수 있습니다.

---

> **💡 Q&A: mapToDouble() vs map() - 성능 차이가 있나요?**
>
> **결론**: 숫자를 다룰 때는 `mapToInt/Double`을 쓰는 것이 성능과 편의성 모두 좋습니다.
>
> **이유 1: 박싱/언박싱 패널티**
> - `map()` 사용 시: int → Integer 포장(Boxing) 필요 → 메모리 낭비 & CPU 연산 추가
> - `mapToInt()` 사용 시: IntStream으로 변환되어 포장 없이 int 그대로 처리
>
> **이유 2: 숫자 전용 API 제공**
> - `IntStream`/`DoubleStream`에는 `.sum()`, `.average()`, `.max()`, `.min()` 등 편리한 메서드 제공
> - 일반 `Stream<Integer>`에서는 `.reduce()`를 써야 해서 번거로움
>
> ```java
> // 권장: 숫자 전용 스트림
> int total = items.stream()
>     .mapToInt(Item::quantity)
>     .sum();
>
> // 비권장: 박싱 오버헤드
> Integer total = items.stream()
>     .map(Item::quantity)
>     .reduce(0, Integer::sum);
> ```
