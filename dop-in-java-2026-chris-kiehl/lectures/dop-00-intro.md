# Java로 정복하는 데이터 지향 프로그래밍 v0.2
## 참조: Data-Oriented Programming in Java

**버전**: 0.2 (Enhanced Edition)

**대상**: 백엔드 자바 개발자

**목표**: 복잡성을 수학적으로 제어하고, 컴파일러에게 검증을 위임하는 견고한 시스템 구축

**도구**: Java 25 (Record, Sealed Interface, Pattern Matching, Record Patterns)

**원전**: *Data-Oriented Programming in Java* by Chris Kiehl

---

## God Class 판별 기준 (God Class Detection Criteria)

> **다른 말로 (In other words):**
> - "하나의 클래스가 너무 많은 책임을 가지고 있어서 변경과 테스트가 어려운 안티패턴"
> - "의존성이 모든 곳으로 퍼져나가 시스템 전체를 오염시키는 클래스"

God Class는 "모든 것을 알고 모든 것을 하는" 클래스입니다. 다음 조건 중 **2개 이상** 해당되면 God Class를 의심하세요:

**Table 0.1**: God Class 판별 기준표

| 기준 | 설명 | 예시 |
|-----|------|------|
| **높은 LOC** | 500줄 이상의 코드 | `OrderService.java` 2000줄 |
| **다중 책임** | 하나의 클래스가 여러 도메인 관심사 처리 | 주문 + 결제 + 배송 + 알림 |
| **과다 의존성** | 10개 이상의 다른 클래스 주입 | `@Autowired` 15개 |
| **혼합된 추상화 수준** | 고수준 비즈니스 로직과 저수준 I/O가 섞임 | 주문 계산 + DB 쿼리 + HTTP 호출 |
| **빈약한 응집도** | 메서드들이 클래스의 일부 필드만 사용 | `calculateTotal()`은 items만, `sendEmail()`은 customer만 사용 |
| **수정의 파급효과** | 한 기능 수정 시 관련 없어 보이는 테스트 실패 | 할인 로직 수정 → 배송 테스트 실패 |

### 리트머스 테스트

**Code 0.1**: God Class 경고 신호 - 리트머스 테스트
```java
// God Class 경고 신호
public class OrderService {
    @Autowired private UserRepository userRepo;        // 1. 사용자
    @Autowired private ProductRepository productRepo;  // 2. 상품
    @Autowired private PaymentGateway paymentGateway;  // 3. 결제
    @Autowired private ShippingService shippingService;// 4. 배송
    @Autowired private EmailService emailService;      // 5. 알림
    @Autowired private InventoryService inventoryService; // 6. 재고
    @Autowired private CouponService couponService;    // 7. 쿠폰
    @Autowired private TaxCalculator taxCalculator;    // 8. 세금
    // ... 10개 이상의 의존성 = God Class 가능성 높음

    public Order createOrder(...) { /* 200줄 */ }
    public void processPayment(...) { /* 150줄 */ }
    public void arrangeShipping(...) { /* 100줄 */ }
    // 메서드마다 다른 필드 사용 = 낮은 응집도
}
```

**DOP 해결책**: 데이터(Record)와 로직(Calculations)을 분리하여 각 관심사별 작은 모듈로 분해

---

## DOP 4대 원칙 상세 (DOP 4 Core Principles in Detail)

> **Visual Reference:**
> ![Software Architecture Patterns](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F8c543b87-0714-4781-b5e4-0c0b98371a63_1280x1664.gif)
> *Source: [ByteByteGo - Software Architecture Patterns](https://blog.bytebytego.com/p/software-architecture-patterns)*

> **다른 말로 (In other words):**
> - "데이터는 투명하게, 로직은 순수하게, 변경은 새 객체로"
> - "타입으로 복잡성을 제어하고 컴파일러에게 검증을 위임"

### 원칙 1: 코드와 데이터의 분리 (Separate Code from Data) ⭐

> **핵심 질문**: "이 클래스에서 데이터(상태)와 로직(행동)이 분리되어 있는가?"

#### 무엇을 의미하는가?

데이터는 **Record**로, 로직은 **static 메서드**로 분리합니다.

**Code 0.2**: OOP vs DOP - 코드와 데이터 분리
```java
// [X] OOP: 데이터와 로직이 하나의 클래스에
public class Order {
    private List<OrderItem> items;
    private OrderStatus status;

    public Money calculateTotal() { ... }  // 로직
    public void addItem(OrderItem item) { ... }  // 로직 + 상태 변경
    public void ship() { ... }  // 로직 + 상태 변경
}

// [O] DOP: 데이터와 로직 분리
public record Order(List<OrderItem> items, OrderStatus status) {}  // 데이터만

public class OrderCalculations {  // 로직만
    public static Money calculateTotal(Order order) { ... }
    public static Order addItem(Order order, OrderItem item) { ... }
    public static Order ship(Order order) { ... }
}
```

#### 왜 분리하는가?

**Table 0.2**: 코드와 데이터 분리의 장점

| 관점 | 설명 |
|-----|------|
| **테스트 용이성** | 순수 함수는 입력만 주면 출력 검증 가능, Mock 불필요 |
| **재사용성** | 로직이 특정 클래스에 묶이지 않아 다양한 데이터에 적용 가능 |
| **병렬 처리** | 상태 없는 함수는 스레드 안전, 동기화 불필요 |
| **추론 가능성** | 함수가 외부 상태를 변경하지 않아 동작 예측 쉬움 |

---

#### 관점 1: 레스토랑 비유

> **요리사(로직)와 재료(데이터)의 분리**
>
> - **재료(데이터)**: 당근, 양파, 소고기 — 그 자체로는 아무 일도 하지 않음
> - **레시피(로직)**: "당근과 양파를 볶고, 소고기를 넣어 조리한다"
>
> OOP는 "똑똑한 재료"를 만듭니다:
> ```java
> carrot.cookYourself();  // 당근이 스스로를 요리?
> ```
>
> DOP는 "멍청한 재료 + 똑똑한 요리사"입니다:
> ```java
> Chef.cook(carrot, onion, beef);  // 요리사가 재료를 요리
> ```
>
> **이점**: 요리사(로직)를 바꾸면 같은 재료로 다른 요리 가능.
> OOP에서는 재료 클래스를 수정해야 새로운 요리법 추가 가능.

---

#### 관점 2: Spring Service 관점

**Code 0.3**: Spring에서의 코드-데이터 분리
```java
// [X] 전통적 Spring: Entity에 로직 포함 (Rich Domain Model)
@Entity
public class Order {
    @Id private Long id;
    private OrderStatus status;
    @OneToMany private List<OrderItem> items;

    public Money calculateTotal() {  // 로직이 Entity에
        return items.stream()
            .map(OrderItem::getPrice)  // OOP 클래스: getXxx() 스타일
            .reduce(Money.ZERO, Money::add);
    }

    public void ship() {  // 상태 변경 로직도 Entity에
        if (this.status != OrderStatus.PAID)
            throw new IllegalStateException();
        this.status = OrderStatus.SHIPPED;
    }
}

// [O] DOP 스타일 Spring: Record + @Service
public record OrderData(Long id, OrderStatus status, List<OrderItem> items) {}

@Service
public class OrderService {
    public Money calculateTotal(OrderData order) {
        return order.items().stream()
            .map(OrderItem::price)  // Record: xxx() 스타일 (get 접두사 없음)
            .reduce(Money.ZERO, Money::add);
    }

    public OrderData ship(OrderData order) {
        if (order.status() != OrderStatus.PAID)
            throw new IllegalStateException();
        return new OrderData(order.id(), OrderStatus.SHIPPED, order.items());
    }
}
```

> **Spring 개발자에게 익숙한 패턴**: 사실 `@Service`, `@Repository`에 로직을 두고 DTO/Entity에 데이터만 두는 것이 이미 DOP입니다!
> DOP는 이 패턴을 도메인 레이어까지 확장합니다.

---

#### 관점 3: 테스트 관점

**Code 0.4**: 테스트 용이성 - OOP vs DOP
```java
// [X] OOP 테스트: Mock이 필요한 경우가 많음
@Test
void testCalculateTotal_OOP() {
    // Order 내부에서 Repository를 호출한다면?
    Order order = new Order();
    order.addItem(mockItem1);  // 내부 상태 변경
    order.addItem(mockItem2);

    // 내부에서 할인 서비스를 호출한다면 Mock 필요
    when(mockDiscountService.getDiscount(any())).thenReturn(0.1);

    assertEquals(Money.of(9000), order.calculateTotal());
}

// [O] DOP 테스트: 순수 함수, Mock 불필요
@Test
void testCalculateTotal_DOP() {
    // 입력 데이터만 준비
    OrderData order = new OrderData(
        1L,
        OrderStatus.PENDING,
        List.of(
            new OrderItem(1L, Money.of(5000)),
            new OrderItem(2L, Money.of(5000))
        )
    );

    // 순수 함수 호출 - 외부 의존성 없음
    Money total = OrderCalculations.calculateTotal(order);

    assertEquals(Money.of(10000), total);
}
```

> **핵심**: 순수 함수는 **같은 입력 → 같은 출력**이 보장됩니다.
> 외부 상태에 의존하지 않아 테스트가 결정론적(deterministic)입니다.

---

#### 관점 4: 동시성 관점

**Code 0.5**: 멀티스레드 환경 - OOP vs DOP
```java
// [X] OOP: 상태 공유로 인한 동시성 문제
public class ShoppingCart {
    private List<Item> items = new ArrayList<>();

    public void addItem(Item item) {
        items.add(item);  // 동시에 여러 스레드가 호출하면?
    }

    public Money getTotal() {
        return items.stream()...;  // 순회 중 다른 스레드가 수정하면?
    }
}
// ConcurrentModificationException 또는 데이터 손상 위험

// [O] DOP: 상태 없는 함수는 스레드 안전
public record ShoppingCart(List<Item> items) {}

public class CartCalculations {
    // 순수 함수: 입력만 사용, 상태 변경 없음
    public static Money getTotal(ShoppingCart cart) {
        return cart.items().stream()
            .map(Item::price)
            .reduce(Money.ZERO, Money::add);
    }

    // 새 객체 반환: 원본 불변
    public static ShoppingCart addItem(ShoppingCart cart, Item item) {
        var newItems = new ArrayList<>(cart.items());
        newItems.add(item);
        return new ShoppingCart(List.copyOf(newItems));
    }
}
// 여러 스레드가 동시에 호출해도 안전 (각자 새 객체 생성)
```

> **핵심**: 상태 없는 함수(Stateless Function)는 **동기화 없이도 스레드 안전**합니다.
> 함수가 외부 상태를 읽지도, 쓰지도 않기 때문입니다.

---

#### 관점 5: AI 코드 생성 관점

**Code 0.6**: AI 도구와의 협업 - 명확한 입출력
```java
// [X] OOP: AI가 이해하기 어려운 코드
public class OrderProcessor {
    @Autowired private PaymentGateway gateway;
    @Autowired private InventoryService inventory;
    @Autowired private NotificationService notifier;

    public void processOrder(Order order) {
        // AI 질문: "이 메서드가 뭘 반환하나요? 어떤 부수효과가 있나요?"
        // 답: void 반환, 3개 외부 서비스 호출, order 상태 변경...
        // AI가 전체 컨텍스트 파악 어려움
    }
}

// [O] DOP: AI가 이해하기 쉬운 코드
public class OrderCalculations {
    // AI 질문: "이 함수가 뭘 하나요?"
    // 답: Order를 받아서 Money를 반환 (입출력이 시그니처에 명시)
    public static Money calculateTotal(Order order) { ... }

    // AI 질문: "주문에 할인을 적용하려면?"
    // 답: Order와 Discount를 받아서 새 Order 반환
    public static Order applyDiscount(Order order, Discount discount) { ... }
}
```

> **AI 도구(Copilot, Claude)는 순수 함수를 더 잘 이해합니다:**
> - 입력 타입과 출력 타입이 시그니처에 명확히 드러남
> - 부수효과가 없어 함수의 동작 범위가 예측 가능
> - 테스트 코드 생성도 용이 (입력 데이터만 만들면 됨)

---

#### 관점 6: OOP vs DOP 배치 비교

**Table 0.3**: OOP vs DOP 코드 배치

| 요소 | OOP | DOP |
|-----|-----|-----|
| **데이터 위치** | 클래스 필드 (private) | Record (public, 불변) |
| **로직 위치** | 클래스 메서드 (인스턴스) | *Calculations 클래스 (static) |
| **상태 변경** | `this.field = newValue` | 새 Record 인스턴스 반환 |
| **테스트** | Mock 객체, 상태 설정 | 순수 입출력 검증 |
| **동시성** | synchronized, Lock | 불변 + 순수 함수 |

---

#### 경계선: 언제 데이터에 메서드를 넣는가?

**예외적으로 데이터에 포함해도 되는 메서드:**

1. **유효성 검증** (Compact Constructor):
   ```java
   public record Email(String value) {
       public Email {
           if (!value.contains("@")) throw new IllegalArgumentException();
       }
   }
   ```

2. **Wither 메서드** (불변 업데이트 헬퍼):
   ```java
   public record Order(Long id, OrderStatus status, List<Item> items) {
       public Order withStatus(OrderStatus newStatus) {
           return new Order(id, newStatus, items);
       }
   }
   ```

3. **단순 파생 값** (다른 필드에서 바로 계산):
   ```java
   public record Rectangle(int width, int height) {
       public int area() { return width * height; }
   }
   ```

> **핵심**: 외부 의존성이 필요하거나 복잡한 로직은 반드시 외부 함수로 분리하세요.

---

### 원칙 2: 일반적 형태로 데이터 표현 (Represent Data with Generic Structures) ⭐

> **핵심 질문**: "이 데이터를 JSON으로 직렬화할 수 있는가?"

#### 무엇을 의미하는가?

도메인 특화 컬렉션 대신 **표준 자료구조**(List, Map, Set, Record)를 사용합니다.

**Code 0.7**: 커스텀 컬렉션 vs DOP 표준 자료구조
```java
// [X] 안티패턴: 커스텀 컬렉션
public class OrderItems extends ArrayList<OrderItem> {
    public Money calculateTotal() { ... }  // 데이터에 로직 섞임
    public void addWithValidation(OrderItem item) { ... }
}

// [O] DOP: 표준 자료구조 + 외부 함수
public record Order(List<OrderItem> items, ...) {}

public class OrderCalculations {
    public static Money calculateTotal(List<OrderItem> items) { ... }
}
```

#### 왜 중요한가?

**Table 0.4**: 표준 자료구조 사용의 장점

| 관점 | 설명 |
|-----|------|
| **상호운용성** | JSON, DB, 외부 API와 자연스러운 변환 |
| **도구 활용** | Stream API, Jackson, JPA 등 기존 도구 그대로 사용 |
| **테스트 용이성** | 특별한 팩토리 없이 `List.of(...)` 로 테스트 데이터 생성 |
| **추론 가능성** | `List<OrderItem>`은 누구나 이해, `OrderItems`는 구현을 봐야 이해 |

---

#### 관점 1: 레고 블록 비유

> **표준 자료구조는 레고 블록과 같습니다.**
>
> - **레고**: 어떤 세트의 블록이든 서로 결합 가능 (표준 규격)
> - **듀플로**: 레고와 호환되지 않음 (독자 규격)
>
> `List<OrderItem>`은 레고 블록입니다:
> - Stream API, Jackson, JPA 모두 `List`를 알고 있음
> - 어떤 도구든 바로 사용 가능
>
> `OrderItems extends ArrayList`는 듀플로입니다:
> - 겉보기엔 비슷하지만 도구들이 "OrderItems가 뭐지?" 라고 물음
> - 커스텀 직렬화, 커스텀 매핑 필요

---

#### 관점 2: 새 팀원이 왔을 때

**Code 0.8**: 새 팀원의 관점 - 커스텀 vs 표준 타입
```java
// 시나리오: 새 팀원이 주문 시스템 코드를 처음 봄

// [X] 커스텀 타입
OrderItemCollection items = order.getItems();
// "OrderItemCollection이 뭐지? add()가 있나? 어떤 검증을 하지?"
// -> OrderItemCollection.java를 열어서 500줄 읽어야 함

// [O] 표준 타입
List<OrderItem> items = order.items();
// "List네. add, remove, stream 다 되겠지."
// -> 바로 작업 시작 가능
```

---

#### 관점 3: 요구사항 변경 시

**Code 0.9**: 요구사항 변경 시 - 커스텀 vs 표준 타입
```java
// 새 요구사항: "주문 아이템을 카테고리별로 그룹핑해주세요"

// [X] 커스텀 타입이면
public class OrderItems extends ArrayList<OrderItem> {
    // groupByCategory() 메서드 추가해야 함
    // 기존 테스트 깨질 수 있음
    // PR 리뷰 필요
}

// [O] 표준 타입이면
List<OrderItem> items = order.items();
Map<Category, List<OrderItem>> grouped = items.stream()
    .collect(Collectors.groupingBy(OrderItem::category));
// 끝. 새 메서드 추가 불필요. Stream API가 이미 제공.
```

---

#### 관점 4: 디버깅할 때

**Code 0.10**: 디버깅 시 - 커스텀 vs 표준 타입
```java
// 프로덕션에서 NPE 발생. 로그에 데이터를 찍어봐야 함.

// [X] 커스텀 타입
log.info("items: {}", orderItems);
// 출력: "OrderItems@3f2a1c" (toString 오버라이드 안 했으면)
// 또는: "OrderItems{items=[...], internalState=..., cachedTotal=...}"
// -> 어떤 상태가 문제인지 파악 어려움

// [O] 표준 타입
log.info("items: {}", items);
// 출력: "[OrderItem[productId=1, qty=2], OrderItem[productId=3, qty=1]]"
// -> Record의 자동 toString()으로 깔끔하게 확인
```

---

#### 관점 5: 다른 시스템과 통신할 때

**Code 0.11**: 외부 시스템 통신 시 - 커스텀 vs 표준 타입
```java
// 외부 결제 API에 주문 정보를 전송해야 함

// [X] 커스텀 타입
public class OrderItems extends ArrayList<OrderItem> { ... }
// Jackson: "OrderItems? 이거 어떻게 직렬화하지?"
// -> @JsonSerialize 커스텀 어노테이션 필요
// -> API 스펙 문서에 OrderItems 구조 별도 설명 필요

// [O] 표준 타입
public record Order(List<OrderItem> items, Money total) {}
// Jackson: "List와 Record? 알지."
// -> 자동 직렬화: {"items": [...], "total": {"amount": 10000}}
// -> OpenAPI 스펙 자동 생성 가능
```

---

#### 경계선: 언제 커스텀 타입을 만드는가?

- **Value Object** (Money, Email, OrderId): 불변성 + 유효성 검증이 필요할 때
- **Aggregate Root** (Order): 일관성 경계를 정의할 때
- **컬렉션 래퍼**: 가능한 피하고, 필요 시 위임(delegation) 패턴 사용

> **핵심**: 컬렉션에 로직을 넣지 마세요. 로직은 `*Calculations` 클래스로 분리하세요.

---

### 원칙 3: 불변성 (Data is Immutable / Immutability) ⭐

> **핵심 질문**: "이 객체가 생성된 후에 내부 상태가 변경될 수 있는가?"

#### 무엇을 의미하는가?

모든 변경은 **새로운 객체 생성**으로. 기존 객체는 절대 수정하지 않습니다.

**Code 0.12**: 가변 vs 불변 - 객체 변경 방식
```java
// [X] 가변 (Mutable): 기존 객체 수정
public class Order {
    private OrderStatus status;

    public void ship() {
        this.status = OrderStatus.SHIPPED;  // 기존 객체 상태 변경
    }
}

Order order = new Order(OrderStatus.PAID);
order.ship();  // order 자체가 변경됨

// [O] 불변 (Immutable): 새 객체 생성
public record Order(OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(newStatus);  // 새 객체 반환, 원본 불변
    }
}

Order paidOrder = new Order(OrderStatus.PAID);
Order shippedOrder = paidOrder.withStatus(OrderStatus.SHIPPED);
// paidOrder는 여전히 PAID, shippedOrder는 SHIPPED
```

#### 왜 불변성인가?

**Table 0.5**: 불변성의 장점

| 관점 | 설명 |
|-----|------|
| **예측 가능성** | 객체가 한번 생성되면 절대 변하지 않아 추론이 쉬움 |
| **스레드 안전** | 동기화 없이 여러 스레드에서 안전하게 공유 |
| **디버깅 용이** | 상태 변경 지점 추적 불필요, 모든 상태가 명시적 |
| **캐싱 안전** | 불변 데이터는 부작용 없이 캐싱 가능 |
| **Undo 구현 용이** | 이전 상태 객체를 보관하면 바로 복원 가능 |

---

#### 관점 1: 시간 여행 디버깅 비유

> **모든 상태가 스냅샷으로 보존됩니다.**
>
> 가변 객체는 "현재 상태"만 존재합니다:
> ```
> t=0: Order(PENDING)
> t=1: Order(PAID)      // PENDING 상태는 사라짐
> t=2: Order(SHIPPED)   // PAID 상태도 사라짐
> 버그 발생! "어? 주문이 언제 PAID가 됐지?" -> 추적 불가
> ```
>
> 불변 객체는 "모든 시점의 상태"가 존재합니다:
> ```
> t=0: pendingOrder  = Order(PENDING)
> t=1: paidOrder     = Order(PAID)
> t=2: shippedOrder  = Order(SHIPPED)
> 버그 발생! -> 모든 상태를 로그에서 확인 가능
> ```
>
> **마치 Git처럼**: 모든 커밋(상태)이 보존되어 언제든 과거로 돌아갈 수 있습니다.

---

#### 관점 2: Git 비유

**Code 0.13**: Git 커밋처럼 불변한 데이터
```java
// Git은 "수정"이 아니라 "새 커밋 생성"입니다
// git commit --amend도 실제로는 새 커밋을 만들고 포인터를 이동

// [X] 가변: 파일을 직접 수정 (히스토리 없음)
document.setContent("새 내용");  // 이전 내용 사라짐

// [O] 불변: Git처럼 새 버전 생성
record Document(String content, int version) {
    Document withContent(String newContent) {
        return new Document(newContent, version + 1);
    }
}

Document v1 = new Document("초안", 1);
Document v2 = v1.withContent("수정본");
Document v3 = v2.withContent("최종본");

// v1, v2, v3 모두 존재 - 언제든 이전 버전 참조 가능
List<Document> history = List.of(v1, v2, v3);
```

> **핵심**: 불변 데이터는 **자동으로 히스토리를 형성**합니다.
> 각 상태 변경이 새 객체이므로, 이전 참조만 보관하면 전체 히스토리 추적 가능.

---

#### 관점 3: 멀티스레드 관점

**Code 0.14**: 멀티스레드 환경 - 가변 vs 불변
```java
// [X] 가변: 동기화가 필요한 위험한 코드
public class MutableCounter {
    private int count = 0;

    public void increment() {
        count++;  // 읽기-수정-쓰기: 원자적이지 않음!
    }

    public int getCount() {
        return count;  // 다른 스레드가 수정 중일 수 있음
    }
}

// 두 스레드가 동시에 increment() 호출하면?
// 기대값: 2, 실제값: 1 또는 2 (Race Condition)

// [O] 불변: 동기화 없이 안전
public record ImmutableCounter(int count) {
    public ImmutableCounter increment() {
        return new ImmutableCounter(count + 1);  // 새 객체 반환
    }
}

// 여러 스레드가 같은 counter를 읽어도 안전
ImmutableCounter counter = new ImmutableCounter(0);

// Thread 1
ImmutableCounter c1 = counter.increment();  // counter는 여전히 0

// Thread 2
ImmutableCounter c2 = counter.increment();  // counter는 여전히 0

// 각 스레드는 독립적인 새 객체를 가짐
// 원본 counter는 절대 변하지 않음
```

> **핵심**: 불변 객체는 **공유해도 안전**합니다.
> 읽기만 가능하므로 동기화(synchronized, Lock)가 필요 없습니다.

---

#### 관점 4: 캐싱/메모이제이션 관점

**Code 0.15**: 불변 데이터의 안전한 캐싱
```java
// [X] 가변: 캐싱이 위험함
public class MutableProduct {
    private Money price;
    public void setPrice(Money price) { this.price = price; }
    public Money getPrice() { return price; }
}

Map<ProductId, MutableProduct> cache = new HashMap<>();
MutableProduct product = cache.get(productId);
product.setPrice(Money.of(0));  // 캐시 안의 데이터가 오염됨!

// [O] 불변: 캐싱이 안전함
public record Product(ProductId id, Money price) {
    public Product withPrice(Money newPrice) {
        return new Product(id, newPrice);
    }
}

Map<ProductId, Product> cache = new ConcurrentHashMap<>();
Product product = cache.get(productId);
Product discounted = product.withPrice(Money.of(0));
// cache 안의 원본은 변경되지 않음!

// 메모이제이션도 안전
Map<Order, Money> totalCache = new ConcurrentHashMap<>();
Money total = totalCache.computeIfAbsent(order, OrderCalculations::calculateTotal);
// order가 불변이므로 캐시 키로 안전하게 사용 가능
```

> **핵심**: 불변 데이터는 **캐시 무효화 걱정이 없습니다.**
> 데이터가 변하지 않으므로 캐시된 값이 항상 유효합니다.

---

#### 관점 5: Undo/Redo 관점

**Code 0.16**: Undo/Redo 구현 - 불변 데이터의 강점
```java
// [X] 가변: Undo 구현이 복잡함
public class MutableEditor {
    private String content;
    private Stack<String> undoStack = new Stack<>();

    public void type(String text) {
        undoStack.push(content);  // 수동으로 백업
        content += text;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            content = undoStack.pop();  // 복원
        }
    }
}

// [O] 불변: Undo가 자연스럽게 구현됨
public record EditorState(String content) {
    public EditorState type(String text) {
        return new EditorState(content + text);
    }
}

// 사용
List<EditorState> history = new ArrayList<>();
EditorState state = new EditorState("");

state = state.type("Hello");
history.add(state);

state = state.type(" World");
history.add(state);

// Undo: 이전 상태로 돌아가기
state = history.get(history.size() - 2);  // "Hello"

// Redo: 다음 상태로 이동
state = history.get(history.size() - 1);  // "Hello World"
```

> **핵심**: 불변 데이터에서 Undo는 **이전 객체 참조**일 뿐입니다.
> 별도의 백업/복원 로직이 필요 없습니다.

---

#### 관점 6: 가변 vs 불변 트레이드오프

**Table 0.6**: 가변 vs 불변 트레이드오프

| 관점 | 가변 (Mutable) | 불변 (Immutable) |
|-----|---------------|-----------------|
| **메모리** | 객체 재사용 (효율적) | 새 객체 생성 (GC 부담) |
| **성능** | 직접 수정 (빠름) | 복사 후 수정 (약간 느림) |
| **스레드 안전** | Lock 필요 | 기본 안전 |
| **디버깅** | 상태 추적 어려움 | 모든 상태 보존 |
| **예측성** | 어디서 변경될지 모름 | 변경 불가, 예측 쉬움 |
| **캐싱** | 무효화 관리 필요 | 항상 안전 |

> **현대 JVM 최적화**: 단명 객체(short-lived object)는 Eden 영역에서 빠르게 GC됩니다.
> 불변 객체의 "메모리 오버헤드"는 대부분의 애플리케이션에서 무시할 수준입니다.

---

#### Java Record와 불변성

**Code 0.17**: Record의 얕은 불변성 주의사항
```java
// Record는 필드 재할당은 막지만, 필드 내용물의 변경은 막지 않음

// [X] 위험: 얕은 불변성의 함정
public record Order(List<OrderItem> items) {}

Order order = new Order(new ArrayList<>());
order.items().add(new OrderItem(...));  // 컴파일 됨! 내부 리스트가 변경됨

// [O] 안전: 깊은 불변성 보장
public record Order(List<OrderItem> items) {
    public Order {
        items = List.copyOf(items);  // 불변 리스트로 복사
    }
}

Order order = new Order(new ArrayList<>());
order.items().add(...);  // UnsupportedOperationException!
```

> **핵심**: Record 사용 시 **컬렉션 필드는 반드시 `List.copyOf()`** 등으로 불변 보장하세요.

---

#### 불변 업데이트 패턴: Wither

**Code 0.18**: Wither 패턴으로 불변 업데이트
```java
public record Order(
    OrderId id,
    OrderStatus status,
    List<OrderItem> items,
    Money total
) {
    // Wither 메서드들
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, newStatus, items, total);
    }

    public Order withAddedItem(OrderItem item) {
        var newItems = new ArrayList<>(items);
        newItems.add(item);
        return new Order(id, status, List.copyOf(newItems), total);
    }

    public Order withTotal(Money newTotal) {
        return new Order(id, status, items, newTotal);
    }
}

// 사용 예시: 메서드 체이닝
Order finalOrder = initialOrder
    .withAddedItem(newItem)
    .withTotal(calculatedTotal)
    .withStatus(OrderStatus.CONFIRMED);
```

> **참고**: JEP 468 (Derived Record Creation / `with` expression)은 Java 25에 포함되지 않았습니다.
> 따라서 수동으로 Wither 메서드를 작성해야 합니다.

---

### 원칙 4: 스키마와 표현의 분리 (Separate Schema from Representation) ⭐

> **핵심 질문**: "데이터의 구조 정의와 데이터의 실제 값을 분리했는가?"

#### 무엇을 의미하는가?

- **스키마(Schema)**: 데이터의 **구조와 규칙** (타입 정의, 검증 규칙)
- **표현(Representation)**: 데이터의 **실제 값** (인스턴스)

**Code 0.19**: 스키마와 표현의 분리 예제
```java
// 스키마: 구조 정의
public sealed interface PaymentMethod {
    record CreditCard(String cardNumber, YearMonth expiry) implements PaymentMethod {}
    record BankTransfer(String accountNumber, String bankCode) implements PaymentMethod {}
    record DigitalWallet(WalletType type, String token) implements PaymentMethod {}
}

// 표현: 실제 값
PaymentMethod method = new CreditCard("4111-1111-1111-1111", YearMonth.of(2027, 12));
```

---

#### 관점 1: DB 스키마 비유 (Java 개발자에게 익숙한 개념)

> **이미 알고 있는 개념입니다!**
>
> | SQL 세계 | Java DOP |
> |---------|----------|
> | DDL (CREATE TABLE) | sealed interface + record 정의 |
> | DML (INSERT/SELECT 결과) | record 인스턴스 |
> | 테이블 스키마 | 타입 정의 |
> | 행(row) 데이터 | 객체 인스턴스 |
>
> ```sql
> -- 스키마 (DDL)
> CREATE TABLE orders (
>     id BIGINT PRIMARY KEY,
>     status VARCHAR(20) CHECK (status IN ('PENDING', 'PAID', 'SHIPPED'))
> );
> -- 표현 (DML)
> INSERT INTO orders VALUES (1, 'PAID');
> ```
>
> DOP에서도 똑같습니다:
> ```java
> // 스키마 (타입 정의)
> sealed interface OrderStatus permits Pending, Paid, Shipped {}
> // 표현 (인스턴스)
> OrderStatus status = new Paid(LocalDateTime.now(), paymentId);
> ```

---

#### 관점 2: 엑셀 템플릿 비유

> **빈 양식 vs 작성된 문서**
>
> - **스키마 = 엑셀 템플릿**: 열 이름, 데이터 타입, 유효성 규칙이 정의됨
>   - "이름" 열: 문자열, 필수
>   - "나이" 열: 숫자, 0-150
>   - "등급" 열: VIP/GOLD/SILVER 중 하나
>
> - **표현 = 작성된 엑셀 파일**: 실제 데이터가 채워진 상태
>   - 홍길동, 30, VIP
>   - 김영희, 25, GOLD
>
> 템플릿(스키마)은 한 번 정의하면 여러 문서(표현)에 재사용됩니다.

---

#### 관점 3: Java 개발자가 이미 아는 것

**Code 0.20**: Java에서의 스키마와 표현
```java
// Java 개발자는 이미 스키마와 표현을 분리하고 있습니다!

// 스키마: .java 파일 (컴파일 타임에 존재)
public class User {
    private String name;
    private int age;
}

// 표현: 힙 메모리의 객체 (런타임에 존재)
User user = new User("홍길동", 30);
```

> **그런데 DOP의 원칙 4가 다른 점은?**
>
> OOP에서는 스키마(클래스)가 **행동(메서드)**도 포함합니다.
> DOP에서는 스키마가 **구조만** 정의하고, 행동은 외부 함수로 분리합니다.
>
> ```java
> // OOP: 스키마 + 행동이 섞임
> public class Order {
>     private List<Item> items;
>     public Money calculateTotal() { ... }  // 행동이 스키마에 포함
> }
>
> // DOP: 스키마와 행동 분리
> public record Order(List<Item> items) {}  // 스키마 (구조만)
> public class OrderCalculations {          // 행동 (별도)
>     public static Money calculateTotal(Order order) { ... }
> }
> ```

---

#### 관점 4: API 문서 관점

**Code 0.21**: API 문서 관점의 스키마와 표현
```java
// OpenAPI/Swagger 스펙을 생각해보세요

// 스키마: API 문서에 정의된 응답 구조
// {
//   "type": "object",
//   "properties": {
//     "status": { "enum": ["PENDING", "PAID", "SHIPPED"] },
//     "paidAt": { "type": "string", "format": "date-time" }
//   }
// }

// 표현: 실제 API 응답
// { "status": "PAID", "paidAt": "2025-01-20T10:00:00" }
```

> DOP의 sealed interface는 **API 스키마를 코드로 표현**한 것입니다.
> 컴파일러가 "이 API 응답에서 처리 안 한 케이스 있어요" 라고 알려줍니다.

---

#### 관점 5: 왜 굳이 분리해야 하나? (실전 문제)

**Code 0.22**: 새로운 상태 추가 시 - OOP vs DOP
```java
// 문제 상황: 주문 상태에 "REFUNDED"를 추가해야 함

// [X] 스키마와 표현이 섞인 OOP
public class Order {
    private String status;  // "PENDING", "PAID", "SHIPPED"

    public void ship() {
        if (!"PAID".equals(status)) throw new IllegalStateException();
        this.status = "SHIPPED";
    }
}
// 문제: REFUNDED 추가 시 ship(), cancel() 등 모든 메서드 검토 필요
// 컴파일러는 아무 경고 안 함

// [O] 스키마와 표현이 분리된 DOP
public sealed interface OrderStatus {
    record Pending() implements OrderStatus {}
    record Paid(PaymentId paymentId) implements OrderStatus {}
    record Shipped(TrackingNumber tracking) implements OrderStatus {}
    record Refunded(RefundId refundId, String reason) implements OrderStatus {}  // 새로 추가
}

// 컴파일러가 switch 문마다 "Refunded 케이스 처리 안 했어요!" 경고
// -> 수정해야 할 모든 곳을 컴파일러가 알려줌
```

---

#### 관점 6: 한 줄 요약

**Table 0.7**: 스키마와 표현 한 줄 요약

| 개념 | 비유 | Java 코드 |
|-----|------|----------|
| 스키마 | 설계도 | `sealed interface`, `record` 정의 |
| 표현 | 실물 | `new Record(...)` 인스턴스 |

> **원칙 4의 핵심**:
> - 스키마에는 **가능한 모든 상태**를 명시적으로 열거
> - sealed interface의 permits/record가 컴파일러에게 "이게 전부야"라고 알려줌
> - 새 상태 추가 시 컴파일러가 모든 처리 누락을 찾아줌

---

#### 왜 분리하는가?

**Table 0.8**: 스키마와 표현의 책임 분리

| 관점 | 스키마 책임 | 표현 책임 |
|-----|-----------|----------|
| **검증** | 컴파일 타임 타입 체크 | 런타임 값 보유 |
| **진화** | 새로운 케이스 추가 | 기존 데이터 호환 |
| **직렬화** | 구조 정보 제공 (JSON Schema) | 실제 바이트 변환 |
| **문서화** | 가능한 상태 정의 | 현재 상태 표현 |

#### 실전 예제: 다형적 JSON 직렬화

**Code 0.23**: 다형적 JSON 직렬화 예제
```java
// 스키마: sealed interface로 가능한 모든 상태 정의
public sealed interface OrderStatus {
    record Pending(LocalDateTime createdAt) implements OrderStatus {}
    record Paid(LocalDateTime paidAt, PaymentId paymentId) implements OrderStatus {}
    record Shipped(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
    record Canceled(LocalDateTime canceledAt, CancelReason reason) implements OrderStatus {}
}

// Jackson이 스키마를 인식하여 다형적 직렬화
// {"type": "Paid", "paidAt": "2025-01-20T10:00:00", "paymentId": "PAY-123"}
```

#### OOP와의 차이

**Table 0.9**: OOP와 DOP의 차이

| OOP | DOP |
|-----|-----|
| 클래스가 스키마와 표현을 모두 담당 | 스키마(sealed interface) ≠ 표현(record instance) |
| 상속으로 스키마 확장 (깨지기 쉬움) | 조합(composition)으로 확장 |
| instanceof + 캐스팅 | 패턴 매칭으로 안전한 분기 |

---

## DOP vs Domain Modeling Made Functional (DMMF) 비교 (DOP vs DMMF Comparison)

> **왜 비교하나요?**
>
> DMMF(Scott Wlaschin)는 F# 기반이지만, DOP와 **90% 같은 철학**을 공유합니다.
> 둘 다 "타입으로 불가능한 상태를 제거"하고 "순수 함수로 로직을 표현"합니다.
> 차이점을 이해하면 **Java에서 더 나은 DOP**를 적용할 수 있습니다.

---

### 관점 1: 한 눈에 보는 코드 비교

**Code 0.24**: DOP vs DMMF - 이메일 주소 처리 비교
```java
// ===== 문제: "이메일 주소"를 어떻게 다룰 것인가? =====

// [X] 전통적 Java (String 남용)
public class User {
    private String email;  // "abc", "", "not-an-email" 모두 가능
}

// [O] DOP (Chris Kiehl 스타일)
public record Email(String value) {
    public Email {
        if (!value.contains("@")) throw new IllegalArgumentException();
    }
}
public record User(Email email) {}  // Email 타입 강제

// [O] DMMF (Scott Wlaschin 스타일) - Java로 번역
public record UnvalidatedEmail(String value) {}  // 검증 전
public record ValidatedEmail(String value) {     // 검증 후
    public ValidatedEmail {
        if (!value.contains("@")) throw new IllegalArgumentException();
    }
}
// 검증 전/후를 타입으로 구분!
```

> **차이점**: DOP는 "유효한 값"만 허용, DMMF는 "검증 전/후 상태"를 타입으로 분리

---

### 관점 2: 에러 처리 철학

**Code 0.25**: DOP vs DMMF - 에러 처리 비교
```java
// ===== 문제: 주문 생성 시 여러 검증을 거쳐야 함 =====

// [X] 전통적 Java (예외 던지기)
public Order createOrder(Request req) {
    User user = userRepo.findById(req.userId())
        .orElseThrow(() -> new UserNotFoundException());  // 예외 1
    if (user.isSuspended()) throw new UserSuspendedException();  // 예외 2
    Product product = productRepo.findById(req.productId())
        .orElseThrow(() -> new ProductNotFoundException());  // 예외 3
    if (product.getStock() < req.quantity())
        throw new InsufficientStockException();  // 예외 4
    return new Order(...);
}
// 문제: 어떤 예외가 던져질지 시그니처에 안 보임

// [O] DOP 스타일 (Result 체이닝)
public Result<Order, OrderError> createOrder(Request req) {
    return findUser(req.userId())
        .flatMap(user -> checkNotSuspended(user))
        .flatMap(user -> findProduct(req.productId()))
        .flatMap(product -> checkStock(product, req.quantity()))
        .map(product -> new Order(...));
}
// 장점: 모든 실패 케이스가 OrderError 타입에 명시됨

// [O] DMMF 스타일 (Railway-Oriented Programming) - Java로 번역
// 동일한 개념이지만, DMMF는 "레일" 비유를 강조
// Success 레일: ----[User]----[Product]----[Order]---->
// Failure 레일: ----[UserNotFound]---->
//               ----[Suspended]---->
//               ----[ProductNotFound]---->
```

> **핵심**: DOP와 DMMF 모두 `Result` 타입 사용. DMMF는 "두 개의 레일" 비유로 설명.

---

### 관점 3: 워크플로우를 타입으로 강제 (DMMF의 강점)

**Code 0.26**: DMMF 스타일 - 워크플로우를 타입으로 강제
```java
// ===== 문제: 주문이 "결제 없이 배송됨" 버그 발생 =====

// [X] 전통적 Java (상태 필드)
public class Order {
    private String status;  // "PENDING", "PAID", "SHIPPED"

    public void ship() {
        // 개발자가 status 체크를 깜빡하면?
        this.status = "SHIPPED";  // 결제 안 됐는데 배송됨!
    }
}

// [O] DMMF 스타일 (타입으로 워크플로우 강제) - Java로 번역
public sealed interface OrderWorkflow {
    record UnvalidatedOrder(List<UnvalidatedItem> items) implements OrderWorkflow {}
    record ValidatedOrder(List<ValidatedItem> items) implements OrderWorkflow {}
    record PaidOrder(ValidatedOrder order, PaymentId paymentId) implements OrderWorkflow {}
    record ShippedOrder(PaidOrder order, TrackingNumber tracking) implements OrderWorkflow {}
}

// 핵심: ship() 메서드의 시그니처가 "PaidOrder만 받음"
public ShippedOrder ship(PaidOrder order, TrackingNumber tracking) {
    return new ShippedOrder(order, tracking);
}

// 컴파일 에러!
// ship(new ValidatedOrder(...), tracking);  // PaidOrder가 아니라서 컴파일 안 됨
```

> **DMMF의 핵심 아이디어**: "다음 단계 함수는 이전 단계의 출력 타입만 받는다"
> Java에서도 100% 적용 가능!

---

### 관점 4: Primitive Obsession 해결

**Code 0.27**: Primitive Obsession 해결 - Constrained Types
```java
// ===== 문제: String, int를 남용하는 코드 =====

// [X] Primitive Obsession
public Order createOrder(
    String customerId,    // "C001" 또는 "아무거나"
    String productId,     // "P001" 또는 ""
    int quantity,         // -5도 가능
    String email          // "invalid"도 가능
) { ... }

// 실수: createOrder(productId, customerId, ...) - 순서 바꿔도 컴파일 됨!

// [O] DMMF 스타일 (Constrained Types) - Java로 번역
public record CustomerId(String value) {
    public CustomerId { Objects.requireNonNull(value); }
}
public record ProductId(String value) {
    public ProductId { Objects.requireNonNull(value); }
}
public record Quantity(int value) {
    public Quantity { if (value < 1) throw new IllegalArgumentException(); }
}
public record Email(String value) {
    public Email { if (!value.contains("@")) throw new IllegalArgumentException(); }
}

public Order createOrder(
    CustomerId customerId,
    ProductId productId,
    Quantity quantity,
    Email email
) { ... }

// 컴파일 에러!
// createOrder(productId, customerId, ...) - 타입이 달라서 컴파일 안 됨!
```

> **DOP도 이걸 권장하지만**, DMMF는 "모든 primitive를 도메인 타입으로" 더 강조합니다.

---

### 관점 5: Spring Boot 개발자를 위한 실전 비교

**Code 0.28**: Spring Boot에서의 DOP vs DMMF 스타일
```java
// ===== Spring Boot에서 DOP vs DMMF 스타일 =====

// [DOP 스타일] - Spring과 자연스럽게 공존
@RestController
public class OrderController {
    @Autowired private OrderRepository repo;

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest req) {
        // Functional Core
        Result<Order, OrderError> result = OrderCalculations.createOrder(
            req.toOrder()
        );

        // Imperative Shell
        return result.fold(
            order -> ResponseEntity.ok(repo.save(order)),
            error -> ResponseEntity.badRequest().body(error)
        );
    }
}

// [DMMF 스타일] - 더 순수하지만 Spring과 약간의 마찰
// DMMF는 의존성을 함수 파라미터로 전달하는 것을 선호
public class OrderWorkflows {
    // 의존성을 파라미터로 받음 (Partial Application 스타일)
    public static Function<UnvalidatedOrder, Result<PlacedOrder, OrderError>>
        placeOrder(
            Function<ProductId, Result<Product, OrderError>> findProduct,
            Function<Order, OrderId> saveOrder
        ) {
        return unvalidated ->
            validate(unvalidated)
                .flatMap(validated -> price(validated, findProduct))
                .flatMap(priced -> Result.success(saveOrder.apply(priced)));
    }
}
// 장점: 테스트 시 Mock 주입이 쉬움
// 단점: Spring DI 패턴과 스타일이 다름
```

---

### 공통 철학 (둘 다 동의하는 것)

**Table 0.10**: DOP와 DMMF의 공통 철학

| 원칙 | DOP 표현 | DMMF 표현 | Java 코드 |
|-----|---------|----------|----------|
| 불변 데이터 | Record는 불변 | Immutable by default | `record Order(...)` |
| 타입으로 상태 표현 | Sealed Interface | Discriminated Union | `sealed interface` |
| 순수 함수 | static 메서드 | Pure Function | `static` + no side effects |
| 불가능한 상태 제거 | 컴파일 타임 보장 | "Make illegal states unrepresentable" | exhaustive switch |
| 에러도 데이터 | Result<S,F> | Railway-Oriented | `sealed interface Result` |

---

### 차이점 요약

**Table 0.11**: DOP와 DMMF의 차이점

| 관점 | DOP (Java) | DMMF (F#) |
|-----|-----------|-----------|
| **언어 기반** | OOP 언어에 FP 적용 | 순수 FP 언어 |
| **Spring과의 관계** | 자연스러운 공존 | 마찰 있음 |
| **도메인 타입 강도** | 권장 | 필수 (Primitive Obsession 거부) |
| **워크플로우 표현** | 상태 enum/sealed | 타입으로 단계 분리 |
| **DDD 용어** | 언급 적음 | Bounded Context, Ubiquitous Language 강조 |

---

### 언제 무엇을 적용할까?

**Table 0.12**: DOP vs DMMF 적용 가이드

| 상황 | 추천 | 이유 |
|-----|------|------|
| Spring Boot + JPA 레거시 | **DOP** | 점진적 도입 용이, 기존 코드와 공존 |
| 새 프로젝트, 복잡한 도메인 | **DOP + DMMF** | DOP 기반 + 워크플로우 타입, Constrained Types 아이디어 조합 |
| 함수형 언어 사용 가능 | **DMMF 원칙 직접** | 언어가 FP를 직접 지원 |
| 단순 CRUD | **어느 쪽도 과도** | Active Record나 전통 MVC로 충분 |

---

### 핵심 교훈: DMMF에서 Java로 가져올 것

**Code 0.29**: DMMF에서 Java로 가져올 핵심 패턴
```java
// 1. 워크플로우를 타입으로 강제
public sealed interface OrderWorkflow {
    record Unvalidated(List<RawItem> items) implements OrderWorkflow {}
    record Validated(List<ValidItem> items) implements OrderWorkflow {}
    record Priced(List<PricedItem> items, Money total) implements OrderWorkflow {}
    record Placed(OrderId id, PlacedDetails details) implements OrderWorkflow {}
}

// 2. Primitive Obsession 제거
public record OrderId(String value) {}
public record Email(String value) { /* validation */ }
public record Money(BigDecimal amount, Currency currency) {}

// 3. 함수 시그니처로 전이 규칙 표현
Validated validate(Unvalidated order);  // Unvalidated -> Validated만 가능
Priced price(Validated order);          // Validated -> Priced만 가능
Placed place(Priced order);             // Priced -> Placed만 가능

// 잘못된 순서는 컴파일 에러!
// price(new Unvalidated(...));  // 타입 불일치!
```

---

## Java 17 → Java 25 DOP 관련 변경 사항 (Java 17 → Java 25 DOP-Related Changes)

**Table 0.9**: Java 17 → Java 25 DOP 관련 주요 기능

| 버전 | 핵심 기능 | JEP | 상태 | DOP 영향 |
|------|----------|-----|------|---------|
| **Java 21** | Record Patterns | 440 | Final | ⭐ 핵심 - 패턴에서 레코드 분해 |
| **Java 21** | Pattern Matching for switch | 441 | Final | ⭐ 핵심 - switch 표현식 완성 |
| **Java 21** | Sequenced Collections | 431 | Final | 유용 - `getFirst()`, `getLast()` |
| **Java 22** | Unnamed Variables & Patterns (`_`) | 456 | Final | 중요 - 사용하지 않는 변수 표시 |
| **Java 25** | Primitive Types in Patterns | 507 | Preview | 보통 - 기본 타입 패턴 매칭 |
| **Java 25** | Scoped Values | 506 | Final | 보통 - 불변 컨텍스트 전달 |

> ⚠️ **중요**: JEP 468 (Derived Record Creation / `with` expression)은 Java 25에 **포함되지 않았습니다**.
> 따라서 Record의 값을 변경한 새 객체를 만들려면 수동 `withXxx()` 메서드가 여전히 필요합니다.

---

## 전체 목차 (Table of Contents)

### Part I: 사고의 전환 (Foundations / Shifting Perspective)
- **Chapter 1: 객체 지향의 환상과 데이터의 실체**
  - OOP 캡슐화의 문제점(God Class)과 DOP 4대 원칙(코드/데이터 분리, 일반적 표현, 불변성, 스키마/표현 분리) 소개.
- **Chapter 2: 데이터란 무엇인가? (Identity vs Value)**
  - 정체성(Identity)과 값(Value)의 차이, Java Record를 활용한 Value Type 모델링, 얕은 불변성과 깊은 불변성.

### Part II: 데이터 모델링의 수학 (Algebraic Data Types / Mathematical Data Modeling)
- **Chapter 3: 타입 시스템의 기수(Cardinality) 이론**
  - 시스템 복잡도와 기수의 관계, 곱 타입(Product Type)의 상태 폭발 문제와 합 타입(Sum Type/Sealed Interface)을 통한 해결.
- **Chapter 4: 불가능한 상태를 표현 불가능하게 만들기**
  - 런타임 검증 대신 컴파일 타임에 불가능한 상태를 제거하는 설계, 망라성(Exhaustiveness) 체크 활용.

### Part III: 데이터의 흐름과 제어 (Behavior & Control Flow / Data Flow and Control)
- **Chapter 5: 전체 함수(Total Functions)와 실패 처리**
  - 부분 함수(예외 발생)의 위험성과 전체 함수(Result 반환)의 안정성, "Failure as Data" 패턴.
- **Chapter 6: 파이프라인과 결정론적 시스템**
  - 결정론적 순수 함수의 장점, I/O와 로직을 분리하는 샌드위치 아키텍처(Impure-Pure-Impure).

### Part IV: 고급 기법과 대수적 추론 (Advanced Theory / Advanced Techniques and Algebraic Reasoning)
- **Chapter 7: 대수적 속성을 활용한 설계**
  - 결합법칙, 멱등성, 항등원 등 수학적 속성을 활용한 병렬 처리 및 재시도 안전성 확보.
- **Chapter 8: 인터프리터 패턴: Rule as Data**
  - 비즈니스 규칙을 코드가 아닌 데이터로 표현하여 유연성과 동적 변경 가능성 확보.

### Part V: 레거시 탈출 (Refactoring & Architecture / Escaping Legacy Code)
- **Chapter 9: 현실 세계의 DOP: JPA/Spring과의 공존**
  - JPA Entity(Identity/Mutable)와 Domain Record(Value/Immutable)의 공존 및 변환 전략, 점진적 리팩토링 가이드.

### Appendix
- **Appendix A**: 흔한 실수와 안티패턴 모음
- **Appendix B**: DOP Java 치트시트
- **Appendix C**: 전체 퀴즈 정답 및 해설
- **Appendix D**: Final Boss Quiz
