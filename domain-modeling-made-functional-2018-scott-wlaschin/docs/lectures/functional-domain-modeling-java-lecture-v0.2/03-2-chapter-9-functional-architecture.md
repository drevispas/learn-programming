# Chapter 9: 함수형 아키텍처 패턴

> Part III: 아키텍처

---

### 학습 목표
1. Onion Architecture의 계층 구조를 이해한다
2. Side Effect를 도메인 로직과 분리하는 방법을 익힌다
3. Pure Function의 테스트 용이성을 활용할 수 있다
4. Dependency Injection을 함수형 스타일로 적용할 수 있다

---

### 9.1 Onion Architecture

#### 💡 비유: 양파 껍질
> **아키텍처는 양파입니다.**
>
> - **가장 안쪽(Core)**: 도메인 모델 - 순수한 비즈니스 규칙
> - **중간층**: 애플리케이션 서비스 - 유스케이스 조율
> - **바깥층**: 인프라 - DB, 외부 API, 웹 프레임워크
>
> **의존성은 항상 안쪽으로 향합니다.** 도메인은 아무것도 의존하지 않습니다.

**그림 9.1**: Onion Architecture 계층 구조

![Onion Architecture - Clean Architecture Onion View](https://learn.microsoft.com/en-us/dotnet/architecture/modern-web-apps-azure/media/image5-7.png)

*출처: [Microsoft Learn - Common Web Application Architectures](https://learn.microsoft.com/en-us/dotnet/architecture/modern-web-apps-azure/common-web-application-architectures)*

```
┌──────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE                           │
│    Controllers, JPA Repositories, External APIs, Mappers         │
│                                                                  │
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │                    APPLICATION SERVICES                      │ │
│ │         Use Cases, Repository Interfaces                     │ │
│ │                                                              │ │
│ │ ┌──────────────────────────────────────────────────────────┐ │ │
│ │ │                      DOMAIN MODEL                        │ │ │
│ │ │                                                          │ │ │
│ │ │    Entities: Order, Customer, Product                    │ │ │
│ │ │    Value Objects: Money, Email, OrderId                  │ │ │
│ │ │    Domain Services: OrderDomainService                   │ │ │
│ │ │                                                          │ │ │
│ │ └──────────────────────────────────────────────────────────┘ │ │
│ │                                                              │ │
│ └──────────────────────────────────────────────────────────────┘ │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

              Dependency Direction: Outside --> Inside
```

> **중요**: Repository Interface는 **domain 패키지**에 위치합니다! (DIP)
>
> Domain: "나는 저장을 하고 싶어. 하지만 DB가 Oracle인지 MySQL인지, 파일인지 알 바 아니야."
> → 인터페이스 정의 (OrderRepository)
>
> Infra: "도메인 주인님, 제가 그 인터페이스에 맞춰서 JPA로 구현해 왔습니다."
> → 구현체 (OrderRepositoryImpl)

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Onion Architecture는 다음 프로젝트에서 검증되었습니다:
- Netflix의 마이크로서비스 아키텍처
- Microsoft의 .NET 애플리케이션 권장 패턴
- Spring Framework 공식 문서 권장 구조

**Expert Opinions:**
- **Jeffrey Palermo** (Onion Architecture 창시자): "의존성은 항상 안쪽으로 향해야 한다"
- **Robert C. Martin** (Uncle Bob): "Clean Architecture는 Onion의 변형이다"
- **Eric Evans**: "도메인 레이어는 기술적 관심사로부터 격리되어야 한다"

**참고 자료:**
- [The Onion Architecture](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/) - Jeffrey Palermo
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) - Robert C. Martin
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) - Alistair Cockburn

---

### 9.2 함수형 Dependency Injection

#### 인터페이스 없는 DI

전통적인 방식은 인터페이스를 만들고 `@Autowired`로 구현체를 주입받습니다. 함수형 프로그래밍에서는 **함수를 파라미터로 전달**하는 것만으로 충분합니다.

**코드 9.1**: 함수형 Dependency Injection
```java
// 의존성: 환율 계산 함수 (인터페이스가 아닌 Functional Interface)
public interface GetExchangeRate {
    BigDecimal get(Currency from, Currency to);
}

// 비즈니스 로직
public class PriceService {
    // 의존성을 메서드 파라미터로 받음
    public Money convertPrice(Money price, Currency to, GetExchangeRate getRate) {
        BigDecimal rate = getRate.get(price.currency(), to);
        return new Money(price.amount().multiply(rate), to);
    }
}
```

#### Currying과 Partial Application

매번 의존성을 넘기는 것이 귀찮다면, 함수를 리턴하는 함수(Higher-Order Function)를 사용해 의존성을 미리 주입(설정)해둘 수 있습니다.

**코드 9.2**: Currying과 Partial Application
```java
// 설정 단계 (Composition Root)
GetExchangeRate realExchangeRate = new RealExchangeRateApi();

// 의존성 주입: 함수를 부분 적용하여 새로운 함수 생성
Function<Money, Money> krwConverter =
    price -> priceService.convertPrice(price, Currency.KRW, realExchangeRate);

// 사용 단계: 의존성을 몰라도 됨
Money krw = krwConverter.apply(usd100);
```

---

### 9.3 Side Effect 격리

#### 💡 비유: 회계사와 금고
> **Pure Function은 회계사입니다.**
>
> 회계사는 장부(입력)를 보고 계산(로직)만 합니다.
> 직접 금고(DB)를 열거나 돈을 옮기지 않습니다.
>
> **Side Effect는 금고 관리인입니다.**
>
> 회계사의 지시(결과)에 따라
> 금고 관리인이 실제로 돈을 옮깁니다.

---

### 9.4 Functional Core, Imperative Shell

> "우리 팀은 Spring 쓰고 JPA 쓰는데요?" 100% 순수하게 짤 수는 없습니다.
> 현실적인 타협점: **Functional Core, Imperative Shell**

**그림 9.2**: Functional Core, Imperative Shell 패턴

![Functional Core Imperative Shell](https://kennethlange.com/wp-content/uploads/2021/03/functional_core_imperative_shell-624x351.png)

*출처: [The Functional Core, Imperative Shell Pattern - Kenneth Lange](https://kennethlange.com/functional-core-imperative-shell/)*

> 순수 함수로 구성된 **Functional Core** (내부)와
> 부수효과를 처리하는 **Imperative Shell** (외부)로 분리합니다.

**Functional Core (순수 영역)**:
- 비즈니스 로직, 계산, 판단, 데이터 변환 등 **"머리 쓰는 일"**은 Pure Function으로
- Mock 없이 빡세게 테스트

**Imperative Shell (불순 영역)**:
- DB 저장, API 호출, 로그 출력 등 **"몸 쓰는 일"**은 바깥쪽으로
- 로직이 거의 없게 만들어서 테스트 부담 감소

**코드 9.3**: Functional Core, Imperative Shell 패턴
```java
// 순수한 도메인 로직 (Side Effect 없음)
public class OrderDomainService {
    // 입력 → 출력, 외부 의존성 없음
    // Optional을 파라미터로 받지 않고 메서드 오버로딩 사용
    public PricedOrder calculatePrice(ValidatedOrder order) {
        return calculatePriceInternal(order, Money.ZERO);
    }

    public PricedOrder calculatePrice(ValidatedOrder order, Coupon coupon) {
        Money subtotal = calculateSubtotal(order);
        Money discount = coupon.calculateDiscount(subtotal);
        return calculatePriceInternal(order, discount);
    }

    // 내부 헬퍼 메서드
    private Money calculateSubtotal(ValidatedOrder order) {
        return order.lines().stream()
            .map(line -> line.price().multiply(line.quantity()))
            .reduce(Money.ZERO, Money::add);
    }

    private PricedOrder calculatePriceInternal(ValidatedOrder order, Money discount) {
        Money subtotal = calculateSubtotal(order);
        return new PricedOrder(order.customerId(), order.lines(),
            subtotal, discount, subtotal.subtract(discount));
    }
}

// Side Effect를 가진 애플리케이션 서비스
public class PlaceOrderUseCase {
    private final OrderRepository orderRepository;      // Side Effect: DB
    private final PaymentGateway paymentGateway;        // Side Effect: 외부 API
    private final OrderDomainService domainService;     // 순수 로직

    @Transactional
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand cmd) {
        // 1. 검증 및 보강 (예시)
        ValidatedOrder validated = validateOrder(cmd);
        Coupon coupon = findCoupon(cmd);

        // 2. 순수 로직: 가격 계산 (테스트 쉬움)
        PricedOrder priced = domainService.calculatePrice(validated, coupon);

        // 3. Side Effect: 결제
        PaymentResult payment = paymentGateway.charge(priced.totalAmount());

        // 4. Side Effect: 저장
        Order order = createOrder(validated, payment);
        Order savedOrder = orderRepository.save(order);

        return Result.success(new OrderPlaced(savedOrder.id()));
    }

    // 실제 구현은 examples/functional-domain-modeling/ 프로젝트 참조
    private ValidatedOrder validateOrder(PlaceOrderCommand cmd) {
        return new ValidatedOrder(cmd.customerId(), cmd.lines());
    }

    private Coupon findCoupon(PlaceOrderCommand cmd) {
        return cmd.couponId()
            .map(couponRepository::findById)
            .orElse(Coupon.NONE);
    }

    private Order createOrder(ValidatedOrder order, PaymentResult payment) {
        return new Order(
            OrderId.generate(),
            order.customerId(),
            order.lines(),
            payment.amount(),
            new Paid(LocalDateTime.now(), payment.txId())
        );
    }
}
```

> 💡 전체 워크플로우 구현은 `examples/functional-domain-modeling/src/main/java/com/ecommerce/application/PlaceOrderUseCase.java`를 참조하세요.

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Functional Core, Imperative Shell은 다음 프로젝트에서 검증되었습니다:
- F# 기반 금융 시스템 (Jane Street, Bloomberg)
- Scala 기반 마이크로서비스 (Twitter, LinkedIn)
- Kotlin Arrow 라이브러리 권장 패턴

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "도메인 로직을 순수하게 유지하면 테스트와 추론이 쉬워진다"
- **Gary Bernhardt**: "Functional Core, Imperative Shell은 실용적인 FP 접근법이다"
- **Mark Seemann**: "함수형 아키텍처는 테스트 가능성을 극대화한다"

**참고 자료:**
- [Domain Modeling Made Functional](https://pragprog.com/titles/swdddf/) - Scott Wlaschin
- [Functional Core, Imperative Shell](https://www.destroyallsoftware.com/screencasts/catalog/functional-core-imperative-shell) - Gary Bernhardt
- [Dependency Rejection](https://blog.ploeh.dk/2017/02/02/dependency-rejection/) - Mark Seemann

---

### 9.5 Pure Function과 테스트

#### 💡 비유: 계산기
> **Pure Function은 계산기입니다.**
> 같은 버튼을 누르면 항상 같은 결과. 언제 어디서 눌러도 1 + 1 = 2.
>
> 테스트하기 매우 쉽습니다:
> - 입력 준비
> - 함수 호출
> - 결과 확인
>
> DB 연결, 네트워크, 시간 등 외부 요소 불필요.

**코드 9.4**: Pure Function 테스트 예제
```java
class OrderDomainServiceTest {
    private final OrderDomainService service = new OrderDomainService();

    @Test
    void calculatePrice_withCoupon_appliesDiscount() {
        // Given: 순수한 입력 데이터
        var order = new ValidatedOrder(
            new CustomerId(1L),
            List.of(new ValidatedOrderLine(productId, quantity(2), Money.krw(10000)))
        );
        var coupon = new PercentageCoupon(10);  // 10% 할인

        // When: Pure Function 호출
        PricedOrder result = service.calculatePrice(order, coupon);

        // Then: 결과 검증 (외부 의존성 없음!)
        assertThat(result.subtotal()).isEqualTo(Money.krw(20000));
        assertThat(result.discount()).isEqualTo(Money.krw(2000));
        assertThat(result.totalAmount()).isEqualTo(Money.krw(18000));
    }

    @Test
    void canCancel_unpaidOrder_returnsTrue() {
        var order = new Order(orderId, customerId, lines, new Unpaid());
        assertThat(service.canCancel(order)).isTrue();
    }

    @Test
    void canCancel_shippingOrder_returnsFalse() {
        var order = new Order(orderId, customerId, lines, new Shipping(tracking));
        assertThat(service.canCancel(order)).isFalse();
    }
}
```

> 💡 전체 테스트 코드는 `examples/functional-domain-modeling/src/test/java/com/ecommerce/` 디렉토리를 참조하세요.

---

### 퀴즈 Chapter 9

#### Q9.1 [개념 확인] Onion Architecture

Onion Architecture에서 의존성 방향은?

**A.** 안쪽 → 바깥쪽 (Domain → Infrastructure)<br/>
**B.** 바깥쪽 → 안쪽 (Infrastructure → Domain)<br/>
**C.** 양방향<br/>
**D.** 의존성 없음

---

#### Q9.2 [설계 문제] Side Effect

다음 중 Side Effect가 있는 작업은?

**A.** 주문 금액 계산<br/>
**B.** 할인율 적용<br/>
**C.** 데이터베이스에 저장<br/>
**D.** 취소 가능 여부 판단

---

#### Q9.3 [코드 분석] Pure Function

Pure Function의 특징이 아닌 것은?

**A.** 같은 입력에 항상 같은 출력<br/>
**B.** 외부 상태를 변경하지 않음<br/>
**C.** 데이터베이스를 조회함<br/>
**D.** 테스트하기 쉬움

---

#### Q9.4 [설계 문제] 계층 분리

OrderRepository 인터페이스는 어느 계층에 정의해야 하나요?

**A.** Infrastructure<br/>
**B.** Domain<br/>
**C.** Application<br/>
**D.** Presentation

---

#### Q9.5 [코드 분석] 테스트 용이성

순수한 도메인 로직을 분리하면 테스트가 쉬워지는 이유는?

**A.** 코드가 짧아져서<br/>
**B.** DB 연결, Mock 없이 입력/출력만으로 테스트 가능<br/>
**C.** 실행 속도가 빨라서<br/>
**D.** IDE 지원이 좋아서

---

정답은 Appendix D에서 확인할 수 있습니다.
