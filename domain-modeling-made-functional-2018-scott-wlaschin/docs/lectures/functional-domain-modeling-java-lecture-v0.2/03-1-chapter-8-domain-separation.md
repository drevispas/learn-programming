# Chapter 8: 도메인과 외부 세계 분리

> Part III: 아키텍처

---

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
        return switch (status) {
            case Unpaid u, Paid p -> Result.success(new Order(id, customerId, lines, totalAmount,
                new Cancelled(LocalDateTime.now(), reason)));
            case Shipping s -> Result.failure(new OrderError.InvalidState("배송 중"));
            case Delivered d -> Result.failure(new OrderError.InvalidState("배송 완료"));
            case Cancelled c -> Result.failure(new OrderError.InvalidState("이미 취소됨"));
        };
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
