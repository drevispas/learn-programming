# 14. Event Sourcing (이벤트 소싱)

> **Sources**: DOP Ch.9 (Event Sourcing), DMMF Ch.11 (Event-Driven Architecture), FP 관점의 fold 패턴

---

## 1. Event Store Fundamentals (이벤트 저장소 기초)

### 핵심 개념
- **관련 키워드**: Event Store, Append-only, Immutable Log, Audit Trail, Time Travel
- **통찰**: 상태를 직접 저장하는 대신 상태 변화를 일으킨 이벤트들을 순서대로 저장하면, 완전한 이력을 보존하면서도 현재 상태를 언제든 재구성할 수 있다.
- **설명**: 전통적인 CRUD 방식은 현재 상태만 저장한다. "주문 상태: 배송중"이라면, 이전에 "결제 완료"였는지, 언제 변경되었는지 알 수 없다. Event Sourcing은 발생한 모든 사건(Event)을 append-only 로그로 저장한다.

  핵심 원리: "상태는 이벤트의 함수다. State = f(Events)"

  이 방식의 장점:
  - 완전한 감사 로그 (audit trail) - 모든 변경 이력 추적
  - 시간 여행 (time travel) - 특정 시점의 상태 재현
  - 이벤트 재생 (replay) - 버그 수정 후 과거 이벤트를 새 로직으로 재처리
  - 도메인 이해 향상 - 비즈니스에서 실제로 일어나는 "사건"을 명시적으로 모델링

**[그림 14.1]** Event Store vs Traditional State Storage
```
TRADITIONAL (CRUD)              EVENT SOURCING
==================              ==============

+-------------+                 +----------------+
| Order       |                 | Event Store    |
|-------------|                 |----------------|
| id: 1       |                 | 1. OrderPlaced |
| status: PAID|  <-- only       | 2. OrderPaid   |  <-- full history
| total: 100  |      current    | 3. ...         |
+-------------+      state      +----------------+
                                       |
 "What was the                         v
  previous status?"             current state =
  --> Unknown!                  fold(events)
```

### 개념이 아닌 것
- **"Event Sourcing = 메시지 큐"**: Event Store는 영구 저장소이고, 메시지 큐는 전달 매체. 둘은 다른 목적
- **"모든 시스템에 필요"**: 감사 로그나 이력 추적이 중요하지 않은 단순 CRUD에는 오버엔지니어링

### Before: Traditional OOP

**[코드 14.1]** Traditional OOP: 상태만 저장 - 변경 이력 손실
```java
 1| // package: com.ecommerce.order
 2| // [X] 상태만 저장 - 변경 이력 손실
 3| @Entity
 4| public class OrderEntity {
 5|   @Id private Long id;
 6|   private String status;           // 현재 상태만 덮어씀
 7|   private BigDecimal total;
 8|   private LocalDateTime updatedAt;
 9|
10|   public void pay(PaymentInfo payment) {
11|     this.status = "PAID";          // 이전 상태 사라짐
12|     this.updatedAt = LocalDateTime.now();
13|   }
14|
15|   public void ship(TrackingNumber tracking) {
16|     this.status = "SHIPPED";       // "언제 결제됐지?" 알 수 없음
17|     this.updatedAt = LocalDateTime.now();
18|   }
19| }
```
- **의도 및 코드 설명**: JPA Entity로 주문의 현재 상태만 저장. 상태 변경 시 이전 값을 덮어씀
- **뭐가 문제인가**:
  - 변경 이력 손실: "언제 결제했지?", "취소 전 상태가 뭐였지?" 답할 수 없음
  - 감사 불가: 규제 요건(금융, 의료)에서 필수인 이력 추적 불가능
  - 디버깅 어려움: 잘못된 상태에 도달한 경로를 알 수 없음
  - 비즈니스 인사이트 손실: "결제 후 평균 배송까지 걸리는 시간" 같은 분석 불가

### After: Modern Approach

**[코드 14.2]** Modern: 이벤트로 상태 변화 기록 - 완전한 이력 보존
```java
 1| // package: com.ecommerce.order
 2| // [O] 이벤트로 상태 변화 기록 - 완전한 이력 보존
 3|
 4| // 1. 도메인 이벤트 정의: sealed interface로 모든 이벤트 타입 열거
 5| public sealed interface OrderEvent {
 6|   record OrderPlaced(
 7|     OrderId orderId, CustomerId customerId, List<OrderItem> items,
 8|     Money total, LocalDateTime occurredAt
 9|   ) implements OrderEvent {}
10|
11|   record OrderPaid(
12|     OrderId orderId, PaymentId paymentId, Money amount,
13|     LocalDateTime occurredAt
14|   ) implements OrderEvent {}
15|
16|   record OrderShipped(
17|     OrderId orderId, TrackingNumber tracking,
18|     LocalDateTime occurredAt
19|   ) implements OrderEvent {}
20|
21|   record OrderDelivered(
22|     OrderId orderId, LocalDateTime deliveredAt
23|   ) implements OrderEvent {}
24|
25|   record OrderCancelled(
26|     OrderId orderId, CancelReason reason,
27|     LocalDateTime occurredAt
28|   ) implements OrderEvent {}
29| }
30|
31| // 2. Event Store 인터페이스
32| public interface EventStore {
33|   void append(String streamId, List<OrderEvent> events);  // append-only
34|   List<OrderEvent> loadStream(String streamId);           // 이벤트 스트림 조회
35|   List<OrderEvent> loadStream(String streamId, int fromVersion); // 특정 버전 이후
36| }
37|
38| // 3. 현재 상태는 이벤트에서 계산 (다음 섹션에서 자세히)
39| OrderState currentState = OrderAggregate.reconstitute(
40|   eventStore.loadStream("order-" + orderId)
41| );
```
- **의도 및 코드 설명**: 이벤트를 sealed interface의 record로 정의. Event Store는 append-only로 이벤트를 저장하고, 현재 상태는 이벤트 스트림에서 계산
- **무엇이 좋아지나**:
  - 완전한 이력: 모든 상태 변화와 발생 시점 기록
  - 감사 로그 자동 확보: 별도 감사 테이블 불필요
  - 시간 여행: 특정 시점의 상태 재현 가능
  - 이벤트 재생: 버그 수정 후 이벤트를 새 로직으로 재처리
  - 디버깅 용이: 상태에 도달한 경로 완전 파악

### 이해를 위한 부가 상세
Event Sourcing에서 이벤트는 **과거 시제**로 명명한다 (`OrderPlaced`, `OrderPaid` - "주문이 생성되었다", "결제되었다"). 이는 이벤트가 이미 발생한 사실(fact)임을 강조한다. 반면 커맨드는 **명령형** (`PlaceOrder`, `PayOrder`)으로, 아직 실행되지 않은 의도를 표현한다.

이벤트는 불변이다. 한번 발생한 사실은 변경할 수 없다. 실수로 잘못된 이벤트가 저장되었다면, "취소" 또는 "보정" 이벤트를 추가로 발행하여 상태를 바로잡는다.

### 틀리기/놓치기 쉬운 부분
- 이벤트 스키마 변경 시 기존 이벤트와의 호환성 관리 필요 (upcasting)
- 이벤트 저장과 외부 시스템 알림을 원자적으로 처리해야 함 (Outbox Pattern)
- 이벤트 양이 많아지면 조회 성능 저하 -> 스냅샷으로 해결 (14.5절)

### 꼭 기억할 것
- 상태 = 이벤트의 함수: `State = fold(initialState, events)`
- 이벤트는 불변의 과거 시제 사실 (OrderPlaced, not OrderWillBePlaced)
- sealed interface로 이벤트 타입을 명시적으로 정의하면 망라적 처리 보장

---

## 2. Aggregate Reconstitution (Aggregate 재구성)

### 핵심 개념
- **관련 키워드**: fold/reduce, Reconstitution, Apply Function, Pure Function, Initial State
- **통찰**: 현재 상태는 초기 상태에 모든 이벤트를 순차적으로 적용(fold)하여 재구성한다. `apply(State, Event) -> State`는 순수 함수다.
- **설명**: Event Sourcing의 핵심은 `apply` 함수다. 이 함수는 현재 상태와 이벤트를 받아 새 상태를 반환하는 순수 함수이다. 초기 상태에서 시작하여 모든 이벤트를 순차적으로 apply하면 현재 상태를 얻는다.

  이것은 함수형 프로그래밍의 `fold/reduce` 패턴과 정확히 일치한다:
  ```
  currentState = events.reduce(initialState, (state, event) -> apply(state, event))
  ```

  apply가 순수 함수이므로:
  - 동일한 이벤트 시퀀스는 항상 동일한 상태를 생성 (결정론적)
  - 테스트가 매우 쉬움 (Mock 불필요)
  - 병렬 재구성 가능 (스냅샷 + 이후 이벤트)

**[그림 14.2]** Aggregate Reconstitution via fold
```
RECONSTITUTION = fold(initialState, events)
============================================

Events:     [E1: OrderPlaced] -> [E2: OrderPaid] -> [E3: OrderShipped]
                   |                   |                   |
                   v                   v                   v
States:    Initial ---apply---> Unpaid ---apply---> Paid ---apply---> Shipped
              {}                {id,items}       {+paymentId}      {+tracking}

Code:
  events.stream().reduce(
    OrderState.initial(),           // Initial state (identity)
    OrderAggregate::apply,          // (State, Event) -> State
    (s1, s2) -> s2                  // Combiner (for parallel)
  )
```

### 개념이 아닌 것
- **"DB에서 상태를 직접 조회"**: Event Sourcing에서 상태는 저장되지 않고, 이벤트에서 계산됨
- **"apply는 복잡한 로직을 포함"**: apply는 상태 전이만 담당. 비즈니스 검증은 Command Handler(Decider)에서

### Before: Traditional OOP

**[코드 14.3]** Traditional OOP: 객체 메서드로 상태 변경 - 이력 추적 불가
```java
 1| // package: com.ecommerce.order
 2| // [X] 객체 메서드로 상태 변경 - 이력 추적 불가
 3| public class Order {
 4|   private String status;
 5|   private PaymentId paymentId;
 6|   private TrackingNumber tracking;
 7|
 8|   public void pay(PaymentId paymentId) {
 9|     this.status = "PAID";
10|     this.paymentId = paymentId;  // 기존 상태 덮어씀
11|   }
12|
13|   public void ship(TrackingNumber tracking) {
14|     this.status = "SHIPPED";
15|     this.tracking = tracking;   // 언제 결제됐는지 모름
16|   }
17| }
18|
19| // DB에서 조회
20| Order order = orderRepository.findById(orderId);
21| // order.status == "SHIPPED" 라면, 결제 시점? 알 수 없음
```
- **의도 및 코드 설명**: 가변 객체의 메서드로 상태를 직접 변경. DB에서 현재 상태만 조회
- **뭐가 문제인가**:
  - 상태 변화 경로 추적 불가
  - 테스트 시 복잡한 셋업 필요 (Mock, DB 초기화)
  - 특정 시점 상태 재현 불가능

### After: Modern Approach

**[코드 14.4]** Modern: fold 패턴으로 상태 재구성 - 순수 함수 기반
```java
 1| // package: com.ecommerce.order
 2| // [O] fold 패턴으로 상태 재구성 - 순수 함수 기반
 3|
 4| // 1. 상태 정의: 불변 record
 5| public sealed interface OrderState {
 6|   record Empty() implements OrderState {}
 7|
 8|   record Placed(
 9|     OrderId id, CustomerId customerId, List<OrderItem> items, Money total
10|   ) implements OrderState {}
11|
12|   record Paid(
13|     OrderId id, CustomerId customerId, List<OrderItem> items, Money total,
14|     PaymentId paymentId, LocalDateTime paidAt
15|   ) implements OrderState {}
16|
17|   record Shipped(
18|     OrderId id, CustomerId customerId, List<OrderItem> items, Money total,
19|     PaymentId paymentId, LocalDateTime paidAt,
20|     TrackingNumber tracking, LocalDateTime shippedAt
21|   ) implements OrderState {}
22|
23|   record Delivered(
24|     OrderId id, CustomerId customerId, List<OrderItem> items, Money total,
25|     PaymentId paymentId, LocalDateTime paidAt,
26|     TrackingNumber tracking, LocalDateTime shippedAt,
27|     LocalDateTime deliveredAt
28|   ) implements OrderState {}
29|
30|   record Cancelled(
31|     OrderId id, CancelReason reason, LocalDateTime cancelledAt
32|   ) implements OrderState {}
33|
34|   static OrderState initial() { return new Empty(); }
35| }
36|
37| // 2. apply 함수: (State, Event) -> State 순수 함수
38| public class OrderAggregate {
39|   public static OrderState apply(OrderState state, OrderEvent event) {
40|     return switch (event) {
41|       case OrderEvent.OrderPlaced e -> new OrderState.Placed(
42|         e.orderId(), e.customerId(), e.items(), e.total()
43|       );
44|       case OrderEvent.OrderPaid e -> {
45|         if (state instanceof OrderState.Placed s) {
46|           yield new OrderState.Paid(
47|             s.id(), s.customerId(), s.items(), s.total(),
48|             e.paymentId(), e.occurredAt()
49|           );
50|         }
51|         throw new IllegalStateException("Cannot pay: " + state);
52|       }
53|       case OrderEvent.OrderShipped e -> {
54|         if (state instanceof OrderState.Paid s) {
55|           yield new OrderState.Shipped(
56|             s.id(), s.customerId(), s.items(), s.total(),
57|             s.paymentId(), s.paidAt(),
58|             e.tracking(), e.occurredAt()
59|           );
60|         }
61|         throw new IllegalStateException("Cannot ship: " + state);
62|       }
63|       case OrderEvent.OrderDelivered e -> {
64|         if (state instanceof OrderState.Shipped s) {
65|           yield new OrderState.Delivered(
66|             s.id(), s.customerId(), s.items(), s.total(),
67|             s.paymentId(), s.paidAt(),
68|             s.tracking(), s.shippedAt(),
69|             e.deliveredAt()
70|           );
71|         }
72|         throw new IllegalStateException("Cannot deliver: " + state);
73|       }
74|       case OrderEvent.OrderCancelled e -> new OrderState.Cancelled(
75|         e.orderId(), e.reason(), e.occurredAt()
76|       );
77|     };
78|   }
79|
80|   // 3. reconstitute: fold를 사용한 상태 재구성
81|   public static OrderState reconstitute(List<OrderEvent> events) {
82|     return events.stream()
83|       .reduce(
84|         OrderState.initial(),
85|         OrderAggregate::apply,
86|         (s1, s2) -> s2  // combiner (parallel 용)
87|       );
88|   }
89| }
90|
91| // 사용 예시
92| List<OrderEvent> events = eventStore.loadStream("order-123");
93| OrderState currentState = OrderAggregate.reconstitute(events);
94| // 모든 이벤트를 통해 현재 상태에 도달한 과정이 명확
```
- **의도 및 코드 설명**: apply는 현재 상태와 이벤트를 받아 새 상태를 반환하는 순수 함수. reconstitute는 fold(reduce)로 초기 상태에서 모든 이벤트를 순차 적용
- **무엇이 좋아지나**:
  - 결정론적: 동일 이벤트 -> 동일 상태
  - 테스트 용이: apply는 순수 함수이므로 Mock 불필요
  - 시간 여행: 원하는 시점까지만 이벤트를 apply하면 해당 시점 상태
  - 디버깅: 각 이벤트 적용 후 상태를 로깅하면 상태 변화 추적 가능

### 이해를 위한 부가 상세
`apply` 함수에서 IllegalStateException을 던지는 것은 **이벤트 스트림이 이미 검증된 후**이므로 실제로는 발생하지 않아야 한다. Command Handler(Decider)에서 비즈니스 규칙을 검증하고, 유효한 경우에만 이벤트가 저장된다. apply에서의 예외는 데이터 손상이나 버그를 나타내는 방어 코드이다.

Monoid 관점에서, `(OrderState, apply, Empty())`는 모노이드를 형성한다. Empty가 항등원이고, apply가 결합법칙을 만족한다. 이 덕분에 병렬 처리가 안전하다.

### 틀리기/놓치기 쉬운 부분
- apply에서 비즈니스 검증을 하지 말 것. 이미 발생한 이벤트는 항상 유효하다고 가정
- 상태 타입(OrderState)과 이벤트 타입(OrderEvent)을 모두 sealed interface로 정의해야 망라성 보장
- 이벤트가 많아지면 재구성 비용 증가 -> 스냅샷으로 해결 (14.5절)

### 꼭 기억할 것
- `apply(State, Event) -> State`는 순수 함수
- `reconstitute = fold(initial, events, apply)`
- apply에서 예외는 데이터 손상 신호, 비즈니스 검증은 Decider에서

---

## 3. Command Handling & Decider Pattern (커맨드 처리)

### 핵심 개념
- **관련 키워드**: Decider, Command, Event, Result, Decide Function, Business Rules
- **통찰**: Decider는 `(State, Command) -> Result<List<Event>, Error>` 형태의 순수 함수로, 비즈니스 규칙을 검증하고 발생할 이벤트를 결정한다.
- **설명**: Event Sourcing에서 상태 변경 요청은 Command로 표현된다. Decider 패턴은 현재 상태와 커맨드를 받아, 비즈니스 규칙을 검증하고, 성공 시 발생할 이벤트 목록을 반환한다.

  ```
  decide(State, Command) -> Result<List<Event>, Error>
  ```

  Decider의 특징:
  - **순수 함수**: 외부 의존성 없이 상태와 커맨드만으로 결정
  - **Result 반환**: 성공 시 이벤트 목록, 실패 시 에러 (예외 대신)
  - **다중 이벤트**: 하나의 커맨드가 여러 이벤트를 발생시킬 수 있음

  이 패턴은 CQRS의 Command 측면과 Railway-Oriented Programming(05장, 06장)을 결합한 것이다.

**[그림 14.3]** Decider Pattern Flow
```
DECIDER: (State, Command) -> Result<Events, Error>
===================================================

                    +----------+
                    | Command  |
                    | (PlaceOrder)
                    +----+-----+
                         |
                         v
+--------+         +-----------+         +----------------+
| State  | ------> | Decider   | ------> | Result         |
| (Empty)|         | (decide)  |         |----------------|
+--------+         +-----------+         | Success:       |
                         |               |  [OrderPlaced] |
                         |               | or             |
                  Business Rules         | Failure:       |
                  - Valid items?         |  InvalidOrder  |
                  - Stock available?     +----------------+
                  - Customer exists?

Code:
  Result<List<OrderEvent>, OrderError> result =
    OrderDecider.decide(currentState, command);
  result.map(events -> eventStore.append(streamId, events));
```

### 개념이 아닌 것
- **"Decider = Service 레이어"**: Decider는 순수 함수, Service는 I/O 포함. Decider는 Functional Core
- **"Command = Event"**: Command는 의도(미래), Event는 사실(과거). `PlaceOrder` vs `OrderPlaced`

### Before: Traditional OOP

**[코드 14.5]** Traditional OOP: 예외 기반 검증 - 부수효과와 로직 혼재
```java
 1| // package: com.ecommerce.order
 2| // [X] 예외 기반 검증 - 부수효과와 로직 혼재
 3| @Service
 4| public class OrderService {
 5|   @Autowired private OrderRepository orderRepository;
 6|   @Autowired private PaymentGateway paymentGateway;
 7|
 8|   @Transactional
 9|   public Order pay(Long orderId, PaymentRequest request) {
10|     Order order = orderRepository.findById(orderId)
11|       .orElseThrow(() -> new OrderNotFoundException(orderId));
12|
13|     if (!"PLACED".equals(order.getStatus())) {
14|       throw new InvalidOrderStateException("Cannot pay");  // 예외로 실패 표현
15|     }
16|
17|     PaymentResult payment = paymentGateway.charge(request); // I/O 섞임
18|     if (!payment.isSuccess()) {
19|       throw new PaymentFailedException(payment.getError());
20|     }
21|
22|     order.setStatus("PAID");
23|     order.setPaymentId(payment.getId());
24|     return orderRepository.save(order);  // 상태 직접 저장
25|   }
26| }
```
- **의도 및 코드 설명**: Service 메서드가 검증, 외부 호출, 상태 변경, 저장을 모두 수행
- **뭐가 문제인가**:
  - 순수 로직과 I/O가 혼재되어 테스트 어려움
  - 예외로 실패를 표현하면 호출자가 처리를 강제받지 않음
  - 비즈니스 규칙이 인프라 코드에 묻혀 파악 어려움
  - 동일 로직 재사용 어려움 (다른 진입점에서)

### After: Modern Approach

**[코드 14.6]** Modern: Decider 패턴 - 순수 함수로 커맨드 처리
```java
 1| // package: com.ecommerce.order
 2| // [O] Decider 패턴 - 순수 함수로 커맨드 처리
 3|
 4| // 1. Command 정의
 5| public sealed interface OrderCommand {
 6|   record PlaceOrder(
 7|     CustomerId customerId, List<OrderItem> items
 8|   ) implements OrderCommand {}
 9|
10|   record PayOrder(
11|     OrderId orderId, PaymentId paymentId, Money amount
12|   ) implements OrderCommand {}
13|
14|   record ShipOrder(
15|     OrderId orderId, TrackingNumber tracking
16|   ) implements OrderCommand {}
17|
18|   record CancelOrder(
19|     OrderId orderId, CancelReason reason
20|   ) implements OrderCommand {}
21| }
22|
23| // 2. Error 정의
24| public sealed interface OrderError {
25|   record OrderNotFound(OrderId id) implements OrderError {}
26|   record InvalidState(String message) implements OrderError {}
27|   record EmptyItems() implements OrderError {}
28|   record AlreadyPaid(OrderId id) implements OrderError {}
29|   record AlreadyCancelled(OrderId id) implements OrderError {}
30| }
31|
32| // 3. Decider: 순수 함수
33| public class OrderDecider {
34|   // (State, Command) -> Result<List<Event>, Error>
35|   public static Result<List<OrderEvent>, OrderError> decide(
36|       OrderState state, OrderCommand command) {
37|     return switch (command) {
38|       case OrderCommand.PlaceOrder cmd -> handlePlace(state, cmd);
39|       case OrderCommand.PayOrder cmd -> handlePay(state, cmd);
40|       case OrderCommand.ShipOrder cmd -> handleShip(state, cmd);
41|       case OrderCommand.CancelOrder cmd -> handleCancel(state, cmd);
42|     };
43|   }
44|
45|   private static Result<List<OrderEvent>, OrderError> handlePlace(
46|       OrderState state, OrderCommand.PlaceOrder cmd) {
47|     // 비즈니스 규칙 검증
48|     if (!(state instanceof OrderState.Empty)) {
49|       return Result.failure(new OrderError.InvalidState("Order already exists"));
50|     }
51|     if (cmd.items().isEmpty()) {
52|       return Result.failure(new OrderError.EmptyItems());
53|     }
54|
55|     // 성공: 이벤트 반환
56|     var event = new OrderEvent.OrderPlaced(
57|       OrderId.generate(),
58|       cmd.customerId(),
59|       cmd.items(),
60|       calculateTotal(cmd.items()),
61|       LocalDateTime.now()
62|     );
63|     return Result.success(List.of(event));
64|   }
65|
66|   private static Result<List<OrderEvent>, OrderError> handlePay(
67|       OrderState state, OrderCommand.PayOrder cmd) {
68|     return switch (state) {
69|       case OrderState.Empty e ->
70|         Result.failure(new OrderError.OrderNotFound(cmd.orderId()));
71|       case OrderState.Placed s -> {
72|         var event = new OrderEvent.OrderPaid(
73|           cmd.orderId(), cmd.paymentId(), cmd.amount(), LocalDateTime.now()
74|         );
75|         yield Result.success(List.of(event));
76|       }
77|       case OrderState.Paid p ->
78|         Result.failure(new OrderError.AlreadyPaid(cmd.orderId()));
79|       case OrderState.Shipped s ->
80|         Result.failure(new OrderError.InvalidState("Already shipped"));
81|       case OrderState.Delivered d ->
82|         Result.failure(new OrderError.InvalidState("Already delivered"));
83|       case OrderState.Cancelled c ->
84|         Result.failure(new OrderError.AlreadyCancelled(cmd.orderId()));
85|     };
86|   }
87|
88|   private static Result<List<OrderEvent>, OrderError> handleShip(
89|       OrderState state, OrderCommand.ShipOrder cmd) {
90|     return switch (state) {
91|       case OrderState.Paid s -> {
92|         var event = new OrderEvent.OrderShipped(
93|           cmd.orderId(), cmd.tracking(), LocalDateTime.now()
94|         );
95|         yield Result.success(List.of(event));
96|       }
97|       default -> Result.failure(
98|         new OrderError.InvalidState("Can only ship paid orders")
99|       );
100|     };
101|   }
102|
103|   private static Result<List<OrderEvent>, OrderError> handleCancel(
104|       OrderState state, OrderCommand.CancelOrder cmd) {
105|     return switch (state) {
106|       case OrderState.Placed s, OrderState.Paid p -> {
107|         var event = new OrderEvent.OrderCancelled(
108|           cmd.orderId(), cmd.reason(), LocalDateTime.now()
109|         );
110|         yield Result.success(List.of(event));
111|       }
112|       case OrderState.Shipped s ->
113|         Result.failure(new OrderError.InvalidState("Cannot cancel shipped order"));
114|       case OrderState.Delivered d ->
115|         Result.failure(new OrderError.InvalidState("Cannot cancel delivered order"));
116|       case OrderState.Cancelled c ->
117|         Result.failure(new OrderError.AlreadyCancelled(cmd.orderId()));
118|       case OrderState.Empty e ->
119|         Result.failure(new OrderError.OrderNotFound(cmd.orderId()));
120|     };
121|   }
122|
123|   private static Money calculateTotal(List<OrderItem> items) {
124|     return items.stream()
125|       .map(item -> item.price().multiply(item.quantity()))
126|       .reduce(Money.ZERO, Money::add);
127|   }
128| }
129|
130| // 4. Application Service: Decider 호출 + I/O
131| public class OrderApplicationService {
132|   private final EventStore eventStore;
133|
134|   public Result<OrderState, OrderError> handle(
135|       String streamId, OrderCommand command) {
136|     // 1. 이벤트 로드 & 상태 재구성
137|     List<OrderEvent> events = eventStore.loadStream(streamId);
138|     OrderState state = OrderAggregate.reconstitute(events);
139|
140|     // 2. Decider 호출 (순수 함수)
141|     return OrderDecider.decide(state, command)
142|       // 3. 성공 시 이벤트 저장
143|       .map(newEvents -> {
144|         eventStore.append(streamId, newEvents);
145|         return OrderAggregate.reconstitute(
146|           Stream.concat(events.stream(), newEvents.stream()).toList()
147|         );
148|       });
149|   }
150| }
```
- **의도 및 코드 설명**: Decider는 순수 함수로 비즈니스 규칙 검증 및 이벤트 결정. Application Service가 I/O(이벤트 로드/저장)를 담당. Functional Core / Imperative Shell 분리
- **무엇이 좋아지나**:
  - Decider는 순수 함수 -> Mock 없이 단위 테스트
  - 비즈니스 규칙이 Decider에 집중 -> 파악 용이
  - Result로 실패 명시 -> 호출자가 처리 강제
  - 동일 Decider를 다른 진입점(REST, Event, CLI)에서 재사용

### 이해를 위한 부가 상세
Decider 패턴의 완전한 시그니처는 다음과 같다:
```java
(State, Command) -> Result<List<Event>, Error>  // decide
(State, Event) -> State                          // apply
State initial()                                  // initial state
```

이 세 함수만으로 Event Sourcing Aggregate의 모든 로직을 표현할 수 있다. 이를 "Decider triad"라고 부르기도 한다.

### 틀리기/놓치기 쉬운 부분
- Decider 내부에서 I/O(DB 조회, 외부 API 호출)를 하면 순수성이 깨짐. 필요한 데이터는 Command에 담아 전달
- 하나의 Command가 여러 Event를 발생시킬 수 있음 (예: 대량 주문 처리)
- Decider의 테스트는 "Given events, When command, Then events or error" 형식으로 작성

### 꼭 기억할 것
- Decider: `(State, Command) -> Result<List<Event>, Error>`
- Command = 의도(미래), Event = 사실(과거)
- Decider는 순수 함수, Application Service가 I/O 담당

---

## 4. Projection & Read Model (프로젝션과 CQRS)

### 핵심 개념
- **관련 키워드**: Projection, Read Model, CQRS, Eventual Consistency, Materialized View
- **통찰**: 이벤트 스트림을 다양한 형태의 읽기 전용 모델(Projection)로 변환하면, 조회 성능과 쿼리 유연성을 확보할 수 있다.
- **설명**: Event Sourcing에서 이벤트 스트림을 매번 fold하여 현재 상태를 구하는 것은 비효율적이다. 또한 집계 쿼리("이번 달 총 매출")나 검색("특정 상품을 주문한 고객 목록") 같은 조회는 이벤트 스트림에서 직접 수행하기 어렵다.

  Projection(프로젝션)은 이벤트 스트림을 소비하여 읽기에 최적화된 별도의 데이터 모델을 구축하는 과정이다. 이는 CQRS(Command Query Responsibility Segregation)의 Query 측면이다.

  핵심 원리:
  - **쓰기 모델**: Event Store (이벤트 append)
  - **읽기 모델**: Projection (이벤트 -> 조회용 테이블/캐시)
  - **동기화**: 이벤트를 구독하여 Projection 업데이트 (Eventual Consistency)

**[그림 14.4]** CQRS with Event Sourcing
```
CQRS: COMMAND SIDE vs QUERY SIDE
=================================

COMMAND SIDE                    QUERY SIDE
(Event Sourcing)                (Projections)

+------------+                  +----------------+
| Command    |                  |  REST / UI     |
+-----+------+                  +-------+--------+
      |                                 |
      v                                 v
+------------+                  +----------------+
| Decider    |                  | Read API       |
+-----+------+                  +-------+--------+
      |                                 |
      v                                 v
+------------+     events       +----------------+
| Event      | --------------> | Projections    |
| Store      |    subscribe     |----------------|
+------------+                  | - OrderSummary |
                                | - DailySales   |
                                | - CustomerStats|
                                +----------------+
                                        |
                                        v
                                +----------------+
                                | Read DB        |
                                | (optimized)    |
                                +----------------+
```

### 개념이 아닌 것
- **"CQRS = 별도 DB 필수"**: 같은 DB의 다른 테이블로도 가능. 분리 수준은 요구사항에 따라 결정
- **"Eventual Consistency = 데이터 불일치"**: 일시적 지연이 있지만, 결국 일관성 달성

### Before: Traditional OOP

**[코드 14.7]** Traditional OOP: 단일 모델로 쓰기와 읽기 - 복잡한 쿼리
```java
 1| // package: com.ecommerce.order
 2| // [X] 단일 모델로 쓰기와 읽기 - 복잡한 쿼리
 3| @Entity
 4| public class OrderEntity {
 5|   @Id private Long id;
 6|   private String status;
 7|   @ManyToOne private CustomerEntity customer;
 8|   @OneToMany private List<OrderItemEntity> items;
 9| }
10|
11| // 복잡한 집계 쿼리
12| public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
13|   // N+1 문제 발생 가능
14|   @Query("SELECT o FROM OrderEntity o JOIN FETCH o.items WHERE o.status = :status")
15|   List<OrderEntity> findByStatus(String status);
16|
17|   // 집계를 위한 복잡한 쿼리
18|   @Query("SELECT SUM(i.price * i.quantity) FROM OrderEntity o " +
19|          "JOIN o.items i WHERE o.createdAt BETWEEN :start AND :end")
20|   BigDecimal calculateTotalSales(LocalDateTime start, LocalDateTime end);
21| }
```
- **의도 및 코드 설명**: 하나의 Entity 모델로 쓰기와 읽기를 모두 처리. 복잡한 조회는 JPQL/SQL로 해결
- **뭐가 문제인가**:
  - 쓰기에 최적화된 모델이 읽기에는 비효율적
  - 복잡한 집계 쿼리로 인한 성능 저하
  - 스키마 변경이 쓰기와 읽기 양쪽에 영향
  - N+1 문제, 복잡한 JOIN

### After: Modern Approach

**[코드 14.8]** Modern: Projection으로 읽기 전용 모델 구축
```java
 1| // package: com.ecommerce.order
 2| // [O] Projection으로 읽기 전용 모델 구축
 3|
 4| // 1. 읽기 전용 모델 (Query에 최적화)
 5| public record OrderSummary(
 6|   String orderId,
 7|   String customerName,
 8|   String status,
 9|   Money total,
10|   LocalDateTime lastUpdated
11| ) {}
12|
13| public record DailySalesReport(
14|   LocalDate date,
15|   int orderCount,
16|   Money totalAmount
17| ) {}
18|
19| // 2. Projection: 이벤트 -> 읽기 모델 변환
20| public class OrderProjection {
21|   private final OrderSummaryRepository summaryRepo;
22|   private final DailySalesRepository salesRepo;
23|
24|   // 이벤트 핸들러: 각 이벤트 타입별로 Projection 업데이트
25|   public void on(OrderEvent event) {
26|     switch (event) {
27|       case OrderEvent.OrderPlaced e -> {
28|         var summary = new OrderSummary(
29|           e.orderId().value(),
30|           fetchCustomerName(e.customerId()),  // 비정규화된 데이터
31|           "PLACED",
32|           e.total(),
33|           e.occurredAt()
34|         );
35|         summaryRepo.save(summary);
36|
37|         // 일별 매출 업데이트
38|         updateDailySales(e.occurredAt().toLocalDate(), e.total());
39|       }
40|       case OrderEvent.OrderPaid e -> {
41|         summaryRepo.updateStatus(e.orderId().value(), "PAID", e.occurredAt());
42|       }
43|       case OrderEvent.OrderShipped e -> {
44|         summaryRepo.updateStatus(e.orderId().value(), "SHIPPED", e.occurredAt());
45|       }
46|       case OrderEvent.OrderDelivered e -> {
47|         summaryRepo.updateStatus(e.orderId().value(), "DELIVERED", e.deliveredAt());
48|       }
49|       case OrderEvent.OrderCancelled e -> {
50|         summaryRepo.updateStatus(e.orderId().value(), "CANCELLED", e.occurredAt());
51|         // 취소 시 일별 매출에서 차감
52|         var summary = summaryRepo.findById(e.orderId().value());
53|         if (summary.isPresent()) {
54|           subtractDailySales(e.occurredAt().toLocalDate(), summary.get().total());
55|         }
56|       }
57|     }
58|   }
59|
60|   private void updateDailySales(LocalDate date, Money amount) {
61|     salesRepo.findById(date)
62|       .ifPresentOrElse(
63|         report -> salesRepo.save(new DailySalesReport(
64|           date, report.orderCount() + 1, report.totalAmount().add(amount)
65|         )),
66|         () -> salesRepo.save(new DailySalesReport(date, 1, amount))
67|       );
68|   }
69|
70|   private void subtractDailySales(LocalDate date, Money amount) {
71|     salesRepo.findById(date)
72|       .ifPresent(report -> salesRepo.save(new DailySalesReport(
73|         date, report.orderCount() - 1, report.totalAmount().subtract(amount)
74|       )));
75|   }
76| }
77|
78| // 3. 읽기 전용 Query Service
79| public class OrderQueryService {
80|   private final OrderSummaryRepository summaryRepo;
81|   private final DailySalesRepository salesRepo;
82|
83|   // 단순 조회 - JOIN 없음, 비정규화된 데이터
84|   public Optional<OrderSummary> findOrder(String orderId) {
85|     return summaryRepo.findById(orderId);
86|   }
87|
88|   public List<OrderSummary> findByStatus(String status) {
89|     return summaryRepo.findByStatus(status);
90|   }
91|
92|   // 집계 - 이미 계산된 값 조회
93|   public DailySalesReport getDailySales(LocalDate date) {
94|     return salesRepo.findById(date)
95|       .orElse(new DailySalesReport(date, 0, Money.ZERO));
96|   }
97|
98|   public Money getMonthlySales(YearMonth month) {
99|     return salesRepo.findByDateBetween(
100|       month.atDay(1), month.atEndOfMonth()
101|     ).stream()
102|       .map(DailySalesReport::totalAmount)
103|       .reduce(Money.ZERO, Money::add);
104|   }
105| }
106|
107| // 4. 이벤트 구독 설정 (Spring 예시)
108| @Component
109| public class OrderEventSubscriber {
110|   private final OrderProjection projection;
111|
112|   @EventListener
113|   public void handleOrderEvent(OrderEvent event) {
114|     projection.on(event);
115|   }
116| }
```
- **의도 및 코드 설명**: Projection이 이벤트를 구독하여 읽기 전용 모델(OrderSummary, DailySalesReport)을 업데이트. Query Service는 단순 조회만 수행
- **무엇이 좋아지나**:
  - 읽기에 최적화된 비정규화 모델 (JOIN 없음)
  - 집계 값이 미리 계산되어 있음 (실시간 집계 불필요)
  - 쓰기 모델 변경이 읽기 모델에 직접 영향 없음
  - 다양한 Projection 추가 가능 (검색용, 분석용, 리포트용)

### 이해를 위한 부가 상세
Projection은 여러 개일 수 있다. 같은 이벤트 스트림에서:
- `OrderSummary`: 주문 상세 조회용
- `DailySalesReport`: 매출 리포트용
- `CustomerOrderHistory`: 고객별 주문 이력용
- `SearchIndex`: Elasticsearch 검색용

각 Projection은 자신이 필요한 이벤트만 처리하고, 자신만의 데이터 구조로 저장한다.

### 틀리기/놓치기 쉬운 부분
- Projection 재구축: 버그 수정이나 스키마 변경 시 처음부터 이벤트를 다시 처리해야 함. 이를 위한 재생(replay) 메커니즘 필요
- Eventual Consistency: Projection 업데이트까지 약간의 지연 발생. UI에서 이를 고려해야 함
- 멱등성: 동일 이벤트가 여러 번 처리되어도 결과가 같아야 함 (이벤트 ID로 중복 체크)

### 꼭 기억할 것
- Projection = 이벤트 -> 읽기 모델 변환
- CQRS: Command(쓰기)와 Query(읽기) 모델 분리
- 다양한 Projection으로 다양한 조회 요구 충족
- Eventual Consistency 고려 필요

---

## 5. Snapshot Optimization (스냅샷 최적화)

### 핵심 개념
- **관련 키워드**: Snapshot, Memento, Performance, Reconstitution, Checkpoint
- **통찰**: 이벤트가 많아지면 재구성 비용이 증가한다. 특정 시점의 상태를 스냅샷으로 저장하면, 해당 시점 이후의 이벤트만 적용하여 재구성 시간을 단축할 수 있다.
- **설명**: 주문에 100개의 이벤트가 있다면, 매번 100개 이벤트를 fold해야 현재 상태를 얻는다. 스냅샷은 특정 시점(예: 50번째 이벤트)의 상태를 저장해두고, 이후에는 스냅샷 + 51번째 이후 이벤트만 적용한다.

  스냅샷 전략:
  - **주기적**: N개 이벤트마다 스냅샷 생성
  - **시간 기반**: 일정 시간마다 스냅샷 생성
  - **수동**: 특정 이벤트(예: 주문 완료) 시 스냅샷 생성

  스냅샷은 순수한 성능 최적화이다. 스냅샷 없이도 시스템은 정확히 동작하며, 스냅샷이 손상되어도 이벤트에서 재구성 가능하다.

**[그림 14.5]** Snapshot Optimization
```
SNAPSHOT = CHECKPOINT FOR RECONSTITUTION
=========================================

Without Snapshot (N events):
  [E1] -> [E2] -> [E3] -> ... -> [E99] -> [E100]
    |                                        |
    +----------- fold 100 events ------------+

With Snapshot (snapshot at E50):
  [Snapshot@E50] -> [E51] -> [E52] -> ... -> [E100]
        |                                      |
        +-------- fold only 50 events ---------+

Snapshot = OrderState serialized at version 50
```

### 개념이 아닌 것
- **"스냅샷 = 필수 기능"**: 이벤트 수가 적다면 불필요. 성능 문제가 실제로 발생할 때 도입
- **"스냅샷이 진실의 원천"**: 이벤트가 진실의 원천. 스냅샷은 캐시에 불과

### Before: Traditional OOP

**[코드 14.9]** Traditional OOP: 캐시 없는 매번 전체 조회
```java
 1| // package: com.ecommerce.order
 2| // [X] 캐시 없는 매번 전체 조회 (성능 문제는 DB 튜닝으로 해결 시도)
 3| public class OrderService {
 4|   private final EventStore eventStore;
 5|
 6|   public OrderState getOrder(String orderId) {
 7|     // 매번 모든 이벤트를 로드하고 fold
 8|     List<OrderEvent> events = eventStore.loadStream("order-" + orderId);
 9|     return OrderAggregate.reconstitute(events);  // 이벤트 1000개면 1000번 apply
10|   }
11| }
```
- **의도 및 코드 설명**: 매 조회마다 전체 이벤트를 로드하여 재구성
- **뭐가 문제인가**:
  - 이벤트가 많은 Aggregate의 재구성 비용 증가
  - 메모리 사용량 증가 (모든 이벤트를 메모리에 로드)
  - 응답 시간 저하

### After: Modern Approach

**[코드 14.10]** Modern: 스냅샷으로 재구성 최적화
```java
 1| // package: com.ecommerce.order
 2| // [O] 스냅샷으로 재구성 최적화
 3|
 4| // 1. 스냅샷 저장소
 5| public record Snapshot<S>(S state, int version) {}
 6|
 7| public interface SnapshotStore {
 8|   <S> Optional<Snapshot<S>> load(String streamId, Class<S> type);
 9|   <S> void save(String streamId, Snapshot<S> snapshot);
10| }
11|
12| // 2. 스냅샷을 활용한 재구성
13| public class OrderAggregateWithSnapshot {
14|   private static final int SNAPSHOT_THRESHOLD = 50;  // 50 이벤트마다 스냅샷
15|
16|   private final EventStore eventStore;
17|   private final SnapshotStore snapshotStore;
18|
19|   public OrderState reconstitute(String streamId) {
20|     // 1. 스냅샷 로드 시도
21|     Optional<Snapshot<OrderState>> snapshot =
22|       snapshotStore.load(streamId, OrderState.class);
23|
24|     // 2. 스냅샷 이후 이벤트만 로드
25|     int fromVersion = snapshot.map(Snapshot::version).orElse(0);
26|     List<OrderEvent> events = eventStore.loadStream(streamId, fromVersion);
27|
28|     // 3. 스냅샷 상태에서 시작하여 이후 이벤트만 적용
29|     OrderState initialState = snapshot
30|       .map(Snapshot::state)
31|       .orElse(OrderState.initial());
32|
33|     return events.stream()
34|       .reduce(initialState, OrderAggregate::apply, (s1, s2) -> s2);
35|   }
36|
37|   public void handle(String streamId, OrderCommand command) {
38|     // 1. 재구성
39|     OrderState state = reconstitute(streamId);
40|
41|     // 2. Decider 호출
42|     Result<List<OrderEvent>, OrderError> result =
43|       OrderDecider.decide(state, command);
44|
45|     result.ifSuccess(newEvents -> {
46|       // 3. 이벤트 저장
47|       eventStore.append(streamId, newEvents);
48|
49|       // 4. 스냅샷 조건 확인 후 저장
50|       int totalEvents = eventStore.getStreamSize(streamId);
51|       if (shouldCreateSnapshot(totalEvents)) {
52|         OrderState newState = OrderAggregate.reconstitute(
53|           eventStore.loadStream(streamId)
54|         );
55|         snapshotStore.save(streamId, new Snapshot<>(newState, totalEvents));
56|       }
57|     });
58|   }
59|
60|   private boolean shouldCreateSnapshot(int eventCount) {
61|     return eventCount % SNAPSHOT_THRESHOLD == 0;
62|   }
63| }
64|
65| // 3. 스냅샷 전략 인터페이스 (선택적 확장)
66| public sealed interface SnapshotStrategy {
67|   boolean shouldSnapshot(int eventCount, OrderState state, OrderEvent lastEvent);
68|
69|   // N개 이벤트마다
70|   record EveryN(int n) implements SnapshotStrategy {
71|     public boolean shouldSnapshot(int count, OrderState s, OrderEvent e) {
72|       return count % n == 0;
73|     }
74|   }
75|
76|   // 특정 이벤트 발생 시
77|   record OnEvent(Class<? extends OrderEvent> eventType) implements SnapshotStrategy {
78|     public boolean shouldSnapshot(int count, OrderState s, OrderEvent e) {
79|       return eventType.isInstance(e);
80|     }
81|   }
82|
83|   // 상태가 특정 조건일 때
84|   record OnState(Predicate<OrderState> condition) implements SnapshotStrategy {
85|     public boolean shouldSnapshot(int count, OrderState s, OrderEvent e) {
86|       return condition.test(s);
87|     }
88|   }
89| }
90|
91| // 사용 예시
92| SnapshotStrategy strategy = new SnapshotStrategy.EveryN(100);  // 100개마다
93| // 또는
94| SnapshotStrategy strategy = new SnapshotStrategy.OnEvent(OrderEvent.OrderDelivered.class);  // 배송 완료 시
```
- **의도 및 코드 설명**: 스냅샷 저장소에서 최근 스냅샷을 로드하고, 그 이후 이벤트만 적용하여 재구성. 스냅샷 생성 조건을 전략 패턴으로 유연하게 정의
- **무엇이 좋아지나**:
  - 재구성 시간 단축: O(N) -> O(N - snapshot_version)
  - 메모리 사용량 감소: 최근 이벤트만 로드
  - 유연한 스냅샷 전략: 주기, 이벤트 타입, 상태 조건 등

### 이해를 위한 부가 상세
스냅샷 직렬화 형식 선택 시 고려사항:
- **JSON**: 사람이 읽을 수 있고, 스키마 변경에 유연하지만 크기가 큼
- **Protobuf/Avro**: 크기가 작고 빠르지만, 스키마 정의 필요
- **Java Serialization**: 간단하지만, 클래스 변경에 취약하고 보안 이슈 있음

스냅샷은 캐시이므로 무효화 전략도 필요하다. 도메인 모델(OrderState) 구조가 변경되면 기존 스냅샷을 무효화하고 이벤트에서 재구성해야 한다.

### 틀리기/놓치기 쉬운 부분
- 스냅샷 손상 시 이벤트에서 재구성 가능해야 함 (스냅샷은 캐시)
- 스냅샷 저장과 이벤트 저장을 원자적으로 처리할 필요는 없음 (스냅샷 없어도 동작)
- 너무 자주 스냅샷을 생성하면 저장소 비용 증가, 너무 드물면 재구성 비용 증가 -> 적절한 임계값 선택

### 꼭 기억할 것
- 스냅샷 = 성능 최적화, 선택적 기능
- 이벤트가 진실의 원천, 스냅샷은 캐시
- 스냅샷 버전 + 이후 이벤트로 재구성
- 전략 패턴으로 스냅샷 생성 조건 유연하게 정의
