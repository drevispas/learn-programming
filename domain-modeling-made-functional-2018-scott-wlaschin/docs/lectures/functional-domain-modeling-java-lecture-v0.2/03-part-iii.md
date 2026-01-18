# Part III: 아키텍처

---

## Chapter 8: 도메인과 외부 세계 분리

### 학습 목표
1. Persistence Ignorance 원칙을 이해한다
2. DTO와 Domain 모델 분리의 필요성을 설명할 수 있다
3. Anti-Corruption Layer를 구현할 수 있다
4. 외부 시스템과의 경계를 명확히 정의할 수 있다

---

### 8.1 Persistence Ignorance

#### 💡 비유: VIP와 매니저
> **도메인 모델은 VIP입니다.**
> VIP는 자신의 전문 분야(비즈니스 로직)에만 집중합니다.
> "데이터가 어디에 저장되는지", "어떤 DB를 쓰는지" 신경 쓰지 않습니다.
>
> **Repository는 매니저입니다.**
> 매니저가 VIP의 일정, 이동, 숙소를 모두 관리합니다.
> VIP는 그냥 업무(비즈니스 로직)만 하면 됩니다.

**코드 8.1**: Persistence Ignorance - 도메인 모델과 Repository 분리
```java
// 도메인 모델: DB를 전혀 모름 (JPA 어노테이션 없음!)
public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    OrderStatus status
) {
    // 순수한 비즈니스 로직만
    public Result<Order, OrderError> cancel(CancelReason reason) {
        if (!(status instanceof Unpaid || status instanceof Paid))
            return Result.failure(new OrderError.InvalidState("취소 불가능한 상태"));
        return Result.success(new Order(id, customerId, lines, totalAmount,
                        new Cancelled(LocalDateTime.now(), reason)));
    }
}

// Repository 인터페이스: 도메인 레이어에 정의
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
    void delete(OrderId id);
}

// Repository 구현: 인프라 레이어에 정의
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);  // Entity → Domain 변환
    }
}
```

> 💡 전체 동작 코드는 `examples/functional-domain-modeling/` 프로젝트에서 확인하세요.
> 특히 Repository 패턴 구현은 `src/main/java/com/ecommerce/` 디렉토리를 참조하세요.

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Persistence Ignorance는 다음 프로젝트에서 검증되었습니다:
- Spring Data JPA의 권장 패턴 (Repository Interface in Domain)
- Hexagonal Architecture (Ports & Adapters) - Netflix, Uber
- Clean Architecture - Uncle Bob의 엔터프라이즈 패턴

**Expert Opinions:**
- **Eric Evans** (DDD 창시자): "Domain Model should be free of infrastructure concerns"
- **Vaughn Vernon**: "Repository는 Aggregate의 컬렉션처럼 동작해야 한다"
- **Martin Fowler**: "Repository는 도메인과 데이터 매핑 레이어 사이를 중재한다"

**참고 자료:**
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/) - Eric Evans
- [Implementing Domain-Driven Design](https://vaughnvernon.com/) - Vaughn Vernon
- [Patterns of Enterprise Application Architecture](https://martinfowler.com/eaaCatalog/) - Martin Fowler

---

### 8.2 Trust Boundary와 DTO

#### 도메인 코어를 보호하라

**그림 8.0**: Trust Boundary (신뢰 경계) 다이어그램

![Trust Boundary DFD](https://threat-modeling.com/wp-content/uploads/2022/10/How-to-use-Data-Flow-Diagrams-in-Threat-Modeling-Example-2-1.jpg)
*출처: [Data Flow Diagrams in Threat Modeling - Threat-Modeling.com](https://threat-modeling.com/data-flow-diagrams-in-threat-modeling/)*

> 점선으로 표시된 **Trust Boundary**가 신뢰할 수 있는 영역과 그렇지 않은 영역을 구분합니다.
> 데이터가 경계를 넘을 때마다 **검증과 변환**이 필요합니다.

**신뢰 경계**:
- **외부**: 신뢰할 수 없는 데이터 (JSON, String, Raw Data)
- **경계**: 유효성 검사 및 변환 (DTO -> Domain Object)
- **내부**: 신뢰할 수 있는 도메인 객체 (Immutable, Valid)

**코드 8.2**: Trust Boundary - DTO와 Domain 변환
```java
// DTO: 외부 통신용 (불변성 보장을 위해 Record 사용)
public record OrderDto(String orderId, BigDecimal amount) {}

// 도메인 요약 모델
public record OrderSummary(OrderId orderId, Money amount) {}

// 변환: DTO -> Domain (입력)
// 💡 Trust Boundary에서는 외부 입력의 예외를 Result로 변환하는 것이 허용됩니다
// 도메인 내부 로직에서는 Exception을 사용하지 마세요
public Result<OrderSummary, String> toDomain(OrderDto dto) {
    try {
        return Result.success(new OrderSummary(
            new OrderId(dto.orderId()),
            new Money(dto.amount(), Currency.KRW)
        ));
    } catch (IllegalArgumentException e) {
        return Result.failure(e.getMessage());
    }
}

// 변환: Domain -> DTO (출력)
public OrderDto toDto(OrderSummary order) {
    return new OrderDto(
        order.orderId().value(),
        order.amount().amount()
    );
}
```

#### ❌ Anti-pattern: 도메인 모델을 직접 노출

```java
// 문제 코드: 도메인 모델을 API 응답으로 직접 반환
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable String id) {
    return orderRepository.findById(new OrderId(id)).orElseThrow();
}
```

**왜 나쁜가?**
1. **내부 구조 노출**: 도메인 모델 변경이 API 스펙 변경으로 이어짐
2. **보안 위험**: 민감한 필드(password, internalId 등)가 노출될 수 있음
3. **직렬화 문제**: 순환 참조, Lazy Loading 예외 등 발생 가능

**실제 버그 사례:**
```java
// 2019년 특정 핀테크 사고 - User 엔티티 직접 반환
// password 해시, 주민번호 등이 API 응답에 포함됨
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

**반박 예상 질문:**
> "DTO 변환하면 코드량이 너무 늘어나지 않나요?"

**답변:** MapStruct, ModelMapper 등 매핑 라이브러리로 boilerplate 최소화.
보안 사고 한 번의 비용이 DTO 코드 작성 비용보다 훨씬 큼.

---

### 8.3 Anti-Corruption Layer

#### 💡 비유: 통역사
> **ACL은 통역사입니다.**
> 외국 손님(외부 시스템)이 오면 통역사가 번역합니다.
> 우리 팀(도메인)은 우리 언어(도메인 타입)만 사용합니다.

**그림 8.1**: Anti-Corruption Layer 패턴

![Anti-Corruption Layer](https://learn.microsoft.com/en-us/azure/architecture/patterns/_images/anti-corruption-layer.png)
*출처: [Anti-Corruption Layer Pattern - Microsoft Azure Architecture Center](https://learn.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer)*

> 외부 시스템의 모델을 도메인 모델로 변환하는 **번역 계층**을 둡니다.
> 도메인 모델이 외부 시스템에 오염되지 않도록 보호합니다.

**코드 8.3**: Anti-Corruption Layer - 외부 API 응답 변환
```java
// 외부 결제 API 응답
public record ExternalPaymentResponse(
    String result_code,      // "0000" = 성공
    String result_msg,
    String transaction_id
) {}

// ACL: 외부 형식 → 도메인 형식 변환
public class PaymentGatewayAdapter {
    public Result<PaymentApproval, PaymentError> processPayment(PaymentRequest request) {
        try {
            ExternalPaymentResponse response = externalClient.pay(request);
            return translateResponse(response);
        } catch (ExternalApiException e) {
            return Result.failure(new PaymentError.SystemError(e.getMessage()));
        }
    }

    private Result<PaymentApproval, PaymentError> translateResponse(
        ExternalPaymentResponse response
    ) {
        if (!"0000".equals(response.result_code())) {
            return Result.failure(translateErrorCode(response.result_code()));
        }
        return Result.success(
            new PaymentApproval(
                new TransactionId(response.transaction_id()
            )
        ));
    }
}
```

> 💡 ACL 패턴의 전체 구현은 `examples/functional-domain-modeling/src/main/java/com/ecommerce/application/` 디렉토리를 참조하세요.

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Anti-Corruption Layer는 다음 프로젝트에서 검증되었습니다:
- 마이크로서비스 통합 (Netflix, Amazon)
- 레거시 시스템 마이그레이션 (Strangler Fig 패턴)
- 외부 API 통합 (결제, 배송, 인증 등)

**Expert Opinions:**
- **Eric Evans** (DDD 창시자): "ACL은 Bounded Context 간 번역 레이어로 필수적이다"
- **Michael Feathers**: "레거시 코드와 새 코드 사이에 ACL을 두어라"
- **Sam Newman**: "마이크로서비스 통합에서 ACL은 필수 패턴이다"

**참고 자료:**
- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/) - Eric Evans
- [Building Microservices](https://samnewman.io/books/building_microservices/) - Sam Newman
- [Working Effectively with Legacy Code](https://www.oreilly.com/library/view/working-effectively-with/0131177052/) - Michael Feathers

---

### 8.4 toDomain() 위치: Controller가 변환 (문지기 역할)

서비스 메서드 시그니처가 **타입(Type) 그 자체로 문서**가 되어야 합니다.

**코드 8.4**: Controller에서 DTO → Domain 변환
```java
// [Controller] - 문지기 역할
@PostMapping("/users")
public Result<Void, String> registerUser(@RequestBody UserDTO dto) {
    // 1. 여기서 변환 및 1차 검증
    User user = UserMapper.toDomain(dto);

    // 2. 서비스에는 '순수한 도메인 객체'만 넘김
    return userService.register(user);
}

// [Service] - 비즈니스 로직 전담
public Result<Void, String> register(User user) {
    // 이미 'User' 타입이므로 이름이 비었거나 나이가 음수일 확률 0%
    // 중복 가입 여부 등 '비즈니스 로직'에만 집중!
    if (userRepository.exists(user.username())) {
        return Result.failure("이미 존재하는 유저입니다.");
    }
    userRepository.save(user);
    return Result.success(null);
}
```

---

### 퀴즈 Chapter 8

#### Q8.1 도메인 모델이 JPA 어노테이션을 직접 가지면 안 되는 이유는?

**A.** 성능이 느려져서<br/>
**B.** 도메인이 인프라(DB)에 의존하게 되어 결합도가 높아짐<br/>
**C.** 코드가 길어져서<br/>
**D.** 테스트가 어려워져서

---

#### Q8.2 [설계 문제] DTO 분리

Domain 모델과 API Response를 분리하는 이유가 아닌 것은?

**A.** 도메인 변경이 API에 영향주지 않도록<br/>
**B.** API 응답에 추가 정보(statusDescription 등) 포함 가능<br/>
**C.** 코드량을 줄이기 위해<br/>
**D.** 보안 민감 정보(password 등) 노출 방지

---

#### Q8.3 [코드 분석] ACL

Anti-Corruption Layer의 역할은?

**A.** 외부 시스템의 형식을 도메인 형식으로 변환<br/>
**B.** 데이터베이스 트랜잭션 관리<br/>
**C.** 로깅<br/>
**D.** 캐싱

---

#### Q8.4 [설계 문제] 경계 정의

외부 결제 API가 응답 형식을 바꾸면 수정해야 하는 곳은?

**A.** 도메인 모델<br/>
**B.** Anti-Corruption Layer (Adapter)<br/>
**C.** 컨트롤러<br/>
**D.** 모든 곳

---

#### Q8.5 [코드 분석] Mapper

OrderMapper.toDomain()의 역할은?

**A.** Domain → Entity 변환<br/>
**B.** Entity → Domain 변환<br/>
**C.** Domain → Response 변환<br/>
**D.** Request → Domain 변환

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 9: 함수형 아키텍처 패턴

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

![Functional Core Imperative Shell](https://kennethlange.com/wp-content/uploads/2021/03/functional_core_imperative_shell-624x351.png)<br/>
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
