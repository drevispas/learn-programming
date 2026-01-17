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
    public Order cancel(CancelReason reason) {
        if (!(status instanceof Unpaid || status instanceof Paid)) {
            throw new IllegalStateException("취소 불가능한 상태");
        }
        return new Order(id, customerId, lines, totalAmount,
                        new Cancelled(LocalDateTime.now(), reason));
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

---

### 8.2 Trust Boundary와 DTO

#### 도메인 코어를 보호하라

**신뢰 경계**:
- **외부**: 신뢰할 수 없는 데이터 (JSON, String, Raw Data)
- **경계**: 유효성 검사 및 변환 (DTO -> Domain Object)
- **내부**: 신뢰할 수 있는 도메인 객체 (불변, 유효함)

```java
// DTO: 외부 통신용
public class OrderDto {
    public String orderId;
    public BigDecimal amount;
}

// 도메인 요약 모델
public record OrderSummary(OrderId orderId, Money amount) {}

// 변환: DTO -> Domain (입력)
public Result<OrderSummary, String> toDomain(OrderDto dto) {
    try {
        return Result.success(new OrderSummary(
            new OrderId(dto.orderId),
            new Money(dto.amount, Currency.KRW)
        ));
    } catch (IllegalArgumentException e) {
        return Result.failure(e.getMessage());
    }
}

// 변환: Domain -> DTO (출력)
public OrderDto toDto(OrderSummary order) {
    OrderDto dto = new OrderDto();
    dto.orderId = order.orderId().value();
    dto.amount = order.amount().amount();
    return dto;
}
```

---

### 8.3 Anti-Corruption Layer

#### 💡 비유: 통역사

> **ACL은 통역사입니다.**
> 외국 손님(외부 시스템)이 오면 통역사가 번역합니다.
> 우리 팀(도메인)은 우리 언어(도메인 타입)만 사용합니다.

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
        return Result.success(new PaymentApproval(
            new TransactionId(response.transaction_id())
        ));
    }
}
```

---

### 8.4 toDomain() 위치: Controller가 변환 (문지기 역할)

서비스 메서드 시그니처가 **타입(Type) 그 자체로 문서**가 되어야 합니다.

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

A. 성능이 느려져서
B. 도메인이 인프라(DB)에 의존하게 되어 결합도가 높아짐
C. 코드가 길어져서
D. 테스트가 어려워져서

---

#### Q8.2 [설계 문제] DTO 분리
Domain 모델과 API Response를 분리하는 이유가 아닌 것은?

A. 도메인 변경이 API에 영향주지 않도록
B. API 응답에 추가 정보(statusDescription 등) 포함 가능
C. 코드량을 줄이기 위해
D. 보안 민감 정보(password 등) 노출 방지

---

#### Q8.3 [코드 분석] ACL
Anti-Corruption Layer의 역할은?

A. 외부 시스템의 형식을 도메인 형식으로 변환
B. 데이터베이스 트랜잭션 관리
C. 로깅
D. 캐싱

---

#### Q8.4 [설계 문제] 경계 정의
외부 결제 API가 응답 형식을 바꾸면 수정해야 하는 곳은?

A. 도메인 모델
B. Anti-Corruption Layer (Adapter)
C. 컨트롤러
D. 모든 곳

---

#### Q8.5 [코드 분석] Mapper
OrderMapper.toDomain()의 역할은?

A. Domain → Entity 변환
B. Entity → Domain 변환
C. Domain → Response 변환
D. Request → Domain 변환

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 9: 함수형 아키텍처 패턴

### 학습 목표
1. Onion Architecture의 계층 구조를 이해한다
2. 부수효과를 도메인 로직과 분리하는 방법을 익힌다
3. 순수 함수의 테스트 용이성을 활용할 수 있다
4. 의존성 주입을 함수형 스타일로 적용할 수 있다

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

```
┌─────────────────────────────────────────────────────────┐
│                    Infrastructure                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Application Services               │   │
│  │  ┌───────────────────────────────────────────┐ │   │
│  │  │              Domain Model                 │ │   │
│  │  │  - Entities (Order, Customer)            │ │   │
│  │  │  - Value Objects (Money, Email)          │ │   │
│  │  │  - Domain Services                        │ │   │
│  │  └───────────────────────────────────────────┘ │   │
│  │  - Use Cases (PlaceOrder, CancelOrder)        │   │
│  │  - Repository Interfaces                       │   │
│  └─────────────────────────────────────────────────┘   │
│  - Controllers, JPA Repositories, External APIs       │
│  - Mappers, Adapters                                   │
└─────────────────────────────────────────────────────────┘
```

> **중요**: Repository Interface는 **domain 패키지**에 위치합니다! (DIP)
>
> Domain: "나는 저장을 하고 싶어. 하지만 DB가 Oracle인지 MySQL인지, 파일인지 알 바 아니야."
> → 인터페이스 정의 (OrderRepository)
>
> Infra: "도메인 주인님, 제가 그 인터페이스에 맞춰서 JPA로 구현해 왔습니다."
> → 구현체 (OrderRepositoryImpl)

---

### 9.2 함수형 의존성 주입 (Dependency Injection)

#### 인터페이스 없는 DI

전통적인 방식은 인터페이스를 만들고 `@Autowired`로 구현체를 주입받습니다. 함수형 프로그래밍에서는 **함수를 파라미터로 전달**하는 것만으로 충분합니다.

```java
// 의존성: 환율 계산 함수 (인터페이스가 아닌 함수형 인터페이스)
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

#### 커링(Currying)과 부분 적용(Partial Application)

매번 의존성을 넘기는 것이 귀찮다면, 함수를 리턴하는 함수(고차 함수)를 사용해 의존성을 미리 주입(설정)해둘 수 있습니다.

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

### 9.3 부수효과 격리

#### 💡 비유: 회계사와 금고

> **순수 함수는 회계사입니다.**
>
> 회계사는 장부(입력)를 보고 계산(로직)만 합니다.
> 직접 금고(DB)를 열거나 돈을 옮기지 않습니다.
>
> **부수효과는 금고 관리인입니다.**
>
> 회계사의 지시(결과)에 따라
> 금고 관리인이 실제로 돈을 옮깁니다.

---

### 9.4 Functional Core, Imperative Shell

> "우리 팀은 Spring 쓰고 JPA 쓰는데요?" 100% 순수하게 짤 수는 없습니다.
> 현실적인 타협점: **Functional Core, Imperative Shell**

**Functional Core (순수 영역)**:
- 비즈니스 로직, 계산, 판단, 데이터 변환 등 **"머리 쓰는 일"**은 순수 함수로
- Mock 없이 빡세게 테스트

**Imperative Shell (불순 영역)**:
- DB 저장, API 호출, 로그 출력 등 **"몸 쓰는 일"**은 바깥쪽으로
- 로직이 거의 없게 만들어서 테스트 부담 감소

```java
// 순수한 도메인 로직 (부수효과 없음)
public class OrderDomainService {
    // 입력 → 출력, 외부 의존성 없음
    // Optional을 파라미터로 받지 않고 메서드 오버로딩 사용
    public PricedOrder calculatePrice(ValidatedOrder order) {
        return calculatePrice(order, Money.ZERO);
    }

    public PricedOrder calculatePrice(ValidatedOrder order, Coupon coupon) {
        Money subtotal = order.lines().stream()
            .map(line -> line.price().multiply(line.quantity()))
            .reduce(Money.ZERO, Money::add);

        Money discount = coupon.calculateDiscount(subtotal);

        return new PricedOrder(order.customerId(), order.lines(),
            subtotal, discount, subtotal.subtract(discount));
    }

    // 내부에서만 사용하는 private 메서드
    private PricedOrder calculatePrice(ValidatedOrder order, Money discount) {
        Money subtotal = order.lines().stream()
            .map(line -> line.price().multiply(line.quantity()))
            .reduce(Money.ZERO, Money::add);

        return new PricedOrder(order.customerId(), order.lines(),
            subtotal, discount, subtotal.subtract(discount));
    }
}

// 부수효과를 가진 애플리케이션 서비스
public class PlaceOrderUseCase {
    private final OrderRepository orderRepository;      // 부수효과: DB
    private final PaymentGateway paymentGateway;        // 부수효과: 외부 API
    private final OrderDomainService domainService;     // 순수 로직

    @Transactional
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand cmd) {
        // 1. 검증 및 보강 (예시)
        ValidatedOrder validated = validateOrder(cmd);
        Coupon coupon = findCoupon(cmd);

        // 2. 순수 로직: 가격 계산 (테스트 쉬움)
        PricedOrder priced = domainService.calculatePrice(validated, coupon);

        // 3. 부수효과: 결제
        PaymentResult payment = paymentGateway.charge(priced.totalAmount());

        // 4. 부수효과: 저장
        Order order = createOrder(validated, payment);
        Order savedOrder = orderRepository.save(order);

        return Result.success(new OrderPlaced(savedOrder.id()));
    }

    // 예시용 스텁
    private ValidatedOrder validateOrder(PlaceOrderCommand cmd) {
        throw new UnsupportedOperationException("검증 로직 생략");
    }

    private Coupon findCoupon(PlaceOrderCommand cmd) {
        throw new UnsupportedOperationException("쿠폰 조회 로직 생략");
    }

    private Order createOrder(ValidatedOrder order, PaymentResult payment) {
        throw new UnsupportedOperationException("주문 생성 로직 생략");
    }
}
```

---

### 9.5 순수 함수와 테스트

#### 💡 비유: 계산기

> **순수 함수는 계산기입니다.**
> 같은 버튼을 누르면 항상 같은 결과. 언제 어디서 눌러도 1 + 1 = 2.
>
> 테스트하기 매우 쉽습니다:
> - 입력 준비
> - 함수 호출
> - 결과 확인
>
> DB 연결, 네트워크, 시간 등 외부 요소 불필요.

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

        // When: 순수 함수 호출
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

---

### 퀴즈 Chapter 9

#### Q9.1 [개념 확인] Onion Architecture
Onion Architecture에서 의존성 방향은?

A. 안쪽 → 바깥쪽 (Domain → Infrastructure)
B. 바깥쪽 → 안쪽 (Infrastructure → Domain)
C. 양방향
D. 의존성 없음

---

#### Q9.2 [설계 문제] 부수효과
다음 중 부수효과가 있는 작업은?

A. 주문 금액 계산
B. 할인율 적용
C. 데이터베이스에 저장
D. 취소 가능 여부 판단

---

#### Q9.3 [코드 분석] 순수 함수
순수 함수의 특징이 아닌 것은?

A. 같은 입력에 항상 같은 출력
B. 외부 상태를 변경하지 않음
C. 데이터베이스를 조회함
D. 테스트하기 쉬움

---

#### Q9.4 [설계 문제] 계층 분리
OrderRepository 인터페이스는 어느 계층에 정의해야 하나요?

A. Infrastructure
B. Domain
C. Application
D. Presentation

---

#### Q9.5 [코드 분석] 테스트 용이성
순수한 도메인 로직을 분리하면 테스트가 쉬워지는 이유는?

A. 코드가 짧아져서
B. DB 연결, Mock 없이 입력/출력만으로 테스트 가능
C. 실행 속도가 빨라서
D. IDE 지원이 좋아서

---

정답은 Appendix D에서 확인할 수 있습니다.
