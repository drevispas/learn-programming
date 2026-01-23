# Appendix C: Java 25 Cheat Sheet (Java 25 기능 정리)

> 이 문서에서 사용하는 Java 25 기능들의 문법과 활용법을 정리한다. 각 기능의 도입 버전, 핵심 문법, DOP/FP에서의 활용을 포함한다.

---

## 1. Records (Java 16+)

Record는 불변 데이터 클래스를 간결하게 정의하는 기능이다. 자동으로 생성자, getter(accessor), equals(), hashCode(), toString()을 제공한다.

### 기본 문법

```java
// 기본 Record 정의
public record Point(int x, int y) {}

// 사용
Point p = new Point(3, 4);
int x = p.x();       // getter는 필드명과 동일 (getX가 아님!)
int y = p.y();
```

### Compact Constructor (검증)

Record 생성 시 불변 조건을 강제하는 축약 생성자이다. 파라미터 목록과 `this.field =` 대입이 생략된다.

```java
public record Money(BigDecimal amount, Currency currency) {
    // Compact Constructor: 파라미터 목록 생략
    public Money {
        if (amount == null) throw new IllegalArgumentException("금액 필수");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("음수 금액 불가");
        if (currency == null) throw new IllegalArgumentException("통화 필수");
        // this.amount = amount; 자동 대입 (명시 불필요)
    }
}

// 방어적 복사: 가변 컬렉션을 불변으로 변환
public record Order(OrderId id, List<OrderItem> items) {
    public Order {
        items = List.copyOf(items); // 방어적 복사
    }
}
```

### 팩토리 메서드

```java
public record UserId(long value) {
    public static UserId of(long value) { return new UserId(value); }
    public static UserId generate() { return new UserId(System.nanoTime()); }
}
```

### Wither Pattern (수동 구현)

JEP 468(Derived Record Creation, `with` 구문)은 Java 25에 **포함되지 않았다**. 따라서 필드 변경 시 수동으로 `withXxx()` 메서드를 구현해야 한다.

```java
public record Order(OrderId id, OrderStatus status, Money total) {
    // Wither: 일부 필드만 변경한 새 인스턴스 반환
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, newStatus, this.total);
    }
    public Order withTotal(Money newTotal) {
        return new Order(this.id, this.status, newTotal);
    }
}

// 사용
Order paid = unpaidOrder.withStatus(new Paid(LocalDateTime.now(), txId));
// unpaidOrder는 변경되지 않음 (불변!)
```

### Record에서 추가 메서드 정의

```java
public record Money(BigDecimal amount, Currency currency) {
    // 비즈니스 메서드
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isZero() { return amount.signum() == 0; }

    // 정적 팩토리
    public static Money zero(Currency c) { return new Money(BigDecimal.ZERO, c); }
    public static Money krw(long value) { return new Money(BigDecimal.valueOf(value), Currency.KRW); }
}
```

### Record 제약사항

- 모든 필드는 `final` (불변)
- 상속 불가 (`extends` 사용 불가, 암묵적으로 `java.lang.Record` 상속)
- 인터페이스 구현은 가능 (`implements`)
- 인스턴스 필드 추가 불가 (Record 헤더에 선언된 필드만 가능)
- `abstract` 불가
- JPA `@Entity`로 직접 사용 불가 (기본 생성자, setter 없음)

---

## 2. Sealed Interfaces (Java 17+)

Sealed interface는 구현 가능한 타입을 `permits`로 제한하여, 컴파일러가 모든 하위 타입을 알 수 있게 한다. 이를 통해 switch에서 **망라적(exhaustive) 패턴 매칭**이 가능하다.

### 기본 문법

```java
// permits로 구현 타입 명시
public sealed interface Shape permits Circle, Rectangle, Triangle {}
public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double base, double height) implements Shape {}
```

### 중첩 정의 (Nested)

같은 파일 내에서 중첩 정의하면 `permits` 생략 가능하다.

```java
public sealed interface OrderStatus {
    record Unpaid(LocalDateTime deadline) implements OrderStatus {}
    record Paid(LocalDateTime paidAt, TransactionId txId) implements OrderStatus {}
    record Shipped(LocalDateTime shippedAt, TrackingNumber tracking) implements OrderStatus {}
    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
    record Cancelled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
}
// permits 불필요: 중첩 record가 자동으로 permitted subtype
```

### non-sealed 키워드

sealed 계층에서 특정 하위 타입의 확장을 허용하려면 `non-sealed`를 사용한다.

```java
public sealed interface Animal permits Dog, Cat, Bird {}
public record Dog(String name) implements Animal {}
public record Cat(String name) implements Animal {}
// Bird는 더 확장 가능
public non-sealed interface Bird extends Animal {}
public record Parrot(String name, String color) implements Bird {}
public record Eagle(String name) implements Bird {}
```

### 망라적 switch (default 불필요)

sealed interface의 모든 permitted subtype을 처리하면 `default`가 필요 없다. 새 subtype 추가 시 미처리 switch에서 컴파일 에러가 발생한다.

```java
double area(Shape shape) {
    return switch (shape) {
        case Circle(var r) -> Math.PI * r * r;
        case Rectangle(var w, var h) -> w * h;
        case Triangle(var b, var h) -> 0.5 * b * h;
        // default 불필요! 새 Shape 추가 시 컴파일 에러로 누락 방지
    };
}
```

### sealed interface에서 default 사용은 안티패턴

```java
// [X] default가 새 case 추가 시 누락을 숨김
return switch (status) {
    case Active a -> "활성";
    default -> "비활성"; // 새 상태 추가해도 여기서 잡힘!
};

// [O] 모든 case를 명시적으로 처리
return switch (status) {
    case Active a -> "활성";
    case Inactive i -> "비활성";
    case Suspended s -> "정지"; // 새 상태 추가 시 컴파일러가 강제
};
```

---

## 3. Pattern Matching (Java 21+)

### Switch Expression (Java 14+)

값을 반환하는 switch. 표현식이므로 변수에 대입하거나 return할 수 있다.

```java
String label = switch (day) {
    case MONDAY, TUESDAY -> "근무일";
    case SATURDAY, SUNDAY -> "주말";
    default -> "기타";
};
```

### Pattern Matching for switch (Java 21+ JEP 441)

switch에서 타입 패턴, 가드 조건을 사용할 수 있다.

```java
String describe(Object obj) {
    return switch (obj) {
        case Integer i when i > 0 -> "양수 정수: " + i;
        case Integer i -> "정수: " + i;
        case String s when s.isEmpty() -> "빈 문자열";
        case String s -> "문자열: " + s;
        case null -> "널";
        default -> "기타: " + obj.getClass();
    };
}
```

### Record Patterns (Java 21+ JEP 440)

Record의 필드를 분해하여 변수에 바인딩한다.

```java
sealed interface Shape permits Circle, Rectangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}

String describe(Shape shape) {
    return switch (shape) {
        case Circle(var r) -> "반지름 " + r + "인 원";
        case Rectangle(var w, var h) -> w + " x " + h + " 사각형";
    };
}
```

### 중첩 Record Pattern

Record 안의 Record도 분해 가능하다.

```java
record Point(int x, int y) {}
record Line(Point start, Point end) {}

String describe(Line line) {
    return switch (line) {
        case Line(Point(var x1, var y1), Point(var x2, var y2)) ->
            "(%d,%d) -> (%d,%d)".formatted(x1, y1, x2, y2);
    };
}
```

### Guard (when 절)

패턴 매칭 후 추가 조건을 `when`으로 지정한다. 순서가 중요하다: 구체적인 가드가 먼저 와야 한다.

```java
String classify(OrderStatus status) {
    return switch (status) {
        case Paid p when p.paidAt().plusHours(24).isAfter(LocalDateTime.now()) -> "취소 가능";
        case Paid p -> "취소 불가 (24시간 초과)";
        case Unpaid u when u.deadline().isBefore(LocalDateTime.now()) -> "기한 만료";
        case Unpaid u -> "결제 대기";
        case Shipped s -> "배송 중";
        case Delivered d -> "배송 완료";
        case Cancelled c -> "취소됨";
    };
}
```

### Unnamed Patterns (Java 22+ JEP 456)

사용하지 않는 변수를 `_`로 표기하여 의도를 명확히 한다.

```java
// 특정 필드만 관심 있을 때
String getPaymentId(OrderStatus status) {
    return switch (status) {
        case Paid(_, var txId) -> txId.value();   // paidAt은 무시
        case Unpaid _ -> "N/A";                    // 전체 변수 무시
        case Shipped _, Delivered _, Cancelled _ -> "N/A";
    };
}
```

---

## 4. Text Blocks (Java 15+)

여러 줄 문자열을 가독성 좋게 작성한다.

```java
String json = """
    {
        "type": "and",
        "left": {"type": "equals", "attribute": "country", "value": "KR"},
        "right": {"type": "gte", "attribute": "totalSpend", "threshold": 500000}
    }
    """;

// SQL 쿼리
String sql = """
    SELECT o.id, o.status, o.total_amount
    FROM orders o
    WHERE o.customer_id = ?
      AND o.status = 'PAID'
    ORDER BY o.created_at DESC
    """;
```

### String Templates (Preview)

String Templates는 Java 21에서 Preview로 도입되었으나, Java 25 정식 기능 포함 여부는 JEP 진행 상황에 따라 다르다. 현재는 `String.formatted()` 또는 `+` 연결을 사용한다.

```java
// String.formatted() 활용 (Java 15+)
String msg = "주문 %s, 금액 %s원".formatted(orderId.value(), total.amount());

// 또는 전통적 연결
String msg = "주문 " + orderId.value() + ", 금액 " + total.amount() + "원";
```

---

## 5. Sequenced Collections (Java 21+ JEP 431)

컬렉션에 순서 기반 접근을 위한 메서드를 추가한다.

```java
List<String> list = List.of("A", "B", "C", "D");

// 첫/마지막 원소 접근
String first = list.getFirst();    // "A"
String last = list.getLast();      // "D"

// 역순 뷰
List<String> reversed = list.reversed();  // ["D", "C", "B", "A"]

// SequencedMap
SequencedMap<String, Integer> map = new LinkedHashMap<>();
map.put("one", 1);
map.put("two", 2);
map.put("three", 3);

Map.Entry<String, Integer> firstEntry = map.firstEntry();  // "one"=1
Map.Entry<String, Integer> lastEntry = map.lastEntry();    // "three"=3

// SequencedSet
SequencedSet<String> set = new LinkedHashSet<>(List.of("A", "B", "C"));
String firstEl = set.getFirst();    // "A"
String lastEl = set.getLast();      // "C"
```

---

## 6. Virtual Threads (Java 21+ JEP 444)

OS 스레드와 1:1이 아닌, JVM이 관리하는 경량 스레드이다. 수백만 개의 동시 작업을 저비용으로 처리할 수 있다.

```java
// Virtual Thread 생성
Thread vt = Thread.ofVirtual().start(() -> {
    // 블로킹 I/O도 가상 스레드에서는 OS 스레드를 차단하지 않음
    var result = httpClient.send(request);
    process(result);
});

// ExecutorService와 함께 사용
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Result>> futures = orders.stream()
        .map(order -> executor.submit(() -> processOrder(order)))
        .toList();
    // 수만 건의 주문을 동시 처리 가능
}

// Spring Boot에서 활용 (application.yml)
// spring.threads.virtual.enabled=true
```

### DOP에서의 의미

- 불변 데이터 + 순수 함수는 스레드 안전하므로, Virtual Thread와 자연스럽게 결합
- 동기화(synchronized, Lock) 없이 병렬 처리 가능
- 결합법칙을 만족하는 연산은 Virtual Thread에서도 안전

---

## 7. 기타 유용한 기능

### 불변 컬렉션 (Java 9+)

```java
// 불변 리스트/셋/맵 생성
List<String> list = List.of("a", "b", "c");       // 수정 불가
Set<String> set = Set.of("a", "b", "c");           // 수정 불가
Map<String, Integer> map = Map.of("a", 1, "b", 2); // 수정 불가

// 기존 컬렉션을 불변으로 복사
List<String> immutable = List.copyOf(mutableList); // 깊은 복사 아님, 참조 복사
```

### Optional (Java 8+)

```java
Optional<User> user = userRepository.findById(id);

// map: 값 변환
Optional<String> email = user.map(User::email);

// flatMap: Optional을 반환하는 함수와 체이닝
Optional<Order> order = user.flatMap(u -> orderRepository.findLatest(u.id()));

// orElse / orElseThrow
String name = user.map(User::name).orElse("익명");
User found = user.orElseThrow(() -> new NotFoundException("회원 없음"));
```

### var (Java 10+)

지역 변수 타입 추론. 타입이 명확하거나 너무 길 때 유용하다.

```java
var items = List.of(new OrderItem(id, qty, price)); // List<OrderItem>
var result = OrderDomainService.validate(cmd, member, inventory); // Result<ValidatedOrder, OrderError>
var customer = new Customer("KR", "VIP", 1_500_000); // Customer
```

### Scoped Values (Java 25+ JEP 506)

ThreadLocal의 불변 대안. 스코프 내에서만 유효한 값을 전달한다.

```java
static final ScopedValue<RequestContext> CONTEXT = ScopedValue.newInstance();

void handleRequest(RequestContext ctx) {
    ScopedValue.runWhere(CONTEXT, ctx, () -> {
        // 이 스코프 내에서 CONTEXT.get()으로 접근
        processBusinessLogic();
    });
}

void processBusinessLogic() {
    RequestContext ctx = CONTEXT.get(); // 현재 스코프의 값
    // DOP에서: 불변 컨텍스트를 파라미터 없이 전달
}
```

---

## 8. JEP 468 Status (NOT in Java 25)

### Derived Record Creation (with 구문)

JEP 468은 Record의 필드를 간결하게 변경하는 `with` 구문을 제안했으나, **Java 25에 포함되지 않았다**.

```java
// [X] JEP 468 (미포함): 사용 불가
// Order newOrder = oldOrder with { status = new Paid(...); };

// [O] 수동 Wither 메서드 사용 (현재 유일한 방법)
public record Order(OrderId id, OrderStatus status, Money total) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, newStatus, this.total);
    }
}
Order newOrder = oldOrder.withStatus(new Paid(LocalDateTime.now(), txId));
```

### 대안 전략

필드가 많은 Record에서 Wither를 여러 개 작성하는 것이 번거로우면:

```java
// 방법 1: 필드별 Wither 메서드 (권장)
public record Member(MemberId id, String name, MemberGrade grade, Points points) {
    public Member withGrade(MemberGrade g) { return new Member(id, name, g, points); }
    public Member withPoints(Points p) { return new Member(id, name, grade, p); }
}

// 방법 2: Lombok @With (외부 라이브러리 허용 시)
// @With를 사용하면 자동 생성되지만, 순수 Java 25에서는 수동 구현 필요

// 방법 3: 빌더 스타일 (필드가 매우 많을 때)
public record Config(String host, int port, Duration timeout, boolean ssl) {
    public static Builder from(Config c) { return new Builder(c); }
    public static class Builder {
        private String host; private int port; private Duration timeout; private boolean ssl;
        Builder(Config c) { this.host=c.host; this.port=c.port; this.timeout=c.timeout; this.ssl=c.ssl; }
        public Builder host(String h) { this.host=h; return this; }
        public Builder port(int p) { this.port=p; return this; }
        public Config build() { return new Config(host, port, timeout, ssl); }
    }
}
```

---

## 9. Java 버전별 기능 요약표

| 기능 | 도입 버전 | 상태 (Java 25 기준) | DOP 관련성 |
|------|----------|-------|-----------|
| Record | Java 16 | 정식 | 핵심: 불변 데이터 표현 |
| Sealed Interface | Java 17 | 정식 | 핵심: Sum Type, 망라성 |
| Pattern Matching (instanceof) | Java 16 | 정식 | 타입 안전 분기 |
| Pattern Matching (switch) | Java 21 | 정식 | 핵심: 망라적 switch |
| Record Patterns | Java 21 | 정식 | Record 필드 분해 |
| Text Blocks | Java 15 | 정식 | 가독성 향상 |
| Sequenced Collections | Java 21 | 정식 | getFirst/getLast |
| Unnamed Patterns (_) | Java 22 | 정식 | 불필요한 변수 생략 |
| Virtual Threads | Java 21 | 정식 | 경량 동시성 |
| Scoped Values | Java 25 | 정식 | 불변 컨텍스트 전달 |
| String Templates | - | Preview/미확정 | 문자열 보간 |
| JEP 468 (with 구문) | - | 미포함 | Wither 수동 구현 필요 |
| Primitive Patterns | Java 25 | Preview | 원시 타입 패턴 매칭 |

---

## 10. DOP에서의 Java 25 활용 요약

```java
// 1. Record = 불변 데이터 (Product Type)
public record Money(BigDecimal amount, Currency currency) {}

// 2. Sealed Interface = Sum Type
public sealed interface OrderStatus permits Unpaid, Paid, Shipped {}

// 3. Pattern Matching = 타입 안전 분기
String msg = switch (status) {
    case Paid(var at, var tx) when at.plusDays(1).isAfter(now) -> "취소 가능";
    case Paid p -> "취소 불가";
    case Unpaid _ -> "결제 대기";
    case Shipped _ -> "배송 중";
};

// 4. 불변 컬렉션 = 방어적 복사
public record Cart(List<CartItem> items) {
    public Cart { items = List.copyOf(items); }
}

// 5. Wither = 불변 업데이트 (JEP 468 미포함)
public Order withStatus(OrderStatus s) { return new Order(id, s, total); }

// 6. Result = 실패 명시 (sealed interface)
public sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}
}
```
