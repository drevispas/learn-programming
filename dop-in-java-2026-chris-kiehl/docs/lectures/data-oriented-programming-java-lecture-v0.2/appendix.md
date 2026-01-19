# Appendix

---

## Appendix A: 흔한 실수와 안티패턴 모음 (강화)

### A.1 Record에 가변 컬렉션 저장 (방어적 복사 누락)

**Code A.1**: Record에 가변 컬렉션 저장 - 안티패턴과 올바른 방법
```java
// ❌ 안티패턴
public record Cart(List<CartItem> items) {}
List<CartItem> mutableList = new ArrayList<>();
Cart cart = new Cart(mutableList);
mutableList.add(newItem);  // cart 내부가 변경됨!

// ✅ 올바른 방법
public record Cart(List<CartItem> items) {
    public Cart { items = List.copyOf(items); }
}
```

### A.2 sealed interface에 default 사용

**Code A.2**: sealed interface에 default 사용 - 안티패턴과 올바른 방법
```java
// ❌ 새 상태 추가 시 누락 감지 안 됨
return switch (status) {
    case Active a -> "활성";
    default -> "비활성";
};

// ✅ 모든 case 명시
return switch (status) {
    case Active a -> "활성";
    case Inactive i -> "비활성";
};
```

### A.3 Entity에 비즈니스 로직 추가 (Active Record)

**Code A.3**: Entity에 비즈니스 로직 추가 - 안티패턴과 올바른 방법
```java
// ❌ DOP 철학 위반
public record User(String id, String name) {
    public void saveToDb() { Database.save(this); }
}

// ✅ 데이터와 로직 분리
public record User(String id, String name) {}
public class UserRepository { void save(User u) {...} }
```

### A.4 Rule Engine에서 값과 속성 혼동

**Code A.4**: Rule Engine에서 값과 속성 혼동 - 안티패턴과 올바른 방법
```java
// ❌ 규칙 생성 시점에 결과 확정
record GTE(int left, int right) implements Rule {}

// ✅ 평가 시점에 데이터 참조
record GTE(String attribute, int threshold) implements Rule {}
```

### A.5 Optional을 필드로 사용

**Code A.5**: Optional을 필드로 사용 - 안티패턴과 올바른 방법
```java
// ❌ 안티패턴: Record 필드에 Optional 사용
public record Order(
    OrderId id,
    Optional<CouponCode> couponCode  // 직렬화 문제, null 문제
) {}

// ✅ 올바른 방법: 합 타입 사용
sealed interface AppliedCoupon {
    record None() implements AppliedCoupon {}
    record Applied(CouponCode code, Money discount) implements AppliedCoupon {}
}

public record Order(OrderId id, AppliedCoupon coupon) {}
```

---

## Appendix B: DOP Java 치트시트

### B.1 Record 기본 문법 (Java 16+)

**Code B.1**: Record 기본 문법
```java
// 기본 Record (Java 16+)
public record Point(int x, int y) {}

// Compact Constructor (검증)
public record Money(BigDecimal amount) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("음수 금액");
        }
    }
}

// 팩토리 메서드
public record UserId(long value) {
    public static UserId of(long value) {
        return new UserId(value);
    }
}

// with 패턴 (수동 구현 - JEP 468 미포함으로 필수)
public record Order(OrderId id, OrderStatus status) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.id, newStatus);
    }
}
```

### B.2 Sealed Interface 문법 (Java 17+)

**Code B.2**: Sealed Interface 문법
```java
// 기본 Sealed Interface
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height) implements Shape {}

// 중첩 정의
sealed interface PaymentStatus {
    record Pending() implements PaymentStatus {}
    record Completed(String transactionId) implements PaymentStatus {}
    record Failed(String reason) implements PaymentStatus {}
}
```

### B.3 Pattern Matching (Java 21+)

**Code B.3**: Pattern Matching
```java
// Record Patterns in switch (Java 21+ JEP 440, 441)
String describe(Shape shape) {
    return switch (shape) {
        case Circle(var r) -> "원 (반지름: " + r + ")";
        case Rectangle(var w, var h) -> "사각형 (" + w + "x" + h + ")";
        case Triangle(var b, var h) -> "삼각형 (밑변: " + b + ")";
    };
}

// 가드 조건 (Java 21+)
String categorize(Shape shape) {
    return switch (shape) {
        case Circle(var r) when r > 10 -> "큰 원";
        case Circle(var r) -> "작은 원";
        case Rectangle r -> "사각형";
        case Triangle t -> "삼각형";
    };
}

// Unnamed Variables (Java 22+ JEP 456) - 사용하지 않는 변수에 _ 사용
String getPaymentId(OrderStatus status) {
    return switch (status) {
        case Paid(_, var paymentId) -> paymentId;  // 첫 번째 필드는 무시
        case Unpaid _ -> "N/A";  // 전체 변수를 무시
        case Shipping _, Delivered _, Cancelled _ -> "N/A";
    };
}
```

### B.4 Result 타입 활용

**Code B.4**: Result 타입 활용
```java
// Result 정의
sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}
}

// 사용
Result<User, String> result = findUser(id);

// 처리
String message = switch (result) {
    case Success(var user) -> "찾음: " + user.name();
    case Failure(var error) -> "에러: " + error;
};

// map/flatMap
result
    .map(user -> user.email())
    .flatMap(email -> sendEmail(email));
```

### B.5 불변 컬렉션 (Java 9+)

**Code B.5**: 불변 컬렉션
```java
// 불변 리스트 생성 (Java 9+)
List<String> immutable = List.of("a", "b", "c");

// 기존 리스트를 불변으로 복사 (Java 10+)
List<String> copied = List.copyOf(mutableList);

// 불변 맵 (Java 9+)
Map<String, Integer> map = Map.of("a", 1, "b", 2);

// Sequenced Collections (Java 21+ JEP 431)
List<String> list = List.of("a", "b", "c");
String first = list.getFirst();  // "a"
String last = list.getLast();    // "c"
List<String> reversed = list.reversed();  // ["c", "b", "a"]
```

### B.6 방어적 복사 패턴

**Code B.6**: 방어적 복사 패턴
```java
// Record에서 불변성 보장
public record Order(OrderId id, List<OrderItem> items) {
    public Order {
        Objects.requireNonNull(id);
        Objects.requireNonNull(items);
        items = List.copyOf(items);  // 방어적 복사
    }
}
```

### B.7 Java 25 DOP 기능

**Code B.7**: Java 25 DOP 기능
```java
// Primitive Types in Patterns (Java 25 Preview - JEP 507)
// --enable-preview 플래그 필요
int categorize(Object obj) {
    return switch (obj) {
        case Integer i when i > 0 -> 1;
        case Integer i when i < 0 -> -1;
        case Integer _ -> 0;
        default -> throw new IllegalArgumentException();
    };
}

// Scoped Values (Java 25+ JEP 506) - 불변 컨텍스트 전달
// ThreadLocal 대신 불변 값 전달에 적합
static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

void processRequest(User user) {
    ScopedValue.runWhere(CURRENT_USER, user, () -> {
        // 이 스코프 내에서 CURRENT_USER.get()으로 접근 가능
        handleRequest();
    });
}
```

> ⚠️ **JEP 468 (Derived Record Creation) 미포함 안내**:
> `with` expression (`record with { field = value; }`)은 Java 25에 **포함되지 않았습니다**.
> Record 필드 변경 시 수동 `withXxx()` 메서드 또는 생성자를 사용하세요.

---

## Appendix C: 전체 정답 및 해설 (강화)

**Table C.1**: 전체 정답표

| Ch | Q1 | Q2 | Q3 | Q4 | Q5 |
|----|----|----|----|----|-----|
| 1  | C  | B  | B  | C  | 코드 |
| 2  | B  | C  | B  | B  | 코드 |
| 3  | B  | C  | A  | C  | 코드 |
| 4  | B  | B  | A  | A  | 코드 |
| 5  | B  | A  | A  | B  | 코드 |
| 6  | A  | C  | B  | B  | 코드 |
| 7  | C  | C  | B  | B  | 코드 |
| 8  | B  | A  | A  | B  | 코드 |
| 9  | B  | B  | C  | B  | 코드 |

### 주요 해설

**Ch 4 (불가능한 상태)**
- Q4.1: B - 유효하지 않은 상태를 타입으로 표현할 수 없게 설계
- Q4.2: B - isDelivered가 false인데 다른 필드가 있을 수 있는 불일치 상태
- Q4.3: A - default가 Shipped 처리를 가로채서 망라성 검증 무효화
- Q4.4: A - Unused와 Used로 분리하면 불일치 상태 불가능
- Q4.5: 코드 - sealed interface로 각 상태별 필요한 데이터만 포함

**Ch 5 (전체 함수)**
- Q5.1: B - 전체 함수는 모든 입력에 대해 정의된 결과를 반환
- Q5.2: A - `divide`만 부분 함수 (0으로 나눌 때 예외)
- Q5.3: A - 10 * 2 = 20, 20 > 15이므로 Success(20), getOrElse 결과는 20
- Q5.4: B - sealed interface로 NotFound, AlreadyUsed, Expired, MinOrderAmountNotMet 정의
- Q5.5: 코드 - Result<User, WithdrawError>를 반환하도록 변환

**Ch 6 (파이프라인)**
- Q6.1: A - 오직 A만 순수 함수 (부수효과 없음)
- Q6.2: C - Meat 레이어는 순수한 비즈니스 로직만 포함
- Q6.3: B - productRepository.find()와 taxService.getCurrentRate()가 I/O
- Q6.4: B - Product, TaxRate를 파라미터로 받는 순수 함수로 분리
- Q6.5: 코드 - applyPromotionDiscount(Order, Promotion) 순수 함수 추출

**Ch 7 (대수적 속성)**
- Q7.1: C - 문자열 연결은 결합법칙 만족 (a + (b + c) == (a + b) + c)
- Q7.2: C - Math.abs는 멱등 (abs(abs(x)) == abs(x))
- Q7.3: B - 중복 호출 시 할인이 계속 적용됨 (90% → 81% → ...)
- Q7.4: B - 할인 적용 여부를 상태로 저장하고 확인
- Q7.5: 코드 - Money와 유사하게 zero() 항등원 구현

**Ch 8 (인터프리터)**
- Q8.1: B - 값 대신 속성 이름을 저장해야 다른 데이터에 재사용 가능
- Q8.2: A - And(Not(UserGrade(VIP)), MinTotal(30000))
- Q8.3: A - 왼쪽부터 깊이 우선 탐색 (VIP → MinTotal → And → FASHION → Or)
- Q8.4: B - Rule sealed interface에 새 record 추가 + 해석기에 case 추가
- Q8.5: 코드 - Or(NewMember, And(MinTotal(100000), ProductCategory(ELECTRONICS)))

**Ch 9 (JPA/Spring)**
- Q9.1: B - Entity는 가변/Identity, Record는 불변/Value
- Q9.2: B - Mapper는 Entity ↔ Domain Record 변환 담당
- Q9.3: C - 주문 금액 계산은 Domain Layer의 순수 함수
- Q9.4: B - 가장 복잡한 비즈니스 로직을 순수 함수로 추출하여 시작
- Q9.5: 코드 - switch문으로 ProductStatusEnum → ProductStatus 변환

### 함정 문제 해설

**Q1.6 (DOP 철학)**: **C** - Active Record 패턴은 데이터에 DB 의존성과 부수 효과를 섞어 DOP 철학을 정면 위반

**Q2.3 (불변성의 함정)**: **B** `[Alice, Bob]` - Record 필드가 가변 List를 참조하면 외부에서 변경 가능. 방어적 복사 필요

**Q3.2 (봉인된 운명)**: **B** 컴파일 에러 - Sealed Interface는 모든 케이스 처리를 강제

**Q8.1 (GTE 설계 실수)**: **B** - 값 대신 속성 이름을 저장해야 다른 데이터에 재사용 가능

---

## Appendix D: Final Boss Quiz - DOP 마스터 검증 (신규)

### 문제 1: 불변성의 함정

**Code D.1**: Final Boss 문제 1 - 불변성의 함정
```java
public record Team(String name, List<String> members) {}

List<String> list = new ArrayList<>();
list.add("Alice");
Team team = new Team("Alpha", list);
list.add("Bob");
System.out.println(team.members());
```

**정답**: `[Alice, Bob]` - 방어적 복사 누락으로 불변성 파괴

---

### 문제 2: 봉인된 운명

sealed interface에 새 케이스 추가 후 switch 미수정 시?

**정답**: B (컴파일 에러) - Sealed Interface의 핵심 장점

---

### 문제 3: DOP 아키텍처

DOP 철학에 가장 어긋나는 코드?

**정답**: C (Active Record 패턴) - 데이터에 부수 효과를 섞으면 안 됨

---

**[강의 마무리]**

DOP 핵심 정리:
1. **데이터와 로직을 분리** (Record + Static 함수)
2. **불변성 유지** (방어적 복사, with 패턴)
3. **합 타입으로 상태 축소** (Sealed Interface)
4. **전체 함수로 실패 명시** (Result 타입)
5. **Rule as Data로 유연성 확보** (인터프리터 패턴)
