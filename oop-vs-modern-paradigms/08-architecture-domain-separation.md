# 07. Architecture & Domain Separation (아키텍처와 도메인 분리)

> Sources: DMMF Ch.8 (Domain Separation), DMMF Ch.9 (Functional Architecture), DOP Ch.6 (Pipeline/Determinism), DOP Ch.9 (JPA/Spring Coexistence)

---

## 1. Functional Core / Imperative Shell (함수형 코어 / 명령형 셸)

### 핵심 개념
- **관련 키워드**: Functional Core, Imperative Shell, pure logic, side effects, testability, Gary Bernhardt
- **통찰**: 비즈니스 로직(판단, 계산, 변환)은 순수 함수로, 부수효과(DB, API, 알림)는 바깥 껍질에서 처리하면, 핵심 로직을 Mock 없이 테스트할 수 있다.
- **설명**:

Functional Core / Imperative Shell은 Gary Bernhardt가 제안하고 DMMF와 DOP 모두에서 핵심 아키텍처로 채택한 패턴이다. "머리 쓰는 일"(계산, 판단, 데이터 변환)은 Pure Function으로 작성하여 Functional Core에 두고, "몸 쓰는 일"(DB 저장, API 호출, 파일 읽기)은 Imperative Shell에서 수행한다.

Functional Core의 함수들은 같은 입력에 항상 같은 출력을 보장한다. 따라서 DB 연결, 네트워크 Mock, 시간 Stubbing 없이 단순한 입력/출력 비교로 테스트할 수 있다. 이것이 이 패턴의 가장 큰 실용적 이점이다.

Imperative Shell은 "로직이 거의 없도록" 설계한다. 데이터를 수집하고 Core의 순수 함수를 호출한 뒤, 결과를 저장/전달하는 역할만 담당한다. Shell이 얇을수록 테스트 부담이 줄어든다.

**[그림 07.1]** Functional Core / Imperative Shell (함수형 코어 / 명령형 셸)
```
+==================================================================+
|                    IMPERATIVE SHELL                               |
|  (I/O, Side Effects, Framework Integration)                      |
|                                                                   |
|  +------------------------------------------------------------+  |
|  |                  FUNCTIONAL CORE                            |  |
|  |  (Pure Functions, Business Logic, Domain Rules)             |  |
|  |                                                             |  |
|  |  - OrderCalculations.calculateTotal(items, tax)             |  |
|  |  - OrderDomainService.canCancel(order)                      |  |
|  |  - PriceCalculations.applyDiscount(subtotal, coupon)        |  |
|  |                                                             |  |
|  |  [No DB, No API, No Clock, No Random]                       |  |
|  |  [Testable with simple input/output assertions]             |  |
|  +------------------------------------------------------------+  |
|                                                                   |
|  PlaceOrderUseCase.execute():                                     |
|    1. Read from DB (Shell)                                        |
|    2. Call pure functions (Core)                                   |
|    3. Save to DB (Shell)                                          |
+==================================================================+
```

### 개념이 아닌 것
- **"100% 순수하게 작성해야 한다"**: 현실에서는 불가능하다. 핵심은 비율을 최대화하는 것이다. 비즈니스 로직의 80-90%가 순수하면 충분히 효과적이다.
- **"DI(의존성 주입)를 안 써도 된다"**: Imperative Shell에서는 여전히 Repository, Gateway 등의 의존성이 필요하다. 다만 Core에는 의존성이 없다.

### Before: Traditional OOP

**[코드 07.1]** Traditional OOP: 비즈니스 로직과 부수효과가 뒤섞인 서비스
```java
 1| // package: com.ecommerce.order
 2| // [X] 비즈니스 로직과 부수효과가 뒤섞인 서비스
 3| public class OrderService {
 4|   private final OrderRepository orderRepository;
 5|   private final PaymentGateway paymentGateway;
 6|   private final CouponRepository couponRepository;
 7| 
 8|   public Order placeOrder(PlaceOrderCommand cmd) {
 9|     // 로직과 I/O가 뒤섞임
10|     Order order = new Order();
11|     order.setCustomerId(cmd.customerId());
12| 
13|     for (var line : cmd.lines()) {
14|       Product product = productRepository.findById(line.productId());  // I/O
15|       if (product.stock() < line.quantity()) {
16|         throw new OutOfStockException(product.id());
17|       }
18|       order.addLine(product, line.quantity());  // 로직
19|     }
20| 
21|     Coupon coupon = couponRepository.find(cmd.couponCode());  // I/O
22|     order.applyDiscount(coupon);                              // 로직
23| 
24|     PaymentResult payment = paymentGateway.charge(order.total());  // I/O
25|     order.setPaymentId(payment.txId());                            // 로직
26|     order.setStatus(OrderStatus.PAID);
27| 
28|     return orderRepository.save(order);  // I/O
29|   }
30| }
```
- **의도 및 코드 설명**: 하나의 메서드에 I/O 호출과 비즈니스 로직이 번갈아 나온다.
- **뭐가 문제인가**:
  - 단위 테스트 불가: productRepository, couponRepository, paymentGateway 모두 Mock 필요
  - 로직 재사용 불가: 할인 계산 로직을 다른 곳에서 쓸 수 없음
  - 변경 영향 범위 큼: DB 스키마 변경이 비즈니스 로직 코드 수정으로 이어짐
  - 디버깅 어려움: 중간 I/O 실패 시 상태 추적 곤란

### After: Modern Approach

**[코드 07.2]** Modern: Functional Core: 순수한 도메인 로직
```java
 1| // package: com.ecommerce.order
 2| // [O] Functional Core: 순수한 도메인 로직
 3| public class OrderDomainService {
 4|   public static PricedOrder calculatePrice(ValidatedOrder order, Optional<Coupon> coupon) {
 5|     Money subtotal = order.lines().stream()
 6|       .map(line -> line.unitPrice().multiply(line.quantity()))
 7|       .reduce(Money.zero(), Money::add);
 8| 
 9|     Money discount = coupon
10|       .map(c -> c.calculateDiscount(subtotal))
11|       .orElse(Money.zero());
12| 
13|     return new PricedOrder(order.customerId(), order.lines(),
14|       subtotal, discount, subtotal.subtract(discount));
15|   }
16| 
17|   public static boolean canCancel(Order order) {
18|     return switch (order.status()) {
19|       case Unpaid u, Paid p -> true;
20|       case Shipping s, Delivered d, Cancelled c -> false;
21|     };
22|   }
23| }
24| 
25| // [O] Imperative Shell: 부수효과 조율
26| public class PlaceOrderUseCase {
27|   private final OrderRepository orderRepository;
28|   private final PaymentGateway paymentGateway;
29|   private final CouponRepository couponRepository;
30| 
31|   @Transactional
32|   public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand cmd) {
33|     // Shell: 데이터 수집
34|     var validationResult = validateOrder(cmd);
35|     if (validationResult.isFailure()) return validationResult.mapError(e -> e);
36|     ValidatedOrder validated = validationResult.value();
37| 
38|     Optional<Coupon> coupon = cmd.couponCode()
39|       .flatMap(couponRepository::findByCode);
40| 
41|     // Core: 순수 로직 호출 (테스트 쉬움!)
42|     PricedOrder priced = OrderDomainService.calculatePrice(validated, coupon);
43| 
44|     // Shell: 외부 호출
45|     var paymentResult = paymentGateway.charge(priced.totalAmount());
46|     if (paymentResult.isFailure()) {
47|       return Result.failure(new OrderError.PaymentFailed(paymentResult.error().message()));
48|     }
49| 
50|     // Shell: 저장
51|     Order order = new Order(OrderId.generate(), priced, paymentResult.value().txId());
52|     orderRepository.save(order);
53| 
54|     return Result.success(new OrderPlaced(order.id(), priced.totalAmount()));
55|   }
56| }
```
- **의도 및 코드 설명**: `OrderDomainService`는 순수 함수만 포함하며, `PlaceOrderUseCase`는 I/O를 조율한다.
- **무엇이 좋아지나**:
  - Core 테스트: `OrderDomainService.calculatePrice()`를 값만 넣어 테스트
  - 관심사 분리: 비즈니스 규칙 변경은 Core만, 인프라 변경은 Shell만 수정
  - 재사용: Core 함수를 다른 UseCase에서도 활용 가능
  - Shell 최소화: Shell에 로직이 거의 없어 통합 테스트로 충분

### 이해를 위한 부가 상세
- Functional Core의 함수는 `static`으로 선언하면 "인스턴스 상태 없음"을 코드 레벨에서 표현할 수 있다.
- DOP에서는 이를 `*Calculations` 클래스(예: `OrderCalculations`, `PriceCalculations`)로 명명한다.

### 틀리기/놓치기 쉬운 부분
- Core 함수 안에서 `LocalDateTime.now()`를 호출하면 순수성이 깨진다. 시간도 파라미터로 전달하라.
- Shell이 너무 두꺼워지면(복잡한 분기 로직이 Shell에 있으면) Core로 로직을 이전하라.

### 꼭 기억할 것
- **Core = "머리 쓰는 일"** (계산, 판단, 변환) -> 순수 함수, Mock 없이 테스트
- **Shell = "몸 쓰는 일"** (DB, API, 알림) -> 얇게 유지, 통합 테스트

---

## 2. Dependency Injection vs Function Parameters (의존성 주입 vs 함수 파라미터)

### 핵심 개념
- **관련 키워드**: DI, Function Parameter, Currying, Partial Application, Higher-Order Function
- **통찰**: 전통적 DI(인터페이스 + @Autowired)는 Shell에서 사용하고, Core에서는 필요한 값을 함수 파라미터로 직접 전달하는 것이 더 단순하고 테스트하기 쉽다.
- **설명**:

전통적 OOP에서는 모든 의존성을 인터페이스로 추상화하고 DI 컨테이너가 주입한다. 이 방식은 유연하지만, 비즈니스 로직 테스트에도 Mock 설정이 필요해진다.

함수형 접근에서는 의존성을 두 가지로 분류한다. **데이터 의존성**(TaxRate, ExchangeRate 같은 값)은 함수 파라미터로 직접 전달한다. **기능 의존성**(DB 저장, API 호출 같은 부수효과)은 Shell에 두고 Core에 전달하지 않는다.

DMMF에서는 이를 더 나아가 "함수를 파라미터로 전달"하는 패턴을 제시한다. `GetExchangeRate` 같은 Functional Interface를 파라미터로 받으면, 인터페이스 없이도 교체 가능성을 확보할 수 있다.

**[그림 07.2]** Dependency Injection vs Function Parameters (의존성 주입 vs 함수 파라미터)
```
=== Traditional DI vs Functional Parameter ===

Traditional DI:
  +------------------+      +------------------+
  | OrderService     |----->| TaxService (I/F) |
  | @Autowired       |      +------------------+
  | taxService       |             ^
  +------------------+             |
                             +------------------+
                             | RealTaxService   |
                             | (Implementation) |
                             +------------------+

Functional Parameter:
  +-----------------------------------------------+
  | calculateTotal(items, taxRate)                |
  |   taxRate is just a VALUE, not a dependency   |
  +-----------------------------------------------+

  Shell: TaxRate rate = taxService.getCurrentRate();
         Money total = calculateTotal(items, rate);
```

### 개념이 아닌 것
- **"DI를 사용하면 안 된다"**: Imperative Shell에서는 DI가 여전히 유용하다. Repository, Gateway 등의 인프라 의존성은 DI로 주입받는 것이 적합하다.
- **"모든 의존성을 파라미터로 넘겨야 한다"**: 파라미터가 5개 이상이면 오히려 가독성이 떨어진다. 이 경우 Parameter Object(record)를 사용하라.

### Before: Traditional OOP

**[코드 07.3]** Traditional OOP: 비즈니스 로직에도 DI 사용 - 테스트 시 Mock 필수
```java
 1| // package: com.ecommerce.shared
 2| // [X] 비즈니스 로직에도 DI 사용 - 테스트 시 Mock 필수
 3| @RequiredArgsConstructor
 4| public class PriceService {
 5|   private final TaxService taxService;
 6|   private final ExchangeRateService exchangeRateService;
 7|   private final DiscountPolicyService discountPolicyService;
 8|
 9|   public Money calculateFinalPrice(Order order) {
10|     TaxRate taxRate = taxService.getCurrentRate();
11|     ExchangeRate rate = exchangeRateService.getRate(Currency.USD, Currency.KRW);
12|     DiscountPolicy policy = discountPolicyService.getActivePolicy();
13|
14|     Money base = order.subtotal();
15|     Money taxed = base.applyTax(taxRate);
16|     Money converted = taxed.convert(rate);
17|     return converted.applyDiscount(policy);
18|   }
19|   // 테스트: 3개의 Mock 설정 필요
20| }
```
- **의도 및 코드 설명**: 세율, 환율, 할인 정책을 모두 서비스 인터페이스를 통해 호출한다.
- **뭐가 문제인가**:
  - 3개의 Mock 설정: 단순한 가격 계산 테스트에도 복잡한 Mock 설정 필요
  - 숨겨진 의존성: 메서드 시그니처만 보면 order만 필요한 것처럼 보임
  - 비결정론적: 외부 서비스 상태에 따라 결과 변동
  - 재사용 불가: 다른 컨텍스트에서 사용하려면 같은 서비스 인프라 필요

### After: Modern Approach

**[코드 07.4]** Modern: 데이터 의존성은 파라미터로, 기능 의존성은 Shell에서
```java
 1| // package: com.ecommerce.shared
 2| // [O] 데이터 의존성은 파라미터로, 기능 의존성은 Shell에서
 3| public class PriceCalculations {
 4|   // 순수 함수: 모든 의존성이 값(데이터)으로 전달됨
 5|   public static Money calculateFinalPrice(
 6|     Money subtotal,
 7|     TaxRate taxRate,
 8|     ExchangeRate exchangeRate,
 9|     Optional<DiscountPolicy> discountPolicy
10|   ) {
11|     Money taxed = subtotal.applyTax(taxRate);
12|     Money converted = taxed.convert(exchangeRate);
13|     return discountPolicy
14|       .map(policy -> converted.applyDiscount(policy))
15|       .orElse(converted);
16|   }
17| }
18|
19| // Shell에서 DI 사용하여 데이터 수집 후 Core 호출
20| @RequiredArgsConstructor
21| public class CalculatePriceUseCase {
22|   private final TaxService taxService;
23|   private final ExchangeRateService rateService;
24|
25|   public Money execute(Order order, Currency targetCurrency) {
26|     // Shell: 데이터 수집
27|     TaxRate taxRate = taxService.getCurrentRate();
28|     ExchangeRate rate = rateService.getRate(order.currency(), targetCurrency);
29|
30|     // Core: 순수 계산 (Mock 없이 테스트 가능)
31|     return PriceCalculations.calculateFinalPrice(
32|       order.subtotal(), taxRate, rate, order.discountPolicy()
33|     );
34|   }
35| }
36|
37| // 테스트: Mock 없이 직접 값 전달
38| @Test
39| void calculateFinalPrice_appliesTaxAndExchangeRate() {
40|   Money result = PriceCalculations.calculateFinalPrice(
41|     Money.usd(100),
42|     new TaxRate(10),               // 10% 세금
43|     new ExchangeRate(1300),         // 1 USD = 1300 KRW
44|     Optional.empty()
45|   );
46|   assertEquals(Money.krw(143000), result);  // 100 * 1.1 * 1300
47| }
```
- **의도 및 코드 설명**: `PriceCalculations`는 모든 의존성을 값으로 받는 순수 함수다. Shell인 `CalculatePriceUseCase`에서 DI로 서비스를 받아 데이터를 수집한 후, 순수 함수에 전달한다.
- **무엇이 좋아지나**:
  - Mock 없는 테스트: 값만 넣으면 결과 검증
  - 명시적 의존성: 메서드 시그니처가 모든 입력을 노출
  - 결정론적: 같은 입력이면 항상 같은 출력
  - 재사용: 어떤 컨텍스트에서도 값만 준비하면 사용 가능

### 이해를 위한 부가 상세
- DMMF에서는 Functional Interface를 파라미터로 전달하는 Higher-Order Function 패턴도 제시한다:

**[코드 07.5]** Dependency Injection vs Function Parameters (의존성 주입 vs 함수 파라미터)
```java
1| // package: com.ecommerce.shared
2|  public Money convertPrice(Money price, Currency to, GetExchangeRate getRate) {
3|    BigDecimal rate = getRate.get(price.currency(), to);
4|    return new Money(price.amount().multiply(rate), to);
5|  }
```
  이 방식은 함수 자체가 의존성이 되어, Currying/Partial Application으로 미리 주입할 수 있다.

### 틀리기/놓치기 쉬운 부분
- 파라미터가 5-6개 이상이면 Parameter Object로 묶어라: `PriceContext(taxRate, exchangeRate, discountPolicy)`
- Core 함수에 Repository를 전달하면 안 된다. Repository는 I/O이므로 Shell에서만 사용한다.

### 꼭 기억할 것
- **Core에서는 "값" 의존성만 파라미터로 받는다** (TaxRate, ExchangeRate 등의 데이터).
- **Shell에서는 전통적 DI를 사용한다** (Repository, Gateway 등의 기능 의존성).
- 이 분리로 "테스트에 Mock이 필요한 코드"가 Shell에만 남게 된다.

---

## 3. Onion Architecture (DMMF) vs Sandwich Architecture (DOP)

### 핵심 개념
- **관련 키워드**: Onion Architecture, Sandwich Architecture, Hexagonal, Clean Architecture, dependency direction, layer
- **통찰**: DMMF의 Onion Architecture와 DOP의 Sandwich Architecture는 같은 원칙(도메인 순수성)을 다른 비유로 표현한 것이며, 각각의 강점이 있다.
- **설명**:

Onion Architecture(DMMF)는 동심원 구조로 아키텍처를 표현한다. 가장 안쪽에 Domain Model(순수 비즈니스 규칙), 중간에 Application Services(유스케이스 조율), 바깥에 Infrastructure(DB, 외부 API)를 배치한다. 핵심 규칙은 "**의존성은 항상 안쪽으로 향한다**". 도메인은 인프라를 모르고, 인프라가 도메인에 의존한다.

Sandwich Architecture(DOP)는 시간 순서로 코드를 배치한다. Top Bun(I/O: 데이터 수집) -> Meat(Pure: 비즈니스 계산) -> Bottom Bun(I/O: 결과 저장). 하나의 UseCase 메서드 내에서 이 구조를 따른다.

두 패턴의 **공통 원칙**은 "비즈니스 로직을 순수하게 유지하고, 부수효과를 경계로 밀어낸다"이다. Onion은 **전체 시스템 구조**를, Sandwich는 **개별 UseCase의 코드 배치**를 가이드한다.

**[그림 07.3]** Onion Architecture (DMMF) vs Sandwich Architecture (DOP)
```
=== DMMF: Onion Architecture (System Structure) ===

         +------------------------------------------+
         |           INFRASTRUCTURE                  |
         |  Controllers, JPA Repos, External APIs    |
         |                                           |
         |   +----------------------------------+    |
         |   |      APPLICATION SERVICES        |    |
         |   |   UseCases, Repo Interfaces      |    |
         |   |                                  |    |
         |   |   +------------------------+    |    |
         |   |   |     DOMAIN MODEL       |    |    |
         |   |   |  Pure Business Rules   |    |    |
         |   |   |  (No Dependencies)     |    |    |
         |   |   +------------------------+    |    |
         |   |                                  |    |
         |   +----------------------------------+    |
         |                                           |
         +------------------------------------------+
              Deps: Outside --> Inside (always)


=== DOP: Sandwich Architecture (UseCase Code Layout) ===

         +------------------------------------------+
         |  TOP BUN (Impure: Read/Collect)          |
         |  - repository.find(id)                   |
         |  - service.getRate()                     |
         +------------------------------------------+
         |  MEAT (Pure: Calculate/Decide)           |
         |  - Calculations.compute(data, rate)      |
         |  - DomainService.validate(order)         |
         +------------------------------------------+
         |  BOTTOM BUN (Impure: Write/Notify)       |
         |  - repository.save(order)                |
         |  - notification.send(event)              |
         +------------------------------------------+
```

### 개념이 아닌 것
- **"둘 중 하나만 선택해야 한다"**: Onion은 패키지/모듈 구조, Sandwich는 메서드 내부 코드 배치에 관한 것이다. 함께 사용할 수 있다.
- **"Sandwich는 Onion보다 단순하니까 더 좋다"**: 대규모 시스템에서는 Onion의 계층 분리가 팀 간 경계와 의존성 관리에 더 적합하다.

### Before: Traditional OOP

**[코드 07.6]** Traditional OOP: 계층 없이 모든 것이 뒤섞인 서비스
```java
 1| // package: com.ecommerce.order
 2| // [X] 계층 없이 모든 것이 뒤섞인 서비스
 3| @RequiredArgsConstructor
 4| public class OrderService {
 5|   private final JpaOrderRepository jpaRepository;  // 인프라 직접 참조
 6|   private final PaymentClient paymentClient;       // 외부 API 직접 참조
 7|
 8|   public OrderEntity placeOrder(OrderDto dto) {
 9|     // 도메인 로직이 인프라 타입(Entity, DTO)에 직접 의존
10|     OrderEntity entity = new OrderEntity();
11|     entity.setCustomerId(dto.getCustomerId());
12|     entity.setAmount(dto.getAmount());
13|
14|     // 비즈니스 규칙이 인프라 호출과 섞임
15|     if (entity.getAmount().compareTo(BigDecimal.valueOf(50000)) > 0) {
16|       PaymentResponse resp = paymentClient.authorize(entity.getAmount());
17|       entity.setPaymentId(resp.getTxId());
18|     }
19|
20|     return jpaRepository.save(entity);
21|   }
22| }
```
- **의도 및 코드 설명**: 하나의 서비스가 JPA Entity, 외부 API Client, 비즈니스 규칙을 모두 직접 사용한다.
- **뭐가 문제인가**:
  - 의존성 방향 위반: 비즈니스 로직이 JPA Entity에 의존
  - 도메인 오염: OrderEntity가 비즈니스 규칙과 인프라 구조를 모두 담당
  - 교체 불가: JPA를 다른 저장소로 바꾸면 비즈니스 로직도 수정 필요
  - 테스트 불가: JPA와 PaymentClient를 Mock해야 비즈니스 규칙 테스트 가능

### After: DMMF 관점 (DDD + FP) - Onion Architecture

**[코드 07.7]** DMMF: Domain Layer: 순수 비즈니스 규칙 (가장 안쪽)
```java
 1| // package: com.ecommerce.order
 2| // [O] Domain Layer: 순수 비즈니스 규칙 (가장 안쪽)
 3| public record Order(
 4|   OrderId id, CustomerId customerId, List<OrderLine> lines,
 5|   Money totalAmount, OrderStatus status
 6| ) {
 7|   public Result<Order, OrderError> cancel(CancelReason reason) {
 8|     return switch (status) {
 9|       case Unpaid u, Paid p -> Result.success(
10|         new Order(id, customerId, lines, totalAmount,
11|           new Cancelled(LocalDateTime.now(), reason)));
12|       case Shipping s -> Result.failure(new OrderError.InvalidState("배송 중 취소 불가"));
13|       case Delivered d -> Result.failure(new OrderError.InvalidState("배송 완료 취소 불가"));
14|       case Cancelled c -> Result.failure(new OrderError.InvalidState("이미 취소됨"));
15|     };
16|   }
17| }
18| 
19| // Domain Layer: Repository 인터페이스 (도메인이 정의, 인프라가 구현)
20| public interface OrderRepository {
21|   Optional<Order> findById(OrderId id);
22|   Order save(Order order);
23| }
24| 
25| // Application Layer: UseCase (중간층)
26| public class PlaceOrderUseCase {
27|   private final OrderRepository orderRepository;      // 인터페이스만 의존
28|   private final PaymentGateway paymentGateway;        // 인터페이스만 의존
29|   private final OrderDomainService domainService;     // 순수 로직
30| 
31|   @Transactional
32|   public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand cmd) {
33|     ValidatedOrder validated = domainService.validate(cmd);
34|     PricedOrder priced = domainService.calculatePrice(validated);
35|     var payment = paymentGateway.charge(priced.totalAmount());
36|     Order order = domainService.createOrder(priced, payment.value().txId());
37|     orderRepository.save(order);
38|     return Result.success(new OrderPlaced(order.id()));
39|   }
40| }
41| 
42| // Infrastructure Layer: Repository 구현 (바깥층)
43| public class JpaOrderRepository implements OrderRepository {
44|   private final OrderJpaRepository jpaRepo;
45|   private final OrderMapper mapper;
46| 
47|   @Override
48|   public Optional<Order> findById(OrderId id) {
49|     return jpaRepo.findById(id.value()).map(mapper::toDomain);
50|   }
51| 
52|   @Override
53|   public Order save(Order order) {
54|     OrderEntity entity = mapper.toEntity(order);
55|     return mapper.toDomain(jpaRepo.save(entity));
56|   }
57| }
```

### After: DOP 관점 - Sandwich Architecture

**[코드 07.8]** DOP: Calculations 클래스: 순수 함수 (Meat)
```java
 1| // package: com.ecommerce.order
 2| // [O] Calculations 클래스: 순수 함수 (Meat)
 3| public class OrderCalculations {
 4|   public static PricedOrder calculatePrice(ValidatedOrder order, TaxRate taxRate) {
 5|     Money subtotal = order.lines().stream()
 6|       .map(line -> line.unitPrice().multiply(line.quantity()))
 7|       .reduce(Money.zero(), Money::add);
 8|     Money total = subtotal.applyTax(taxRate);
 9|     return new PricedOrder(order.customerId(), order.lines(), subtotal, total);
10|   }
11| 
12|   public static boolean requiresPaymentAuthorization(Money amount) {
13|     return amount.isGreaterThan(Money.krw(50000));
14|   }
15| }
16| 
17| // UseCase: Sandwich 구조 (Top Bun -> Meat -> Bottom Bun)
18| public class PlaceOrderUseCase {
19|   private final OrderRepository orderRepository;
20|   private final PaymentGateway paymentGateway;
21|   private final TaxService taxService;
22| 
23|   public Result<Order, OrderError> execute(PlaceOrderCommand cmd) {
24|     // === Top Bun: I/O (데이터 수집) ===
25|     var validated = validateOrder(cmd);
26|     if (validated.isFailure()) return validated.flatMap(v -> null);
27|     TaxRate taxRate = taxService.getCurrentRate();
28| 
29|     // === Meat: Pure Logic (비즈니스 계산) ===
30|     PricedOrder priced = OrderCalculations.calculatePrice(validated.value(), taxRate);
31|     boolean needsAuth = OrderCalculations.requiresPaymentAuthorization(priced.totalAmount());
32| 
33|     // === Bottom Bun: I/O (부수효과) ===
34|     if (needsAuth) {
35|       var payResult = paymentGateway.authorize(priced.totalAmount());
36|       if (payResult.isFailure()) {
37|         return Result.failure(new OrderError.PaymentFailed(payResult.error().message()));
38|       }
39|     }
40|     Order order = new Order(OrderId.generate(), priced);
41|     return Result.success(orderRepository.save(order));
42|   }
43| }
```

### 관점 차이 분석
- **공통점**: 두 접근 모두 "비즈니스 로직을 순수하게 유지하고, I/O를 경계로 밀어낸다"는 원칙을 공유한다. 도메인 모델은 JPA, HTTP, 외부 API를 모른다.
- **DMMF 주장**: Onion Architecture는 **패키지/모듈 수준의 의존성 방향**을 규정한다. Repository Interface는 도메인에 정의하고 인프라가 구현한다(DIP). 대규모 팀에서 Bounded Context 간 경계를 명확히 할 때 효과적이다.
- **DOP 주장**: Sandwich Architecture는 **개별 메서드 수준의 코드 배치**를 규정한다. I/O -> Pure -> I/O 순서로 코드를 배치하여 순수 영역을 명확히 한다. 단일 UseCase의 가독성과 테스트 용이성에 초점을 맞춘다.
- **실무 권장**: 두 패턴을 함께 사용하라. 시스템 전체 구조는 Onion(패키지 의존성), 개별 UseCase 내부는 Sandwich(코드 배치)로 설계한다.

### 틀리기/놓치기 쉬운 부분
- Onion Architecture에서 Repository Interface를 Application Layer에 두는 실수가 많다. **Domain Layer**에 정의해야 DIP가 성립한다.
- Sandwich 구조에서 Meat 영역 중간에 I/O가 불가피할 때(예: "계산 결과에 따라 추가 데이터 조회"), 함수를 더 작게 분리하거나 필요한 데이터를 미리 수집하라.

### 꼭 기억할 것
- **Onion = 시스템 구조** (패키지/모듈 레벨, 의존성 방향)
- **Sandwich = 코드 배치** (메서드 레벨, 실행 순서)
- 두 패턴은 상호 보완적이며 함께 적용할 수 있다.

---

## 4. Domain Layer Purity (도메인 레이어 순수성)

### 핵심 개념
- **관련 키워드**: Persistence Ignorance, Trust Boundary, DTO, Anti-Corruption Layer, domain purity
- **통찰**: 도메인 모델은 DB 저장 방식, 외부 API 형식, 프레임워크 어노테이션을 전혀 모르는 순수한 비즈니스 규칙만 표현해야 한다.
- **설명**:

Persistence Ignorance는 도메인 모델이 저장소의 구체적 기술(JPA, MongoDB, File 등)을 모르는 것을 의미한다. JPA `@Entity` 어노테이션이 도메인 record에 붙으면 도메인이 인프라에 의존하게 되어, DB 스키마 변경이 비즈니스 로직 변경으로 이어진다.

Trust Boundary는 외부(HTTP 요청, 외부 API 응답)와 내부(도메인 모델) 사이의 경계이다. 데이터가 이 경계를 넘을 때마다 **검증과 변환**이 필요하다. 외부의 String, Map 등 raw 데이터는 DTO로 받고, 도메인 Value Object로 변환한다. 반대로 도메인 모델을 외부에 노출할 때도 DTO로 변환한다.

Anti-Corruption Layer(ACL)는 외부 시스템의 모델이 도메인 모델을 "오염"시키지 않도록 번역하는 계층이다. 외부 결제 API의 응답 형식이 바뀌어도 ACL(Adapter)만 수정하면 도메인은 영향받지 않는다.

**[그림 07.4]** Domain Layer Purity (도메인 레이어 순수성)
```
=== Domain Layer Purity ===

  EXTERNAL WORLD          TRUST BOUNDARY          DOMAIN
  (Untrusted)             (Validation +           (Pure, Immutable)
                           Conversion)

  +-------------+    +-------------------+    +------------------+
  | HTTP JSON   |--->| Controller        |--->| Domain Record    |
  | (raw String)|    | DTO -> Domain     |    | (Value Objects)  |
  +-------------+    +-------------------+    +------------------+

  +-------------+    +-------------------+    +------------------+
  | JPA Entity  |<---| Repository Impl   |<---| Domain Record    |
  | (@Entity)   |    | Domain -> Entity  |    | (No JPA annot.)  |
  +-------------+    +-------------------+    +------------------+

  +-------------+    +-------------------+    +------------------+
  | External API|<---| ACL (Adapter)     |<---| Domain Types     |
  | Response    |    | External -> Domain|    | (PaymentApproval)|
  +-------------+    +-------------------+    +------------------+
```

### 개념이 아닌 것
- **"DTO를 만들면 코드량만 늘어난다"**: DTO의 가치는 보안(민감 정보 미노출), 안정성(도메인 변경이 API에 영향 안 줌), 유연성(API 응답에 추가 정보 포함 가능)이다.
- **"도메인 Record에 검증 로직을 넣으면 안 된다"**: Compact Constructor에서 자기 자신의 불변식(invariant)을 검증하는 것은 도메인의 책임이다.

### Before: Traditional OOP

**[코드 07.9]** Traditional OOP: 도메인 모델에 인프라 관심사가 섞임
```java
 1| // package: com.ecommerce.order
 2| // [X] 도메인 모델에 인프라 관심사가 섞임
 3| @Entity
 4| @Table(name = "orders")
 5| public class Order {
 6|   @Id @GeneratedValue
 7|   private Long id;
 8| 
 9|   @Column(name = "customer_id")
10|   private Long customerId;
11| 
12|   @Column(name = "total_amount")
13|   private BigDecimal totalAmount;
14| 
15|   @Enumerated(EnumType.STRING)
16|   private OrderStatusEnum status;
17| 
18|   // JPA 요구사항: 기본 생성자
19|   protected Order() {}
20| 
21|   // Setter로 상태 변경 가능 (불변성 위반)
22|   public void setStatus(OrderStatusEnum status) {
23|     this.status = status;
24|   }
25| 
26|   // 비즈니스 로직이 Entity에 섞임
27|   public void cancel() {
28|     if (this.status == OrderStatusEnum.SHIPPED) {
29|       throw new IllegalStateException("배송 중 취소 불가");
30|     }
31|     this.status = OrderStatusEnum.CANCELLED;
32|   }
33| }
34| 
35| // API에서 Entity 직접 반환 - 보안 위험
36| @GetMapping("/orders/{id}")
37| public Order getOrder(@PathVariable Long id) {
38|   return orderRepository.findById(id).orElseThrow();
39| }
```
- **의도 및 코드 설명**: JPA Entity가 도메인 모델 역할까지 담당하며, API 응답으로 직접 반환된다.
- **뭐가 문제인가**:
  - 인프라 의존: @Entity, @Column 등 JPA 어노테이션이 도메인을 오염
  - 가변성: setter로 불변식 위반 가능
  - 보안 위험: 내부 필드(id, 관리자 필드 등)가 API 응답에 노출
  - 변경 전파: DB 스키마 변경 = 도메인 로직 변경 = API 응답 변경

### After: Modern Approach

**[코드 07.10]** Modern: 도메인 Record: 인프라를 전혀 모름 (Persistence Ignorance)
```java
 1| // package: com.ecommerce.order
 2| // [O] 도메인 Record: 인프라를 전혀 모름 (Persistence Ignorance)
 3| public record Order(
 4|   OrderId id,
 5|   CustomerId customerId,
 6|   List<OrderLine> lines,
 7|   Money totalAmount,
 8|   OrderStatus status
 9| ) {
10|   // 순수한 비즈니스 로직만
11|   public Result<Order, OrderError> cancel(CancelReason reason) {
12|     return switch (status) {
13|       case Unpaid u, Paid p -> Result.success(
14|         new Order(id, customerId, lines, totalAmount,
15|           new Cancelled(LocalDateTime.now(), reason)));
16|       case Shipping s -> Result.failure(new OrderError.InvalidState("배송 중 취소 불가"));
17|       case Delivered d, Cancelled c -> Result.failure(
18|         new OrderError.InvalidState("취소 불가 상태"));
19|     };
20|   }
21| }
22| 
23| // Mapper: 도메인 <-> 인프라 변환 (ACL 역할)
24| public class OrderMapper {
25|   public static Order toDomain(OrderEntity entity) {
26|     OrderStatus status = switch (entity.getStatus()) {
27|       case PENDING -> new OrderStatus.Unpaid();
28|       case PAID -> new OrderStatus.Paid(entity.getPaidAt(), new PaymentId(entity.getPaymentId()));
29|       case SHIPPED -> new OrderStatus.Shipping(new TrackingNumber(entity.getTrackingNumber()));
30|       case DELIVERED -> new OrderStatus.Delivered(entity.getDeliveredAt());
31|       case CANCELLED -> new OrderStatus.Cancelled(entity.getCancelledAt(),
32|         CancelReason.valueOf(entity.getCancelReason()));
33|     };
34|     return new Order(
35|       new OrderId(entity.getId().toString()),
36|       new CustomerId(entity.getCustomerId()),
37|       mapLines(entity.getItems()),
38|       Money.krw(entity.getTotalAmount().longValue()),
39|       status
40|     );
41|   }
42| 
43|   public static OrderEntity toEntity(Order order) {
44|     // Domain -> Entity 변환 (역방향)
45|     OrderEntity entity = new OrderEntity();
46|     entity.setId(Long.parseLong(order.id().value()));
47|     entity.setCustomerId(order.customerId().value());
48|     entity.setTotalAmount(BigDecimal.valueOf(order.totalAmount().amount()));
49|     entity.setStatus(toEntityStatus(order.status()));
50|     return entity;
51|   }
52| }
53| 
54| // Controller: DTO로 변환하여 응답
55| @GetMapping("/orders/{id}")
56| public Result<OrderDto, String> getOrder(@PathVariable String id) {
57|   return orderRepository.findById(new OrderId(id))
58|     .map(OrderDto::from)
59|     .map(Result::success)
60|     .orElse(Result.failure("주문을 찾을 수 없습니다"));
61| }
```
- **의도 및 코드 설명**: Domain Record는 JPA 어노테이션이 없으며, Mapper가 Entity <-> Domain 변환을 담당한다. API 응답은 DTO로 변환된다.
- **무엇이 좋아지나**:
  - 도메인 순수: 인프라 변경이 도메인에 영향 없음
  - 불변 보장: record는 setter 없음, 상태 변경은 새 인스턴스 생성
  - 보안: DTO로 필요한 정보만 노출
  - 유연성: DB를 MongoDB로 바꿔도 도메인 코드 변경 없음

### 이해를 위한 부가 상세
- Controller에서 DTO -> Domain 변환을 수행하면 Service 메서드의 시그니처가 "이미 검증된 도메인 객체"만 받게 되어 신뢰할 수 있다.
- Anti-Corruption Layer는 외부 결제 API 등에도 적용된다. 외부 응답의 `result_code: "0000"`을 도메인의 `PaymentApproval` 타입으로 변환한다.

### 틀리기/놓치기 쉬운 부분
- Domain Record에서 다른 Aggregate를 객체 참조하면 안 된다. ID 참조(`CustomerId`)를 사용하여 순환 참조와 Lazy Loading 문제를 방지한다.
- Mapper 코드가 지루할 수 있지만, MapStruct 같은 도구로 자동 생성하거나, 보안 사고 1건의 비용이 Mapper 코드 작성보다 훨씬 크다는 점을 기억하라.

### 꼭 기억할 것
- 도메인 모델에는 **@Entity, @Column, @JsonProperty 등 어떤 인프라 어노테이션도 없어야** 한다.
- 경계를 넘는 데이터는 항상 **Mapper(통역사)**를 통해 변환한다.
- Domain -> Entity, Domain -> DTO, External -> Domain 각각 별도의 Mapper가 필요하다.

---

## 5. JPA/Spring Coexistence Strategies (JPA/Spring 공존 전략)

### 핵심 개념
- **관련 키워드**: JPA Entity, Domain Record, Mapper, gradual refactoring, legacy coexistence, infrastructure separation
- **통찰**: JPA Entity(가변, Identity 기반)와 Domain Record(불변, Value 기반)는 공존할 수 있으며, Mapper가 두 세계를 연결한다.
- **설명**:

현실의 Spring Boot 프로젝트에서는 JPA를 당장 제거할 수 없다. JPA Entity는 Hibernate가 상태를 추적해야 하므로 가변(Mutable)이며, `@Id`로 정체성(Identity)을 가진다. 반면 DOP의 Domain Record는 불변(Immutable)이며, 값(Value)으로 비교된다.

공존 전략의 핵심은 **도메인 레이어에서는 Record만 사용하고, 인프라 레이어에서만 Entity를 다루는 것**이다. Repository 구현체(Infrastructure)가 Entity <-> Record 변환을 담당하며, 도메인 로직은 Entity의 존재를 모른다.

점진적 리팩토링 전략은: (1) 가장 복잡한 비즈니스 로직 식별, (2) 해당 로직을 순수 함수(Calculations)로 추출, (3) Mock 없이 테스트 작성, (4) UseCase에서 순수 함수 호출로 교체. 이렇게 안에서부터 바깥으로 점진적으로 DOP를 도입한다.

**[그림 07.5]** JPA/Spring Coexistence Strategies (JPA/Spring 공존 전략)
```
=== JPA/Spring Coexistence Architecture ===

  +------------------+     +-----------------+     +------------------+
  | Controller       |     | UseCase         |     | Domain           |
  | (DTO in/out)     |---->| (Orchestration) |---->| (Pure Records)   |
  +------------------+     +-----------------+     +------------------+
                                    |                       ^
                                    v                       |
                            +-----------------+    +------------------+
                            | Repository Impl |    | Mapper           |
                            | (JPA Entity)    |--->| Entity <-> Record|
                            +-----------------+    +------------------+
                                    |
                                    v
                            +-----------------+
                            | Database        |
                            | (JPA/Hibernate) |
                            +-----------------+

  Domain Record: Immutable, Value-based, No JPA annotations
  JPA Entity:    Mutable, Identity-based, @Entity @Id @Column
  Mapper:        Translates between the two worlds
```

### 개념이 아닌 것
- **"JPA를 사용하면 DOP를 적용할 수 없다"**: JPA는 인프라 레이어에서만 사용하면 도메인의 순수성을 유지할 수 있다.
- **"한 번에 모든 Entity를 Record로 바꿔야 한다"**: 점진적 리팩토링으로, 가장 복잡한 로직부터 순수 함수로 추출하면 된다.

### Before: Traditional OOP

**[코드 07.11]** Traditional OOP: Entity에 비즈니스 로직이 섞인 안티패턴
```java
 1| // package: com.ecommerce.order
 2| // [X] Entity에 비즈니스 로직이 섞인 안티패턴
 3| @Entity
 4| public class OrderEntity {
 5|   @Id @GeneratedValue private Long id;
 6|   private Long customerId;
 7|   private BigDecimal totalAmount;
 8|   @Enumerated(EnumType.STRING) private OrderStatusEnum status;
 9|   @OneToMany(cascade = CascadeType.ALL)
10|   private List<OrderItemEntity> items = new ArrayList<>();
11| 
12|   // 비즈니스 로직이 Entity에 있음
13|   public void addItem(OrderItemEntity item) {
14|     items.add(item);
15|     recalculateTotal();
16|   }
17| 
18|   public void cancel() {
19|     if (status == OrderStatusEnum.SHIPPED) {
20|       throw new IllegalStateException("배송 중 취소 불가");
21|     }
22|     this.status = OrderStatusEnum.CANCELLED;
23|   }
24| 
25|   private void recalculateTotal() {
26|     this.totalAmount = items.stream()
27|       .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
28|       .reduce(BigDecimal.ZERO, BigDecimal::add);
29|   }
30| }
31| 
32| // Service가 Entity를 직접 조작
33| public class OrderService {
34|   public OrderEntity createOrder(CreateOrderRequest request) {
35|     OrderEntity entity = new OrderEntity();
36|     entity.setCustomerId(request.customerId());
37|     entity.setStatus(OrderStatusEnum.PENDING);
38|     for (var item : request.items()) {
39|       entity.addItem(new OrderItemEntity(item.productId(), item.quantity(), item.price()));
40|     }
41|     return orderRepository.save(entity);
42|   }
43| }
```
- **의도 및 코드 설명**: JPA Entity가 비즈니스 로직(addItem, cancel, recalculateTotal)을 직접 담당한다.
- **뭐가 문제인가**:
  - 도메인 오염: 비즈니스 규칙이 @Entity 클래스에 갇힘
  - 테스트 어려움: Entity 테스트에 JPA 컨텍스트 필요
  - 가변 상태: setter와 리스트 조작으로 상태 추적 어려움
  - Lazy Loading 함정: 트랜잭션 밖에서 items 접근 시 예외

### After: Modern Approach

**[코드 07.12]** Modern: Domain Record: 순수 비즈니스 로직
```java
  1| // package: com.ecommerce.order
  2| // [O] Domain Record: 순수 비즈니스 로직
  3| public record Order(
  4|   OrderId id,
  5|   CustomerId customerId,
  6|   List<OrderItem> items,
  7|   Money totalAmount,
  8|   OrderStatus status
  9| ) {}
 10| 
 11| // 순수 함수: 비즈니스 로직을 static 함수로 추출
 12| public class OrderCalculations {
 13|   public static Money calculateTotal(List<OrderItem> items) {
 14|     return items.stream()
 15|       .map(item -> item.unitPrice().multiply(item.quantity()))
 16|       .reduce(Money.zero(), Money::add);
 17|   }
 18| 
 19|   public static Order addItem(Order order, OrderItem newItem) {
 20|     List<OrderItem> newItems = new ArrayList<>(order.items());
 21|     newItems.add(newItem);
 22|     Money newTotal = calculateTotal(newItems);
 23|     return new Order(order.id(), order.customerId(), newItems, newTotal, order.status());
 24|   }
 25| 
 26|   public static Result<Order, OrderError> cancel(Order order, CancelReason reason) {
 27|     return switch (order.status()) {
 28|       case Unpaid u, Paid p -> Result.success(
 29|         new Order(order.id(), order.customerId(), order.items(),
 30|           order.totalAmount(), new Cancelled(LocalDateTime.now(), reason)));
 31|       case Shipping s -> Result.failure(new OrderError.CannotCancel("배송 중"));
 32|       case Delivered d, Cancelled c -> Result.failure(
 33|         new OrderError.CannotCancel("취소 불가 상태"));
 34|     };
 35|   }
 36| }
 37| 
 38| // JPA Entity: 인프라 전용 (비즈니스 로직 없음)
 39| @Entity @Table(name = "orders")
 40| public class OrderEntity {
 41|   @Id @GeneratedValue private Long id;
 42|   private Long customerId;
 43|   private BigDecimal totalAmount;
 44|   @Enumerated(EnumType.STRING) private OrderStatusEnum status;
 45|   private LocalDateTime createdAt;
 46|   private LocalDateTime paidAt;
 47|   private String paymentId;
 48|   // ... JPA 전용 필드들
 49|   // 비즈니스 로직 없음 - getter/setter만
 50| }
 51| 
 52| // Mapper: 두 세계의 통역사
 53| public class OrderMapper {
 54|   public static Order toDomain(OrderEntity entity) {
 55|     OrderStatus status = switch (entity.getStatus()) {
 56|       case PENDING -> new OrderStatus.Unpaid();
 57|       case PAID -> new OrderStatus.Paid(entity.getPaidAt(), new PaymentId(entity.getPaymentId()));
 58|       case SHIPPED -> new OrderStatus.Shipping(new TrackingNumber(entity.getTrackingNumber()));
 59|       case DELIVERED -> new OrderStatus.Delivered(entity.getDeliveredAt());
 60|       case CANCELLED -> new OrderStatus.Cancelled(
 61|         entity.getCancelledAt(), CancelReason.valueOf(entity.getCancelReason()));
 62|     };
 63|     return new Order(
 64|       new OrderId(entity.getId().toString()),
 65|       new CustomerId(entity.getCustomerId()),
 66|       mapItems(entity.getItems()),
 67|       Money.krw(entity.getTotalAmount().longValue()),
 68|       status
 69|     );
 70|   }
 71| 
 72|   public static OrderEntity toEntity(Order order) {
 73|     OrderEntity entity = new OrderEntity();
 74|     entity.setCustomerId(order.customerId().value());
 75|     entity.setTotalAmount(BigDecimal.valueOf(order.totalAmount().amount()));
 76|     entity.setStatus(toEntityStatus(order.status()));
 77|     return entity;
 78|   }
 79| }
 80| 
 81| // Repository 구현: Mapper를 통해 변환
 82| public class JpaOrderRepository implements OrderRepository {
 83|   private final OrderJpaRepository jpaRepo;
 84| 
 85|   @Override
 86|   public Optional<Order> findById(OrderId id) {
 87|     return jpaRepo.findById(Long.parseLong(id.value()))
 88|       .map(OrderMapper::toDomain);
 89|   }
 90| 
 91|   @Override
 92|   public Order save(Order order) {
 93|     OrderEntity entity = OrderMapper.toEntity(order);
 94|     OrderEntity saved = jpaRepo.save(entity);
 95|     return OrderMapper.toDomain(saved);
 96|   }
 97| }
 98| 
 99| // 테스트: Mock 없이 순수 함수 테스트
100| @Test
101| void calculateTotal_multipleItems() {
102|   List<OrderItem> items = List.of(
103|     new OrderItem(productId1, Quantity.of(2), Money.krw(10000)),
104|     new OrderItem(productId2, Quantity.of(1), Money.krw(5000))
105|   );
106|   assertEquals(Money.krw(25000), OrderCalculations.calculateTotal(items));
107| }
108| 
109| @Test
110| void cancel_shippingOrder_returnsError() {
111|   Order order = new Order(orderId, customerId, items, total, new Shipping(tracking));
112|   var result = OrderCalculations.cancel(order, CancelReason.USER_REQUEST);
113|   assertTrue(result.isFailure());
114| }
```
- **의도 및 코드 설명**: Entity는 JPA 전용(getter/setter만), 비즈니스 로직은 `OrderCalculations`(순수 함수), Mapper가 두 세계를 연결한다.
- **무엇이 좋아지나**:
  - 도메인 순수성: JPA 변경이 비즈니스 로직에 영향 없음
  - Mock 없는 테스트: Calculations 메서드를 값만 넣어 검증
  - 불변성: Domain Record는 상태 변경 시 새 인스턴스 생성
  - 점진적 도입: 기존 Entity는 그대로 두고 Calculations만 추출하여 시작

### 이해를 위한 부가 상세
- 점진적 리팩토링 순서: (1) 복잡한 로직 식별 -> (2) Calculations 클래스에 순수 함수 추출 -> (3) 테스트 작성 -> (4) UseCase에서 Calculations 호출 -> (5) Entity에서 해당 로직 제거.
- Domain Record에서 다른 Aggregate를 참조할 때는 ID만 사용한다: `CustomerId customerId` (Customer 객체가 아님). 이렇게 하면 순환 참조와 Lazy Loading 문제를 방지한다.

### 틀리기/놓치기 쉬운 부분
- Entity의 Setter를 한 번에 다 제거하지 마라. 새 기능부터 Record를 사용하고, 기존 코드는 점진적으로 마이그레이션한다.
- Mapper에서 Lazy Loading 예외가 발생할 수 있다. Repository 구현에서 `@Transactional(readOnly = true)`와 함께 필요한 연관관계를 Eager로 조회하거나 Fetch Join을 사용한다.
- 트랜잭션 경계는 Application Layer(UseCase)에서 관리한다. Domain Layer는 트랜잭션을 모른다.

### 꼭 기억할 것
- **Entity = 인프라 전용** (JPA가 요구하는 가변 객체, 비즈니스 로직 없음)
- **Domain Record = 비즈니스 전용** (순수, 불변, JPA 어노테이션 없음)
- **Mapper = 통역사** (두 세계를 연결하는 변환 계층)
- 점진적 도입이 핵심: 가장 복잡한 로직부터 순수 함수로 추출하여 시작한다.
