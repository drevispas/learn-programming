# 06. Pipeline & Workflow Composition (파이프라인과 워크플로우 합성)

> Sources: DMMF Ch.5 (Workflow Pipeline), DMMF Ch.7 (Pipeline Assembly), DOP Ch.6 (Pipeline and Determinism)

---

## 1. Workflow as Pipeline (워크플로우를 파이프라인으로)

### 핵심 개념
- **관련 키워드**: Pipeline, Workflow, intermediate types, assembly line, type-driven design
- **통찰**: 비즈니스 워크플로우를 "타입이 변환되는 파이프라인"으로 모델링하면, 각 단계의 입출력이 타입으로 문서화된다.
- **설명**:

비즈니스 프로세스는 공장 조립 라인과 같다. 원재료(Command)가 들어와서 여러 공정(함수)을 거쳐 완제품(Event)이 나온다. 각 공정의 출력은 다음 공정의 입력이 되며, 중간 산출물(intermediate type)은 해당 시점의 데이터 품질을 보장한다.

DMMF에서는 이를 "워크플로우를 타입으로 표현"한다고 설명한다. `PlaceOrderCommand`(검증 전 raw data) -> `ValidatedOrder`(검증된 데이터) -> `PricedOrder`(가격 계산 완료) -> `OrderPlaced`(확정된 과거 사실) 각 단계마다 다른 타입을 사용함으로써, 검증되지 않은 데이터가 가격 계산 단계로 흘러가는 것이 컴파일 타임에 방지된다.

이 접근법의 핵심 이점은 **자기 문서화(self-documenting)**이다. 코드를 읽는 것만으로 "이 함수는 검증된 주문을 받아 가격이 붙은 주문을 만든다"는 것을 알 수 있다.

```
=== Order Processing Pipeline ===

+-------------------+     +-------------------+     +-------------------+
| PlaceOrderCommand |     | ValidatedOrder    |     | PricedOrder       |
| (unvalidated)     |---->| (validated)       |---->| (priced)          |
| - raw strings     |     | - value objects   |     | - total calculated|
| - no guarantees   |     | - type-safe IDs   |     | - discounts applied|
+-------------------+     +-------------------+     +-------------------+
                                                            |
                                                            v
                          +-------------------+     +-------------------+
                          | OrderPlaced       |<----| PaidOrder         |
                          | (immutable event) |     | (payment confirmed)|
                          | - past tense      |     | - transaction ID  |
                          +-------------------+     +-------------------+
```

### 개념이 아닌 것
- **"모든 단계에 새 타입을 만들어야 한다"**: 변환이 의미 없는 단계(예: 로깅)에는 새 타입이 불필요하다. 데이터 품질이 변하는 경계에서만 타입을 분리한다.
- **"파이프라인은 반드시 선형이어야 한다"**: 분기(조건부 로직)도 가능하다. 핵심은 각 경로의 입출력이 명확한 것이다.

### Before: Traditional OOP
```java
// [X] 하나의 Order 클래스가 모든 상태를 담당
public class OrderService {
    public Order processOrder(Map<String, Object> request) {
        Order order = new Order();
        // 검증 전 데이터와 검증 후 데이터가 같은 타입
        order.setCustomerId((String) request.get("customerId"));
        order.setItems((List) request.get("items"));

        // 검증
        if (order.getCustomerId() == null) throw new ValidationException("...");
        // 검증 후에도 같은 order 객체 재사용

        // 가격 계산
        order.setTotalAmount(calculateTotal(order));
        // order가 "검증됨" 상태인지 "가격 계산됨" 상태인지 타입으로 구분 불가

        // 결제
        order.setPaymentId(processPayment(order));
        order.setStatus("PAID");

        return order;
    }
}
```
- **의도 및 코드 설명**: 하나의 Order 객체가 생성 -> 검증 -> 가격 계산 -> 결제까지 모든 상태를 담당한다.
- **뭐가 문제인가**:
  - 타입 안전성 없음: 검증 전 order와 검증 후 order가 같은 타입
  - setter로 상태 변경: 어느 시점에 어떤 필드가 설정되었는지 보장 불가
  - null 가능성: totalAmount가 null인 order가 결제 단계에 도달 가능
  - 문서화 부재: 코드를 읽어도 워크플로우의 각 단계 구분 어려움

### After: Modern Approach
```java
// [O] 중간 타입으로 데이터 품질 표현
// Command: 사용자 의도 (검증 전 raw data)
public record PlaceOrderCommand(
    String customerId,
    List<UnvalidatedOrderLine> lines,
    String shippingAddress,
    String couponCode
) {}

public record UnvalidatedOrderLine(String productId, int quantity) {}

// 검증 후: 유효한 상태 (Value Object 사용)
public record ValidatedOrder(
    CustomerId customerId,
    List<ValidatedOrderLine> lines,
    ShippingAddress shippingAddress,
    CouponCode couponCode
) {}

public record ValidatedOrderLine(ProductId productId, Quantity quantity, Money unitPrice) {}

// 가격 계산 후
public record PricedOrder(
    CustomerId customerId,
    List<ValidatedOrderLine> lines,
    Money subtotal,
    Money discount,
    Money totalAmount
) {}

// 이벤트: 확정된 과거 사실 (불변)
public record OrderPlaced(
    OrderId orderId,
    Money totalAmount,
    LocalDateTime occurredAt
) {}

// 파이프라인: 타입이 변환을 문서화
public class PlaceOrderWorkflow {
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder(command)             // Command -> ValidatedOrder
            .map(this::calculatePrice)            // ValidatedOrder -> PricedOrder
            .flatMap(this::processPayment)        // PricedOrder -> PaidOrder
            .map(this::createEvent);              // PaidOrder -> OrderPlaced
    }
}
```
- **의도 및 코드 설명**: 각 단계의 출력이 다른 타입(record)이므로, 검증되지 않은 데이터가 가격 계산에 사용되는 것이 컴파일 타임에 방지된다.
- **무엇이 좋아지나**:
  - 타입 = 문서: 함수 시그니처만 보면 워크플로우 파악 가능
  - 컴파일 안전: 잘못된 순서의 호출이 타입 에러
  - 불변성: 각 단계의 산출물은 record로 불변
  - 테스트 용이: 각 단계를 독립적으로 테스트 가능

### 이해를 위한 부가 상세
- Command는 "미래/명령형"(PlaceOrderCommand), Event는 "과거/완료형"(OrderPlaced)으로 네이밍한다.
- Record의 compact constructor에서 검증하면, ValidatedOrder 인스턴스가 존재하는 것만으로 "검증 통과"가 보장된다.

### 틀리기/놓치기 쉬운 부분
- Record 간 변환 시 공통 필드를 빠뜨리기 쉽다. ValidatedOrder -> PricedOrder 변환에서 lines 필드를 누락하면 정보 손실이 발생한다.
- 중간 타입이 너무 많으면 오히려 복잡해질 수 있다. "데이터 품질이 변하는 경계"에서만 타입을 분리하라.

### 꼭 기억할 것
- 워크플로우의 각 단계를 **서로 다른 타입**으로 표현하면, 타입 시스템이 올바른 순서와 데이터 품질을 보장한다.
- Command(요청) -> Domain Types(처리 중) -> Event(결과)가 파이프라인의 기본 구조이다.

---

## 2. Function Composition (함수 합성)

### 핵심 개념
- **관련 키워드**: Composition, andThen, compose, type alignment, pipeline operator
- **통찰**: 출력 타입이 다음 함수의 입력 타입과 일치하면 레고 블록처럼 자유롭게 조립할 수 있다.
- **설명**:

함수 합성은 두 함수 `f: A -> B`와 `g: B -> C`를 연결하여 `h: A -> C`를 만드는 것이다. 핵심 조건은 **타입 정렬(type alignment)**: f의 출력 타입이 g의 입력 타입과 일치해야 한다. 이 조건만 만족하면 함수는 레고 블록처럼 자유롭게 조합된다.

DMMF에서는 이를 "함수 합성 파이프라인"이라 부르며, 각 함수가 하나의 책임만 담당하고 타입으로 연결되는 구조를 강조한다. DOP에서는 이를 "결정론적 파이프라인"이라 부르며, 순수 함수의 합성으로 예측 가능한 시스템을 구축하는 데 초점을 맞춘다.

Java에서는 `Function.andThen()`이나 `Result.map()/flatMap()`을 통해 합성을 구현한다. Result를 반환하는 함수들의 합성에서는 flatMap이 "접착제" 역할을 한다.

```
=== Function Composition: Type Alignment ===

 f: A --> B     g: B --> C     h: C --> D

 Composed: A --> B --> C --> D
           f    g    h

 Rule: output type of f MUST match input type of g

=== With Result (flatMap as glue) ===

 f: A --> Result<B, E>
 g: B --> Result<C, E>
 h: C --> D

 Composed: f(a).flatMap(g).map(h)
           A --> Result<B,E> --> Result<C,E> --> Result<D,E>
```

### 개념이 아닌 것
- **"합성은 순서대로 실행하는 것"**: 단순 순차 실행과 합성의 차이는 "새로운 함수를 만드는 것"이다. 합성된 함수는 그 자체로 재사용 가능한 단위이다.
- **"모든 함수를 합성해야 한다"**: Side effect가 있는 함수(DB 저장 등)는 합성 대상이 아니라 Imperative Shell에서 호출한다.

### Before: Traditional OOP
```java
// [X] 절차적으로 단계를 나열 - 합성 불가능한 구조
public class OrderProcessor {
    public OrderResult process(OrderRequest request) {
        // 각 단계가 내부 상태를 변경하는 메서드
        this.validated = validate(request);        // 인스턴스 변수에 저장
        this.priced = calculatePrice();            // this.validated 참조
        this.paid = processPayment();              // this.priced 참조
        return createResult();                     // this.paid 참조
    }

    private ValidatedData validate(OrderRequest r) { ... }
    private PricedData calculatePrice() {
        // this.validated를 참조 - 합성 불가!
        return new PricedData(this.validated.items(), ...);
    }
}
```
- **의도 및 코드 설명**: 각 단계가 인스턴스 변수를 통해 이전 결과를 참조한다.
- **뭐가 문제인가**:
  - 인스턴스 상태에 의존: 함수 단독 테스트 불가
  - 순서 변경 불가: calculatePrice()는 반드시 validate() 이후에 호출되어야 함 (암묵적)
  - 재사용 불가: calculatePrice()를 다른 컨텍스트에서 사용할 수 없음
  - 합성 불가: 각 함수의 입출력이 명시적이지 않음

### After: Modern Approach
```java
// [O] 함수 합성 - 입출력이 명확한 순수 함수
public class PlaceOrderWorkflow {
    // 각 함수의 시그니처가 합성 조건을 보여줌
    private final Function<PlaceOrderCommand, Result<ValidatedOrder, OrderError>>
        validateOrder;
    private final Function<ValidatedOrder, PricedOrder>
        calculatePrice;
    private final Function<PricedOrder, Result<PaidOrder, OrderError>>
        processPayment;
    private final Function<PaidOrder, OrderPlaced>
        createEvent;

    // 합성: 타입이 정렬되어 자연스럽게 연결
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder.apply(command)       // Result<ValidatedOrder, E>
            .map(calculatePrice)                  // Result<PricedOrder, E>
            .flatMap(processPayment)              // Result<PaidOrder, E>
            .map(createEvent);                    // Result<OrderPlaced, E>
    }
}

// 각 함수는 독립적으로 테스트 가능
public class OrderCalculations {
    // 순수 함수: 입력만으로 출력 결정
    public static PricedOrder calculatePrice(ValidatedOrder order) {
        Money subtotal = order.lines().stream()
            .map(line -> line.unitPrice().multiply(line.quantity()))
            .reduce(Money.zero(), Money::add);
        return new PricedOrder(order.customerId(), order.lines(),
            subtotal, Money.zero(), subtotal);
    }
}
```
- **의도 및 코드 설명**: 각 함수의 시그니처 `A -> B` 또는 `A -> Result<B, E>`가 합성 조건을 명시한다. 함수 참조를 필드로 보관하여 DI(의존성 주입)도 가능하다.
- **무엇이 좋아지나**:
  - 합성 가능: 각 함수가 독립적이고, 타입으로 연결됨
  - 테스트 용이: 각 함수를 개별 단위 테스트 가능
  - 교체 용이: validateOrder를 다른 구현으로 교체 가능
  - 읽기 쉬움: execute() 메서드가 워크플로우의 전체 흐름을 한눈에 보여줌

### 이해를 위한 부가 상세
- Java에서는 `Function<A, B>.andThen(Function<B, C>)`로 두 함수를 합성할 수 있지만, Result를 반환하는 함수는 `flatMap`으로 합성해야 한다.
- Partial Application(부분 적용)으로 의존성을 미리 주입하면, 순수 함수처럼 합성 가능한 함수를 만들 수 있다.

### 틀리기/놓치기 쉬운 부분
- 타입이 맞지 않으면 합성이 불가능하다. 예를 들어 `validateOrder`가 `Result<ValidatedOrder, ValidationError>`를 반환하고 `processPayment`가 `Result<PaidOrder, PaymentError>`를 기대하면, 에러 타입이 달라 체이닝 불가. `mapError`로 통일 필요.
- `map`으로 전달하는 함수 안에서 예외를 던지면 합성 파이프라인이 깨진다.

### 꼭 기억할 것
- 함수 합성의 전제조건: **출력 타입 = 다음 입력 타입** (타입 정렬).
- Result를 반환하는 함수는 `flatMap`으로, 순수 변환은 `map`으로 연결한다.

---

## 3. Deterministic Systems (결정론적 시스템)

### 핵심 개념
- **관련 키워드**: Deterministic, Pure Function, referential transparency, predictability, testability
- **통찰**: 같은 입력에 항상 같은 출력을 보장하는 결정론적 함수로 비즈니스 로직을 작성하면, 테스트와 디버깅이 극적으로 쉬워진다.
- **설명**:

결정론적 함수(Deterministic Function)는 순수 함수(Pure Function)와 같은 개념이다. 외부 상태를 읽지도 쓰지도 않으며, 같은 입력이면 언제 어디서 호출해도 항상 같은 출력을 반환한다. DOP에서는 이를 "자판기 vs 바리스타" 비유로 설명한다. 자판기는 같은 버튼을 누르면 항상 같은 결과, 바리스타는 기분/재료/시간에 따라 다른 커피.

비결정론적 요소(DB 조회, API 호출, 현재 시간, 랜덤 값)를 함수 내부에서 사용하면 테스트가 어려워진다. 해결책은 이러한 외부 의존성을 **파라미터로 전달**하는 것이다. `taxService.getCurrentRate()` 대신 `TaxRate taxRate`를 파라미터로 받으면, 함수는 결정론적이 된다.

이 원칙은 파이프라인 설계의 근간이다. 비즈니스 로직을 결정론적 함수로 작성하고, I/O를 경계에 밀어내면 "Functional Core / Imperative Shell" 아키텍처가 자연스럽게 도출된다.

```
=== Deterministic vs Non-Deterministic ===

+------------------------------------------+
| Non-Deterministic Function               |
|                                          |
|  input --+--> [DB call]                  |
|           +--> [API call]                |
|           +--> [Current time]            |
|           +--> output (varies!)          |
|                                          |
| Same input -> DIFFERENT output each time |
+------------------------------------------+

+------------------------------------------+
| Deterministic Function                   |
|                                          |
|  (input, taxRate, time) --> output       |
|                                          |
| External deps become PARAMETERS          |
| Same params -> ALWAYS same output        |
+------------------------------------------+
```

### 개념이 아닌 것
- **"모든 함수가 순수해야 한다"**: I/O는 어딘가에서 반드시 수행되어야 한다. 핵심은 비즈니스 로직과 I/O를 분리하는 것이다.
- **"순수 함수는 느리다"**: 순수 함수는 캐싱, 병렬 실행, 지연 평가가 자유롭기 때문에 오히려 최적화 기회가 더 많다.

### Before: Traditional OOP
```java
// [X] 비결정론적 함수 - I/O가 로직에 섞여있음
public class PriceCalculator {
    private final TaxService taxService;
    private final CouponService couponService;

    public Money calculateTotal(List<OrderItem> items) {
        // 외부 상태(taxRate) 참조 - 비결정론적
        TaxRate taxRate = taxService.getCurrentRate();

        Money subtotal = items.stream()
            .map(item -> item.unitPrice().multiply(item.quantity()))
            .reduce(Money.zero(), Money::add);

        // 외부 API 호출 - 비결정론적
        Optional<Coupon> coupon = couponService.getActiveCoupon();
        Money discounted = coupon.map(c -> subtotal.applyDiscount(c)).orElse(subtotal);

        return discounted.applyTax(taxRate);
    }
    // 테스트하려면 taxService, couponService를 Mock해야 함
}
```
- **의도 및 코드 설명**: 가격 계산 로직 중간에 외부 서비스 호출이 섞여 있다.
- **뭐가 문제인가**:
  - 테스트 시 Mock 필수: taxService, couponService를 Mocking해야 단위 테스트 가능
  - 재현 불가: "어제는 됐는데 오늘은 안 된다" (세율 변경, 쿠폰 만료)
  - 순서 의존: taxService 호출 시점에 따라 결과 달라질 수 있음
  - 병렬 실행 불안전: 외부 상태 변경 가능성

### After: Modern Approach
```java
// [O] 결정론적 함수 - 외부 의존성을 파라미터로
public class OrderCalculations {
    // 순수 함수: 모든 의존성이 파라미터
    public static Money calculateTotal(
        List<OrderItem> items,
        TaxRate taxRate,
        Optional<Coupon> coupon
    ) {
        Money subtotal = items.stream()
            .map(item -> item.unitPrice().multiply(item.quantity()))
            .reduce(Money.zero(), Money::add);

        Money discounted = coupon
            .map(c -> subtotal.applyDiscount(c))
            .orElse(subtotal);

        return discounted.applyTax(taxRate);
    }
}

// 호출부 (Imperative Shell)에서 I/O 수행
public class PlaceOrderUseCase {
    public Result<Order, OrderError> execute(OrderRequest request) {
        // I/O: 외부 데이터 수집
        TaxRate taxRate = taxService.getCurrentRate();
        Optional<Coupon> coupon = couponService.getActiveCoupon();
        List<OrderItem> items = resolveItems(request);

        // Pure: 결정론적 계산
        Money total = OrderCalculations.calculateTotal(items, taxRate, coupon);

        // I/O: 결과 저장
        Order order = new Order(request.customerId(), items, total);
        return Result.success(orderRepository.save(order));
    }
}
```
- **의도 및 코드 설명**: `OrderCalculations.calculateTotal()`은 모든 입력을 파라미터로 받는 순수 함수다. I/O는 UseCase(Imperative Shell)에서 수행된다.
- **무엇이 좋아지나**:
  - Mock 없는 테스트: 값만 넣으면 결과 검증 가능
  - 항상 재현 가능: 같은 입력이면 언제든 같은 결과
  - 재사용 가능: 다른 컨텍스트에서도 동일 함수 사용
  - 병렬 실행 안전: 외부 상태를 읽지도 쓰지도 않음

### 이해를 위한 부가 상세
- 결정론적 함수를 `static` 메서드로 선언하면 "인스턴스 상태에 의존하지 않음"을 명확히 표현할 수 있다.
- 현재 시간(`LocalDateTime.now()`)도 비결정론적이다. 시간에 의존하는 로직은 `Clock` 또는 `LocalDateTime`을 파라미터로 받아야 한다.

### 틀리기/놓치기 쉬운 부분
- `static` 메서드라도 내부에서 전역 변수나 싱글톤을 참조하면 비결정론적이다. 진짜 순수 함수인지 확인하라.
- 순수 함수에 전달할 파라미터가 너무 많아지면 Parameter Object(record)로 묶어라.

### 꼭 기억할 것
- 결정론적 함수의 핵심: **외부 의존성을 내부에서 호출하지 말고, 파라미터로 전달받아라.**
- 이 원칙을 따르면 비즈니스 로직은 "계산기"처럼 동작하여 테스트, 디버깅, 추론이 쉬워진다.

---

## 4. Sandwich Architecture Intro (I/O -> Pure -> I/O)

### 핵심 개념
- **관련 키워드**: Sandwich Architecture, Impure-Pure-Impure, Functional Core, Imperative Shell, I/O isolation
- **통찰**: I/O(데이터 수집) -> Pure Logic(비즈니스 계산) -> I/O(결과 저장)의 3층 구조로 로직을 조직하면, 핵심 비즈니스 로직을 Mock 없이 테스트할 수 있다.
- **설명**:

샌드위치 아키텍처는 DOP에서 제시하는 코드 조직 패턴이다. 식빵-고기-식빵 구조처럼 I/O(Impure) -> 비즈니스 로직(Pure) -> I/O(Impure)로 코드를 배치한다.

Top Bun(윗빵)은 필요한 데이터를 외부에서 수집하는 I/O 영역이다. DB 조회, API 호출, 설정 읽기 등을 수행한다. Meat(고기)는 수집된 데이터를 입력으로 받아 순수 계산만 수행하는 영역이다. 할인 적용, 세금 계산, 상태 전이 판단 등이 여기에 해당한다. Bottom Bun(아랫빵)은 계산 결과를 외부로 출력하는 I/O 영역이다. DB 저장, 알림 발송, 로그 출력 등을 수행한다.

이 패턴의 핵심 이점은 Meat 영역을 Mock 없이 단위 테스트할 수 있다는 것이다. 순수 함수이므로 입력값만 준비하면 결과를 검증할 수 있다. 또한 비즈니스 로직 변경과 인프라 변경이 서로 영향을 주지 않는다.

```
+==================================================+
|  TOP BUN (Impure - Data Collection)              |
|  - userRepository.find(userId)                   |
|  - couponService.getVipCoupon()                  |
|  - taxService.getCurrentRate()                   |
+==================================================+
                      |
                      v (pure data passed down)
+==================================================+
|  MEAT (Pure - Business Logic)                    |
|  - OrderCalculations.calculateTotal(items, tax)  |
|  - subtotal.applyDiscount(coupon)                |
|  - OrderCalculations.createOrder(user, items)    |
|                                                  |
|  [TESTABLE WITHOUT MOCKS]                        |
+==================================================+
                      |
                      v (result passed down)
+==================================================+
|  BOTTOM BUN (Impure - Side Effects)              |
|  - orderRepository.save(order)                   |
|  - notificationService.sendConfirmation(order)   |
|  - eventPublisher.publish(orderPlaced)           |
+==================================================+
```

### 개념이 아닌 것
- **"모든 메서드를 3부분으로 나눠야 한다"**: 간단한 CRUD는 이 패턴이 과한 수 있다. 비즈니스 로직이 복잡한 경우에 가치가 크다.
- **"I/O를 한 줄도 섞으면 안 된다"**: 현실에서는 Meat 영역 중간에 I/O가 필요한 경우도 있다. 그런 경우 함수를 더 작게 분리하거나, 필요한 데이터를 미리 수집하라.

### Before: Traditional OOP
```java
// [X] I/O와 로직이 뒤섞인 안티패턴
public class OrderService {
    public Order processOrder(OrderRequest request) {
        User user = userRepository.find(request.userId()).orElseThrow();

        if (user.grade() == Grade.VIP) {
            Coupon vipCoupon = couponService.getVipCoupon();  // I/O in middle!
            request = request.withCoupon(vipCoupon);
        }

        Money total = calculateTotal(request.items());
        if (request.coupon().isPresent()) {
            total = total.applyDiscount(request.coupon().get());
        }
        TaxRate taxRate = taxService.getCurrentRate();  // I/O in middle!
        total = total.applyTax(taxRate);

        Order order = new Order(user.id(), request.items(), total);
        return orderRepository.save(order);
    }
}
```
- **의도 및 코드 설명**: 비즈니스 로직(할인 적용, 세금 계산) 사이에 I/O 호출이 산재해 있다.
- **뭐가 문제인가**:
  - 테스트 불가: calculateTotal만 테스트하려 해도 userRepository, couponService, taxService Mock 필요
  - 예측 불가: 외부 상태에 따라 결과 변동
  - 재사용 불가: 이 로직을 다른 서비스에서 사용하려면 같은 의존성 주입 필요
  - 디버깅 어려움: 중간 I/O 실패 시 어떤 상태까지 진행됐는지 파악 어려움

### After: Modern Approach
```java
// [O] 샌드위치 구조 - I/O와 로직 분리
public class PlaceOrderUseCase {
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
        Money subtotal = OrderCalculations.calculateSubtotal(request.items());
        Money discounted = coupon.map(c -> subtotal.applyDiscount(c)).orElse(subtotal);
        Money total = discounted.applyTax(taxRate);
        Order order = OrderCalculations.createOrder(user.id(), request.items(), total);

        // === Bottom Bun: 부수효과 (Impure) ===
        Order savedOrder = orderRepository.save(order);
        notificationService.sendOrderConfirmation(savedOrder);

        return Result.success(savedOrder);
    }
}

// Meat 영역의 순수 함수들 - Mock 없이 테스트 가능
public class OrderCalculations {
    public static Money calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.unitPrice().multiply(item.quantity()))
            .reduce(Money.zero(), Money::add);
    }

    public static Order createOrder(CustomerId customerId, List<OrderItem> items, Money total) {
        return new Order(OrderId.generate(), customerId, items, total, new Unpaid());
    }
}
```
- **의도 및 코드 설명**: I/O를 최상단과 최하단에 몰아넣고, 중간의 Meat 영역은 순수 계산만 수행한다.
- **무엇이 좋아지나**:
  - Meat 영역 독립 테스트: `OrderCalculations` 메서드들은 값만 넣으면 검증 가능
  - 명확한 경계: I/O와 로직의 책임이 코드 위치로 구분됨
  - 재사용 가능: 순수 함수는 다른 UseCase에서도 재사용
  - 디버깅 용이: Meat 영역은 같은 입력이면 항상 같은 결과

### 이해를 위한 부가 상세
- 샌드위치 아키텍처는 DMMF의 "Functional Core / Imperative Shell"과 같은 개념을 다른 비유로 표현한 것이다. DOP는 "샌드위치", DMMF는 "양파(Onion)" 비유를 선호한다.
- Top Bun에서 수집할 데이터가 너무 많으면 Data Object(record)로 묶어서 Meat에 전달하라.

### 틀리기/놓치기 쉬운 부분
- Meat 영역에서 `LocalDateTime.now()`를 호출하면 비결정론적이 된다. 현재 시간도 Top Bun에서 수집하여 파라미터로 전달하라.
- 샌드위치가 "완벽하게" 분리되지 않아도 괜찮다. 80/20 법칙으로, 핵심 비즈니스 로직만 순수하게 추출해도 큰 효과가 있다.

### 꼭 기억할 것
- 샌드위치 구조: **I/O(수집) -> Pure(계산) -> I/O(저장)** 으로 코드를 배치한다.
- Meat(순수 영역)에는 DB 호출, API 호출, 시간 조회 등 **어떤 외부 의존도 없어야** 한다.
- 이 패턴은 Chapter 07에서 다루는 Onion Architecture와 Functional Core/Imperative Shell의 구체적 구현 가이드이다.
