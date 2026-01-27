# 01. DDD Fundamentals (DDD 기초)

> **Sources**: DMMF Ch.1 (DDD 개요), Eric Evans "Domain-Driven Design" (2003), Vaughn Vernon "Implementing DDD" (2013)

---

## 1. What is DDD? (DDD란 무엇인가?)

### 핵심 개념
- **관련 키워드**: Domain-Driven Design, Strategic DDD, Tactical DDD, Ubiquitous Language
- **통찰**: DDD는 "복잡한 비즈니스 문제를 해결하기 위해 도메인 전문가와 개발자가 같은 언어로 소통하며, 도메인 모델을 코드에 반영하는" 설계 철학이다.
- **설명**: Eric Evans가 2003년에 제시한 DDD는 두 가지 수준으로 나뉩니다. **전략적 DDD**는 큰 그림(도메인 분해, 팀 경계)을 다루고, **전술적 DDD**는 코드 수준의 빌딩 블록(Entity, Value Object, Aggregate 등)을 다룹니다.

  DDD가 필요한 시점은 명확합니다: (1) 도메인 로직이 복잡하여 기술보다 비즈니스 이해가 중요할 때, (2) 여러 팀이 같은 시스템을 개발할 때, (3) 레거시 시스템을 점진적으로 현대화할 때.

**[그림 01.1]** DDD 전략 vs 전술 (Strategic vs Tactical DDD)
```
+--------------------------------------------------+
|                     DDD                          |
|--------------------------------------------------|
|                                                  |
|   STRATEGIC DDD            TACTICAL DDD          |
|   (The Big Picture)        (Code Building Blocks)|
|                                                  |
|   +----------------+       +------------------+  |
|   | Domains        |       | Entity           |  |
|   | Subdomains     |       | Value Object     |  |
|   | Bounded Context|       | Aggregate        |  |
|   | Context Map    |       | Repository       |  |
|   | Ubiq. Language |       | Domain Service   |  |
|   +----------------+       | Domain Event     |  |
|                            +------------------+  |
|                                                  |
|   WHO: Architects,         WHO: Developers       |
|        Domain Experts      SCOPE: Module/Package |
|   SCOPE: Organization                            |
+--------------------------------------------------+
```

### 개념이 아닌 것
- **"DDD = Entity와 Repository 사용"**: 그것은 전술적 DDD의 일부일 뿐. 전략적 DDD 없이 전술만 쓰면 효과 반감
- **"DDD = 복잡한 아키텍처"**: DDD는 도구가 아니라 사고방식. 단순한 CRUD에는 과잉 설계

### Before: Traditional OOP

**[코드 01.1]** Traditional OOP: Anemic Domain Model (빈약한 도메인 모델)
```java
 1| // package: com.ecommerce.order
 2| // [X] Anemic Domain Model: 데이터만 있고 행위는 Service에
 3| @Entity
 4| public class Order {
 5|   @Id private Long id;
 6|   private String status;
 7|   private BigDecimal amount;
 8|   private String customerId;
 9|   // getter/setter만 존재
10|   public String getStatus() { return status; }
11|   public void setStatus(String status) { this.status = status; }
12|   // 비즈니스 로직 없음!
13| }
14|
15| @Service
16| public class OrderService {
17|   public void payOrder(Order order, PaymentInfo payment) {
18|     if (!"PENDING".equals(order.getStatus()))
19|       throw new IllegalStateException("결제 불가 상태");
20|     // 모든 비즈니스 로직이 Service에 집중
21|     paymentGateway.charge(payment);
22|     order.setStatus("PAID");
23|     orderRepository.save(order);
24|   }
25| }
```
- **의도 및 코드 설명**: Entity는 데이터 저장소, Service가 모든 로직을 담당
- **뭐가 문제인가**:
  - Order가 자신의 상태 전이 규칙을 모름 (지식 유출)
  - Service에 로직이 집중되어 God Class 화
  - Order를 사용하는 모든 곳에서 상태 검증 로직 중복
  - 도메인 전문가가 코드를 읽을 수 없음

### After: DMMF 관점 (DDD + FP)

**[코드 01.2]** DMMF: Rich Domain Model + 타입으로 상태 강제
```java
 1| // package: com.ecommerce.order
 2| // [O] Rich Domain Model: 도메인 로직이 타입 안에
 3|
 4| // --- 상태를 타입으로 표현 ---
 5| public sealed interface OrderStatus
 6|   permits Pending, Paid, Shipped, Cancelled {}
 7|
 8| public record Pending(LocalDateTime createdAt) implements OrderStatus {}
 9| public record Paid(LocalDateTime paidAt, PaymentId paymentId) implements OrderStatus {}
10| public record Shipped(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
11| public record Cancelled(LocalDateTime cancelledAt, String reason) implements OrderStatus {}
12|
13| // --- Order: 자신의 상태 전이 규칙을 알고 있음 ---
14| public record Order(
15|   OrderId id,
16|   CustomerId customerId,
17|   List<OrderItem> items,
18|   Money totalAmount,
19|   OrderStatus status
20| ) {
21|   public Order {
22|     Objects.requireNonNull(id, "OrderId는 필수");
23|     Objects.requireNonNull(customerId, "CustomerId는 필수");
24|     Objects.requireNonNull(items, "items는 필수");
25|     Objects.requireNonNull(totalAmount, "totalAmount는 필수");
26|     Objects.requireNonNull(status, "status는 필수");
27|     items = List.copyOf(items);
28|   }
29|
30|   // 도메인 로직: Order가 스스로 결제 가능 여부 판단
31|   public Result<Order, OrderError> pay(PaymentId paymentId, LocalDateTime at) {
32|     return switch (status) {
33|       case Pending _ -> Result.success(
34|         new Order(id, customerId, items, totalAmount, new Paid(at, paymentId)));
35|       case Paid _, Shipped _, Cancelled _ ->
36|         Result.failure(new OrderError.CannotPay(id, status));
37|     };
38|   }
39|
40|   public Result<Order, OrderError> ship(TrackingNumber tracking, LocalDateTime at) {
41|     return switch (status) {
42|       case Paid _ -> Result.success(
43|         new Order(id, customerId, items, totalAmount, new Shipped(at, tracking)));
44|       case Pending _, Shipped _, Cancelled _ ->
45|         Result.failure(new OrderError.CannotShip(id, status));
46|     };
47|   }
48| }
49|
50| // --- UseCase: Orchestration만 담당 ---
51| @RequiredArgsConstructor
52| public class PayOrderUseCase {
53|   private final OrderRepository orderRepository;
54|   private final PaymentGateway paymentGateway;
55|
56|   public Result<Order, OrderError> execute(OrderId orderId, PaymentInfo info) {
57|     Order order = orderRepository.findById(orderId).orElseThrow();
58|     PaymentId paymentId = paymentGateway.charge(info);
59|     return order.pay(paymentId, LocalDateTime.now())
60|       .peek(orderRepository::save);
61|   }
62| }
```

### After: DOP 관점

**[코드 01.3]** DOP: Record(데이터) + Calculations(로직) 분리
```java
 1| // package: com.ecommerce.order
 2| // [O] DOP: 데이터와 계산의 명확한 분리
 3|
 4| // --- Data ---
 5| public sealed interface OrderStatus
 6|   permits Pending, Paid, Shipped, Cancelled {}
 7|
 8| public record Pending(LocalDateTime createdAt) implements OrderStatus {}
 9| public record Paid(LocalDateTime paidAt, PaymentId paymentId) implements OrderStatus {}
10| public record Shipped(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
11| public record Cancelled(LocalDateTime cancelledAt, String reason) implements OrderStatus {}
12|
13| public record Order(
14|   OrderId id,
15|   CustomerId customerId,
16|   List<OrderItem> items,
17|   Money totalAmount,
18|   OrderStatus status
19| ) {
20|   public Order {
21|     Objects.requireNonNull(id, "OrderId는 필수");
22|     Objects.requireNonNull(customerId, "CustomerId는 필수");
23|     Objects.requireNonNull(items, "items는 필수");
24|     Objects.requireNonNull(totalAmount, "totalAmount는 필수");
25|     Objects.requireNonNull(status, "status는 필수");
26|     items = List.copyOf(items);
27|   }
28| }
29|
30| // --- Calculations (순수 함수) ---
31| public class OrderTransitions {
32|   public static Result<Order, OrderError> pay(Order order, PaymentId paymentId, LocalDateTime at) {
33|     return switch (order.status()) {
34|       case Pending _ -> Result.success(
35|         new Order(order.id(), order.customerId(), order.items(),
36|           order.totalAmount(), new Paid(at, paymentId)));
37|       case Paid _, Shipped _, Cancelled _ ->
38|         Result.failure(new OrderError.CannotPay(order.id(), order.status()));
39|     };
40|   }
41|
42|   public static Result<Order, OrderError> ship(Order order, TrackingNumber tracking, LocalDateTime at) {
43|     return switch (order.status()) {
44|       case Paid _ -> Result.success(
45|         new Order(order.id(), order.customerId(), order.items(),
46|           order.totalAmount(), new Shipped(at, tracking)));
47|       case Pending _, Shipped _, Cancelled _ ->
48|         Result.failure(new OrderError.CannotShip(order.id(), order.status()));
49|     };
50|   }
51| }
52|
53| // --- Orchestrator ---
54| @RequiredArgsConstructor
55| public class PayOrderUseCase {
56|   private final OrderRepository orderRepository;
57|   private final PaymentGateway paymentGateway;
58|
59|   public Result<Order, OrderError> execute(OrderId orderId, PaymentInfo info) {
60|     Order order = orderRepository.findById(orderId).orElseThrow();
61|     PaymentId paymentId = paymentGateway.charge(info);
62|     return OrderTransitions.pay(order, paymentId, LocalDateTime.now())
63|       .peek(orderRepository::save);
64|   }
65| }
```

### 관점 차이 분석
- **공통점**: 둘 다 Anemic Model을 거부하고, 도메인 로직을 캡처하며, 타입으로 상태를 표현
- **DMMF 주장**: 도메인 로직은 타입 자체에 속해야 자연스러움. `order.pay()`가 `OrderTransitions.pay(order)`보다 직관적
- **DOP 주장**: 데이터(Order)와 연산(OrderTransitions)을 분리하면 테스트와 재사용이 용이
- **실무 권장**: 간단한 상태 전이는 Record 메서드로, 복잡한 비즈니스 규칙은 별도 Calculations로

### 이해를 위한 부가 상세

**[표 01.1]** Anemic vs Rich Domain Model
| 관점 | Anemic (빈약) | Rich (풍부) |
|------|--------------|-------------|
| Entity 역할 | 데이터 저장소 | 행위 + 불변식 보호 |
| Service 역할 | 모든 로직 담당 | Orchestration만 |
| 상태 전이 규칙 | Service에 산재 | Entity/타입에 캡슐화 |
| 테스트 용이성 | Mock 다수 필요 | 순수 함수 단위 테스트 |
| 도메인 전문가 이해도 | 낮음 | 높음 |

### 틀리기/놓치기 쉬운 부분
- **"Rich Model = Entity에 모든 로직"**: 아닙니다. Entity는 자신의 불변식과 상태 전이만 알면 됩니다. 외부 서비스 호출은 UseCase의 역할
- **"DDD는 항상 필요하다"**: 단순 CRUD, 데이터 파이프라인, 배치 작업에는 DDD가 오버엔지니어링

### 꼭 기억할 것
1. **DDD = 전략 + 전술**: 전술(Entity, VO)만 쓰면 효과 반감. 전략(BC, Ubiquitous Language)이 더 중요
2. **Anemic vs Rich**: 로직이 Service에만 있으면 Anemic. Entity가 자신의 규칙을 알면 Rich
3. **언제 DDD**: 도메인이 복잡하고, 비즈니스 규칙이 자주 변경되고, 여러 팀이 협업할 때

---

## 2. Strategic DDD: Domains and Subdomains (전략적 DDD: 도메인과 서브도메인)

### 핵심 개념
- **관련 키워드**: Domain, Subdomain, Core Domain, Supporting Subdomain, Generic Subdomain
- **통찰**: 모든 도메인이 동등하게 중요한 것은 아니다. 핵심 도메인에 최고 인력과 자원을 집중해야 한다.
- **설명**: 큰 시스템은 여러 서브도메인으로 분해됩니다. 이 중 **Core Domain**은 경쟁 우위의 원천으로 직접 개발해야 하고, **Supporting Subdomain**은 핵심을 지원하지만 차별화 요소가 아니며, **Generic Subdomain**은 범용 솔루션(인증, 결제 게이트웨이 등)으로 대체 가능합니다.

**[그림 01.2]** E-commerce 도메인 분해 (Domain Decomposition)
```
+---------------------------------------------------------------+
|                     E-commerce System                         |
|---------------------------------------------------------------|
|                                                               |
|  +------------------+   CORE DOMAIN (직접 개발, 최고 인력)     |
|  | Pricing &        |   - 경쟁 우위의 원천                     |
|  | Recommendation   |   - 동적 가격 책정, 개인화 추천           |
|  +------------------+                                         |
|                                                               |
|  +------------------+   +------------------+                   |
|  | Order Management |   | Inventory        |  SUPPORTING      |
|  +------------------+   +------------------+  SUBDOMAIN        |
|                                              (자체 개발하되    |
|                                               핵심 아님)       |
|                                                               |
|  +------------------+   +------------------+   +----------+   |
|  | Authentication   |   | Payment Gateway  |   | Shipping |   |
|  +------------------+   +------------------+   +----------+   |
|                         GENERIC SUBDOMAIN                     |
|                         (외부 솔루션 활용: Auth0, Stripe, DHL) |
+---------------------------------------------------------------+
```

### 개념이 아닌 것
- **"Core = 가장 복잡한 것"**: 복잡도가 아니라 비즈니스 가치 기준. 결제가 복잡해도 외부 솔루션 쓰면 Generic
- **"Subdomain = 마이크로서비스"**: Subdomain은 논리적 경계, 마이크로서비스는 배포 단위. 1:1 대응 아님

### Before: 서브도메인 구분 없는 설계

**[코드 01.4]** 모든 것을 동일 수준으로 다루는 모놀리식
```java
 1| // package: com.ecommerce.app
 2| // [X] 모든 것이 하나의 모듈에 혼재
 3| @Service
 4| public class EcommerceService {
 5|   // 핵심 도메인: 가격/추천 로직
 6|   public BigDecimal calculateDynamicPrice(Product p, Customer c) { /*...*/ }
 7|   // 지원 도메인: 주문
 8|   public Order createOrder(OrderRequest req) { /*...*/ }
 9|   // 일반 도메인: 인증 (직접 구현?)
10|   public boolean authenticate(String username, String password) { /*...*/ }
11|   // 일반 도메인: 결제 (직접 구현?)
12|   public boolean processPayment(PaymentInfo info) { /*...*/ }
13| }
```
- **의도 및 코드 설명**: 모든 도메인 로직이 하나의 서비스에 혼재
- **뭐가 문제인가**:
  - 핵심 도메인(가격/추천)과 범용 기능(인증/결제)이 같은 수준으로 취급
  - 범용 기능을 직접 구현하여 리소스 낭비
  - 핵심 도메인 개발자가 인증 버그 수정에 시간 소비

### After: Modern Approach

**[코드 01.5]** Subdomain별 분리와 자원 배분
```java
 1| // package: com.ecommerce.*
 2| // [O] 서브도메인별 분리 + 전략적 자원 배분
 3|
 4| // --- Core Domain: 직접 개발, 최고 인력 ---
 5| package com.ecommerce.pricing;
 6|
 7| public record PricingContext(
 8|   ProductId productId,
 9|   Customer customer,
10|   LocalDateTime requestTime,
11|   MarketConditions conditions
12| ) {}
13|
14| public class DynamicPricingEngine {
15|   public static Money calculatePrice(PricingContext ctx) {
16|     // 복잡한 ML 기반 가격 책정 로직
17|     // 이 부분이 경쟁 우위의 원천
18|     return /*...*/;
19|   }
20| }
21|
22| // --- Supporting Subdomain: 자체 개발하되 핵심 아님 ---
23| package com.ecommerce.order;
24|
25| public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {
26|   public Order { items = List.copyOf(items); }
27| }
28|
29| // --- Generic Subdomain: 외부 솔루션 활용 ---
30| package com.ecommerce.auth;
31|
32| // Auth0, Keycloak 등 외부 서비스 래핑만
33| @RequiredArgsConstructor
34| public class AuthenticationService {
35|   private final Auth0Client auth0Client;  // 외부 SDK
36|
37|   public AuthResult authenticate(Credentials creds) {
38|     return auth0Client.login(creds.username(), creds.password());
39|   }
40| }
41|
42| package com.ecommerce.payment;
43|
44| // Stripe, Toss 등 외부 서비스 래핑
45| @RequiredArgsConstructor
46| public class PaymentGateway {
47|   private final StripeClient stripeClient;  // 외부 SDK
48|
49|   public PaymentResult charge(PaymentRequest req) {
50|     return stripeClient.charge(req.amount(), req.cardToken());
51|   }
52| }
```

### 이해를 위한 부가 상세

**[표 01.2]** Subdomain 유형별 전략
| Subdomain 유형 | 투자 수준 | 개발 전략 | 예시 |
|---------------|----------|----------|------|
| Core | 최고 | 직접 개발, 최고 인력 배치 | 추천 알고리즘, 가격 최적화 |
| Supporting | 중간 | 자체 개발하되 표준 패턴 사용 | 주문 관리, 재고 관리 |
| Generic | 최소 | 외부 솔루션/SaaS 활용 | 인증, 결제, 이메일 |

### 꼭 기억할 것
1. **Core 식별**: "이것이 없으면 우리 비즈니스가 경쟁력을 잃는가?"에 Yes면 Core
2. **Generic은 사지 마라, 빌려라**: 결제, 인증, 이메일은 SaaS로. 직접 구현은 리소스 낭비
3. **팀 배치**: Core Domain 팀에 시니어, Generic은 주니어 또는 외주

---

## 3. Bounded Context and Context Mapping (바운디드 컨텍스트와 컨텍스트 매핑)

### 핵심 개념
- **관련 키워드**: Bounded Context, Ubiquitous Language, Context Mapping, Anti-Corruption Layer
- **통찰**: 같은 단어가 맥락에 따라 다른 의미를 가진다. "고객"이 주문팀에서는 "배송받는 사람"이고, 정산팀에서는 "돈을 주는 사람"이다.
- **설명**: Bounded Context는 특정 용어가 일관된 의미를 가지는 경계입니다. 이 경계 안에서는 Ubiquitous Language(공통 언어)가 통용됩니다. 경계 간 소통은 Context Mapping을 통해 명시적으로 관리합니다.

**[그림 01.3]** Bounded Context: 같은 용어, 다른 의미
```
+----------------+   +----------------+   +----------------+
|   Auth Context |   |  Order Context |   | Settle Context |
|----------------|   |----------------|   |----------------|
|                |   |                |   |                |
|  +----------+  |   |  +----------+  |   |  +----------+  |
|  |  User    |  |   |  | Customer |  |   |  |  Payee   |  |
|  |----------|  |   |  |----------|  |   |  |----------|  |
|  | id       |  |   |  | id       |  |   |  | id       |  |
|  | username |  |   |  | address  |  |   |  | bankAcct |  |
|  | password |  |   |  | phone    |  |   |  | taxId    |  |
|  | roles    |  |   |  +----------+  |   |  +----------+  |
|  +----------+  |   |                |   |                |
|                |   |  Same person,  |   |  Same person,  |
|  "Who can      |   |  different     |   |  different     |
|   login?"      |   |  attributes    |   |  attributes    |
+----------------+   +----------------+   +----------------+
        |                   |                    |
        +-------------------+--------------------+
                            |
                    Context Mapping
                    (explicit translation)
```

### 개념이 아닌 것
- **"BC = 마이크로서비스 = 팀"**: BC는 언어적 경계, 서비스는 배포 단위, 팀은 조직 단위. 상관관계는 있지만 동일하지 않음
- **"BC 간 직접 호출하면 안 됨"**: 호출 가능하지만, 반드시 명시적 변환(ACL)을 거쳐야 함

### Before: 단일 통합 모델

**[코드 01.6]** 모든 맥락에서 같은 User 타입 사용
```java
 1| // package: com.ecommerce.shared
 2| // [X] 모든 맥락에서 같은 User 타입 사용
 3| @Entity
 4| public class User {
 5|   @Id private Long id;
 6|   private String username;
 7|   private String password;      // Auth에만 필요
 8|   private String address;       // Order에만 필요
 9|   private String phone;         // Order에만 필요
10|   private String bankAccount;   // Settlement에만 필요
11|   private String taxId;         // Settlement에만 필요
12|   private Set<Role> roles;      // Auth에만 필요
13|   // 모든 필드가 합쳐져 God Entity
14| }
```
- **의도 및 코드 설명**: 하나의 User 타입이 모든 컨텍스트의 요구사항을 담음
- **뭐가 문제인가**:
  - User 수정 시 모든 컨텍스트에 영향
  - 인증 팀이 배송 주소 필드를 잘못 건드릴 수 있음
  - 필드가 30개가 넘어가면 관리 불가

### After: Modern Approach

**[코드 01.7]** Bounded Context별 독립 모델
```java
 1| // [O] Bounded Context별 독립 모델
 2|
 3| // --- Auth Context ---
 4| package com.ecommerce.auth;
 5|
 6| public record AppUser(
 7|   UserId id,
 8|   Username username,
 9|   PasswordHash passwordHash,
10|   Set<Role> roles
11| ) {}
12|
13| // --- Order Context ---
14| package com.ecommerce.order;
15|
16| public record Customer(
17|   CustomerId id,
18|   ShippingAddress address,
19|   PhoneNumber phone
20| ) {}
21|
22| // --- Settlement Context ---
23| package com.ecommerce.settlement;
24|
25| public record Payee(
26|   PayeeId id,
27|   BankAccount bankAccount,
28|   TaxId taxId
29| ) {}
30|
31| // --- Anti-Corruption Layer: 컨텍스트 간 변환 ---
32| package com.ecommerce.order.acl;
33|
34| public class AuthContextTranslator {
35|   public static CustomerId toCustomerId(AppUser appUser) {
36|     return new CustomerId(appUser.id().value());
37|   }
38|
39|   public static Optional<Customer> fromUserService(UserServiceResponse response) {
40|     // 외부 컨텍스트의 모델을 우리 컨텍스트 모델로 변환
41|     // 불필요한 필드는 무시, 필요한 필드만 추출
42|     return Optional.of(new Customer(
43|       new CustomerId(response.getId()),
44|       new ShippingAddress(response.getAddress()),
45|       new PhoneNumber(response.getPhone())
46|     ));
47|   }
48| }
```

### Context Mapping 패턴들

**[표 01.3]** Context Mapping 패턴
| 패턴 | 설명 | 사용 시점 |
|------|------|----------|
| **Shared Kernel** | 두 BC가 공유하는 작은 모델 | 긴밀하게 협력하는 두 팀 |
| **Customer-Supplier** | 하류 BC가 상류 BC에 요구사항 전달 | 상류 팀이 하류 팀의 요구를 반영할 의향 있을 때 |
| **Conformist** | 하류 BC가 상류 모델을 그대로 수용 | 상류 팀 변경이 불가능할 때 |
| **Anti-Corruption Layer (ACL)** | 하류 BC가 변환 계층으로 상류 모델 격리 | 레거시 또는 외부 시스템과 통합 시 |
| **Open Host Service** | 상류 BC가 표준화된 API 제공 | 다수의 하류 BC가 있을 때 |
| **Published Language** | 교환용 표준 언어(JSON Schema 등) 정의 | 여러 BC 간 공식 계약 필요 시 |

### 꼭 기억할 것
1. **같은 단어 ≠ 같은 의미**: "User"가 컨텍스트마다 다른 타입이어야 정상
2. **ACL은 필수**: 외부 시스템 모델이 내 도메인을 오염시키면 안 됨
3. **BC 경계 = 팀 경계**: 가능하면 BC와 팀을 일치시켜 커뮤니케이션 비용 최소화

---

## 4. Tactical DDD Building Blocks (전술적 DDD 빌딩 블록)

### 핵심 개념
- **관련 키워드**: Entity, Value Object, Aggregate, Repository, Domain Service, Domain Event
- **통찰**: 전술적 DDD는 도메인 모델을 코드로 표현하는 빌딩 블록을 제공한다.
- **설명**: 전술적 DDD의 6가지 빌딩 블록은 코드 수준에서 도메인을 모델링하는 도구입니다. 각 블록은 특정 역할이 있으며, 함께 사용하여 복잡한 도메인을 표현합니다.

**[그림 01.4]** Tactical DDD 빌딩 블록
```
+-------------------------------------------------------------+
|                     AGGREGATE (Order)                       |
|-------------------------------------------------------------|
|                                                             |
|  +-----------------+       +------------------+             |
|  | AGGREGATE ROOT  |       | VALUE OBJECTS    |             |
|  | (Order Entity)  |       |------------------|             |
|  |-----------------|       | - OrderId        |             |
|  | - id: OrderId   |       | - Money          |             |
|  | - status        |<------| - Address        |             |
|  | - totalAmount   |       +------------------+             |
|  +------------------+                                       |
|          |                                                  |
|          | contains                                         |
|          v                                                  |
|  +------------------+                                       |
|  | ENTITY           |                                       |
|  | (OrderItem)      |                                       |
|  |------------------|                                       |
|  | - id: ItemId     |   Consistency boundary: only          |
|  | - productId      |   Aggregate Root can be referenced    |
|  | - quantity       |   from outside                        |
|  +------------------+                                       |
+-------------------------------------------------------------+
           |                                      |
           | persisted by                         | emits
           v                                      v
+------------------+                    +------------------+
| REPOSITORY       |                    | DOMAIN EVENT     |
|------------------|                    |------------------|
| - save(Order)    |                    | OrderPlaced      |
| - findById(id)   |                    | OrderPaid        |
+------------------+                    +------------------+
           ^                                      ^
           |                                      |
           +----------------+---------------------+
                            |
                      uses  |
                            |
                  +------------------+
                  | USE CASE         |
                  | (Application)    |
                  |------------------|
                  | - orchestration  |
                  | - Repository     |<----+
                  | - DomainService  |     |
                  +------------------+     |
                            |              |
                      calls |              |
                            v              |
                  +------------------+     |
                  | DOMAIN SERVICE   |-----+
                  |------------------|  operates on
                  | - stateless      |  Aggregate data
                  | - pure functions |
                  | - multi-Aggregate|
                  |   coordination   |
                  +------------------+
```

### 4.1 Entity (엔티티)

**[코드 01.8]** Entity: 식별자 기반 동등성
```java
 1| // package: com.ecommerce.order
 2| // [O] Entity: ID가 같으면 동일 객체
 3| public record Order(
 4|   OrderId id,  // 식별자
 5|   CustomerId customerId,
 6|   List<OrderItem> items,
 7|   Money totalAmount,
 8|   OrderStatus status
 9| ) {
10|   public Order { items = List.copyOf(items); }
11|
12|   // Entity 동등성: ID만으로 판단
13|   @Override
14|   public boolean equals(Object o) {
15|     return o instanceof Order other && this.id.equals(other.id);
16|   }
17|
18|   @Override
19|   public int hashCode() { return id.hashCode(); }
20| }
```

### 4.2 Value Object (값 객체)

**[코드 01.9]** Value Object: 값 기반 동등성 (→ Ch.02 상세)
```java
 1| // package: com.ecommerce.shared
 2| // [O] Value Object: 모든 필드가 같으면 동일
 3| public record Money(BigDecimal amount, Currency currency) {
 4|   public Money {
 5|     if (amount.compareTo(BigDecimal.ZERO) < 0)
 6|       throw new IllegalArgumentException("음수 금액 불가");
 7|   }
 8|
 9|   public Money add(Money other) {
10|     if (this.currency != other.currency)
11|       throw new IllegalArgumentException("통화 불일치");
12|     return new Money(amount.add(other.amount), currency);
13|   }
14| }
```

### 4.3 Aggregate (애그리거트)

**[코드 01.10]** Aggregate: 일관성 경계
```java
 1| // package: com.ecommerce.order
 2| // [O] Aggregate: Order가 Root, OrderItem은 내부 Entity
 3|
 4| // Aggregate Root - 외부에서는 이것만 참조 가능
 5| public record Order(
 6|   OrderId id,
 7|   CustomerId customerId,
 8|   List<OrderItem> items,  // 내부 Entity
 9|   Money totalAmount,
10|   OrderStatus status
11| ) {
12|   public Order { items = List.copyOf(items); }
13|
14|   // 아이템 추가도 Root를 통해서만
15|   public Order addItem(ProductId productId, Quantity qty, Money unitPrice) {
16|     var newItem = new OrderItem(ItemId.generate(), productId, qty, unitPrice);
17|     var newItems = new ArrayList<>(items);
18|     newItems.add(newItem);
19|     return new Order(id, customerId, List.copyOf(newItems),
20|       recalculateTotal(newItems), status);
21|   }
22|
23|   private Money recalculateTotal(List<OrderItem> items) {
24|     return items.stream()
25|       .map(OrderItem::lineTotal)
26|       .reduce(Money.zero(Currency.KRW), Money::add);
27|   }
28| }
29|
30| // 내부 Entity - 외부에서 직접 참조 불가
31| public record OrderItem(
32|   ItemId id,
33|   ProductId productId,
34|   Quantity quantity,
35|   Money unitPrice
36| ) {
37|   public Money lineTotal() {
38|     return unitPrice.multiply(quantity.value());
39|   }
40| }
```

### 4.4 Repository (리포지토리)

**[코드 01.11]** Repository: 컬렉션 추상화 (DAO와 구분)
```java
 1| // package: com.ecommerce.order
 2| // [O] Repository: Aggregate Root 단위 저장/조회
 3|
 4| // 도메인 계층 인터페이스
 5| public interface OrderRepository {
 6|   Optional<Order> findById(OrderId id);
 7|   void save(Order order);
 8|   List<Order> findByCustomerId(CustomerId customerId);
 9|   // DAO와 달리 SQL 쿼리가 아닌 도메인 메서드
10| }
11|
12| // 인프라 계층 구현
13| @Repository
14| @RequiredArgsConstructor
15| public class JpaOrderRepository implements OrderRepository {
16|   private final OrderJpaRepository jpa;  // Spring Data JPA
17|   private final OrderMapper mapper;
18|
19|   @Override
20|   public Optional<Order> findById(OrderId id) {
21|     return jpa.findById(id.value())
22|       .map(mapper::toDomain);  // Entity -> Domain Record
23|   }
24|
25|   @Override
26|   public void save(Order order) {
27|     jpa.save(mapper.toEntity(order));  // Domain Record -> Entity
28|   }
29| }
```

**[표 01.4]** Repository vs DAO
| 관점 | Repository | DAO |
|------|-----------|-----|
| 추상화 수준 | 도메인 개념 (Order) | 데이터 개념 (row) |
| 반환 타입 | Domain Aggregate | Entity/DTO |
| 메서드 | `findByCustomer()` | `selectByCustomerId()` |
| 계층 | 도메인 인터페이스 + 인프라 구현 | 인프라에 위치 |

### 4.5 Domain Service (도메인 서비스)

**[코드 01.12]** Domain Service: 상태 없는 도메인 로직
```java
 1| // package: com.ecommerce.order
 2| // [O] Domain Service: 여러 Aggregate 협력이 필요한 로직
 3|
 4| public class OrderDomainService {
 5|   // 순수 함수: 외부 의존 없음
 6|   public static Result<Order, OrderError> validateAndCreate(
 7|     CreateOrderCommand cmd,
 8|     Customer customer,
 9|     StockInfo stockInfo,
10|     List<PricingRule> pricingRules
11|   ) {
12|     // 여러 Aggregate/VO를 조합하는 도메인 로직
13|     return validateStock(cmd.items(), stockInfo)
14|       .flatMap(_ -> calculatePricing(cmd.items(), pricingRules))
15|       .map(pricing -> new Order(
16|         OrderId.generate(),
17|         customer.id(),
18|         cmd.items(),
19|         pricing.total(),
20|         new Pending(LocalDateTime.now())
21|       ));
22|   }
23|
24|   private static Result<Void, OrderError> validateStock(
25|     List<OrderItem> items, StockInfo stock
26|   ) { /* ... */ }
27|
28|   private static Result<Pricing, OrderError> calculatePricing(
29|     List<OrderItem> items, List<PricingRule> rules
30|   ) { /* ... */ }
31| }
```

### 4.6 Domain Event (도메인 이벤트) → Ch.15 상세

**[코드 01.13]** Domain Event: 과거 시제로 표현
```java
 1| // package: com.ecommerce.order
 2| // [O] Domain Event: 도메인에서 발생한 중요한 사건
 3|
 4| public sealed interface OrderEvent {
 5|   OrderId orderId();
 6|   LocalDateTime occurredAt();
 7|
 8|   record OrderPlaced(
 9|     OrderId orderId,
10|     CustomerId customerId,
11|     List<OrderItem> items,
12|     Money totalAmount,
13|     LocalDateTime occurredAt
14|   ) implements OrderEvent {}
15|
16|   record OrderPaid(
17|     OrderId orderId,
18|     PaymentId paymentId,
19|     LocalDateTime occurredAt
20|   ) implements OrderEvent {}
21|
22|   record OrderShipped(
23|     OrderId orderId,
24|     TrackingNumber tracking,
25|     LocalDateTime occurredAt
26|   ) implements OrderEvent {}
27| }
```

### 꼭 기억할 것
1. **Entity vs Value Object**: ID로 구분하면 Entity, 값으로 구분하면 VO
2. **Aggregate = 일관성 경계**: 외부에서는 Root만 참조, 트랜잭션 = Aggregate 단위
3. **Repository ≠ DAO**: Repository는 도메인 개념을 반환, DAO는 데이터 행을 반환
4. **Domain Service**: 여러 Aggregate가 협력해야 할 때, 상태 없는 순수 함수로

---

## 5. Traditional DDD vs Modern Paradigms (전통적 DDD vs 현대 패러다임)

### 핵심 개념
- **관련 키워드**: Traditional DDD, Functional DDD, DOP, Immutability, Event Sourcing
- **통찰**: DDD의 개념은 변하지 않지만, 구현 방식은 패러다임에 따라 달라진다.
- **설명**: Eric Evans의 원래 DDD(2003)는 가변 객체 기반 OOP를 가정했습니다. 현대의 FP/DOP 패러다임에서는 같은 개념을 불변 Record와 순수 함수로 구현합니다.

**[표 01.5]** Traditional DDD vs DMMF vs DOP
| 개념 | Traditional DDD | DMMF (FP) | DOP |
|------|-----------------|-----------|-----|
| **Entity** | Mutable class, setter | Immutable record + wither | Record + 외부 Calculations |
| **Value Object** | Immutable class | Record + 도메인 메서드 | Record만 (메서드 없음) |
| **Aggregate** | Mutable root, 내부 수정 | Event-sourced, fold | Immutable + 외부 함수 |
| **Repository** | Interface + JPA Entity | 동일 | 동일 |
| **Domain Service** | Class + DI | Static pure functions | Calculations class |
| **상태 변경** | setter, mutate | wither (새 객체) | 외부 함수 (새 객체) |
| **동시성** | synchronized, lock | 자동 (불변) | 자동 (불변) |

### Traditional DDD vs Modern: 코드 비교

**[코드 01.14]** Traditional DDD (가변)
```java
 1| // package: com.ecommerce.order
 2| // Traditional DDD: 가변 Entity
 3| public class Order {
 4|   private Long id;
 5|   private OrderStatus status;
 6|   private List<OrderItem> items = new ArrayList<>();
 7|
 8|   public void addItem(OrderItem item) {
 9|     this.items.add(item);  // 내부 상태 변경
10|     recalculateTotal();
11|   }
12|
13|   public void pay(PaymentId paymentId) {
14|     if (this.status != OrderStatus.PENDING)
15|       throw new IllegalStateException("결제 불가 상태");
16|     this.status = OrderStatus.PAID;  // 상태 변경
17|     this.paymentId = paymentId;
18|   }
19| }
```

**[코드 01.15]** Modern (불변 + 순수 함수)
```java
 1| // package: com.ecommerce.order
 2| // Modern: 불변 Record + 순수 함수
 3| public record Order(
 4|   OrderId id,
 5|   List<OrderItem> items,
 6|   Money totalAmount,
 7|   OrderStatus status
 8| ) {
 9|   public Order { items = List.copyOf(items); }
10| }
11|
12| public class OrderTransitions {
13|   public static Order addItem(Order order, OrderItem item) {
14|     var newItems = new ArrayList<>(order.items());
15|     newItems.add(item);
16|     return new Order(
17|       order.id(),
18|       List.copyOf(newItems),
19|       recalculateTotal(newItems),
20|       order.status()
21|     );  // 새 객체 반환
22|   }
23|
24|   public static Result<Order, OrderError> pay(Order order, PaymentId paymentId) {
25|     return switch (order.status()) {
26|       case Pending _ -> Result.success(
27|         new Order(order.id(), order.items(), order.totalAmount(),
28|           new Paid(LocalDateTime.now(), paymentId)));  // 새 객체 반환
29|       default -> Result.failure(new OrderError.CannotPay(order.id()));
30|     };
31|   }
32| }
```

### FP/DOP 연결점 요약

**[표 01.6]** DDD 개념과 FP/DOP 구현 연결
| DDD 개념 | FP/DOP 구현 | 상세 챕터 |
|---------|------------|----------|
| Value Object | `record` + Compact Constructor | Ch.02 |
| Entity Identity | `record` + ID 기반 equals 오버라이드 | Ch.02, 03 |
| Aggregate State | `sealed interface` (Sum Type) | Ch.03, 04 |
| Repository | Functional Core / Imperative Shell | Ch.07 |
| Domain Event | Event Sourcing | Ch.15 |
| Domain Service | Pure functions, Calculations | Ch.02, 06 |

### 꼭 기억할 것
1. **DDD 개념은 동일**: Entity, VO, Aggregate 개념은 패러다임과 무관
2. **구현만 다름**: 가변 → 불변, 메서드 → 외부 순수 함수
3. **이점**: 불변 + 순수 함수 = 동시성 안전, 테스트 용이, 히스토리 추적

---

## 6. DDD Anti-Patterns (DDD 안티패턴)

### 핵심 개념
- **관련 키워드**: Anemic Domain Model, Big Ball of Mud, Smart UI, Database-Driven Design
- **통찰**: DDD를 잘못 적용하면 기존보다 더 나쁜 결과를 초래할 수 있다.
- **설명**: DDD 안티패턴은 DDD를 적용한다고 하면서 실제로는 핵심 원칙을 위반하는 경우입니다. 이를 인식하고 피해야 합니다.

**[표 01.7]** DDD 안티패턴
| 안티패턴 | 증상 | 원인 | 해결책 |
|---------|------|------|--------|
| **Anemic Domain Model** | Entity에 getter/setter만, 도메인 규칙이 Service에 산재 | "객체는 데이터" 사고 | 타입 시스템으로 불변식/상태 전이 캡처 (아래 상세) |
| **Big Ball of Mud** | 경계 없는 거대한 모놀리스 | BC 분리 실패 | Context Mapping으로 분리 |
| **Smart UI** | UI 코드에 비즈니스 로직 | 빠른 개발 압박 | 도메인 계층 분리 |
| **Database-Driven Design** | 테이블 구조가 도메인 모델 결정 | DBA 주도 설계 | 도메인 먼저, 영속화는 나중 |

### Anemic Domain Model (빈약한 도메인 모델)

**[코드 01.16]** Anemic Domain Model
```java
 1| // package: com.ecommerce.order
 2| // [X] Anemic: Entity = 데이터 홀더, Service = 로직 전부
 3|
 4| @Entity
 5| public class Order {
 6|   @Id private Long id;
 7|   private BigDecimal amount;
 8|   private String status;
 9|   // getter/setter만
10| }
11|
12| @Service
13| public class OrderService {
14|   public void pay(Order order) {
15|     if (!"PENDING".equals(order.getStatus()))  // 도메인 규칙이 Service에
16|       throw new IllegalStateException();
17|     order.setStatus("PAID");  // 상태 전이 로직도 Service에
18|   }
19| }
```

### Anemic Model vs DOP: 무엇이 다른가?

얼핏 보면 DOP도 "데이터(Record)와 로직(Calculations) 분리"이니 Anemic처럼 보입니다. 차이점은 **도메인 지식이 어디에 캡처되는가**입니다.

**[표 01.8]** Anemic Model vs DOP 비교
| 관점 | Anemic Model (안티패턴) | DOP (권장) |
|------|------------------------|-----------|
| **타입 정의** | `String status` (원시 타입) | `sealed interface OrderStatus` (Sum Type) |
| **불변식** | Service에서 if 문으로 검사 | Compact Constructor에서 강제 |
| **상태 전이 규칙** | Service에 산재된 if/else | switch 표현식의 망라적 매칭 |
| **컴파일 타임 검증** | 없음 | 타입 시스템이 잘못된 상태 방지 |
| **도메인 전문가 이해** | 코드 읽기 어려움 | 타입 정의만 보면 도메인 이해 |

**핵심 차이**:
- Anemic: 도메인 규칙이 **절차적 코드에 숨겨져** 있음
- DOP: 도메인 규칙이 **타입 정의에 명시적으로** 드러남

**[코드 01.17]** Anemic vs DOP: 상태 검사 비교
```java
 1| // Anemic: 상태가 String, 규칙이 Service에 숨어있음
 2| if ("PENDING".equals(order.getStatus())) { /* ... */ }
 3|
 4| // DOP: 상태가 타입, 컴파일러가 모든 케이스 강제
 5| switch (order.status()) {
 6|   case Pending p -> /* ... */    // 컴파일러가 Paid, Shipped, Cancelled 누락 경고
 7|   case Paid p -> /* ... */
 8|   case Shipped s -> /* ... */
 9|   case Cancelled c -> /* ... */
10| }
```

**[코드 01.18]** Rich Domain Model (DMMF 스타일 해결책)
```java
 1| // package: com.ecommerce.order
 2| // [O] Rich Domain Model: 상태 전이 함수를 타입에 포함 (DMMF 스타일)
 3| // 주의: DOP 스타일에서는 이 함수를 OrderTransitions 클래스로 분리
 4| // 아래는 DMMF 스타일의 예시임
 5|
 6| public record Order(OrderId id, Money amount, OrderStatus status) {
 7|   // 상태 전이 함수: 새 Order 반환 (불변)
 8|   // 비즈니스 계산(가격, 할인 등)은 외부 Calculations로 분리 권장
 9|   public Result<Order, OrderError> pay(PaymentId paymentId) {
10|     return switch (status) {
11|       case Pending _ -> Result.success(
12|         new Order(id, amount, new Paid(LocalDateTime.now(), paymentId)));
13|       default -> Result.failure(new OrderError.CannotPay(id));
14|     };
15|   }
16| }
```

### JPA @Entity vs DDD Entity: 같은 이름, 다른 개념

**[표 01.9]** JPA @Entity vs DDD Entity
| 관점 | JPA @Entity | DDD Entity |
|------|-------------|------------|
| **목적** | 테이블 매핑 (영속화) | 도메인 모델링 (식별자 기반 객체) |
| **위치** | 인프라 계층 | 도메인 계층 |
| **가변성** | 보통 가변 (setter) | 원칙적으로 불변 권장 |
| **예시** | `@Entity class OrderEntity` | `record Order(OrderId id, ...)` |

실무에서 두 개념을 혼용하면 도메인 모델이 DB 스키마에 종속됩니다. 권장 접근:
- **Domain Model**: `record Order` (순수 도메인, JPA 어노테이션 없음)
- **JPA Entity**: `@Entity class OrderEntity` (영속화 전용)
- **Mapper**: 두 모델 간 변환

### Database-Driven Design (데이터베이스 주도 설계)

**[코드 01.19]** Database-Driven Design
```java
 1| // package: com.ecommerce.order
 2| // [X] Database-Driven: 테이블 구조가 도메인을 결정
 3|
 4| // 테이블: orders (id, status_code, amount, customer_id, ...)
 5| // 테이블: order_items (id, order_id, product_id, qty, ...)
 6|
 7| // 도메인 모델이 테이블 구조와 1:1 매핑
 8| @Entity
 9| @Table(name = "orders")
10| public class Order {
11|   @Id @GeneratedValue private Long id;  // DB가 결정
12|   @Column(name = "status_code") private Integer statusCode;  // 숫자 코드?
13|   @ManyToOne @JoinColumn private Customer customer;  // JPA 연관관계
14| }
```

**[코드 01.20]** Domain-First Design (해결책)
```java
 1| // package: com.ecommerce.order
 2| // [O] Domain-First: 도메인 모델을 먼저, 영속화는 나중
 3|
 4| // 도메인 모델 (영속화 관심 없음)
 5| public sealed interface OrderStatus permits Pending, Paid, Shipped {}
 6| public record Order(OrderId id, CustomerId customerId, OrderStatus status) {}
 7|
 8| // JPA Entity (인프라 계층)
 9| @Entity
10| @Table(name = "orders")
11| class OrderEntity {
12|   @Id private UUID id;
13|   private String status;  // "PENDING", "PAID", ...
14|   private UUID customerId;
15| }
16|
17| // Mapper (도메인 <-> Entity 변환)
18| class OrderMapper {
19|   Order toDomain(OrderEntity e) {
20|     OrderStatus status = switch (e.getStatus()) {
21|       case "PENDING" -> new Pending(e.getCreatedAt());
22|       case "PAID" -> new Paid(e.getPaidAt(), new PaymentId(e.getPaymentId()));
23|       // ...
24|     };
25|     return new Order(new OrderId(e.getId()), new CustomerId(e.getCustomerId()), status);
26|   }
27| }
```

### 꼭 기억할 것
1. **Anemic 판별**: Entity에 getter/setter만 있고, Service에 if/else가 가득하면 Anemic
2. **BC 없이 DDD 없다**: 전술적 패턴만 쓰고 전략 무시하면 Big Ball of Mud
3. **도메인 먼저**: 테이블 설계 전에 도메인 모델을 먼저 정의

---

## 정리: 이 챕터에서 다룬 DDD 개념과 이후 챕터 연결

**[표 01.10]** DDD 개념 → 후속 챕터 매핑
| DDD 개념 | 핵심 내용 | 상세 챕터 |
|---------|----------|----------|
| Value Object | 값 기반 불변 타입 | Ch.02 (Value Objects) |
| Entity + Identity | ID 기반 동등성 | Ch.02, 03 |
| Sum Type (상태) | sealed interface로 상태 모델링 | Ch.03 (ADT), Ch.04 (Impossible States) |
| Aggregate | 일관성 경계, 트랜잭션 단위 | Ch.07 (Architecture), Ch.13 (Integration) |
| Domain Service | 순수 함수로 도메인 로직 | Ch.02, Ch.06 (Pipeline) |
| Repository | Functional Core / Imperative Shell | Ch.07 (Architecture) |
| Domain Event | Event Sourcing, CQRS | Ch.15 (Event Sourcing) |
| Ubiquitous Language | 코드가 곧 문서 | Ch.02 (Wrapped Types) |

---

> **다음 챕터**: [02. Foundations & Paradigm Shift](02-foundations-paradigm-shift.md) - DOP 4원칙과 패러다임 전환
